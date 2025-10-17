import sys, os, json, base64, binascii, re, tempfile
from io import BytesIO
from PIL import Image
import numpy as np
import cv2

import torch
import torch.nn as nn
import torchvision.transforms as transforms
import torchvision.models as models
from torchvision.models import ResNet18_Weights
from ultralytics import YOLO

# ---- Salida UTF-8 segura ----
try:
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    sys.stderr.reconfigure(encoding='utf-8', errors='replace')
except Exception:
    pass

# ---- CONFIGURACIÓN ----
DEVICE = torch.device("cuda" if torch.cuda.is_available() else "cpu")
THRESHOLD = float(os.getenv("BIRDEX_THRESHOLD", "0.95"))

BASE_DIR = os.path.dirname(__file__)
MODEL_PATH = os.path.join(BASE_DIR, "modelo_imagenes_birdex.pth")
CLASSES_PATH = os.path.join(BASE_DIR, "birdex_clases.json")
YOLO_MODEL_PATH = os.path.join(BASE_DIR, "yolov8m.pt")

# ---- TRANSFORMACIONES ----
transform = transforms.Compose([
    transforms.Resize((224, 224)),
    transforms.ToTensor(),
    transforms.Normalize(mean=[0.485, 0.456, 0.406],
                         std=[0.229, 0.224, 0.225])
])

# ---- FUNCIONES DE UTILIDAD ----
def err(msg, code=1):
    sys.stderr.write(f"[ERROR] {msg}\n")
    sys.exit(code)

def load_classes(path):
    try:
        with open(path, "r", encoding="utf-8") as f:
            class_to_idx = json.load(f)
        idx_to_class = {v: k for k, v in class_to_idx.items()}
        return [idx_to_class[i] for i in range(len(idx_to_class))]
    except Exception as e:
        err(f"No se pudieron cargar las clases desde {path}: {e}", 10)

def load_model(model_path, num_classes):
    try:
        model = models.resnet18(weights=ResNet18_Weights.IMAGENET1K_V1)
        model.fc = nn.Linear(model.fc.in_features, num_classes)
        state = torch.load(model_path, map_location=DEVICE)
        model.load_state_dict(state)
        model = model.to(DEVICE)
        model.eval()
        return model
    except Exception as e:
        err(f"No se pudo cargar el modelo desde {model_path}: {e}", 11)

def load_yolo_model(path):
    try:
        model = YOLO(path)
        return model
    except Exception as e:
        err(f"No se pudo cargar el modelo YOLO desde {path}: {e}", 12)

def read_json_stdin():
    raw = sys.stdin.read()
    if not raw or not raw.strip():
        err("No se recibió ningún dato por stdin.", 2)
    try:
        return json.loads(raw)
    except Exception as e:
        err(f"Entrada no es JSON válido: {e}", 3)

def extract_base64(s):
    if not s or not isinstance(s, str) or not s.strip():
        err("fileBase64 vacío o inválido", 4)
    s = s.strip()
    if s.lower().startswith("data:"):
        try:
            s = s.split(",", 1)[1]
        except Exception:
            err("Prefijo data URL mal formado", 4)
    s = re.sub(r"\s+", "", s)
    try:
        return base64.b64decode(s, validate=True)
    except binascii.Error as e:
        err(f"Cadena base64 inválida: {e}", 4)

def pick_suffix(b64_str: str, raw_bytes: bytes) -> str:
    m = re.match(r"^data:(?P<mime>[^;]+);base64,", b64_str or "", flags=re.I)
    if m:
        mime = m.group("mime").lower()
        if "quicktime" in mime:
            return ".mov"
        if "webm" in mime:
            return ".webm"
        if "x-matroska" in mime or "mkv" in mime:
            return ".mkv"
        return ".mp4"
    b = raw_bytes
    if len(b) >= 12 and b[4:8] == b"ftyp":
        return ".mp4"
    if b[:4] == b"\x1A\x45\xDF\xA3":
        return ".webm"
    if b[:4] == b"RIFF" and b[8:12] == b"AVI ":
        return ".avi"
    return ".mp4"

def frame_to_pil(frame_bgr):
    rgb = cv2.cvtColor(frame_bgr, cv2.COLOR_BGR2RGB)
    return Image.fromarray(rgb)

def pil_to_tensor(pil_img):
    return transform(pil_img).unsqueeze(0).to(DEVICE)

def infer_one(model, pil_img, classes):
    with torch.no_grad():
        inputs = pil_to_tensor(pil_img)
        outputs = model(inputs)
        probs = torch.softmax(outputs, dim=1).squeeze()
        class_index = int(torch.argmax(probs).item())
        confidence = float(probs[class_index].item())
    label = classes[class_index] if confidence >= THRESHOLD else "Ave no identificada"
    return label, confidence

# ---- NUEVO: detección previa con YOLO ----
def detect_bird_with_yolo(yolo_model, pil_img):
    results = yolo_model(pil_img, verbose=False)
    for result in results:
        for box in result.boxes:
            class_id = int(box.cls[0])
            class_name = yolo_model.names[class_id]
            if class_name.lower() == "bird":
                x1, y1, x2, y2 = map(int, box.xyxy[0].tolist())
                return pil_img.crop((x1, y1, x2, y2))
    return None

# ---- MAIN ----
def main():
    payload = read_json_stdin()
    b64_in = payload.get("fileBase64")
    sample_fps = int(payload.get("sampleFps", 1))
    stop_on_first = bool(payload.get("stopOnFirstAbove", False))
    if sample_fps <= 0:
        sample_fps = 1

    classes = load_classes(CLASSES_PATH)
    model = load_model(MODEL_PATH, len(classes))
    yolo_model = load_yolo_model(YOLO_MODEL_PATH)

    video_bytes = extract_base64(b64_in)
    suffix = pick_suffix(b64_in if isinstance(b64_in, str) else "", video_bytes)

    with tempfile.NamedTemporaryFile(suffix=suffix, delete=True) as tmp:
        tmp.write(video_bytes)
        tmp.flush()

        cap = cv2.VideoCapture(tmp.name)
        if not cap.isOpened():
            err("No se pudo abrir el video (posible códec no soportado).", 5)

        v_fps = cap.get(cv2.CAP_PROP_FPS)
        if not v_fps or v_fps <= 1e-3:
            v_fps = 30.0

        stride = max(1, int(round(v_fps / sample_fps)))
        frame_idx = 0
        detections = []
        best_label, best_conf = None, -1.0
        processed_frames = 0
        max_frames = int(15 * v_fps)

        while True:
            ok, frame = cap.read()
            if not ok or frame is None:
                break
            if frame_idx > max_frames:
                break

            if frame_idx % stride == 0:
                sec = int(frame_idx / v_fps)
                pil_img = frame_to_pil(frame)

                bird_crop = detect_bird_with_yolo(yolo_model, pil_img)
                if bird_crop is not None:
                    label, conf = infer_one(model, bird_crop, classes)
                else:
                    label, conf = "No se detectó ave", 0.0

                detections.append({
                    "second": sec,
                    "label": label,
                    "confidence": conf
                })
                processed_frames += 1

                if conf > best_conf:
                    best_conf, best_label = conf, label

                if stop_on_first and conf >= THRESHOLD:
                    break

            frame_idx += 1

        cap.release()

    if processed_frames == 0:
        err("No se pudo decodificar ningún frame. Revisa formato/códec/base64.", 6)

    print(json.dumps({
        "detections": detections,
        "bestLabel": best_label if best_label is not None else "Ave no identificada",
        "bestConfidence": best_conf if best_conf >= 0 else 0.0
    }, ensure_ascii=False))

if __name__ == "__main__":
    main()
