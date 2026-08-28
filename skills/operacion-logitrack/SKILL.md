# Skill: operacion-logitrack

## Instrucciones para Flujo Automatizado

Este archivo define las reglas operativas que el flujo n8n (nodo AI Agent) debe seguir al interactuar con el servidor MCP de LogiTrack. Estas instrucciones son la evidencia mantenible de lo que el agente puede y no puede hacer.

---

## 1. CONSULTAR PRIMERO RIESGOS Y KPIs

**Siempre** ejecutar en este orden al iniciar una ejecución:

```yaml
Paso 1: consultar_kpis()
  - Obtiene los 4 indicadores: ocupación por bodega, productos en quiebre, productos en riesgo, órdenes por aprobar, movimientos de ayer
  - Usar para contexto general del estado del inventario

Paso 2: consultar_productos_en_riesgo()
  - Obtiene lista de productos con stock below punto de reorden
  - Cada producto incluye: productoId, nombre, proveedorId, stockTotal, consumoDiarioPromedio, puntoReorden, diasCobertura, estadoCobertura, bodegaDestinoId
  - La bodegaDestinoId es la bodega con menor stock de ese producto (empate → menor ID)
```

**NO proceder a crear órdenes sin haber ejecutado estas dos consultas.**

---

## 2. DECISIÓN: ¿Hay productos en riesgo?

- **SÍ hay productos en riesgo** → Continuar al paso 3 (crear máximo UNA orden en borrador)
- **NO hay productos en riesgo** → Saltar al paso 4 (publicar resumen sin orden)

---

## 3. CREAR ORDEN (solo si hay productos en riesgo)

**RESTRICCIÓN CRÍTICA**: Máximo **una** orden en borrador por ejecución.

```yaml
Herramienta: crear_orden_borrador()
Parámetros (basados en el PRIMER producto de la lista de riesgo):
  productoId: [productoId del primer producto en riesgo]
  proveedorId: [proveedorId del producto, o null si no tiene proveedor principal]
  bodegaDestinoId: [bodegaDestinoId sugerida del producto]
  cantidad: ceil(max(1, puntoReorden × 2 - stockTotal))
  precioUnitario: [precioUnitario actual del producto]
```

**Fórmula de cantidad obligatoria:**
```
cantidad = ceil(max(1, puntoReorden * 2 - stockTotal))
```

Ejemplo: si puntoReorden = 10, stockTotal = 3 → max(1, 20 - 3) = 17 → ceil(17) = 17

**Validaciones:**
- El producto DEBE tener proveedor principal (proveedorId no null)
- La bodegaDestinoId DEBE existir
- La cantidad DEBE ser >= 1
- El precioUnitario DEBE ser > 0

---

## 4. PUBLICAR RESUMEN DEL PANEL

**Siempre** publicar un resumen al final de la ejecución (con o sin orden creada).

```yaml
Herramienta: publicar_resumen()
Payload JSON (contrato exacto):
  fecha: "YYYY-MM-DD"  # Fecha actual en zona horaria America/Bogota
  narrativa: string (20-500 caracteres)
  alertas: array (puede estar vacío)
    - severidad: "BAJA" | "MEDIA" | "ALTA"
    - titulo: string
    - detalle: string
    - productoId: Long | null
    - ordenId: Long | null
    - bodegaId: Long | null
    (Al menos uno de productoId/ordenId/bodegaId debe estar presente)
  accionesSugeridas: array (puede estar vacío)
    - tipo: "REVISAR_ORDEN" | "REVISAR_PRODUCTO" | "REVISAR_BODEGA"
    - descripcion: string
    - ordenId: Long | null
    - productoId: Long | null
    - bodegaId: Long | null
    (Exactamente uno de ordenId/productoId/bodegaId debe estar presente)
```

**Ejemplo con orden creada:**
```json
{
  "fecha": "2026-08-27",
  "narrativa": "Hay productos en riesgo y una orden pendiente de aprobación.",
  "alertas": [
    {
      "severidad": "ALTA",
      "titulo": "Producto en riesgo",
      "detalle": "Producto X está por debajo de su punto de reorden.",
      "productoId": 1,
      "ordenId": 100,
      "bodegaId": 3
    }
  ],
  "accionesSugeridas": [
    {
      "tipo": "REVISAR_ORDEN",
      "descripcion": "Revisar la orden 100 antes de aprobarla.",
      "ordenId": 100,
      "productoId": null,
      "bodegaId": null
    }
  ]
}
```

**Ejemplo sin productos en riesgo:**
```json
{
  "fecha": "2026-08-27",
  "narrativa": "No hay productos en riesgo el día de hoy. Inventario estable.",
  "alertas": [],
  "accionesSugeridas": []
}
```

---

## 5. MANEJO DE ERRORES

| Situación | Acción |
|-----------|--------|
| Alguna herramienta MCP falla (timeout, 500, 401, 403) | Informar error específico en la salida, no continuar con pasos posteriores |
| No hay productos en riesgo | Registrar éxito sin crear órdenes, publicar resumen "sin riesgos" |
| Falla creación de orden (400, 403, 500) | Registrar error, **no** publicar resumen con datos de orden ficticia |
| Error de validación en publicar_resumen (400) | Informar error, el último resumen válido permanece disponible |

---

## ⛔ RESTRICCIONES OBLIGATORIAS (NO NEGOCIABLES)

| # | Restricción | Explicación |
|---|-------------|-------------|
| 1 | **Consultar primero riesgos y KPIs** | Obligatorio ejecutar `consultar_kpis()` y `consultar_productos_en_riesgo()` antes de cualquier decisión |
| 2 | **Máximo una orden en borrador por ejecución** | Nunca crear más de una orden aunque haya múltiples productos en riesgo |
| 3 | **NO aprobar, cancelar ni recibir órdenes** | **PROHIBIDO** - El MCP no tiene herramienta para esto, y el rol AGENTE retorna 403 en `PATCH /ordenes/{id}/estado` |
| 4 | **Publicar solo JSON que cumpla el contrato** | Validar estructura antes de llamar `publicar_resumen()` |
| 5 | **Informar error si una herramienta falla** | No silenciar errores, reportar en salida de ejecución n8n |

---

## 🔐 CONTEXTO DE SEGURIDAD

- **Rol utilizado**: AGENTE (usuario `agente_user`)
- **Endpoints permitidos para AGENTE** (según SecurityConfig):
  - GET `/api/kpis`, `/api/productos/riesgo`, `/api/productos/*/stock`, `/api/bodegas/criticas`, `/api/proveedores`, `/api/ordenes/**`
  - POST `/api/ordenes`, `/api/panel/resumen`
- **Endpoints DENEGADOS para AGENTE** (retornan 403):
  - PATCH `/api/ordenes/*/estado` (solo ADMIN)
  - POST `/api/movimientos` (solo ADMIN, EMPLEADO)
  - DELETE cualquier recurso
  - POST `/api/bodegas/**`, POST `/api/productos/**` (solo ADMIN para creación completa, pero AGENTE puede crear órdenes)

---

## 📋 FLUJO RESUMIDO (para n8n AI Agent)

```
INICIO (6:00 AM America/Bogota)
    │
    ▼
consultar_kpis() ──→ consultar_productos_en_riesgo()
    │                      │
    │                      ▼
    │              ┌─────────────────┐
    │              │ Hay productos   │── NO ──→ Publicar resumen "sin riesgos" → FIN
    │              │ en riesgo?      │
    │              └────────┬────────┘
    │                       │ SÍ
    ▼                       ▼
crear_orden_borrador() ──→ publicar_resumen(con datos de orden)
    │                       │
    ▼                       ▼
FIN ←─────────────────── Registrar salida (éxito/error)
```

---

## ✅ Checklist de Validación (para revisión manual)

- [ ] Ejecuta `consultar_kpis()` primero
- [ ] Ejecuta `consultar_productos_en_riesgo()` segundo
- [ ] Si hay riesgo: crea **exactamente una** orden con fórmula correcta
- [ ] Si no hay riesgo: NO crea orden
- [ ] **Nunca** llama endpoint de aprobar/cancelar/recibir orden
- [ ] Publica resumen con JSON válido (estructura exacta)
- [ ] Maneja errores reportándolos, no callendo en silencios
- [ ] Usa zona horaria America/Bogota para fecha
- [ ] Cantidad calculada: `ceil(max(1, puntoReorden * 2 - stockTotal))`