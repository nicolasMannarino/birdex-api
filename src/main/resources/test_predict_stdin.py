import sys, os, json, base64, binascii, re
from io import BytesIO
from PIL import Image, ImageFile
import torch
import torch.nn as nn
import torchvision.transforms as transforms
import torchvision.models as models
from torchvision.models import ResNet18_Weights
from ultralytics import YOLO

# ---- Salida UTF-8 segura en Windows / evitar emojis ----
try:
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    sys.stderr.reconfigure(encoding='utf-8', errors='replace')
except Exception:
    pass

DEVICE = torch.device("cuda" if torch.cuda.is_available() else "cpu")
THRESHOLD = 0.95

BASE_DIR = os.path.dirname(__file__)
MODEL_PATH = os.path.join(BASE_DIR, "modelo_imagenes_birdex.pth")
CLASSES_PATH = os.path.join(BASE_DIR, "birdex_clases.json")
YOLO_PATH = os.path.join(BASE_DIR, "yolov8m.pt")

# ---- Transformaciones del clasificador ----
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

def extract_base64_from_stdin():
    raw = sys.stdin.read()
    if not raw or not raw.strip():
        err("No se recibió ningún dato por stdin.", 2)

    s = raw.strip()

    if s.startswith("{"):
        try:
            data = json.loads(s)
            for k in ("imageBase64", "image", "base64", "b64", "data"):
                if k in data and isinstance(data[k], str) and data[k].strip():
                    s = data[k].strip()
                    break
            else:
                err("JSON recibido pero no se encontró ninguna clave con la imagen base64.", 3)
        except Exception as e:
            err(f"Entrada con formato JSON inválido: {e}", 3)

    if s.lower().startswith("data:"):
        try:
            s = s.split(",", 1)[1]
        except Exception:
            err("Prefijo data URL mal formado.", 3)

    s = re.sub(r"\s+", "", s)

    try:
        return base64.b64decode(s, validate=True)
    except binascii.Error as e:
        err(f"Cadena base64 inválida: {e}", 4)

def open_image(img_bytes):
    ImageFile.LOAD_TRUNCATED_IMAGES = True
    try:
        return Image.open(BytesIO(img_bytes)).convert("RGB")
    except Exception as e:
        err(f"No se pudo abrir la imagen: {e}", 5)

def detect_bird_with_yolo(image):
    """Devuelve el recorte del ave si hay detección, o None si no hay."""
    try:
        yolo_model = YOLO(YOLO_PATH)
    except Exception as e:
        err(f"No se pudo cargar el modelo YOLO: {e}", 12)

    results = yolo_model(image)
    for result in results:
        for box in result.boxes:
            class_id = int(box.cls[0])
            class_name = yolo_model.names[class_id]
            if class_name.lower() == "bird":
                x1, y1, x2, y2 = map(int, box.xyxy[0].tolist())
                return image.crop((x1, y1, x2, y2))
    return None

def main():
    # Cargar clases y modelo de clasificación
    classes = load_classes(CLASSES_PATH)
    model = load_model(MODEL_PATH, len(classes))

    # Obtener imagen
    img_bytes = extract_base64_from_stdin()
    image = open_image(img_bytes)

    # ---- Detectar ave con YOLO ----
    bird_crop = detect_bird_with_yolo(image)
    if bird_crop is None:
        print("No se detectó un ave,0.0")
        return

    # ---- Clasificar ----
    input_tensor = transform(bird_crop).unsqueeze(0).to(DEVICE)
    with torch.no_grad():
        outputs = model(input_tensor)
        probs = torch.softmax(outputs, dim=1).squeeze()
        class_index = int(torch.argmax(probs).item())
        confidence = float(probs[class_index].item())

    etiqueta = classes[class_index] if confidence >= THRESHOLD else "Ave no identificada"
    print(f"{etiqueta},{confidence:.4f}")

if __name__ == "__main__":
    main()
