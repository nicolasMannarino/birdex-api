# Guía para levantar el servicio FastAPI

## 🔹 Inicialización
*(Solo la primera vez que configures el proyecto o si cambias el `pyproject.toml`)*

```bash
# 1. Entrar al proyecto
cd /bx-server/birdnet-service

# 2. Usar la versión de Python del sistema (o la que necesites)
poetry env use python

# 3. Instalar dependencias
poetry install

# 4. (Opcional) Verificar que los paquetes clave estén instalados
poetry run python -c "import numpy, scipy, resampy; print('OK:', numpy.__version__, scipy.__version__, resampy.__version__)"

```
## 🔹 Ejecución diaria  
*(Después de reiniciar la PC o cuando quieras levantar el servicio)*

```bash
# 1. Entrar al proyecto
cd /bx-server/birdnet-service

# 2. Levantar el servidor FastAPI
poetry run uvicorn server:app --host 0.0.0.0 --port 8000 --reload
```