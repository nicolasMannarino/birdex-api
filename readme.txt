HOW TO TURN ON FASTAPI

poetry env use python
poetry env info


poetry env actívate
source .venv/Scripts/actívate
python -V
command -v Python


export PYTHONUTF8=1
uvicorn server:app --host 0.0.0.0 --port 8000 --reload