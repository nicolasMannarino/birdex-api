import sys
import os
import base64
from io import BytesIO
from PIL import Image
import torch
import torchvision.transforms as transforms
import torch.nn as nn
import torchvision.models as models
from torchvision.models import ResNet18_Weights

DEVICE = torch.device("cuda" if torch.cuda.is_available() else "cpu")
CLASS_NAMES = ['Paroaria coronata', 'Ramphastos toco']
THRESHOLD = 0.95

# Construimos la ruta absoluta del modelo
MODEL_PATH = os.path.join(os.path.dirname(__file__), 'modelo_imagenes_birdex.pth')

transform = transforms.Compose([
    transforms.Resize((224, 224)),
    transforms.ToTensor(),
    transforms.Normalize(mean=[0.485, 0.456, 0.406],
                         std=[0.229, 0.224, 0.225])
])

model = models.resnet18(weights=ResNet18_Weights.IMAGENET1K_V1)
model.fc = nn.Linear(model.fc.in_features, len(CLASS_NAMES))
model.load_state_dict(torch.load(MODEL_PATH, map_location=DEVICE))
model = model.to(DEVICE)
model.eval()

# Leer base64 desde stdin
b64_string = sys.stdin.read().strip()
image_data = base64.b64decode(b64_string)
image = Image.open(BytesIO(image_data)).convert("RGB")

input_tensor = transform(image).unsqueeze(0).to(DEVICE)

with torch.no_grad():
    output = model(input_tensor)
    prob = torch.softmax(output, dim=1).squeeze()
    class_index = torch.argmax(prob).item()
    confidence = prob[class_index].item()

etiqueta = CLASS_NAMES[class_index] if confidence >= THRESHOLD else "Ave no identificada"
print(f"{etiqueta},{confidence:.4f}")