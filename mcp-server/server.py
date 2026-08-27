import requests
from fastapi import FastAPI, HTTPException, Depends
from pydantic import BaseModel
import yaml
import os

app = FastAPI(title="LogiTrack MCP Server")

# Cargar configuración
def load_config():
    config_path = os.path.join(os.path.dirname(__file__), "config.yaml")
    with open(config_path, "r") as f:
        return yaml.safe_load(f)

config = load_config()
API_BASE_URL = config["mcp"]["api_base_url"]
AGENT_USERNAME = config["mcp"]["agente_username"]
AGENT_PASSWORD = config["mcp"]["agente_password"]

# Modelo de autenticación - obtener token JWT
class Token(BaseModel):
    access_token: str

def get_token():
    """Obtener token JWT autenticándose como usuario AGENTE"""
    login_url = f"{API_BASE_URL}/auth/login"
    response = requests.post(login_url, data={
        "username": AGENT_USERNAME,
        "password": AGENT_PASSWORD
    })
    if response.status_code == 200:
        return response.json().get("access_token")
    raise HTTPException(status_code=401, detail="No se pudo obtener token de autenticación")

def get_auth_headers():
    """Obtener headers de autorización con token AGENTE"""
    token = get_token()
    return {"Authorization": f"Bearer {token}"}

# Modelos de petición
class ResumenRequest(BaseModel):
    fecha: str
    narrativa: str
    alertas: list
    accionesSugeridas: list

# Tool 1: consultar_stock_producto
@app.get("/tools/consultar_stock_producto")
def consultar_stock_producto(productoId: int):
    """Consultar stock actual de un producto por ID"""
    try:
        url = f"{API_BASE_URL}/productos/{productoId}/stock"
        headers = get_auth_headers()
        response = requests.get(url, headers=headers, timeout=30)
        if response.status_code == 200:
            return response.json()
        raise HTTPException(status_code=response.status_code, detail=f"Error al consultar stock: {response.text}")
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error al consultar stock: {str(e)}")

# Tool 2: consultar_bodegas_criticas
@app.get("/tools/consultar_bodegas_criticas")
def consultar_bodegas_criticas():
    """Consultar bodegas con ocupación >= 90%"""
    try:
        url = f"{API_BASE_URL}/bodegas/criticas"
        headers = get_auth_headers()
        response = requests.get(url, headers=headers, timeout=30)
        if response.status_code == 200:
            return response.json()
        raise HTTPException(status_code=response.status_code, detail=f"Error al consultar bodegas críticas: {response.text}")
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error al consultar bodegas críticas: {str(e)}")

# Tool 3: consultar_productos_en_riesgo
@app.get("/tools/consultar_productos_en_riesgo")
def consultar_productos_en_riesgo():
    """Consultar productos con stock below punto de reorden"""
    try:
        url = f"{API_BASE_URL}/productos/riesgo"
        headers = get_auth_headers()
        response = requests.get(url, headers=headers, timeout=30)
        if response.status_code == 200:
            return response.json()
        raise HTTPException(status_code=response.status_code, detail=f"Error al consultar productos en riesgo: {response.text}")
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error al consultar productos en riesgo: {str(e)}")

# Tool 4: consultar_kpis
@app.get("/tools/consultar_kpis")
def consultar_kpis():
    """Consultar KPIs del panel"""
    try:
        url = f"{API_BASE_URL}/kpis"
        headers = get_auth_headers()
        response = requests.get(url, headers=headers, timeout=30)
        if response.status_code == 200:
            return response.json()
        raise HTTPException(status_code=response.status_code, detail=f"Error al consultar KPIs: {response.text}")
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error al consultar KPIs: {str(e)}")

# Tool 5: crear_orden_borrador
class CrearOrdenRequest(BaseModel):
    productoId: int
    proveedorId: int | None = None
    bodegaDestinoId: int
    cantidad: int
    precioUnitario: float

@app.post("/tools/crear_orden_borrador")
def crear_orden_borrador(request: CrearOrdenRequest):
    """Crear una orden en estado BORRADOR"""
    try:
        url = f"{API_BASE_URL}/ordenes"
        headers = get_auth_headers()
        payload = {
            "productoId": request.productoId,
            "proveedorId": request.proveedorId,
            "bodegaDestinoId": request.bodegaDestinoId,
            "cantidad": request.cantidad,
            "precioUnitario": request.precioUnitario
        }
        response = requests.post(url, json=payload, headers=headers, timeout=30)
        if response.status_code == 201:
            return response.json()
        raise HTTPException(status_code=response.status_code, detail=f"Error al crear orden: {response.text}")
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error al crear orden: {str(e)}")

# Tool 6: publicar_resumen
@app.post("/tools/publicar_resumen")
def publicar_resumen(resumen: ResumenRequest):
    """Validar y publicar un resumen estructurado"""
    try:
        url = f"{API_BASE_URL}/panel/resumen"
        headers = get_auth_headers()
        headers["Content-Type"] = "application/json"
        response = requests.post(url, json=resumen.model_dump(), headers=headers, timeout=30)
        if response.status_code in (200, 201):
            return response.json()
        raise HTTPException(status_code=response.status_code, detail=f"Error al publicar resumen: {response.text}")
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error al publicar resumen: {str(e)}")

# Health check
@app.get("/health")
def health():
    return {"status": "ok", "message": "MCP Server running"}