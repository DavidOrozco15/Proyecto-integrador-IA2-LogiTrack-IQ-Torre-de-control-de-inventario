# Evidencia SDD - LogiTrack IQ

## Trazabilidad: Regla -> Prueba

| Regla (Seccion 02-especificacion.md) | Test | Archivo | Estado |
|---------------------------------------|------|---------|--------|
| Caso 1: Consumo 0 en 30 dias -> SIN_CONSUMO, no en riesgo | `productoSinConsumoEn30Dias_quedaSinConsumo_yNoEntraEnRiesgo` | InventarioAnalyticsServiceTest.java | PASS |
| Caso 2: Stock == punto de reorden -> NO en riesgo | `stockIgualAlPuntoDeReorden_noEstaEnRiesgo` | InventarioAnalyticsServiceTest.java | PASS |
| Caso 3: Cantidad 0 o negativa al crear orden -> 400 | `crearOrden_conCantidadCero_lanzaBadRequest`, `_conCantidadNegativa_` | OrdenCompraServiceTest.java | PASS |
| Caso 4: Transicion invalida (CANCELADA->APROBADA) -> 400 | `cambiarEstado_deCanceladaAAprobada_lanzaBadRequest` | OrdenCompraServiceTest.java | PASS |
| Caso 5: APROBADA->RECIBIDA genera movimiento ENTRADA | `cambiarEstado_deAprobadaARecibida_generaMovimientoEntradaConDatosCorrectos` | OrdenCompraServiceTest.java | PASS |
| Caso 6: AGENTE intenta cambiar estado -> 403 | `agenteIntentaCambiarEstadoDeUnaOrden_respondeForbidden` | OrdenCompraEstadoSecurityTest.java | PASS |
| Caso 7: Resumen con ID inexistente -> 400, no guarda | `publicar_conProductoIdInexistenteEnUnaAlerta_lanzaBadRequest_yNoGuardaNada`, `_conOrdenIdInexistente_` | PanelResumenServiceTest.java | PASS |
| Caso 8: PDF BORRADOR contiene marca de agua, desaparece al cancelar | `pdfDeOrdenBorrador_contieneMarcaDeAgua_yDesapareceAlCancelar` | OrdenCompraPdfIntegrationTest.java | PASS |
| Integracion: POST/GET resumen devuelve mismo contenido | `publicarResumenValido_yLuegoConsultarlo_devuelveElMismoContenido` | PanelResumenIntegrationTest.java | PASS |

## Commits Obligatorios

| # | Mensaje | Hash | Fecha |
|---|---------|------|-------|
| 1 | `docs: define LogiTrack IQ scope` | `eb3430b` | Dia 1 |
| 2 | `test: define reorder and order-state rules` | `e948700` | Dia 2 |
| 3 | `feat: implement LogiTrack IQ rules` | `1ace15c` | Dia 3 |

## Evidencia Rojo (tests fallan antes de implementar)

Archivo: `docs/sdd/evidencia/mvn-test-rojo-dia2.txt`

```
Tests run: 12, Failures: 9, Errors: 0, Skipped: 0
BUILD FAILURE
```

Los tests fallan porque los servicios, DTOs y repositorios aun no existen. Esto confirma que los tests fueron escritos antes de la implementacion (TDD).

## Evidencia Verde (tests pasan despues de implementar)

Archivo: `docs/sdd/evidencia/mvn-test-verde-dia3.txt`

```
Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Todos los tests pasan despues de la implementacion completa del backend, incluyendo:
- Unit tests de reglas de negocio
- Tests de seguridad (roles y permisos)
- Tests de integracion (PDF, resumen)

## Reflexion

El desarrollo de LogiTrack IQ siguió el ciclo TDD: tests en rojo primero, implementación hasta verde, refactorización. Los 12 tests cubren las reglas críticas del enunciado: detección de riesgo, estados de orden, seguridad por roles, PDF con marca de agua, y publicación de resumen. Los 3 commits obligatorios documentan el progreso: definición del alcance, definición de reglas en tests, e implementación completa. Los servicios core (InventarioAnalytics, OrdenCompra, PanelResumen, PdfOrden) manejan validación, auditoría y transaccionalidad. El frontend vanilla HTML/CSS/JS consume la API REST autenticada con JWT. El MCP server y n8n automatizan el flujo diario de inventario. La arquitectura en capas (Controller -> Security -> Service -> Repository) mantiene separación de responsabilidades y facilita el testing.
