import base64
import os
import sys
import io
import tempfile
from datetime import datetime
from pathlib import Path
from typing import List, Optional

from dotenv import load_dotenv
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

# ---------- Forzar UTF-8 global (antes de importar birdnetlib) ----------
import builtins

_open_builtin = open
def open_utf8(path, mode="r", *args, **kwargs):
    if "b" not in mode and "encoding" not in kwargs:
        kwargs["encoding"] = "utf-8"
    return _open_builtin(path, mode, *args, **kwargs)

builtins.open = open_utf8
os.environ["PYTHONUTF8"] = "1"
os.environ["PYTHONIOENCODING"] = "utf-8"
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding="utf-8")

# ---------- Cargar .env ----------
load_dotenv()

# Carpeta base donde está este archivo
BASE_DIR = Path(__file__).resolve().parent

# Rutas absolutas seguras (relativas al server.py)
MODEL_PATH = (BASE_DIR / os.getenv("MODEL_PATH")).resolve()
LABELS_PATH = (BASE_DIR / os.getenv("LABELS_PATH")).resolve()

# --- BirdNET ---
from birdnetlib.analyzer import Analyzer
from birdnetlib import Recording

# -------- FastAPI --------
app = FastAPI(title="BirdNET Service", version="1.0.0")

origins = [o.strip() for o in os.getenv("CORS_ORIGINS", "*").split(",")] or ["*"]
app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# -------- Modelos Pydantic --------
class AnalyzeRequest(BaseModel):
    audio_base64: str = Field(..., description="Audio en Base64 (mp3 o wav)")
    min_conf: float = Field(float(os.getenv("MIN_CONFIDENCE", 0.3)), ge=0.0, le=1.0)
    lat: Optional[float] = None
    lon: Optional[float] = None
    date: Optional[str] = Field(None, description="YYYY-MM-DD")

class Detection(BaseModel):
    start_time: float
    end_time: float
    label: str
    confidence: float

class AnalyzeResponse(BaseModel):
    detections: List[Detection]

# -------- Carga del modelo --------
if not MODEL_PATH.exists() or not LABELS_PATH.exists():
    raise RuntimeError(f"Model/labels not found:\n - {MODEL_PATH}\n - {LABELS_PATH}")

analyzer = Analyzer(
    classifier_model_path=str(MODEL_PATH),
    classifier_labels_path=str(LABELS_PATH),
)

# Intento de asignar tensores (si aplica)
try:
    if hasattr(analyzer, "interpreter") and analyzer.interpreter is not None:
        analyzer.interpreter.allocate_tensors()
        print("[INFO] Tensores asignados correctamente en el inicio")
except Exception as e:
    print(f"[WARN] No se pudo asignar tensores al inicio: {e}")

# -------- Endpoints --------
@app.get("/health")
def health():
    return {"status": "ok"}

@app.get("/debug-model")
def debug_model():
    """Verifica lectura de labels y muestra un sample."""
    try:
        with open(LABELS_PATH, "r", encoding="utf-8") as f:
            labels = [ln.strip() for ln in f if ln.strip()]
        return {
            "labels_path": str(LABELS_PATH),
            "labels_count": len(labels),
            "sample": labels[:5],
        }
    except Exception as e:
        return {"error": f"No pude leer labels: {e}", "labels_path": str(LABELS_PATH)}

@app.post("/analyze", response_model=AnalyzeResponse)
def analyze(req: AnalyzeRequest):
    # Decodificar audio
    try:
        raw = base64.b64decode(req.audio_base64)
    except Exception:
        raise HTTPException(status_code=400, detail="Invalid Base64")

    # Guardar temporalmente como mp3 (sirve también para wav)
    with tempfile.NamedTemporaryFile(suffix=".mp3", delete=False) as f:
        f.write(raw)
        audio_path = f.name

    # Parsear fecha opcional
    date_obj = None
    if req.date:
        try:
            y, m, d = map(int, req.date.split("-"))
            date_obj = datetime(year=y, month=m, day=d)
        except Exception:
            # Si falla, seguimos sin fecha (BirdNET puede trabajar sin ella)
            pass

    try:
        recording = Recording(
            analyzer,
            audio_path,
            min_conf=req.min_conf,
            lat=req.lat,
            lon=req.lon,
            date=date_obj,
        )
        recording.analyze()

        # Fallback de label según versión/modelo
        def pick_label(d: dict) -> str:
            return (
                    d.get("label")
                    or d.get("common_name")
                    or d.get("species_common_name")
                    or d.get("scientific_name")
                    or (d.get("species") if isinstance(d.get("species"), str) else None)
                    or ""
            )

        detections = [
            Detection(
                start_time=float(d.get("start_time", 0.0)),
                end_time=float(d.get("end_time", 0.0)),
                label=pick_label(d),
                confidence=float(d.get("confidence", 0.0)),
            )
            for d in recording.detections
        ]

        return AnalyzeResponse(detections=detections)

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Analyzer error: {e}")

    finally:
        try:
            os.remove(audio_path)
        except Exception:
            pass
