# 03 — Diseño: entidades, decisiones técnicas y diagrama

## 1. Diagrama general del flujo

```
 ┌────────────────────┐        6:00 a.m. America/Bogota
 │   n8n (Schedule)    │──────────────┐
 └────────────────────┘               │
            │ AI Agent + Tools        │
            ▼                         │
 ┌────────────────────┐               │
 │   Servidor MCP      │  login AGENTE│
 │  (Node.js, 6 tools) │◄─────────────┘
 └─────────┬──────────┘
           │ HTTP + JWT (Bearer, rol AGENTE)
           ▼
 ┌───────────────────────────────────────────┐
 │        API Spring Boot (LogiTrack)         │
 │  /api/kpis  /api/productos/riesgo          │
 │  /api/bodegas/criticas  /api/proveedores   │
 │  /api/ordenes  /api/ordenes/{id}/pdf       │
 │  /api/panel/resumen                        │
 └─────────┬───────────────────────┬──────────┘
           │ JPA/Hibernate         │
           ▼                       │
 ┌────────────────────┐            │
 │ PostgreSQL (Supabase) │          │
 │  schema "proyecto"    │          │
 └────────────────────┘            │
                                    │ HTTP + JWT (sessionStorage)
                                    ▼
                          ┌────────────────────┐
                          │   Dashboard nuevo    │
                          │   frontend/ (HTML/   │
                          │   CSS/JS sin framework)│
                          └────────────────────┘
                                    ▲
                                    │ login/aprobar/recibir (ADMIN)
                          ┌────────────────────┐
                          │  Administrador (ADMIN) │
                          └────────────────────┘
```

Principio de diseño: **el backend es la única fuente de verdad**. Ni el dashboard, ni
el servidor MCP, ni n8n calculan stock, KPIs o reglas de negocio — todos consumen la
API REST existente/nueva.

---

## 2. Modelo de entidades (nuevas y modificadas)

```
Proveedor                    Producto (existente, extendido)
├─ id                        ├─ id
├─ nombre                    ├─ nombre
├─ contacto                  ├─ categoria
└─ diasEntrega                ├─ stock            (legado, NO fuente de verdad nueva)
        ▲                     ├─ precio
        │ 0..1                └─ proveedorPrincipal ──► Proveedor (opcional, LAZY)
        │
        │
OrdenCompra
├─ id
├─ producto        ──► Producto (EAGER, obligatorio)
├─ proveedor        ──► Proveedor (EAGER, obligatorio)
├─ bodegaDestino    ──► Bodega (EAGER, obligatoria)
├─ cantidad          (Integer, >=1)
├─ precioUnitario    (BigDecimal)
├─ total             (BigDecimal, calculado en servidor)
├─ fechaCreacion      (LocalDateTime, America/Bogota)
├─ estado             (EstadoOrdenCompra: BORRADOR|APROBADA|RECIBIDA|CANCELADA)
├─ creadoPor         ──► Usuario (EAGER, opcional)
├─ pdfBytes           (bytea, sin @Lob)
└─ pdfGeneradoEn      (LocalDateTime, opcional)

ResumenPanel
├─ id
├─ fecha              (LocalDate, UNIQUE)
├─ contenidoJson      (TEXT: narrativa + alertas[] + accionesSugeridas[] serializado)
├─ autor              ──► Usuario (EAGER, opcional)
└─ creadoEn           (LocalDateTime)

Rol (enum, extendido)
├─ ADMIN
├─ EMPLEADO
└─ AGENTE   (nuevo)
```

Entidades existentes reutilizadas sin cambios de esquema: `Bodega`, `Usuario`,
`MovimientoInventario`, `MovimientoDetalle`, `InventarioBodega`, `Auditoria`.

Relaciones clave con el modelo existente:

- `OrdenCompra.producto` / `.proveedor` / `.bodegaDestino` reutilizan las entidades
  `Producto`, `Proveedor` (nueva) y `Bodega` ya existentes.
- La recepción de una orden (`APROBADA → RECIBIDA`) construye un
  `MovimientoInventario` (`tipoMovimiento = ENTRADA`) con un único
  `MovimientoDetalle{producto, cantidad}` y lo delega a
  `MovimientoInventarioService.registrarMovimiento(...)`, que ya actualiza
  `InventarioBodega` y dispara la auditoría existente para el movimiento.

---

## 3. Componentes y responsabilidades nuevas (backend)

| Capa | Componente | Responsabilidad |
|---|---|---|
| Util | `com.logitrack.util.FechasBogota` | Fuente única de "hoy/ayer/ventana 30 días" en `America/Bogota` |
| Modelo | `Proveedor`, `OrdenCompra`, `ResumenPanel`, enums `EstadoOrdenCompra`, `SeveridadAlerta`, `TipoAccionSugerida` | Entidades JPA nuevas |
| Repo | `ProveedorRepository`, `OrdenCompraRepository`, `ResumenPanelRepository`, `MovimientoDetalleRepository` | Acceso a datos + queries de agregación |
| Servicio | `InventarioAnalyticsService` | KPIs, productos en riesgo, bodegas críticas (solo lectura) |
| Servicio | `OrdenCompraService` | Ciclo de vida de la orden: creación, transición de estados, recepción transaccional |
| Servicio | `PdfOrdenService` | Generación del PDF (PDFBox) con marca de agua condicional |
| Servicio | `PanelResumenService` | Validación de negocio + publicación/reemplazo del resumen |
| DTO | `KpiResponseDTO`, `ProductoRiesgoDTO`, `BodegaCriticaDTO`, `ResumenPanelRequest`, `AlertaDTO`, `AccionSugeridaDTO`, `CambiarEstadoRequest` | Contratos de entrada/salida |
| Controller | `KpiController`, `ProveedorController`, `OrdenCompraController`, `PanelResumenController` + extensiones a `ProductoController`/`BodegaController` | Exposición REST bajo `/api` |
| Seguridad | Extensión de `SecurityConfig` | Reglas de autorización por rol/endpoint (tabla en `02-especificacion.md`) |

Fuera del backend Spring Boot:

| Carpeta | Contenido |
|---|---|
| `frontend/` | Dashboard estático (HTML/CSS/JS), login JWT propio en `sessionStorage` |
| `mcp-server/` | Servidor MCP en Node.js/TypeScript, 6 tools, autenticado como `AGENTE` |
| `n8n/resumen-diario-inventario.json` | Export del flujo n8n |
| `skills/operacion-logitrack/SKILL.md` | Instrucciones operativas del agente |
| `docs/sdd/` | Documentación de especificación y evidencia |

---

## 4. Decisiones técnicas y su justificación

### 4.1 Cálculo de KPIs y riesgo: lectura, no side-effects
`InventarioAnalyticsService` es de solo lectura: reutiliza
`InventarioBodegaRepository.sumStockByProductoId/sumStockByBodegaId` (ya con
`COALESCE(...,0)`) como fuente de stock, en vez de recalcular sumando
`movimiento_detalles` desde cero. Esto evita duplicar lógica que ya está probada en
producción con el dashboard viejo.

### 4.2 Recepción transaccional reutilizando el servicio existente
`OrdenCompraServiceImpl.cambiarEstado(id, RECIBIDA)` construye el `MovimientoInventario`
y llama a `MovimientoInventarioService.registrarMovimiento(...)` **dentro** del mismo
método `@Transactional`. Como la propagación por defecto de Spring es `REQUIRED`,
ambas operaciones (actualizar la orden + crear el movimiento/actualizar inventario)
comparten una sola transacción: si `registrarMovimiento` lanza una excepción (p. ej.
capacidad de bodega excedida), todo se revierte, incluyendo el cambio de estado de la
orden. Esto satisface la exigencia de atomicidad sin escribir lógica de stock nueva.

### 4.3 PDF con PDFBox, sin `@Lob`
Se usa **Apache PDFBox** (licencia Apache 2.0) para generar el PDF en memoria
(`ByteArrayOutputStream`), sin escribir a disco. El campo `pdfBytes` se mapea como
`@Column(columnDefinition = "bytea")` **sin** `@Lob`: con Hibernate 6 + PostgreSQL,
`@Lob` sobre un `byte[]` puede intentar usar Large Objects (OID), lo que falla contra
Supabase. La marca de agua "BORRADOR" se dibuja con
`PDExtendedGraphicsState.setNonStrokingAlphaConstant(0.25f)`, fuente grande (~90pt),
rotada 45°, centrada.

### 4.4 Servidor MCP vía HTTP/SSE, no stdio
Como n8n necesita conectarse por red (no como proceso hijo local), el servidor MCP se
expone por HTTP con transporte SSE (o Streamable HTTP, según soporte del SDK) en un
puerto propio configurable (`MCP_PORT`, default `3939`). El servidor hace login contra
`POST /api/auth/login` con credenciales de un usuario `AGENTE` (vía variables de
entorno), cachea el JWT en memoria, y reintenta login una vez si recibe `401`.

### 4.5 Frontend nuevo: `sessionStorage`, no `localStorage`
A diferencia del dashboard existente (`localStorage`, claves `lt_token`/`lt_usuario`),
el dashboard nuevo guarda el JWT en `sessionStorage` bajo claves propias
(`lt_iq_token`, `lt_iq_usuario`) para no chocar entre ambos y cumplir el requisito
explícito de esta entrega. Al vivir en otro origen (otro puerto), el backend necesita
una configuración CORS nueva (`CorsConfigurationSource` + `http.cors(...)` en
`SecurityConfig`), que hoy no existe.

Para servir el PDF (que requiere el header `Authorization`), el frontend no puede usar
un `<a href>` directo: hace `fetch` con el header, convierte la respuesta a `Blob`, y
la muestra con `URL.createObjectURL(blob)` en un `<iframe>` o pestaña nueva.

### 4.6 Zona horaria centralizada
`FechasBogota` es la única fuente de "hoy", "ayer" y "ventana de 30 días", todas
basadas en `ZoneId.of("America/Bogota")`. Se agrega
`spring.jackson.time-zone=America/Bogota` a `application.properties`. Ningún servicio
nuevo debe usar `LocalDateTime.now()` sin zona explícita.

### 4.7 Auditoría: patrón manual, no el listener genérico
`AuditEntityListener` existe en el proyecto pero no está enganchado a ninguna entidad
(bug preexistente, fuera de este alcance). `OrdenCompraServiceImpl` y
`PanelResumenServiceImpl` siguen el mismo patrón manual `guardarAuditoria(...)` que ya
usan `BodegaServiceImpl`/`ProductoServiceImpl`/`MovimientoInventarioServiceImpl`
(serialización con Jackson + `JavaTimeModule`, guardado directo en
`AuditoriaRepository`).

---

## 5. Secuencia: recepción de una orden (caso crítico)

```
ADMIN               OrdenCompraController      OrdenCompraServiceImpl      MovimientoInventarioService      DB
  │  PATCH /ordenes/5/estado {RECIBIDA}                                                                     │
  ├──────────────────►│                                                                                     │
  │                   ├──────────────────►│  cambiarEstado(5, RECIBIDA)  [@Transactional]                  │
  │                   │                   ├── valida transición APROBADA→RECIBIDA (según tabla de estados)  │
  │                   │                   ├── construye MovimientoInventario ENTRADA (producto/cant/bodega) │
  │                   │                   ├──────────────────────────────►│ registrarMovimiento(mov)        │
  │                   │                   │                               ├── valida stock/capacidad        │
  │                   │                   │                               ├── actualiza InventarioBodega ───┼──►│
  │                   │                   │                               ├── guarda Movimiento ────────────┼──►│
  │                   │                   │                               └── auditoría movimiento ─────────┼──►│
  │                   │                   │◄──────────────────────────────┘ (misma transacción)             │
  │                   │                   ├── limpia pdfBytes/pdfGeneradoEn                                 │
  │                   │                   ├── guarda orden.estado = RECIBIDA ───────────────────────────────┼──►│
  │                   │                   └── auditoría UPDATE "OrdenCompra" ────────────────────────────────┼──►│
  │                   │◄──────────────────┘                                                                  │
  │◄──────────────────┘  200 OK (orden actualizada)                                                          │
```

Si `registrarMovimiento` falla (p. ej. la bodega destino queda sobre su capacidad),
la excepción se propaga y **toda** la transacción se revierte: la orden permanece en
`APROBADA` y no se crea ningún movimiento.

---

## 6. Estructura de carpetas de referencia

```
/ (raíz del repo)
├── src/                          # Backend existente, extendido (no reemplazado)
│   └── main/java/com/logitrack/
│       ├── model/                # + Proveedor, OrdenCompra, ResumenPanel, enums
│       ├── repository/           # + ProveedorRepository, OrdenCompraRepository, ...
│       ├── service/               # + InventarioAnalyticsService, OrdenCompraService, PdfOrdenService, PanelResumenService
│       ├── controller/            # + KpiController, ProveedorController, OrdenCompraController, PanelResumenController
│       ├── dto/                   # + DTOs de KPI/riesgo/resumen
│       ├── util/                  # + FechasBogota
│       └── security/              # SecurityConfig extendido (roles + CORS)
├── frontend/                     # Dashboard nuevo, independiente del static/ existente
│   ├── index.html
│   ├── dashboard.html
│   ├── css/styles.css
│   └── js/{api.js, dashboard.js}
├── mcp-server/                   # Servidor MCP (Node.js/TypeScript), 6 tools
├── n8n/
│   └── resumen-diario-inventario.json
├── skills/
│   └── operacion-logitrack/SKILL.md
└── docs/
    └── sdd/
        ├── 01-propuesta.md
        ├── 02-especificacion.md
        ├── 03-diseno.md
        ├── 04-tareas.md
        └── evidencia-sdd.md
```

---

## 7. Riesgos técnicos identificados y mitigación

| Riesgo | Mitigación |
|---|---|
| `@Lob` sobre `byte[]` falla contra Supabase (OID) | Usar `columnDefinition = "bytea"` sin `@Lob` |
| Zona horaria del servidor distinta a Bogotá afecta "hoy/ayer" | Centralizar en `FechasBogota`, fijar `spring.jackson.time-zone` |
| Recepción de orden deja datos inconsistentes si falla a mitad de camino | Reutilizar `@Transactional` + propagación `REQUIRED` |
| CORS bloquea el frontend nuevo (otro origen) | Bean `CorsConfigurationSource` explícito en `SecurityConfig` |
| JWT no se envía en navegación directa a `GET .../pdf` | `fetch` + `Blob` + `URL.createObjectURL` en el frontend |
| `AGENTE` podría heredar permisos de escritura no deseados vía el catch-all `authenticated()` | Reglas explícitas por método/ruta en `SecurityConfig`, antes del catch-all |
| Publicación de resumen a mitad de camino deja datos corruptos si un ID no existe | Validar existencia de IDs **antes** de serializar/guardar, todo dentro de `@Transactional` |
