# 02 — Especificación: reglas, contratos y ejemplos

## Convenciones generales (heredadas del enunciado)

- **Fuente de verdad:** el backend y su base de datos son la única fuente de
  información. El dashboard, MCP y n8n consultan o usan la API; no calculan ni
  modifican datos directamente en la base de datos.
- **Zona horaria fija:** `America/Bogota`, para "hoy", "ayer" y la ventana de 30 días
  de consumo. Nunca se usa `LocalDateTime.now()` a secas para estos cálculos (depende
  del reloj del servidor).
- **Errores:** se reutiliza `GlobalExceptionHandler`. Validaciones y transiciones
  inválidas → `400`; recursos inexistentes → `404`; acciones prohibidas por rol →
  `403`; sesión no válida → `401`.
- **Prefijo de rutas:** todos los endpoints nuevos usan `/api`, igual que el resto del
  proyecto (el enunciado los describe sin prefijo, pero permite adaptar la estructura).

---

## 1. Reglas base de inventario

- El stock se calcula **siempre** a partir de los movimientos registrados
  (`InventarioBodegaRepository.sumStockByProductoId` / `sumStockByBodegaId`, ambos con
  `COALESCE(...,0)`). El campo `Producto.stock` **no** es fuente de verdad para los
  cálculos nuevos (se mantiene solo por compatibilidad con el dashboard viejo).
- `ENTRADA` suma unidades a la bodega destino; `SALIDA` resta unidades a la bodega
  origen; `TRANSFERENCIA` resta en origen y suma la misma cantidad en destino.
- No se permite una salida o transferencia que deje una bodega con stock negativo
  (regla ya implementada en `MovimientoInventarioServiceImpl`).
- El stock total de un producto es la suma de sus existencias en todas las bodegas.
- La capacidad de una bodega se mide en unidades de producto y debe ser mayor que 0
  (ya validado con `@Min(1)` en `Bodega.capacidad`).

---

## 2. Modelo de datos nuevo

### Proveedor
| Campo | Tipo | Regla |
|---|---|---|
| id | Long | PK autogenerado |
| nombre | String | `NotBlank`, máx. 100 |
| contacto | String | opcional, máx. 150 |
| diasEntrega | Integer | `NotNull`, entero entre 1 y 90 |

Se cargan mediante `data.sql` (o mecanismo equivalente reproducible).

### Producto (extensión)
- Nuevo campo opcional `proveedorPrincipal` (`ManyToOne` LAZY a `Proveedor`,
  columna `proveedor_principal_id`, sin `@NotNull`).
- **Regla clave:** un producto sin proveedor principal **no puede** aparecer como
  producto en riesgo ni generar una orden automática.

### OrdenCompra
| Campo | Tipo | Regla |
|---|---|---|
| id | Long | PK |
| producto | Producto (ManyToOne) | exactamente uno, obligatorio |
| proveedor | Proveedor (ManyToOne) | obligatorio |
| bodegaDestino | Bodega (ManyToOne) | obligatoria |
| cantidad | Integer | `NotNull`, `@Min(1)` (0 o negativo → 400) |
| precioUnitario | BigDecimal | `NotNull`, `@DecimalMin("0.0")` |
| total | BigDecimal | **se calcula en el servidor** (`cantidad × precioUnitario`), nunca se confía en el valor enviado por el cliente |
| fechaCreacion | LocalDateTime | se fija en `@PrePersist` si viene null |
| estado | EstadoOrdenCompra | `BORRADOR` por defecto |
| creadoPor | Usuario (ManyToOne) | usuario autenticado que crea la orden |
| pdfBytes | byte[] | `columnDefinition = "bytea"`, **sin** `@Lob` (evita el fallback a Large Object/OID de Hibernate 6 + PostgreSQL, que falla contra Supabase) |
| pdfGeneradoEn | LocalDateTime | opcional |

### ResumenPanel
| Campo | Tipo | Regla |
|---|---|---|
| id | Long | PK |
| fecha | LocalDate | `NotNull`, **unique** — solo un resumen válido por fecha |
| contenidoJson | String | TEXT, serializado con Jackson + `JavaTimeModule` |
| autor | Usuario (ManyToOne) | opcional |
| creadoEn | LocalDateTime | fecha de publicación |

Una nueva publicación para la misma fecha **reemplaza** el contenido anterior (UPDATE,
no fila nueva) y queda registrada en auditoría con el valor anterior como
`valoresAnteriores`.

### Rol
Se agrega `AGENTE` al enum existente: `ADMIN`, `EMPLEADO`, `AGENTE`.

---

## 3. Indicadores (KPIs) — reglas exactas

| Indicador | Regla exacta |
|---|---|
| Ocupación por bodega | `(stock total en la bodega / capacidad) × 100`, por cada bodega |
| Productos en quiebre | cantidad de productos cuyo stock total (todas las bodegas) es `0` |
| Productos en riesgo | cantidad de productos **con proveedor principal** cuyo stock total es **menor** que su punto de reorden |
| Órdenes por aprobar | cantidad de órdenes en `BORRADOR` y suma de sus `total` |

Para cada producto en riesgo, además:

- **Consumo diario promedio** = unidades en movimientos `SALIDA` de los últimos 30
  días calendario (incluyendo el día de consulta) ÷ 30.
  - Si el consumo es `0`: el valor expuesto es `null`, `diasCobertura` es `null`, y el
    estado mostrado es `SIN_CONSUMO`. Este producto **no** entra en la lista de riesgo
    (no hay forma de calcular un punto de reorden sin consumo).
- **Punto de reorden** = consumo diario promedio × `diasEntrega` del proveedor
  principal × `1.5`.
- **Días de cobertura** = stock total ÷ consumo diario promedio (`null` si el consumo
  es 0).
- **Regla de umbral:** si el stock es **igual** al punto de reorden, el producto **no**
  está en riesgo — la condición es estrictamente menor (`stock < puntoReorden`).
- **Bodega destino sugerida para reponer:** la bodega con **menor stock actual** de ese
  producto (una bodega sin fila en `inventario_bodega` para ese producto cuenta como
  stock `0`); en caso de empate, la de **menor `id`**.

**Movimientos de ayer:** conteo (no suma de unidades) de movimientos por tipo
(`ENTRADA`, `SALIDA`, `TRANSFERENCIA`) del día calendario anterior en
`America/Bogota`. Se muestra como bloque informativo, no como tarjeta principal de KPI.

**Bodega crítica:** ocupación ≥ 90 %.

---

## 4. Estados de la orden y recepción

| Estado actual | Siguiente estado permitido |
|---|---|
| `BORRADOR` | `APROBADA` o `CANCELADA` |
| `APROBADA` | `RECIBIDA` o `CANCELADA` |
| `RECIBIDA` | ninguno |
| `CANCELADA` | ninguno |

- Una transición no listada en esta tabla responde `400 Bad Request` con mensaje claro.
- Al pasar de `APROBADA` a `RECIBIDA`, el sistema crea **automáticamente** un
  movimiento `ENTRADA` para el producto, cantidad y `bodegaDestino` de la orden. La
  actualización de la orden y la creación del movimiento ocurren en **una sola
  transacción**: ambas se completan o ninguna se guarda.
  - Implementación: se reutiliza `MovimientoInventarioService.registrarMovimiento(...)`
    (ya `@Transactional`), invocado dentro del método `@Transactional` de
    `OrdenCompraServiceImpl.cambiarEstado(...)`; Spring lo ejecuta en la misma
    transacción por propagación `REQUIRED`.
- Al cambiar de estado (cualquier transición), el PDF guardado se elimina
  (`pdfBytes = null`, `pdfGeneradoEn = null`). Debe regenerarse para reflejar el estado
  actual.
- Auditoría: se registra la creación de la orden (`INSERT`) y cada transición de
  estado (`UPDATE`) sobre la entidad `"OrdenCompra"`, siguiendo el mismo patrón manual
  (`guardarAuditoria(...)`) que ya usan `BodegaServiceImpl` / `ProductoServiceImpl` /
  `MovimientoInventarioServiceImpl`. No se activa `AuditEntityListener` (existe en el
  proyecto pero no está enganchado a ninguna entidad; se deja fuera de este alcance).

---

## 5. Contrato del PDF de la orden

- `POST /api/ordenes/{id}/pdf` genera el PDF y lo guarda (reemplaza cualquier PDF
  anterior de la misma orden automáticamente).
- El PDF debe incluir: número de orden, fecha de creación, proveedor, producto,
  cantidad, precio unitario, total, bodega destino y estado.
- Si el estado de la orden es `BORRADOR`, el PDF lleva una marca de agua diagonal
  semitransparente y legible con el texto **"BORRADOR"** (texto grande, rotado 45°,
  opacidad baja, centrada).
- `GET /api/ordenes/{id}/pdf` entrega el archivo con `Content-Type: application/pdf`
  para visualizarlo o descargarlo.
- Si aún no se ha generado el PDF (`pdfBytes == null`), `GET /api/ordenes/{id}/pdf`
  responde `404`.
- Al cambiar el estado de la orden, el PDF guardado se elimina (ver sección 4); debe
  generarse nuevamente para reflejar el estado actual.

---

## 6. Contrato del resumen del panel

`POST /api/panel/resumen` acepta **solo** esta estructura y **no admite propiedades
adicionales** (rechazo con `400` si aparece cualquier campo no declarado):

```json
{
  "fecha": "2026-08-24",
  "narrativa": "Hay productos en riesgo y una orden pendiente de aprobación.",
  "alertas": [
    {
      "severidad": "ALTA",
      "titulo": "Producto en riesgo",
      "detalle": "Producto X está por debajo de su punto de reorden.",
      "productoId": 12,
      "ordenId": null,
      "bodegaId": 3
    }
  ],
  "accionesSugeridas": [
    {
      "tipo": "REVISAR_ORDEN",
      "descripcion": "Revisar la orden 14 antes de aprobarla.",
      "ordenId": 14,
      "productoId": null,
      "bodegaId": null
    }
  ]
}
```

Reglas:

- `fecha` usa `YYYY-MM-DD` y corresponde a la fecha actual en `America/Bogota`.
- `narrativa` tiene entre 20 y 500 caracteres.
- `alertas` y `accionesSugeridas` son arreglos, aunque estén vacíos (nunca `null`).
- `severidad` ∈ `{BAJA, MEDIA, ALTA}`.
- `tipo` (de acción sugerida) ∈ `{REVISAR_ORDEN, REVISAR_PRODUCTO, REVISAR_BODEGA}`.
- Cada identificador informado (`productoId`, `ordenId`, `bodegaId`) debe existir en
  su respectivo repositorio.
- **Una alerta** enlaza **al menos un** identificador de los tres.
- **Una acción sugerida** enlaza **exactamente uno** de los tres.
- Un JSON inválido (estructura, longitud, enumeración, propiedad extra, o
  identificador inexistente) responde `400` y **el último resumen válido permanece
  disponible** (no se modifica nada).
- Se valida estructura, longitud, enumeraciones y existencia de IDs — no se exige
  validar el significado de la narrativa en lenguaje natural.
- Solo puede haber **un resumen válido por fecha**: una nueva publicación para la
  misma fecha reemplaza el contenido anterior (UPDATE) y queda en auditoría.

`GET /api/panel/resumen` devuelve el resumen más reciente disponible (criterio fijado
en la sección "Supuestos y decisiones" más abajo).

---

## 7. Seguridad y permisos

Tabla exigida por el enunciado:

| Acción | AGENTE | ADMIN |
|---|---|---|
| Consultar KPIs, stock, riesgos y bodegas críticas | Sí | Sí |
| Crear orden en BORRADOR | Sí | Sí |
| Publicar resumen | Sí | Sí |
| Aprobar, recibir o cancelar una orden | No | Sí |
| Registrar movimientos manualmente | No | Sí |

Tabla completa de permisos (incluye `EMPLEADO`, no explícito en el enunciado pero
necesario para no romper el proyecto anterior — ver "Supuestos y decisiones"):

| Endpoint | Roles permitidos |
|---|---|
| `GET /api/kpis` | ADMIN, AGENTE, EMPLEADO |
| `GET /api/productos/{id}/stock` | ADMIN, AGENTE, EMPLEADO |
| `GET /api/productos/riesgo` | ADMIN, AGENTE, EMPLEADO |
| `GET /api/bodegas/criticas` | ADMIN, AGENTE, EMPLEADO |
| `GET /api/proveedores` | ADMIN, AGENTE, EMPLEADO |
| `GET /api/ordenes`, `GET /api/ordenes/{id}` | ADMIN, AGENTE, EMPLEADO |
| `GET /api/panel/resumen` | ADMIN, AGENTE, EMPLEADO |
| `POST /api/ordenes` | ADMIN, AGENTE |
| `POST /api/panel/resumen` | ADMIN, AGENTE |
| `GET /api/ordenes/{id}/pdf`, `POST /api/ordenes/{id}/pdf` | ADMIN, AGENTE |
| `PATCH /api/ordenes/{id}/estado` | **solo ADMIN** (doble capa: `SecurityConfig` + `@PreAuthorize`) |
| `POST /api/movimientos` (ya existente) | ADMIN, EMPLEADO — **AGENTE explícitamente excluido** |

Auditoría: se deben registrar las acciones que cambian el estado del sistema —
creación de orden, publicación/reemplazo de resumen, transición de orden y recepción.
No es obligatorio auditar consultas.

Cierre de brecha existente: `POST /api/auth/register` hoy permite auto-asignarse
cualquier rol (incluido, potencialmente, `AGENTE`). Se fuerza `rol = EMPLEADO` en ese
endpoint público; crear un `ADMIN` o `AGENTE` solo se hace vía `data.sql` o un
endpoint protegido con `hasRole('ADMIN')`.

---

## 8. Servidor MCP — herramientas exactas

Servidor MCP separado (`mcp-server/`), autenticado contra `POST /api/auth/login` con
un usuario `AGENTE`, reutilizando el JWT en cada llamada. No accede directamente a la
base de datos ni implementa reglas de negocio propias; solo llama a la API REST.

Exactamente estas **seis** herramientas (ni una más):

| # | Herramienta | Endpoint |
|---|---|---|
| 1 | `consultar_stock_producto(productoId)` | `GET /api/productos/{id}/stock` |
| 2 | `consultar_bodegas_criticas()` | `GET /api/bodegas/criticas` |
| 3 | `consultar_productos_en_riesgo()` | `GET /api/productos/riesgo` |
| 4 | `consultar_kpis()` | `GET /api/kpis` |
| 5 | `crear_orden_borrador(productoId, proveedorId, bodegaDestinoId, cantidad, precioUnitario)` | `POST /api/ordenes` |
| 6 | `publicar_resumen(resumen)` | `POST /api/panel/resumen` |

**Restricción obligatoria:** no existe, y no debe existir, ninguna herramienta para
aprobar, cancelar o recibir órdenes, ni para registrar movimientos manualmente.

---

## 9. Skill y flujo n8n

`skills/operacion-logitrack/SKILL.md` debe indicar, como mínimo:

1. Consultar primero productos en riesgo y KPIs.
2. Crear como máximo **una** orden en borrador por ejecución completa del flujo, y
   solo si hay al menos un producto en riesgo.
3. Nunca aprobar, cancelar ni recibir órdenes.
4. El JSON del resumen debe cumplir exactamente el contrato de la sección 6.
5. Si cualquier herramienta falla, informar el error claramente y detener el flujo,
   sin reintentar acciones que modifiquen datos (crear orden, publicar resumen).

Flujo n8n único, **"Resumen diario de inventario"**:

1. `Schedule Trigger` a las 6:00 a. m., zona `America/Bogota`.
2. Nodo `AI Agent` que usa las herramientas MCP y sigue la skill.
3. Consulta KPIs y productos en riesgo.
4. Si hay al menos un producto en riesgo, crea como máximo una orden para el
   **primer** producto de la lista, con:
   `cantidad = ceil(max(1, puntoReorden × 2 − stockTotal))`.
5. Publica el resumen del panel.
6. Registra una salida de éxito o error en la ejecución de n8n.

Para la demostración se permite ejecutar manualmente ese mismo flujo, sin cambiar su
cronograma.

---

## 10. Dashboard web (`frontend/`)

HTML/CSS/JS sin framework, independiente de `src/main/resources/static/`. Debe:

- Mostrar los cuatro indicadores, movimientos de ayer y ocupación por bodega.
- Mostrar narrativa, alertas y acciones del último resumen publicado.
- Mostrar productos en riesgo y órdenes en `BORRADOR`.
- Permitir generar y visualizar el PDF de una orden en `BORRADOR` (con la marca de
  agua diagonal visible).
- Reutilizar el login JWT del proyecto existente (mismo endpoint
  `POST /api/auth/login`).
- Guardar el JWT **solo** en `sessionStorage` (nunca `localStorage`, a diferencia del
  dashboard anterior).
- Mostrar el botón "Aprobar" solo si el usuario autenticado es `ADMIN`.
- Refrescar la tabla de órdenes tras aprobar, sin recargar la página.

No se califican animaciones, interfaz móvil ni diseño avanzado; sí se califica
legibilidad y consumo de endpoints reales.

Nota técnica: `GET /api/ordenes/{id}/pdf` requiere el header `Authorization`, por lo
que no puede servirse con un `<a href>` directo — se resuelve con `fetch` + `Blob` +
`URL.createObjectURL(blob)`.

---

## 11. Pruebas obligatorias (mínimo exigido)

Deben escribirse **antes** de implementar las reglas nuevas:

1. Consumo `0` → cobertura `null` y estado `SIN_CONSUMO`.
2. Stock igual al punto de reorden → no está en riesgo.
3. Cantidad `0` o negativa al crear orden → `400`.
4. Orden cancelada → no se puede aprobar (`400`).
5. Orden aprobada → recibida genera un movimiento de entrada.
6. `AGENTE` intenta aprobar → `403`.
7. Resumen con severidad inválida o id inexistente → `400`, y se conserva el resumen
   anterior.
8. PDF de una orden `BORRADOR`: se guarda y contiene la marca de agua; al cambiar el
   estado, deja de estar disponible hasta regenerarlo.

Más al menos **una** prueba de integración (`@SpringBootTest` + `MockMvc` o
`TestRestTemplate`) para `PATCH /api/ordenes/{id}/estado` o
`POST /api/panel/resumen`.

---

## Supuestos y decisiones (trazabilidad)

Estas decisiones resuelven ambigüedades del enunciado. Se documentan aquí para que
cualquier revisor entienda por qué el código hace lo que hace:

1. **Base de datos:** se mantiene PostgreSQL/Supabase (no MySQL, aunque el diagrama
   de referencia del enunciado lo menciona). Justificación: el proyecto anterior ya
   corre sobre Supabase y migrar de motor no aporta valor al alcance funcional
   pedido; el diagrama se documenta igualmente como
   `n8n → MCP → API Spring Boot → PostgreSQL (Supabase) → Dashboard`.
2. **`frontend/`** es una carpeta nueva en la raíz del repo, hermana de `src/`,
   distinta de `src/main/resources/static/` (que se conserva para el dashboard viejo).
   El nuevo frontend habla con la misma API por HTTP.
3. **Prefijo `/api`** para todos los endpoints nuevos, consistente con el resto del
   proyecto.
4. **Rol `EMPLEADO` en lecturas nuevas:** el enunciado solo define AGENTE vs ADMIN
   explícitamente; se decide que `EMPLEADO` mantiene acceso de lectura a los
   indicadores nuevos (KPIs, riesgo, bodegas críticas, proveedores, órdenes, resumen)
   porque ya tenía acceso equivalente a bodegas/productos en el proyecto anterior, y
   se excluye de creación de órdenes y publicación de resúmenes (esas quedan para
   ADMIN/AGENTE, como pide el enunciado).
5. **`POST /api/movimientos`** (ya existente) se restringe explícitamente a
   `ADMIN`/`EMPLEADO`; `AGENTE` queda fuera incluso aunque hoy caería en el
   catch-all `authenticated()`.
6. **PDF guardado como `bytea`** en la propia fila de `ordenes_compra`, sin `@Lob`,
   para evitar el mapeo a Large Object (OID) de Hibernate 6 + PostgreSQL contra
   Supabase.
7. **Producto sin consumo en 30 días** no puede calificar como "en riesgo" (no hay
   forma de calcular un punto de reorden sin consumo); se marca `SIN_CONSUMO` y se
   excluye de la lista de riesgo, aunque su stock total pueda seguir contando como
   "en quiebre" si es `0`.
8. **Criterio de "último resumen válido"** para `GET /api/panel/resumen`: dado que
   solo puede existir un `ResumenPanel` por fecha (`fecha` es `unique`), se expone el
   resumen de la fecha más reciente existente en la tabla (`MAX(fecha)`), no
   necesariamente el de "hoy" — así, si aún no se publicó el de hoy, el dashboard
   sigue mostrando el último disponible en vez de un 404 innecesario.
9. **Auditoría manual, no `AuditEntityListener`:** el listener genérico existe en el
   proyecto pero nunca fue enganchado a ninguna entidad (`@EntityListeners` no está
   declarado en ningún modelo). Toda la auditoría real hoy sale del patrón manual
   `guardarAuditoria(...)` dentro de cada `ServiceImpl`. Para `OrdenCompra` y
   `ResumenPanel` se sigue ese mismo patrón probado; no se activa el listener genérico
   en este alcance.
10. **Reutilización de `registrarMovimiento`** para la recepción de una orden: se
    invoca el servicio existente (`MovimientoInventarioService`, ya `@Transactional`)
    desde dentro de `OrdenCompraServiceImpl.cambiarEstado(...)` (también
    `@Transactional`), aprovechando la propagación `REQUIRED` de Spring para lograr
    la atomicidad exigida sin duplicar lógica de stock.
