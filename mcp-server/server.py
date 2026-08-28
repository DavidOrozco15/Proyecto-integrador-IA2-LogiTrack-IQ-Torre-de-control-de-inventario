import requests
from fastapi import FastAPI, HTTPException, Depends
from pydantic import BaseModel
import yaml
import os
import logging

# Configurar logging
logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)

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

def get_auth_headers():
    """Obtener headers de autorización con token AGENTE"""
    token = get_token()
    headers = {"Authorization": f"Bearer {token}"}
    logger.debug(f"=== AUTH DEBUG ===")
    logger.debug(f"Token length: {len(token) if token else 0}")
    logger.debug(f"Token preview: {token[:80] if token else 'None'}")
    logger.debug(f"Full header: {headers}")
    return headers

# Cache para evitar múltiples logins
_token_cache = {"token": None, "expires_at": 0}

def get_token():
    """Obtener token JWT autenticándose como usuario AGENTE"""
    import time
    global _token_cache
    
    # Verificar si el token cacheado sigue válido (con 60s de margen)
    if _token_cache["token"] and time.time() < _token_cache["expires_at"] - 60:
        logger.debug(f"Using cached token")
        return _token_cache["token"]
    
    """Obtener token JWT autenticándose como usuario AGENTE"""
    login_url = f"{API_BASE_URL}/auth/login"
    response = requests.post(login_url, json={
        "username": AGENT_USERNAME,
        "password": AGENT_PASSWORD
    })
    logger.debug(f"Login response status: {response.status_code}")
    logger.debug(f"Login response body: {response.text}")
    if response.status_code == 200:
        token_data = response.json()
        token = token_data.get("token")  # Backend devuelve "token", no "access_token"
        logger.debug(f"Token obtained: {token[:50] if token else None}...")
        
        # Cachear token (JWT expira en 24h, ponemos 23h)
        _token_cache["token"] = token
        _token_cache["expires_at"] = time.time() + 23 * 3600
        
        return token
    logger.error(f"Login failed: {response.status_code} - {response.text}")
    raise HTTPException(status_code=401, detail="No se pudo obtener token de autenticación")

def get_auth_headers():
    """Obtener headers de autorización con token AGENTE"""
    token = get_token()
    headers = {"Authorization": f"Bearer {token}"}
    logger.debug(f"=== AUTH DEBUG ===")
    logger.debug(f"Token length: {len(token) if token else 0}")
    logger.debug(f"Token preview: {token[:80] if token else 'None'}")
    logger.debug(f"Full header: {headers}")
    return headers

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
        logger.debug(f"=== REQUEST DEBUG ===")
        logger.debug(f"URL: {url}")
        logger.debug(f"Headers: {headers}")
        response = requests.get(url, headers=headers, timeout=30)
        logger.debug(f"Response status: {response.status_code}")
        logger.debug(f"Response body: {response.text[:500]}")
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

# Tool 3b: consultar_proveedores
@app.get("/tools/consultar_proveedores")
def consultar_proveedores():
    """Consultar todos los proveedores"""
    try:
        url = f"{API_BASE_URL}/proveedores"
        headers = get_auth_headers()
        response = requests.get(url, headers=headers, timeout=30)
        if response.status_code == 200:
            return response.json()
        raise HTTPException(status_code=response.status_code, detail=f"Error al consultar proveedores: {response.text}")
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error al consultar proveedores: {str(e)}")

# Tool 3c: consultar_bodega_sugerida
@app.get("/tools/consultar_bodega_sugerida/{productoId}")
def consultar_bodega_sugerida(productoId: int):
    """Consultar bodega sugerida para un producto (la que tiene menor stock)"""
    try:
        url = f"{API_BASE_URL}/productos/{productoId}/bodega-sugerida"
        headers = get_auth_headers()
        response = requests.get(url, headers=headers, timeout=30)
        if response.status_code == 200:
            return response.json()
        raise HTTPException(status_code=response.status_code, detail=f"Error al consultar bodega sugerida: {response.text}")
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error al consultar bodega sugerida: {str(e)}")

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
            "producto": {"id": request.productoId},
            "proveedor": {"id": request.proveedorId} if request.proveedorId else None,
            "bodegaDestino": {"id": request.bodegaDestinoId},
            "cantidad": request.cantidad,
            "precioUnitario": request.precioUnitario
        }
        # Remove None values
        payload = {k: v for k, v in payload.items() if v is not None}
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

# Tool 7: consultar_ordenes_borrador
@app.get("/tools/consultar_ordenes_borrador")
def consultar_ordenes_borrador():
    """Consultar órdenes en estado BORRADOR"""
    try:
        url = f"{API_BASE_URL}/ordenes?estado=BORRADOR"
        headers = get_auth_headers()
        response = requests.get(url, headers=headers, timeout=30)
        if response.status_code == 200:
            return response.json()
        raise HTTPException(status_code=response.status_code, detail=f"Error: {response.text}")
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error: {str(e)}")

# Health check
@app.get("/health")
def health():
    return {"status": "ok", "message": "MCP Server running"}