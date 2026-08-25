# 01 — Propuesta: LogiTrack IQ (Torre de control de inventario)

## Contexto

LogiTrack S.A. ya cuenta con un backend en Spring Boot (Java 17, PostgreSQL/Supabase,
schema `proyecto`) que administra bodegas, productos, movimientos de inventario,
usuarios y auditoría. Ese sistema funciona, pero se revisa de forma manual: nadie recibe
una alerta cuando un producto está por agotarse, y preparar una compra depende de que
alguien note el problema a tiempo.

Este proyecto **extiende** ese backend — no lo reemplaza ni crea uno independiente —
para convertirlo en una torre de control que detecta riesgo de inventario, propone una
compra, la lleva a través de un ciclo de aprobación y recepción, y expone todo en un
dashboard nuevo. Un flujo de automatización en n8n consulta el sistema mediante un
servidor MCP y publica un resumen diario estructurado para un administrador humano.

## Problema

- El stock disponible solo se conoce revisando manualmente movimientos y bodegas.
- No existe ningún mecanismo que detecte que un producto está por debajo de un nivel
  seguro de inventario.
- No hay un flujo formal para proponer, aprobar y recibir una compra de reposición.
- No hay una vista diaria consolidada (KPIs, alertas, acciones) que un administrador
  pueda revisar en segundos.

## Objetivo general

Integrar Spring Boot, pruebas automatizadas, SDD (Specification-Driven Development),
un servidor MCP, una *skill* operativa y un flujo n8n en una solución pequeña que:

1. Calcule el inventario real a partir de los movimientos ya registrados (no de
   `Producto.stock`).
2. Detecte productos cuyo stock está por debajo de su punto de reorden.
3. Permita que un flujo diario automatizado (n8n → MCP) cree, como máximo, una orden
   de compra en estado `BORRADOR` por ejecución.
4. Permita que un `ADMIN` revise esa orden en un dashboard y la apruebe.
5. Al recibir la orden, registre automáticamente y de forma transaccional un
   movimiento `ENTRADA` en la bodega indicada.
6. Refleje en el dashboard los indicadores, las alertas, las órdenes pendientes y el
   inventario actualizado.

En una frase: el sistema detecta un faltante, prepara una compra, permite recibirla y
demuestra —con datos reales— que el inventario quedó actualizado.

## Qué se debe poder demostrar al finalizar

Un flujo completo de principio a fin, con datos reales:

1. Existe (o se provoca) un producto en riesgo.
2. n8n consulta el backend a través de MCP y crea una orden en `BORRADOR`.
3. Un `ADMIN` aprueba esa orden desde el dashboard.
4. El `ADMIN` la recibe → se crea automáticamente un movimiento `ENTRADA`.
5. El dashboard refleja la orden recibida y el inventario actualizado.

## Alcance (dentro de esta entrega)

- Modelo nuevo: `Proveedor`, `OrdenCompra` (con máquina de estados), `ResumenPanel`,
  relación opcional `Producto.proveedorPrincipal`, rol `AGENTE`.
- Cálculo de KPIs, productos en riesgo y bodegas críticas, siempre a partir de
  movimientos/inventario por bodega, nunca de `Producto.stock`.
- Ciclo de vida completo de una orden de compra: creación en `BORRADOR`, transición de
  estados con reglas fijas, recepción transaccional que genera un movimiento `ENTRADA`.
- Generación y consulta de un PDF por orden, con marca de agua diagonal `BORRADOR`
  cuando corresponda, que se invalida al cambiar de estado.
- Endpoint `POST /api/panel/resumen` con contrato JSON estricto (sin propiedades
  adicionales) y `GET /api/panel/resumen` para el último resumen publicado.
- Seguridad: nuevo rol `AGENTE` con permisos acotados, reutilizando JWT/usuarios/
  auditoría ya existentes.
- Servidor MCP independiente con exactamente 6 herramientas, autenticado como un
  usuario `AGENTE`, sin acceso directo a la base de datos ni herramienta de aprobación.
- `skills/operacion-logitrack/SKILL.md` con las reglas mínimas de operación del agente.
- Un flujo n8n único ("Resumen diario de inventario") que ejecuta el ciclo completo de
  consulta → creación de orden (máx. 1) → publicación de resumen → registro de éxito/error.
- Dashboard nuevo en `frontend/` (HTML/CSS/JS sin framework), con JWT solo en
  `sessionStorage`, que consume la API real y permite generar/ver el PDF de una orden.
- Pruebas automatizadas escritas **antes** de implementar las reglas nuevas (TDD),
  incluyendo al menos una prueba de integración.
- Documentación SDD completa (`docs/sdd/`) con trazabilidad regla → prueba y evidencia
  de commits.

## Fuera de alcance (explícitamente, para esta entrega)

- Aprobación o recepción automática de órdenes por el agente/n8n: **prohibido**, no
  existe ni debe existir una herramienta MCP para eso.
- Más de un resumen de panel válido por fecha (una nueva publicación reemplaza a la
  anterior, no coexisten varias).
- Edición de una orden de compra ya creada (solo se permite cambiar su estado).
- Notificaciones externas (correo, SMS, Slack, etc.) — el resumen del panel se consulta
  desde el dashboard, no se empuja a otros canales.
- Diseño visual avanzado, animaciones o versión móvil del dashboard nuevo (no se
  califica; sí se califica legibilidad y consumo de endpoints reales).
- Migración a MySQL: el proyecto se mantiene sobre PostgreSQL/Supabase (ver decisión
  explícita en `02-especificacion.md`).
- Multi-tenant, múltiples proveedores por producto, o reglas de negocio adicionales no
  descritas en el enunciado.

## Decisiones explícitas fijadas antes de implementar

Estas decisiones resuelven puntos que el enunciado deja abiertos a propósito. Se
detallan con su justificación en `02-especificacion.md`, sección "Supuestos y
decisiones":

1. Base de datos: se continúa sobre PostgreSQL/Supabase (no MySQL).
2. `frontend/` es una carpeta nueva en la raíz del repo, independiente de
   `src/main/resources/static/` (que se mantiene intacta).
3. Prefijo de rutas: todos los endpoints nuevos usan el prefijo `/api`, igual que el
   resto del proyecto.
4. Permisos por defecto no explícitos en el enunciado (lecturas nuevas vs. escrituras)
   se documentan como tabla fija.
5. El PDF de la orden se guarda como `bytea` en la propia fila de `ordenes_compra`
   (sin `@Lob`), no en disco.
6. Un producto sin consumo en los últimos 30 días no puede calificar como "en riesgo"
   (no hay forma de calcular su punto de reorden); se marca `SIN_CONSUMO`.

## Glosario breve

| Término | Significado en este proyecto |
|---|---|
| Orden de compra | Registro que propone comprar un producto para una bodega. No es un PDF por sí misma. |
| BORRADOR | Estado inicial de una orden: existe en base de datos pero no ha sido aprobada ni recibida. |
| PDF de la orden | Documento generado desde una orden guardada; si la orden está en BORRADOR, lleva marca de agua diagonal "BORRADOR". |
| Punto de reorden | Nivel de inventario que decide si un producto aparece como "en riesgo". |
| MCP | Capa que permite que n8n consulte o use funciones limitadas del backend mediante herramientas. |
| Skill | Archivo de instrucciones operativas que indica qué puede y qué no puede hacer el flujo automatizado. |
| Resumen del panel | Contenido estructurado publicado por el flujo para que el dashboard muestre narrativa, alertas y acciones. |
