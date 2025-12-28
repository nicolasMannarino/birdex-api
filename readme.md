# 🦅 BirdEX - Backend System

Sistema de backend para **BirdEX**, compuesto por una arquitectura híbrida:
1. **Core API:** Java + Spring Boot (Lógica de negocio, usuarios, gamificación).
2. **AI Service:** Python + FastAPI (Identificación de aves con BirdNET).

---

### 📱 Cliente Móvil
Este repositorio contiene los servicios del servidor. El código de la aplicación móvil se encuentra aquí:
👉 **[Ir al repositorio Mobile (React Native)](https://github.com/nicolasMannarino/birdex-mobile)**

---

## 🐍 Guía de Ejecución: Servicio de IA (FastAPI)

Este microservicio se encarga de procesar los audios/imágenes para identificar las aves. Se encuentra en la carpeta `/birdnet-service`.
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

👥 Créditos
Desarrollado como proyecto universitario.

Integración IA: Python, FastAPI, Poetry.

Backend Core: Java, Spring Boot.
