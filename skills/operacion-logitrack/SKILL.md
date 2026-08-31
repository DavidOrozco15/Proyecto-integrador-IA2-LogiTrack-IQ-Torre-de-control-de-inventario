# Skill: operacion-logitrack

## Funcion

Este skill documenta las herramientas MCP disponibles para el AI Agent y el flujo automatizado n8n. El MCP server (`mcp-server/server.py`) expone 9 herramientas HTTP que el workflow de n8n utiliza como capa intermedia antes de llegar al backend Spring Boot.

---

## Arquitectura

```
n8n (AI Agent)
    │
    ▼
MCP Server (FastAPI, puerto 8081)
    │  - Autenticacion automatica (login como agente_user)
    │  - Proxy de endpoints del backend
    │
    ▼
Backend Spring Boot (puerto 8080)
```

El MCP server:
- Se autentica como `agente_user` y cachea el token JWT
- Expone endpoints simplificados en `/tools/*`
- Proxya las peticiones al backend con headers de autenticacion

---

## Herramientas MCP (9 total)

### Lectura (6)

| # | Herramienta | Metodo | URL MCP | Parametros | Descripcion |
|---|-------------|--------|---------|------------|-------------|
| 1 | `consultar_kpis` | GET | `/tools/consultar_kpis` | Ninguno | KPIs del panel: ocupacion, quiebre, riesgo, ordenes, movimientos |
| 2 | `consultar_productos_en_riesgo` | GET | `/tools/consultar_productos_en_riesgo` | Ninguno | Productos con stock bajo punto de reorden |
| 3 | `consultar_bodegas_criticas` | GET | `/tools/consultar_bodegas_criticas` | Ninguno | Bodegas con ocupacion >= 90% |
| 4 | `consultar_stock_producto` | GET | `/tools/consultar_stock_producto` | `productoId: int` | Stock actual de un producto especifico |
| 5 | `consultar_proveedores` | GET | `/tools/consultar_proveedores` | Ninguno | Lista de todos los proveedores |
| 6 | `consultar_bodega_sugerida` | GET | `/tools/consultar_bodega_sugerida/{productoId}` | `productoId: int` (en URL) | Bodega con menor stock para ese producto |

### Escritura (2)

| # | Herramienta | Metodo | URL MCP | Body | Descripcion |
|---|-------------|--------|---------|------|-------------|
| 7 | `crear_orden_borrador` | POST | `/tools/crear_orden_borrador` | `{productoId, proveedorId?, bodegaDestinoId, cantidad, precioUnitario}` | Crea orden en estado BORRADOR |
| 8 | `publicar_resumen` | POST | `/tools/publicar_resumen` | `{fecha, narrativa, alertas[], accionesSugeridas[]}` | Publica resumen en el panel |

### Consulta (1)

| # | Herramienta | Metodo | URL MCP | Body | Descripcion |
|---|-------------|--------|---------|------|-------------|
| 9 | `consultar_ordenes_borrador` | GET | `/tools/consultar_ordenes_borrador` | Ninguno | Ordenes en estado BORRADOR |

---

## Flujo n8n (real)

```
INICIO (6:00 AM America/Bogota)
    │
    ▼
Config MCP → mcp_base_url = http://mcp:8081, auth = agente_user
    │
    ├─→ consultar_kpis()           ──→ Normalizar KPIs
    ├─→ consultar_productos_en_riesgo() ──→ Normalizar Productos
    ├─→ consultar_bodegas_criticas()    ──→ Normalizar Bodegas
    └─→ consultar_ordenes_borrador()    ──→ Normalizar Ordenes
                │
                ▼
        Merge KPIs + Productos + Bodegas + Ordenes → Merge MCP Data
                │
                ▼
        AI Agent (OpenRouter) → Genera JSON: {fecha, narrativa, alertas, accionesSugeridas}
                │
                ▼
        Parse AI Output → Extrae JSON
                │
                ▼
        ┌─────────────────────────┐
        │ Hay productos en riesgo? │
        └────────┬────────────────┘
                 │
            ┌────┴────┐
            │ NO      │ SÍ
            ▼         ▼
    publicar_    Preparar Items
    resumen_         │
    sin_orden        ▼
        │       crear_orden_borrador() (POST al MCP)
        │           │
        │           ▼
        │       publicar_resumen_con_orden (POST al MCP)
        │           │
        ▼           ▼
        └─────→ FIN
```

---

## Ejemplos de Uso

### Consultar KPIs
```bash
curl http://localhost:8081/tools/consultar_kpis
```

### Consultar productos en riesgo
```bash
curl http://localhost:8081/tools/consultar_productos_en_riesgo
```

### Crear orden en borrador
```bash
curl -X POST http://localhost:8081/tools/crear_orden_borrador \
  -H "Content-Type: application/json" \
  -d '{"productoId":10,"proveedorId":1,"bodegaDestinoId":3,"cantidad":17,"precioUnitario":180000}'
```

### Publicar resumen
```bash
curl -X POST http://localhost:8081/tools/publicar_resumen \
  -H "Content-Type: application/json" \
  -d '{
    "fecha":"2026-08-30",
    "narrativa":"Hay productos en riesgo y una orden pendiente de aprobacion.",
    "alertas":[{"severidad":"ALTA","titulo":"Producto en riesgo","detalle":"Stock bajo","productoId":10,"ordenId":null,"bodegaId":3}],
    "accionesSugeridas":[{"tipo":"REVISAR_ORDEN","descripcion":"Revisar orden creada","ordenId":100,"productoId":null,"bodegaId":null}]
  }'
```

---

## Seguridad

| Campo | Valor |
|-------|-------|
| Usuario MCP | `agente_user` |
| Password MCP | `admin123` |
| Rol | AGENTE |
| Token | JWT cacheado automaticamente (expira 24h) |

### Permisos del rol AGENTE

| Permitido | Denegado |
|-----------|----------|
| GET en /api/kpis, /api/productos/riesgo, /api/bodegas/criticas, /api/ordenes/** | PATCH /api/ordenes/*/estado |
| POST /api/ordenes, /api/panel/resumen | DELETE cualquier recurso |
| | POST /api/movimientos |

---

## Archivos Relacionados

| Archivo | Funcion |
|---------|---------|
| `mcp-server/server.py` | Servidor MCP con las 9 herramientas |
| `mcp-server/config.yaml` | Configuracion del MCP (credenciales, URLs) |
| `n8n/resumen-diario-inventario.json` | Workflow n8n que usa las herramientas MCP |
| `MCP-n8n-Manual.md` | Manual tecnico completo |
