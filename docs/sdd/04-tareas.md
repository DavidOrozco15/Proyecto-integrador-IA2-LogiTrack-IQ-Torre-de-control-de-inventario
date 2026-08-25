# 04 — Tareas

Checklist de trabajo, organizado por sección. Marcar `[x]` a medida que se avanza.
El orden sugerido respeta la dependencia entre partes (modelo antes que servicios,
tests antes que implementación, backend antes que MCP/n8n/frontend).

## 0. Preparación y decisiones

- [x] Fijar decisiones abiertas del enunciado (permisos por defecto, prefijo `/api`,
      base de datos PostgreSQL/Supabase, almacenamiento del PDF) — documentado en
      `02-especificacion.md`, sección "Supuestos y decisiones".
- [ ] Confirmar los tres mensajes de commit obligatorios y su orden exacto:
      1. `docs: define LogiTrack IQ scope`
      2. `test: define reorder and order-state rules`
      3. `feat: implement LogiTrack IQ rules`
- [x] Crear `docs/sdd/01-propuesta.md`
- [x] Crear `docs/sdd/02-especificacion.md`
- [x] Crear `docs/sdd/03-diseno.md`
- [x] Crear `docs/sdd/04-tareas.md`
- [ ] Commit: `docs: define LogiTrack IQ scope`

## 1. Modelo de datos nuevo

- [ ] Crear enum `Rol.AGENTE` (extender enum existente)
- [ ] Crear entidad `Proveedor` (id, nombre, contacto, diasEntrega)
- [ ] Crear enum `EstadoOrdenCompra` (BORRADOR, APROBADA, RECIBIDA, CANCELADA)
- [ ] Crear entidad `OrdenCompra` con todos sus campos y `@PrePersist`
- [ ] Crear enums `SeveridadAlerta` y `TipoAccionSugerida`
- [ ] Crear entidad `ResumenPanel` (fecha `unique`, `contenidoJson` TEXT)
- [ ] Agregar `Producto.proveedorPrincipal` (ManyToOne LAZY, opcional)
- [ ] Crear `ProveedorRepository`, `OrdenCompraRepository` (con `findByEstado`),
      `ResumenPanelRepository` (con `findByFecha`)
- [ ] Crear `MovimientoDetalleRepository`
- [ ] Actualizar `schema.sql`: tablas `proveedores`, `ordenes_compra`,
      `resumenes_panel`, `ALTER TABLE productos ADD COLUMN proveedor_principal_id`
- [ ] Actualizar `data.sql`: proveedores de prueba, asignar `proveedorPrincipal` a
      2–3 productos, sembrar un usuario con rol `AGENTE`

## 2. Reglas base de inventario y zona horaria

- [ ] Crear `com.logitrack.util.FechasBogota` (hoy, ayer, ventana 30 días)
- [ ] Agregar `spring.jackson.time-zone=America/Bogota` a `application.properties`
- [ ] Verificar que ningún cálculo nuevo use `Producto.stock` como fuente

## 3. Indicadores (KPIs), riesgo y bodegas críticas

- [ ] Agregar `sumSalidasPorProductoEnRango` a `MovimientoDetalleRepository`
- [ ] Crear DTOs: `KpiResponseDTO`, `OcupacionBodegaDTO`, `OrdenesPorAprobarDTO`,
      `MovimientosAyerDTO`, `ProductoRiesgoDTO`, `BodegaCriticaDTO`
- [ ] Crear `InventarioAnalyticsService` / `InventarioAnalyticsServiceImpl`
  - [ ] `calcularKpis()` con las 4 tarjetas + movimientos de ayer
  - [ ] `obtenerProductosEnRiesgo()` con reglas de consumo/reorden/cobertura
  - [ ] `obtenerBodegasCriticas()` (ocupación ≥ 90 %)
- [ ] Crear `KpiController` (`GET /api/kpis`)
- [ ] Agregar a `ProductoController`: `GET /api/productos/{id}/stock`,
      `GET /api/productos/riesgo`
- [ ] Agregar a `BodegaController`: `GET /api/bodegas/criticas`

## 4. Estados de la orden y recepción transaccional

- [ ] Crear `OrdenCompraService` / `OrdenCompraServiceImpl`
  - [ ] `crear(orden)`: valida cantidad > 0, calcula `total` en servidor, estado
        inicial `BORRADOR`, auditoría INSERT
  - [ ] `cambiarEstado(id, nuevoEstado)`: valida transición contra la tabla fija,
        genera movimiento `ENTRADA` transaccional al pasar a `RECIBIDA`, limpia PDF
        en cualquier transición, auditoría UPDATE
- [ ] Crear `CambiarEstadoRequest` DTO
- [ ] Crear `OrdenCompraController`
      (`GET /api/ordenes`, `POST /api/ordenes`, `GET /api/ordenes/{id}`,
      `PATCH /api/ordenes/{id}/estado` con `@PreAuthorize("hasRole('ADMIN')")`)

## 5. Resto de endpoints REST nuevos

- [ ] Crear `ProveedorController` (`GET /api/proveedores`)
- [ ] Confirmar rutas del PDF (sección 6) y del panel (sección 7) quedan expuestas

## 6. PDF de la orden con marca de agua

- [ ] Agregar dependencia `org.apache.pdfbox:pdfbox` al `pom.xml`
- [ ] Crear `PdfOrdenService` / `PdfOrdenServiceImpl.generarPdf(orden)`
  - [ ] Contenido: número de orden, fecha, proveedor, producto, cantidad,
        precio unitario, total, bodega destino, estado
  - [ ] Marca de agua "BORRADOR" (45°, opacidad baja) solo si `estado == BORRADOR`
- [ ] `POST /api/ordenes/{id}/pdf`: genera, guarda `pdfBytes`/`pdfGeneradoEn`
      (reemplaza automáticamente el anterior)
- [ ] `GET /api/ordenes/{id}/pdf`: `404` si no existe; si existe, responde
      `application/pdf` con `Content-Disposition: inline`
- [ ] Confirmar `pdfBytes` mapeado como `bytea` **sin** `@Lob`

## 7. Contrato de `POST /panel/resumen`

- [ ] Crear DTOs `AlertaDTO` (con `@AssertTrue isAlMenosUnIdentificador`),
      `AccionSugeridaDTO` (con `@AssertTrue isExactamenteUnIdentificador`),
      `ResumenPanelRequest` (`@JsonIgnoreProperties(ignoreUnknown = false)`)
- [ ] Crear `PanelResumenService` / `PanelResumenServiceImpl`
  - [ ] `publicar(request)`: valida existencia de IDs referenciados, serializa,
        crea o reemplaza (UPDATE) según `findByFecha`, auditoría INSERT/UPDATE,
        todo `@Transactional`
  - [ ] `obtenerUltimoValido()`: devuelve el de fecha más reciente, `404` si no hay
        ninguno
- [ ] Crear `PanelResumenController`
      (`POST /api/panel/resumen`, `GET /api/panel/resumen`)
- [ ] Agregar `@ExceptionHandler(HttpMessageNotReadableException.class)` a
      `GlobalExceptionHandler` → `400` con mensaje claro (hoy cae en el genérico 500)

## 8. Seguridad: rol AGENTE y permisos nuevos

- [ ] Extender `SecurityConfig.securityFilterChain` con las reglas de la tabla de
      permisos (sección 7 de `02-especificacion.md`), antes del catch-all
- [ ] Restringir `POST /api/movimientos` explícitamente a `ADMIN`/`EMPLEADO`
- [ ] Forzar `rol = EMPLEADO` en `POST /api/auth/register` (cerrar brecha de
      auto-asignación de rol)
- [ ] Agregar bean `CorsConfigurationSource` para permitir el origen del `frontend/`
      nuevo (GET/POST/PATCH, header `Authorization`)

## 9. Servidor MCP (6 herramientas exactas)

- [ ] Inicializar proyecto Node.js/TypeScript en `mcp-server/`
      (`@modelcontextprotocol/sdk`)
- [ ] Login contra `POST /api/auth/login` con usuario `AGENTE` (env vars), cache de
      JWT, reintento único ante `401`
- [ ] Exponer servidor MCP por HTTP/SSE en `MCP_PORT` (default 3939)
- [ ] Registrar exactamente las 6 tools (sin ninguna adicional, sin tool de aprobar)
- [ ] Logging de cada llamada (tool, input, status de respuesta) como evidencia
- [ ] `mcp-server/README.md` con instalación, variables de entorno y ejecución

## 10. Skill y flujo n8n

- [ ] Crear `skills/operacion-logitrack/SKILL.md`
- [ ] Armar el flujo en n8n: `Schedule Trigger` (6:00 a.m. `America/Bogota`) →
      `AI Agent` (tools MCP + system message basado en la skill) → nodo final de
      éxito/error
- [ ] Exportar `n8n/resumen-diario-inventario.json`
- [ ] Capturar evidencia de una ejecución exitosa y una de error controlado

## 11. Dashboard nuevo (`frontend/`)

- [ ] Crear `frontend/index.html` (login) y `frontend/dashboard.html` (panel)
- [ ] Crear `frontend/js/api.js`: JWT en `sessionStorage`, `apiFetch`, helper de
      PDF como blob
- [ ] Crear `frontend/js/dashboard.js`: KPIs, resumen, productos en riesgo, órdenes
      en `BORRADOR`, generar/ver PDF, botón "Aprobar" (solo ADMIN), refresco tras
      aprobar
- [ ] Crear `frontend/css/styles.css` (básico, legible, sin animaciones complejas)
- [ ] Verificar CORS funcionando end-to-end contra el backend real

## 12. Pruebas obligatorias (TDD)

- [ ] Escribir tests **antes** de implementar las reglas de las secciones 3, 4, 6 y 7
- [ ] Unit: consumo 0 → `SIN_CONSUMO`, no aparece en riesgo
- [ ] Unit: stock == punto de reorden → no está en riesgo
- [ ] Unit: cantidad 0 / negativa al crear orden → `400`
- [ ] Unit: transición inválida (p. ej. CANCELADA → APROBADA) → `400`
- [ ] Unit: APROBADA → RECIBIDA → se invoca `registrarMovimiento` con los datos
      correctos (Mockito `ArgumentCaptor`)
- [ ] Seguridad: `AGENTE` intenta `PATCH /api/ordenes/{id}/estado` → `403`
- [ ] Unit: resumen con severidad inválida / ID inexistente → `400`, no se guarda
      (verificar que `save()` nunca se llama)
- [ ] Integración: `POST /api/ordenes/{id}/pdf` (BORRADOR) → PDF contiene
      "BORRADOR" (extraído con `PDFTextStripper`) → `PATCH` a `CANCELADA` →
      `GET .../pdf` responde `404`
- [ ] Integración: `POST /api/panel/resumen` con payload válido → `GET` devuelve el
      mismo contenido
- [ ] Ejecutar `mvn test` en rojo (antes de implementar) y guardar la salida como
      evidencia
- [ ] Commit: `test: define reorder and order-state rules`

## 13. Implementación final y cierre

- [ ] Implementar todo lo pendiente de las secciones 1–11 hasta que los tests pasen
- [ ] Ejecutar `mvn test` en verde y guardar la salida como evidencia
- [ ] Commit: `feat: implement LogiTrack IQ rules`
- [ ] Completar `docs/sdd/evidencia-sdd.md` (enlaces, tabla regla→prueba, hashes de
      los 3 commits, evidencia roja/verde, reflexión ≤150 palabras)
- [ ] Actualizar `README.md`: instalación, ejecución, usuarios de prueba (incluye
      `AGENTE`), rutas principales nuevas
- [ ] Verificar Swagger/OpenAPI muestra los endpoints nuevos y capturar evidencia de
      `401`/`403` en endpoints protegidos
- [ ] Preparar diagrama final `n8n → MCP → API Spring Boot → PostgreSQL (Supabase) →
      Dashboard`
- [ ] Grabar video de 4–6 minutos (sin mostrar/explicar código): datos iniciales →
      ejecución del flujo n8n → producto en riesgo → orden BORRADOR → aprobación
      ADMIN → recepción → movimiento ENTRADA → dashboard actualizado
