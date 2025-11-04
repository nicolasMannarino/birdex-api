# birdex_worker_video_multipart.py
# -*- coding: utf-8 -*-
import sys, os, json, struct, argparse, tempfile, logging, contextlib, warnings, re
from PIL import Image
import cv2
import torch
import torch.nn as nn
import torchvision.transforms as transforms
import torchvision.models as models
from torchvision.models import ResNet18_Weights
from ultralytics import YOLO
from ultralytics.utils import LOGGER

ap = argparse.ArgumentParser()
ap.add_argument("--fps", type=int, default=1)
ap.add_argument("--stop", type=int, default=0)
args = ap.parse_args()
STOP_ON_FIRST = (args.stop == 1)
TARGET_FPS = max(1, args.fps)

BASE_DIR     = os.path.dirname(__file__)
MODEL_PATH   = os.path.join(BASE_DIR, "modelo_imagenes_birdex.pth")
CLASSES_PATH = os.path.join(BASE_DIR, "birdex_clases.json")
YOLO_PATH    = os.path.join(BASE_DIR, "yolov8m.pt")

YOLO_IMGSZ   = int(os.getenv("YOLO_IMGSZ", "640"))
YOLO_CONF    = float(os.getenv("YOLO_CONF", "0.20"))
THRESHOLD    = float(os.getenv("CLS_THRESHOLD", "0.95"))
MAX_SECONDS  = int(os.getenv("MAX_VIDEO_SECONDS", "15"))

os.environ["PYTHONWARNINGS"] = "ignore"
os.environ["KMP_WARNINGS"] = "0"
os.environ["OPENCV_LOG_LEVEL"] = os.environ.get("OPENCV_LOG_LEVEL", "SILENT")
warnings.filterwarnings("ignore")
LOGGER.setLevel(logging.ERROR)

try:
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    sys.stderr.reconfigure(encoding='utf-8', errors='replace')
except Exception:
    pass

@contextlib.contextmanager
def to_stderr():
    with contextlib.redirect_stdout(sys.stderr):
        yield

def read_exact(n: int) -> bytes:
    data = b''
    while len(data) < n:
        chunk = sys.stdin.buffer.read(n - len(data))
        if not chunk:
            raise RuntimeError("stdin cerrado")
        data += chunk
    return data

def err_dict(msg):
    sys.stderr.write(f"[ERROR VID MULTIPART] {msg}\n"); sys.stderr.flush()
    return {"label": "Desconocida", "trustLevel": 0.0, "error": msg}

def pick_suffix_from_bytes(b: bytes) -> str:
    try:
        if len(b) >= 12 and b[4:8] == b"ftyp":
            return ".mp4"
        if b[:4] == b"\x1A\x45\xDF\xA3":
            return ".webm"
        if b[:4] == b"RIFF" and b[8:12] == b"AVI ":
            return ".avi"
    except Exception:
        pass
    return ".mp4"

def load_classes(path):
    with open(path, "r", encoding="utf-8") as f:
        class_to_idx = json.load(f)
    idx_to_class = {v: k for k, v in class_to_idx.items()}
    return [idx_to_class[i] for i in range(len(idx_to_class))]

def load_classifier(model_path, num_classes, device):
    model = models.resnet18(weights=ResNet18_Weights.IMAGENET1K_V1)
    model.fc = nn.Linear(model.fc.in_features, num_classes)
    state = torch.load(model_path, map_location=device)
    model.load_state_dict(state)
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

DEVICE = torch.device("cuda" if torch.cuda.is_available() else "cpu")

try:
    classes = load_classes(CLASSES_PATH)
except Exception as e:
    sys.stderr.write(f"[ERROR VID MULTIPART] No se pudieron cargar clases: {e}\n"); sys.stderr.flush()
    classes = ["Desconocida"]

try:
    clf = load_classifier(MODEL_PATH, len(classes), DEVICE)
except Exception as e:
    sys.stderr.write(f"[ERROR VID MULTIPART] No se pudo cargar el clasificador: {e}\n"); sys.stderr.flush()
    clf = None

try:
    with to_stderr():
        yolo = YOLO(YOLO_PATH)
        if DEVICE.type == "cuda":
            yolo.to("cuda")
        yolo.fuse()
except Exception as e:
    sys.stderr.write(f"[ERROR VID MULTIPART] No se pudo cargar YOLO: {e}\n"); sys.stderr.flush()
    yolo = None

def classify_frame(bgr):
    import cv2
    from PIL import Image

    # Si no hay YOLO cargado, no clasifiques: evitamos falsos positivos
    if yolo is None or clf is None:
        return "Desconocida", 0.0, False

    rgb = cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB)
    pil_full = Image.fromarray(rgb)

    found_bird = False
    pil_crop = None

    # Detectar "bird" con YOLO
    with to_stderr():
        results = yolo(pil_full, imgsz=YOLO_IMGSZ, conf=YOLO_CONF, verbose=False)

    for r in results:
        for box in r.boxes:
            try:
                cls_id = int(box.cls[0]); cls_name = yolo.names[cls_id].lower()
            except Exception:
                cls_name = ""
            if cls_name == "bird":
                x1, y1, x2, y2 = map(int, box.xyxy[0].tolist())
                # sanity check por si el bbox vino roto
                if x2 > x1 and y2 > y1:
                    pil_crop = pil_full.crop((x1, y1, x2, y2))
                    found_bird = True
                    break
        if found_bird:
            break

    # Si YOLO no encontró "bird", devolvemos desconocida
    if not found_bird:
        return "Desconocida", 0.0, False

    # Clasificar únicamente el recorte con pájaro
    with torch.no_grad():
        inp = transform(pil_crop).unsqueeze(0).to(DEVICE)
        logits = clf(inp)
        probs = torch.softmax(logits, dim=1).squeeze()
        idx = int(torch.argmax(probs).item())
        conf = float(probs[idx].item())

    label = classes[idx] if conf >= THRESHOLD else "Desconocida"
    return label, conf, True

while True:
    try:
        header = read_exact(4)
        L = struct.unpack(">I", header)[0]
        vid_bytes = read_exact(L)
    except Exception as e:
        sys.stderr.write(f"[ERROR VID MULTIPART] Lectura stdin: {e}\n"); sys.stderr.flush()
        break

    try:
        suffix = pick_suffix_from_bytes(vid_bytes)
        tmp = tempfile.NamedTemporaryFile(delete=False, suffix=suffix)
        tmp.write(vid_bytes); tmp.flush(); tmp.close()
        path = tmp.name
    except Exception as e:
        out = err_dict(f"No se pudo materializar el video: {e}")
        sys.stdout.write(json.dumps(out, ensure_ascii=False) + "\n"); sys.stdout.flush()
        continue

    best_label, best_conf = "Desconocida", 0.0
    any_bird_detected = False

    try:
        cap = cv2.VideoCapture(path)
        if not cap.isOpened():
            raise RuntimeError("OpenCV no pudo abrir el video")

        native_fps = cap.get(cv2.CAP_PROP_FPS) or 25.0
        if native_fps <= 1e-3:
            native_fps = 25.0

        stride = max(1, int(round(native_fps / TARGET_FPS)))
        max_frames = int(MAX_SECONDS * native_fps)

        idx = 0
        ok, frame = cap.read()
        while ok:
            if idx > max_frames:
                break
            if idx % stride == 0:
                label, conf, found_bird = classify_frame(frame)
                if found_bird:
                    any_bird_detected = True
                    if conf > best_conf:
                        best_label, best_conf = label, float(conf)
                    # Solo cortamos temprano si hay pájaro y pasa el umbral
                    if STOP_ON_FIRST and best_label != "Desconocida" and best_conf >= THRESHOLD:
                        break
            idx += 1
            ok, frame = cap.read()

        cap.release()
    except Exception as e:
        out = err_dict(f"Fallo de procesamiento de video: {e}")
        sys.stdout.write(json.dumps(out, ensure_ascii=False) + "\n"); sys.stdout.flush()
        try: os.remove(path)
        except Exception: pass
        continue

    try:
        os.remove(path)
    except Exception:
        pass

    # Si nunca hubo detección de "bird", devolvemos desconocida 0.0
    if not any_bird_detected:
        best_label, best_conf = "Desconocida", 0.0

    out = {"label": best_label, "trustLevel": best_conf}
    sys.stdout.write(json.dumps(out, ensure_ascii=False) + "\n")
    sys.stdout.flush()