# Skill: operacion-logitrack

## Instrucciones para flujo automatizado:

1. PRIMERO: Consultar riesgos y KPIs
   - Ejecutar: consultar_productos_en_riesgo()
   - Ejecutar: consultar_kpis()

2. DECISIÓN: Si hay productos en riesgo:
   - Si sí: crear como máximo UNA orden en borrador
   - Si no: terminar sin crear órdenes

3. CREAR ORDEN (solo si hay productos en riesgo):
   - herramienta: crear_orden_borrador(
       productoId: [primer producto en riesgo],
       proveedorId: [proveedor del producto, o null si no tiene],
       bodegaDestinoId: [bodegaDestino sugerida del producto],
       cantidad: ceil(max(1, puntoReorden × 2 - stockTotal)),
       precioUnitario: [precioUnitario del producto]
   )
   - **IMPORTANTE**: Solo crear una orden por ejecución
   - **RESTRIPCIÓN**: El AGENTE no puede aprobar, cancelar ni recibir órdenes (retorna 403)

4. PUBLICAR RESUMEN:
   - herramienta: publicar_resumen({
       fecha: "YYYY-MM-DD actual en America/Bogota",
       narrativa: "Hay productos en riesgo y una orden pendiente de aprobación.",
       alertas: [severidad ALTA con producto en riesgo],
       accionesSugeridas: [tipo REVISAR_ORDEN]
     })

5. MANEJO DE ERRORES:
   - Si alguna herramienta falla: informar error específico
   - Si no hay productos en riesgo: registrar éxito sin crear órdenes
   - Si falla la creación de orden: registrar error y no publicar resumen

# RESTRICCIONES OBLIGATORIAS EN SKILL:
- ✓ Consultar primero riesgos y KPIs
- ✓ Crear máximo una orden en borrador por ejecución
- ✓ **No aprobar, cancelar ni recibir órdenes** - RESTRIPCIÓN OBLIGATORIA
- ✓ Publicar solo un JSON que cumpla el contrato del resumen
- ✓ Informar el error si una herramienta falla