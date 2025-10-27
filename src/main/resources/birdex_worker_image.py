# -*- coding: utf-8 -*-
import sys, os, json, struct, logging, contextlib, warnings
from io import BytesIO
from PIL import Image, ImageFile

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
YOLO_PATH    = os.path.join(BASE_DIR, "yolov8n.pt")  # más rápido que 'm'

YOLO_IMGSZ   = int(os.getenv("YOLO_IMGSZ", "384"))   # ajustable por HW
YOLO_CONF    = float(os.getenv("YOLO_CONF", "0.20"))
THRESHOLD    = float(os.getenv("CLS_THRESHOLD", "0.95"))  # umbral de etiqueta

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
    ImageFile.LOAD_TRUNCATED_IMAGES = True
    return Image.open(BytesIO(img_bytes)).convert("RGB")

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
    transforms.Resize((224, 224)),
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
    classes = ["Ave no identificada"]

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

    # Pipeline: YOLO (recorte) -> clasificador
    try:
        crop = None
        if yolo is not None:
            # Inferencia YOLO con cualquier salida redirigida a stderr por seguridad
            with to_stderr():
                results = yolo(image, imgsz=YOLO_IMGSZ, conf=YOLO_CONF, verbose=False)
            for r in results:
                for box in r.boxes:
                    try:
                        cls_id = int(box.cls[0])
                        cls_name = yolo.names[cls_id]
                    except Exception:
                        cls_name = ""
                    if cls_name.lower() == "bird":
                        x1, y1, x2, y2 = map(int, box.xyxy[0].tolist())
                        crop = image.crop((x1, y1, x2, y2))
                        break
                if crop is not None:
                    break

        if crop is None:
            # Fallback: clasificar imagen completa
            crop = image

        if clf is None:
            out = err("Clasificador no disponible")
            sys.stdout.write(out + "\n"); sys.stdout.flush()
            continue

        with torch.no_grad():
            inp = transform(crop).unsqueeze(0).to(DEVICE)
            logits = clf(inp)
            probs = torch.softmax(logits, dim=1).squeeze()
            idx = int(torch.argmax(probs).item())
            conf = float(probs[idx].item())

        label = classes[idx] if conf >= THRESHOLD else "Ave no identificada"
        out = {"label": label, "trustLevel": conf}
        # ÚNICA salida por stdout (1 línea JSON)
        sys.stdout.write(json.dumps(out, ensure_ascii=False) + "\n")
        sys.stdout.flush()

    except Exception as e:
        out = err(f"Fallo de inferencia: {e}")
        sys.stdout.write(out + "\n"); sys.stdout.flush()
