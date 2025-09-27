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

# ---- Salida UTF-8 segura ----
try:
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    sys.stderr.reconfigure(encoding='utf-8', errors='replace')
except Exception:
    pass

DEVICE = torch.device("cuda" if torch.cuda.is_available() else "cpu")
THRESHOLD = float(os.getenv("BIRDEX_THRESHOLD", "0.95"))

BASE_DIR = os.path.dirname(__file__)
MODEL_PATH = os.path.join(BASE_DIR, "modelo_imagenes_birdex.pth")
CLASSES_PATH = os.path.join(BASE_DIR, "birdex_clases.json")

transform = transforms.Compose([
    transforms.Resize((224, 224)),
    transforms.ToTensor(),
    transforms.Normalize(mean=[0.485, 0.456, 0.406],
                         std=[0.229, 0.224, 0.225])
])

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
    # 1) si viene data URI, usar el mime
    m = re.match(r"^data:(?P<mime>[^;]+);base64,", b64_str or "", flags=re.I)
    if m:
        mime = m.group("mime").lower()
        if "quicktime" in mime:   # iOS suele mandar video/quicktime
            return ".mov"
        if "webm" in mime:
            return ".webm"
        if "x-matroska" in mime or "mkv" in mime:
            return ".mkv"
        return ".mp4"
    # 2) heurística por magic bytes
    b = raw_bytes
    if len(b) >= 12 and b[4:8] == b"ftyp":  # MP4/ISOBMFF
        return ".mp4"
    if b[:4] == b"\x1A\x45\xDF\xA3":        # EBML (webm/mkv)
        return ".webm"
    if b[:4] == b"RIFF" and b[8:12] == b"AVI ":
        return ".avi"
    return ".mp4"

def frame_to_pil(frame_bgr):
    import PIL.Image
    rgb = cv2.cvtColor(frame_bgr, cv2.COLOR_BGR2RGB)
    return PIL.Image.fromarray(rgb)

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

def main():
    payload = read_json_stdin()
    b64_in = payload.get("fileBase64")
    sample_fps = int(payload.get("sampleFps", 1))
    stop_on_first = bool(payload.get("stopOnFirstAbove", False))
    if sample_fps <= 0:
        sample_fps = 1

    classes = load_classes(CLASSES_PATH)
    model = load_model(MODEL_PATH, len(classes))

    video_bytes = extract_base64(b64_in)
    suffix = pick_suffix(b64_in if isinstance(b64_in, str) else "", video_bytes)

    # Guardar temporal con sufijo adecuado (ayuda a OpenCV)
    with tempfile.NamedTemporaryFile(suffix=suffix, delete=True) as tmp:
        tmp.write(video_bytes)
        tmp.flush()

        cap = cv2.VideoCapture(tmp.name)
        if not cap.isOpened():
            err("No se pudo abrir el video (posible códec no soportado).", 5)

        v_fps = cap.get(cv2.CAP_PROP_FPS)
        if not v_fps or v_fps <= 1e-3:
            v_fps = 30.0  # fallback

        stride = max(1, int(round(v_fps / sample_fps)))  # cada cuántos frames muestreo
        frame_idx = 0
        detections = []
        best_label, best_conf = None, -1.0
        processed_frames = 0
        max_frames = int(15 * v_fps)  # respetar 15s máx.

        while True:
            ok, frame = cap.read()
            if not ok or frame is None:
                break
            if frame_idx > max_frames:
                break

            if frame_idx % stride == 0:
                sec = int(frame_idx / v_fps)
                pil_img = frame_to_pil(frame)
                label, conf = infer_one(model, pil_img, classes)

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
