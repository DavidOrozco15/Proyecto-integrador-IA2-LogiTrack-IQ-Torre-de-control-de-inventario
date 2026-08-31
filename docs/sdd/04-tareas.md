# 04 — Tareas

Checklist de trabajo, organizado por seccion. Marcar `[x]` a medida que se avanza.
El orden sugerido respeta la dependencia entre partes (modelo antes que servicios,
tests antes que implementacion, backend antes que MCP/n8n/frontend).

## 0. Preparacion y decisiones

- [x] Fijar decisiones abiertas del enunciado (permisos por defecto, prefijo `/api`,
      base de datos PostgreSQL/Supabase, almacenamiento del PDF) — documentado en
      `02-especificacion.md`, seccion "Supuestos y decisiones".
- [x] Confirmar los tres mensajes de commit obligatorios y su orden exacto:
      1. `docs: define LogiTrack IQ scope`
      2. `test: define reorder and order-state rules`
      3. `feat: implement LogiTrack IQ rules`
- [x] Crear `docs/sdd/01-propuesta.md`
- [x] Crear `docs/sdd/02-especificacion.md`
- [x] Crear `docs/sdd/03-diseno.md`
- [x] Crear `docs/sdd/04-tareas.md`
- [x] Commit: `docs: define LogiTrack IQ scope`

## 1. Modelo de datos nuevo

- [x] Crear enum `Rol.AGENTE` (extender enum existente)
- [x] Crear entidad `Proveedor` (id, nombre, contacto, diasEntrega)
- [x] Crear enum `EstadoOrdenCompra` (BORRADOR, APROBADA, RECIBIDA, CANCELADA)
- [x] Crear entidad `OrdenCompra` con todos sus campos y `@PrePersist`
- [x] Crear enums `SeveridadAlerta` y `TipoAccionSugerida`
- [x] Crear entidad `ResumenPanel` (fecha `unique`, `contenidoJson` TEXT)
- [x] Agregar `Producto.proveedorPrincipal` (ManyToOne LAZY, opcional)
- [x] Crear `ProveedorRepository`, `OrdenCompraRepository` (con `findByEstado`),
      `ResumenPanelRepository` (con `findByFecha`)
- [x] Crear `MovimientoDetalleRepository`
- [x] Actualizar `schema.sql`: tablas `proveedores`, `ordenes_compra`,
      `resumenes_panel`, `ALTER TABLE productos ADD COLUMN proveedor_principal_id`
- [x] Actualizar `data.sql`: proveedores de prueba, asignar `proveedorPrincipal` a
      2-3 productos, sembrar un usuario con rol `AGENTE`

## 2. Reglas base de inventario y zona horaria

- [x] Crear `com.logitrack.util.FechasBogota` (hoy, ayer, ventana 30 dias)
- [x] Agregar `spring.jackson.time-zone=America/Bogota` a `application.properties`
- [x] Verificar que ningun calculo nuevo use `Producto.stock` como fuente

## 3. Indicadores (KPIs), riesgo y bodegas criticas

- [x] Agregar `sumSalidasPorProductoEnRango` a `MovimientoDetalleRepository`
- [x] Crear DTOs: `KpiResponseDTO`, `OcupacionBodegaDTO`, `OrdenesPorAprobarDTO`,
      `MovimientosAyerDTO`, `ProductoRiesgoDTO`, `BodegaCriticaDTO`
- [x] Crear `InventarioAnalyticsService` / `InventarioAnalyticsServiceImpl`
  - [x] `calcularKpis()` con las 4 tarjetas + movimientos de ayer
  - [x] `obtenerProductosEnRiesgo()` con reglas de consumo/reorden/cobertura
  - [x] `obtenerBodegasCriticas()` (ocupacion >= 90%)
- [x] Crear `KpiController` (`GET /api/kpis`)
- [x] Agregar a `ProductoController`: `GET /api/productos/{id}/stock`,
      `GET /api/productos/riesgo`
- [x] Agregar a `BodegaController`: `GET /api/bodegas/criticas`

## 4. Estados de la orden y recepcion transaccional

- [x] Crear `OrdenCompraService` / `OrdenCompraServiceImpl`
  - [x] `crear(orden)`: valida cantidad > 0, calcula `total` en servidor, estado
        inicial `BORRADOR`, auditoria INSERT
  - [x] `cambiarEstado(id, nuevoEstado)`: valida transicion contra la tabla fija,
        genera movimiento `ENTRADA` transaccional al pasar a `RECIBIDA`, limpia PDF
        en cualquier transicion, auditoria UPDATE
- [x] Crear `CambiarEstadoRequest` DTO
- [x] Crear `OrdenCompraController`
      (`GET /api/ordenes`, `POST /api/ordenes`, `GET /api/ordenes/{id}`,
      `PATCH /api/ordenes/{id}/estado` con `@PreAuthorize("hasRole('ADMIN')")`)

## 5. Resto de endpoints REST nuevos

- [x] Crear `ProveedorController` (`GET /api/proveedores`)
- [x] Confirmar rutas del PDF (seccion 6) y del panel (seccion 7) quedan expuestas

## 6. PDF de la orden con marca de agua

- [x] Agregar dependencia `org.apache.pdfbox:pdfbox` al `pom.xml`
- [x] Crear `PdfOrdenService` / `PdfOrdenServiceImpl.generarPdf(orden)`
  - [x] Contenido: numero de orden, fecha, proveedor, producto, cantidad,
        precio unitario, total, bodega destino, estado
  - [x] Marca de agua "BORRADOR" (45 grados, opacidad baja) solo si `estado == BORRADOR`
- [x] `POST /api/ordenes/{id}/pdf`: genera, guarda `pdfBytes`/`pdfGeneradoEn`
      (reemplaza automaticamente el anterior)
- [x] `GET /api/ordenes/{id}/pdf`: `404` si no existe; si existe, responde
      `application/pdf` con `Content-Disposition: inline`
- [x] Confirmar `pdfBytes` mapeado como `bytea` **sin** `@Lob`

## 7. Contrato de `POST /panel/resumen`

- [x] Crear DTOs `AlertaDTO` (con `@AssertTrue isAlMenosUnIdentificador`),
      `AccionSugeridaDTO` (con `@AssertTrue isExactamenteUnIdentificador`),
      `ResumenPanelRequest` (`@JsonIgnoreProperties(ignoreUnknown = false)`)
- [x] Crear `PanelResumenService` / `PanelResumenServiceImpl`
  - [x] `publicar(request)`: valida existencia de IDs referenciados, serializa,
        crea o reemplaza (UPDATE) segun `findByFecha`, auditoria INSERT/UPDATE,
        todo `@Transactional`
  - [x] `obtenerUltimoValido()`: devuelve el de fecha mas reciente, `404` si no hay
        ninguno
- [x] Crear `PanelResumenController`
      (`POST /api/panel/resumen`, `GET /api/panel/resumen`)
- [x] Agregar `@ExceptionHandler(HttpMessageNotReadableException.class)` a
      `GlobalExceptionHandler` -> `400` con mensaje claro

## 8. Seguridad: rol AGENTE y permisos nuevos

- [x] Extender `SecurityConfig.securityFilterChain` con las reglas de la tabla de
      permisos (seccion 7 de `02-especificacion.md`), antes del catch-all
- [x] Restringir `POST /api/movimientos` explicitamente a `ADMIN`/`EMPLEADO`
- [x] Forzar `rol = EMPLEADO` en `POST /api/auth/register` (cerrar brecha de
      auto-asignacion de rol)
- [x] Agregar bean `CorsConfigurationSource` para permitir el origen del `frontend/`
      nuevo (GET/POST/PATCH, header `Authorization`)

## 9. Servidor MCP (6 herramientas exactas)

- [x] Inicializar proyecto en `mcp-server/` (Python/FastAPI)
- [x] Login contra `POST /api/auth/login` con usuario `AGENTE` (env vars), cache de
      JWT, reintento unico ante `401`
- [x] Exponer servidor MCP por HTTP en puerto 8081
- [x] Registrar las 6 tools requeridas + 3 tools extra
- [x] Logging de cada llamada (tool, input, status de respuesta) como evidencia
- [x] `mcp-server/evidencia-herramientas.md` con evidencia de tools

## 10. Skill y flujo n8n

- [x] Crear `skills/operacion-logitrack/SKILL.md`
- [x] Armar el flujo en n8n: `Schedule Trigger` (6:00 a.m. `America/Bogota`) ->
      `AI Agent` (tools MCP + system message basado en la skill) -> nodo final de
      exito/error
- [x] Exportar `n8n/resumen-diario-inventario.json`
- [x] Capturar evidencia de una ejecucion exitosa y una de error controlado

## 11. Dashboard nuevo (`frontend/`)

- [x] Crear `frontend/login.html` (login) y `frontend/dashboard.html` (panel)
- [x] Crear `frontend/app.js`: JWT en `sessionStorage`, `apiFetch`, helper de
      PDF como blob
- [x] Crear `frontend/styles.css` (dark theme, glassmorphism, responsive)
- [x] Verificar CORS funcionando end-to-end contra el backend real

## 12. Pruebas obligatorias (TDD)

- [x] Escribir tests **antes** de implementar las reglas de las secciones 3, 4, 6 y 7
- [x] Unit: consumo 0 -> `SIN_CONSUMO`, no aparece en riesgo
- [x] Unit: stock == punto de reorden -> no esta en riesgo
- [x] Unit: cantidad 0 / negativa al crear orden -> `400`
- [x] Unit: transicion invalida (p. ej. CANCELADA -> APROBADA) -> `400`
- [x] Unit: APROBADA -> RECIBIDA -> se invoca `registrarMovimiento` con los datos
      correctos (Mockito `ArgumentCaptor`)
- [x] Seguridad: `AGENTE` intenta `PATCH /api/ordenes/{id}/estado` -> `403`
- [x] Unit: resumen con severidad invalida / ID inexistente -> `400`, no se guarda
      (verificar que `save()` nunca se llama)
- [x] Integracion: `POST /api/ordenes/{id}/pdf` (BORRADOR) -> PDF contiene
      "BORRADOR" (extraido con `PDFTextStripper`) -> `PATCH` a `CANCELADA` ->
      `GET .../pdf` responde `404`
- [x] Integracion: `POST /api/panel/resumen` con payload valido -> `GET` devuelve el
      mismo contenido
- [x] Ejecutar `mvn test` en rojo (antes de implementar) y guardar la salida como
      evidencia
- [x] Commit: `test: define reorder and order-state rules`

## 13. Implementacion final y cierre

- [x] Implementar todo lo pendiente de las secciones 1-11 hasta que los tests pasen
- [x] Ejecutar `mvn test` en verde y guardar la salida como evidencia
- [x] Commit: `feat: implement LogiTrack IQ rules`
- [x] Completar `docs/sdd/evidencia-sdd.md` (enlaces, tabla regla->prueba, hashes de
      los 3 commits, evidencia roja/verde, reflexion <=150 palabras)
- [x] Actualizar `README.md`: instalacion, ejecucion, usuarios de prueba (incluye
      `AGENTE`), rutas principales nuevas
- [x] Verificar Swagger/OpenAPI muestra los endpoints nuevos y capturar evidencia de
      `401`/`403` en endpoints protegidos
- [x] Preparar diagrama final `n8n -> MCP -> API Spring Boot -> PostgreSQL (Supabase) ->
      Dashboard`
- [ ] Grabar video de 4-6 minutos (sin mostrar/explicar codigo): datos iniciales ->
      ejecucion del flujo n8n -> producto en riesgo -> orden BORRADOR -> aprobacion
      ADMIN -> recepcion -> movimiento ENTRADA -> dashboard actualizado
