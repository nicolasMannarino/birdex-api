# -*- coding: utf-8 -*-
import sys, os, json, struct, logging, contextlib, warnings
from io import BytesIO
from PIL import Image, ImageFile, ImageOps

import torch
import torch.nn as nn
import torchvision.transforms as transforms
import torchvision.models as models
from torchvision.models import ResNet18_Weights

from ultralytics import YOLO
from ultralytics.utils import LOGGER

# ================== Config ==================
BASE_DIR     = os.path.dirname(__file__)
MODEL_PATH   = os.path.join(BASE_DIR, "modelo_imagenes_birdex.pth")
CLASSES_PATH = os.path.join(BASE_DIR, "birdex_clases.json")
YOLO_PATH    = os.path.join(BASE_DIR, "yolov8m.pt")  # alineado con el script de video

YOLO_IMGSZ   = int(os.getenv("YOLO_IMGSZ", "640"))   # ajustable por HW
YOLO_CONF    = float(os.getenv("YOLO_CONF", "0.20"))
THRESHOLD    = float(os.getenv("CLS_THRESHOLD", "0.95"))  # umbral de etiqueta
MAX_CANDIDATES = int(os.getenv("MAX_CANDIDATES", "5"))    # top-K recortes YOLO a evaluar
PAD_RATIO      = float(os.getenv("PAD_RATIO", "0.15"))    # padding relativo alrededor del bbox

# Opcional: limitar threads en CPU para mejor p99 con concurrencia.
if os.getenv("TORCH_NUM_THREADS"):
    try:
        torch.set_num_threads(int(os.getenv("TORCH_NUM_THREADS")))
    except Exception:
        pass

# ================== Salida segura ==================
try:
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    sys.stderr.reconfigure(encoding='utf-8', errors='replace')
except Exception:
    pass

warnings.filterwarnings("ignore")
os.environ["PYTHONWARNINGS"] = "ignore"
os.environ["KMP_WARNINGS"] = "0"

# Logger de Ultralytics al mínimo
LOGGER.setLevel(logging.ERROR)

# Context manager: cualquier print dentro se va a stderr (nunca stdout)
@contextlib.contextmanager
def to_stderr():
    with contextlib.redirect_stdout(sys.stderr):
        yield

# ================== Util ==================
def err(msg: str):
    """No derriba el worker; devuelve un JSON de error por stdout."""
    sys.stderr.write(f"[ERROR IMG] {msg}\n")
    sys.stderr.flush()
    return json.dumps({"label": "error", "trustLevel": 0.0, "error": msg}, ensure_ascii=False)

def read_exact(n: int) -> bytes:
    data = b''
    while len(data) < n:
        chunk = sys.stdin.buffer.read(n - len(data))
        if not chunk:
            raise RuntimeError("stdin cerrado")
        data += chunk
    return data

def open_image(img_bytes: bytes) -> Image.Image:
    """Abre y corrige orientación EXIF (muy común en móviles)."""
    ImageFile.LOAD_TRUNCATED_IMAGES = True
    im = Image.open(BytesIO(img_bytes)).convert("RGB")
    return ImageOps.exif_transpose(im)

def pad_box(x1, y1, x2, y2, w, h, pad_ratio=0.15):
    bw, bh = x2 - x1, y2 - y1
    px, py = int(bw * pad_ratio), int(bh * pad_ratio)
    nx1 = max(0, x1 - px)
    ny1 = max(0, y1 - py)
    nx2 = min(w, x2 + px)
    ny2 = min(h, y2 + py)
    # evita recortes degenerados
    if nx2 <= nx1 or ny2 <= ny1:
        nx1, ny1, nx2, ny2 = x1, y1, x2, y2
    return nx1, ny1, nx2, ny2

# ================== Clasificador ==================
def load_classes(path: str):
    with open(path, "r", encoding="utf-8") as f:
        class_to_idx = json.load(f)
    idx_to_class = {v: k for k, v in class_to_idx.items()}
    return [idx_to_class[i] for i in range(len(idx_to_class))]

def load_classifier(model_path: str, num_classes: int, device: torch.device):
    model = models.resnet18(weights=ResNet18_Weights.IMAGENET1K_V1)
    model.fc = nn.Linear(model.fc.in_features, num_classes)
    state = torch.load(model_path, map_location=device)
    model.load_state_dict(state)
    # Quantization dinámica en CPU (acelera lineares)
    if device.type == "cpu":
        try:
            import torch.quantization as tq
            model = tq.quantize_dynamic(model, {nn.Linear}, dtype=torch.qint8)
        except Exception:
            pass
    model = model.to(device)
    model.eval()
    return model

transform = transforms.Compose([
    transforms.Resize((224, 224)),  # mantener alineado a tu entrenamiento
    transforms.ToTensor(),
    transforms.Normalize(mean=[0.485, 0.456, 0.406],
                         std=[0.229, 0.224, 0.225])
])

# ================== Carga única ==================
DEVICE = torch.device("cuda" if torch.cuda.is_available() else "cpu")

try:
    classes = load_classes(CLASSES_PATH)
except Exception as e:
    sys.stderr.write(f"[ERROR IMG] No se pudieron cargar clases: {e}\n"); sys.stderr.flush()
    classes = ["Desconocida"]

try:
    clf = load_classifier(MODEL_PATH, len(classes), DEVICE)
except Exception as e:
    sys.stderr.write(f"[ERROR IMG] No se pudo cargar el clasificador: {e}\n"); sys.stderr.flush()
    clf = None

# Carga de YOLO con stdout redirigido a stderr para evitar “summary” en stdout
try:
    with to_stderr():
        yolo = YOLO(YOLO_PATH)
        if DEVICE.type == "cuda":
            yolo.to("cuda")
        yolo.fuse()
except Exception as e:
    sys.stderr.write(f"[ERROR IMG] No se pudo cargar YOLO: {e}\n"); sys.stderr.flush()
    yolo = None

def classify_pil(pil_img: Image.Image):
    """Devuelve (label, conf) para una sola imagen PIL."""
    if clf is None:
        return "Desconocida", 0.0
    with torch.no_grad():
        inp = transform(pil_img).unsqueeze(0).to(DEVICE)
        logits = clf(inp)
        probs = torch.softmax(logits, dim=1).squeeze()
        idx = int(torch.argmax(probs).item())
        conf = float(probs[idx].item())
        label = classes[idx]
    return label, conf

def classify_best(image_pil: Image.Image, max_candidates: int, pad_ratio: float):
    """
    Genera candidatos: imagen completa + top-K boxes 'bird' (ordenadas por conf*área) con padding.
    Devuelve la mejor predicción (label, conf).
    """
    W, H = image_pil.width, image_pil.height
    candidates = [("full", image_pil)]  # siempre evaluamos la imagen completa

    # Boxes YOLO
    if yolo is not None:
        with to_stderr():
            results = yolo(image_pil, imgsz=YOLO_IMGSZ, conf=YOLO_CONF, verbose=False)
        ranked = []
        for r in results:
            for b in r.boxes:
                try:
                    cls_id = int(b.cls[0]); cls_name = yolo.names[cls_id].lower()
                except Exception:
                    cls_name = ""
                if cls_name == "bird":
                    x1, y1, x2, y2 = map(int, b.xyxy[0].tolist())
                    conf = float(b.conf[0].item()) if hasattr(b, "conf") else 0.0
                    area = max(1, (x2 - x1) * (y2 - y1))
                    score = conf * area
                    ranked.append((score, (x1, y1, x2, y2)))
        ranked.sort(reverse=True, key=lambda t: t[0])
        for _, (x1, y1, x2, y2) in ranked[:max_candidates]:
            px1, py1, px2, py2 = pad_box(x1, y1, x2, y2, W, H, pad_ratio=pad_ratio)
            candidates.append(("yolo", image_pil.crop((px1, py1, px2, py2))))

    # Clasificar todos los candidatos y quedarnos con el de mayor probabilidad
    best_label, best_conf = "Desconocida", 0.0
    for kind, pil_img in candidates:
        label, conf = classify_pil(pil_img)
        if conf > best_conf:
            best_label, best_conf = label, conf
    return best_label, best_conf

# ================== Loop persistente ==================
while True:
    try:
        header = read_exact(4)
        L = struct.unpack(">I", header)[0]
        img_bytes = read_exact(L)
    except Exception as e:
        sys.stderr.write(f"[ERROR IMG] Lectura stdin: {e}\n"); sys.stderr.flush()
        break  # stdin cerrado: salimos

    # Abrir imagen
    try:
        image = open_image(img_bytes)
    except Exception as e:
        out = err(f"No se pudo abrir la imagen: {e}")
        sys.stdout.write(out + "\n"); sys.stdout.flush()
        continue

    # Pipeline: YOLO (múltiples recortes con padding) + imagen completa -> mejor score
    try:
        label, conf = classify_best(image, max_candidates=MAX_CANDIDATES, pad_ratio=PAD_RATIO)
        final_label = label if conf >= THRESHOLD else "Desconocida"
        out = {"label": final_label, "trustLevel": conf}
        # ÚNICA salida por stdout (1 línea JSON)
        sys.stdout.write(json.dumps(out, ensure_ascii=False) + "\n")
        sys.stdout.flush()
    except Exception as e:
        out = err(f"Fallo de inferencia: {e}")
        sys.stdout.write(out + "\n"); sys.stdout.flush()
