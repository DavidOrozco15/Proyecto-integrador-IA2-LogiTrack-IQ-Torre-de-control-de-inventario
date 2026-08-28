# Manual MCP + n8n - LogiTrack IA2

## 📋 Visión General

Este manual contiene toda la información necesaria para operar el servidor MCP (PostgreSQL/Supabase aware) como capa de intermediación entre n8n y la API REST del backend Spring Boot. **El MCP nunca accede directamente a la base de datos** - usa la API REST.

---

## 🏠 Estructura del Proyecto

```
/home/trainer/Proyecto-integrador-IA2-LogiTrack-IQ-Torre-de-control-de-inventario/
├── mcp-server/                    # Servidor MCP
│   ├── config.yaml                # Configuración (URL base, credenciales AGENTE)
│   ├── server.py                  # FastAPI con 6 herramientas HTTP
│   ├── requirements.txt           # Dependencias: fastapi, uvicorn, requests, pydantic
│   ├── evidencia-herramientas.md  # Tabla 6 herramientas + peticiones/response
│   └── README.md                 # Instrucciones corriendo contra Supabase
├── n8n/                           # Flujos n8n
│   └── resumen-diario-inventario.json  # Flujo completo
├── skills/operacion-logitrack/   # Instrucciones operativas
│   └── SKILL.md                  # Skill: operacion-logitrack
└── MCP-n8n-Manual.md              # ← Este manual (en la raíz)
```

---

## 🔧 MCP Server (6 Herramientas Obligatorias)

### Configuración Requerida

Archivo: `mcp-server/config.yaml`

```yaml
mcp:
  name: "LogiTrack MCP Server"
  version: "1.0.0"
  api_base_url: "http://TU_IP_O_HOST:8080/api"  # ← Cambiar según entorno
  timeout: 30
  agente_username: "agente_user"                   # Usuario con rol AGENTE en BD
  agente_password: "agente_pass"                   # Contraseña correspondiente
```

### Endpoints de la API REST (Spring Boot)

Todas las herramientas usan **GET/POST** sobre endpoints ya definidos en el proyecto Spring Boot:

| # | Herramienta | Endpoint | Método | Parámetros | Retorno |
|---|-------------|----------|--------|------------|---------|
| 1 | `consultar_stock_producto(productoId)` | `GET /api/productos/{id}/stock` | GET | `productoId: Long` | Stock actual y detalles del producto |
| 2 | `consultar_bodegas_criticas()` | `GET /api/bodegas/criticas` | GET | Ninguno | Bodegas con ocupación >= 90% |
| 3 | `consultar_productos_en_riesgo()` | `GET /api/productos/riesgo` | GET | Ninguno | Lista productos con stock below punto de reorden |
| 4 | `consultar_kpis()` | `GET /api/kpis` | GET | Ninguno | 4 indicadores (calculadoEn, ocupación, quiebre, riesgo, órdenes, movimientos) |
| 5 | `crear_orden_borrador(productoId, proveedorId, bodegaDestinoId, cantidad, precioUnitario)` | `POST /api/ordenes` | POST | 5 parámetros JSON | Orden creada en estado BORRADOR |
| 6 | `publicar_resumen(resumen)` | `POST /api/panel/resumen` | POST | JSON contrato exacto | Último resumen guardado o 404 |

### Restricciones CRÍTICAS (Obligatorias)

1. **Sin herramienta de aprobar órdenes**: No existe herramienta para aprobar órdenes. El AGENTE retorna **403** al intentar `PATCH /api/ordenes/{id}/estado`.

2. **Autenticación con rol AGENTE**: Todas las herramientas deben autenticarse como usuario con rol **AGENTE** (no admin, no empleado). SecurityConfig ya tiene este protección.

3. **Sin conexión directa a SQL/PostgreSQL**: El MCP NUNCA debe tener JDBC connections. Toda comunicación es SOLO mediante API REST HTTP.

4. **Compatibilidad Supabase (bytea sin @Lob)**: El campo pdfBytes en OrdenCompra tiene `columnDefinition = "bytea"` SIN `@Lob`. El MCP al generar/leer PDFs debe esperar este tipo de dato en la API.

---

## 🔄 Flujo n8n - Resumen Diario de Inventario

### Archivo: `n8n/resumen-diario-inventario.json`

### Estructura del Flujo

1. **Schedule Trigger**: 6:00 a.m. en `America/Bogota`
2. **Nodo AI Agent**: Usa herramientas MCP (las 6 definidas)
3. **Pasos del flujo**:

   a. **Consultar KPIs** (`consultar_kpis()`)
   b. **Consultar productos en riesgo** (`consultar_productos_en_riesgo()`)
   c. **Decision**: Si hay productos en riesgo:
      - Crear orden en borrador para el **primer** producto listado
      - Cantidad: `ceil(max(1, puntoReorden × 2 - stockTotal))`
      - **Solo crear una orden por ejecución**
   d. **Si NO hay productos en riesgo**: saltar creación de orden
   e. **Publicar resumen del panel** (`publicar_resumen()`)
   f. **Registrar salida**: éxito o error en ejecución de n8n

### Configuración Crítica en n8n

- **Authentication**: Configurar credenciales del usuario **AGENTE**
- **API Base URL**: Debe coincidir con `api_base_url` en `mcp-server/config.yaml`
- **Header Authorization**: `Bearer <token>` donde el token es del usuario AGENTE

### Flujo de Decisión en n8n

```
Si hay productos en riesgo:
   → crear_orden_borrador(
       productoId: [primer producto],
       proveedorId: [del producto o null],
       bodegaDestinoId: [sugerido del producto],
       cantidad: ceil(max(1, puntoReorden × 2 - stockTotal)),
       precioUnitario: [del producto]
   )
   → publicar_resumen({
       fecha: "YYYY-MM-DD actual en America/Bogota",
       narrativa: "Hay productos en riesgo y una orden pendiente de aprobación.",
       alertas: [severidad ALTA con producto en riesgo],
       accionesSugeridas: [tipo REVISAR_ORDEN]
   })
Si NO hay productos en riesgo:
   → Saltar creación de orden
   → publicar_resumen con narrativa de "Sin productos en riesgo"
```

---

## 📄 Skill: operacion-logitrack

### Archivo: `skills/operacion-logitrack/SKILL.md`

### Instrucciones para Flujo Automatizado

```
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
```

### Restricciones Obligatorias en Skill:
- ✓ Consultar primero riesgos y KPIs
- ✓ Crear máximo una orden en borrador por ejecución
- ✓ **No aprobar, cancelar ni recibir órdenes** - RESTRIPCIÓN OBLIGATORIA
- ✓ Publicar solo un JSON que cumpla el contrato del resumen
- ✓ Informar el error si una herramienta falla

---

## 🛠️ Pasos para Ponerlo a Funcionar

### Paso 1: Configurar MCP Server

```bash
# 1. Editar config.yaml con tu URL
nano mcp-server/config.yaml
# Cambiar api_base_url a tu backend local:
# api_base_url: "http://localhost:8080/api"

# 2. Asegurar que el backend esté corriendo
# ./mvnw spring-boot:run  (estará en http://localhost:8080)

# 3. Verificar usuario AGENTE en tu BD
# Debe existir un usuario con role='AGENTE'
# INSERT INTO usuarios (...) VALUES (..., 'AGENTE');
```

### Paso 2: Importar Flujo n8n

```bash
# 1. En n8n local (http://localhost:5678):
#   - Menú: Import > Import from File
#   - Seleccionar: n8n/resumen-diario-inventario.json

# 2. Verificar que el nodo AI Agent tenga las 6 herramientas:
#   - consultar_stock_producto
#   - consultar_bodegas_criticas
#   - consultar_productos_en_riesgo
#   - consultar_kpis
#   - crear_orden_borrador
#   - publicar_resumen
```

### Paso 3: Configurar Credenciales en n8n

```bash
# 1. En cada herramienta HTTP Request del nodo AI Agent:
#   - Authentication: None (o JWT según configuración)
#   - Header Authorization: Bearer <token_del_usuario_AGENTE>
#   O usar la autenticación integrada de n8n oauth2 del MCP server

# 2. Probar manualmente:
#   - Ejecutar flujo "Manualmente" en n8n
#   - Debería consultar KPIs → productos en riesgo → crear orden borrador → publicar resumen
```

### Paso 4: Verificar el Flujo Completo

```bash
# Flujo esperado al ejecutar manualmente:
1. ✓ consultar_kpis() - Retorna 4 indicadores
2. ✓ consultar_productos_en_riesgo() - Lista productos con stock bajo
3. ✓ Si hay riesgo:
      - crear_orden_borrador() - Crea 1 orden estado BORRADOR
      - publicar_resumen() - Guarda resumen en panel
4. ✓ Salida: éxito o error registrado en n8n
```

### Paso 5: Probar Seguridad (AGENTE vs ADMIN)

```bash
# Verificar que AGENTE no puede aprobar órdenes:
# 1. Intentar PATCH /api/ordenes/{id}/estado con rol AGENTE
# 2. Debería retornar 403 Forbidden
# 3. Solo ADMIN puede cambiar estado de orden

# Verificar en SecurityConfig.java:
# .requestMatchers(HttpMethod.PATCH, "/api/ordenes/**/estado").hasRole("ADMIN")
```

---

## 🐛 Solución de Problemas Comunes

### Error: "Cannot connect to API backend"

**Causa**: `api_base_url` en `config.yaml` no coincide con tu backend.

**Solución**:
- Verificar que tu backend Spring Boot esté corriendo en el puerto indicado
- Ejemplo: `api_base_url: "http://localhost:8080/api"`
- Probar con `curl http://localhost:8080/api/kpis`

### Error: "403 Forbidden en todas las herramientas"

**Causa**: El usuario AGENTE no tiene las credenciales correctas o no tiene rol AGENTE en la BD.

**Solución**:
- Verificar usuario y contraseña en `config.yaml`
- Verificar en BD: `SELECT * FROM usuarios WHERE rol = 'AGENTE'`
- Contraseña debe estar encriptada con BCrypt como los demás usuarios

### Error: "Herramienta no encontrada en n8n"

**Causa**: El nodo AI Agent no tiene configuradas las 6 herramientas.

**Solución**:
- Importar nuevamente el flujo `n8n/resumen-diario-inventario.json`
- Verificar en el nodo que las 6 herramientas aparecen
- Revisar logs de n8n en la consola

### Error: "PDF generation fails en Supabase"

**Causa**: El MCP está intentando guardar PDF con `@Lob` pero la base de datos tiene `bytea` sin `@Lob`.

**Solución**:
- El modelo `OrdenCompra` ya tiene `@Column(name = "pdf_bytes", columnDefinition = "bytea")` SIN `@Lob`
- Esto ya fue solucionado en el proyecto actual
- El `PdfOrdenService.java` genera PDF y lo guarda como `byte[]`

---

## 📦 Dependencias y Ejecución

### Backend Spring Boot

```bash
# Desde la raíz del proyecto
./mvnw spring-boot:run
# Queda corriendo en: http://localhost:8080
```

### MCP Server

```bash
# Desde la carpeta mcp-server
cd mcp-server
uvicorn server:app --host 0.0.0.0 --port 8081
# Opcional: agregar al config.yaml la URL base
```

### n8n

```bash
# Ya está corriendo en http://localhost:5678
# Importar el flujo: n8n/resumen-diario-inventario.json
```

### Pruebas

```bash
# Desde la raíz del proyecto
./mvnw test  # 12 pruebas, todas deberían pasar (BUILD SUCCESS)
```

---

## ⚠️ Recordatorios Importantes

1. **MCP NUNCA toca la BD directamente** - Solo HTTP requests a la API REST
2. **Siempre autenticar como AGENTE** - Las herramientas fallan con 403 si usan rol distinto
3. **Máximo una orden por ejecución** - La decisión está en el flujo n8n y el SKILL.md
4. **Sin herramienta de aprobar** - Esta es una restricción obligatoria, retorna 403
5. **Zona horaria America/Bogota** - Tanto backend, n8n como datos deben usar esta zona
6. **Tests con H2** - Las 12 pruebas usan H2 en modo PostgreSQL, corren en cualquier lugar con Java

---

## 🔄 Actualización y Mantenimiento

### Cuando cambiar la URL del backend:

1. Editar `mcp-server/config.yaml` - `api_base_url`
2. Reiniciar MCP server: `Ctrl+C` y `uvicorn server:app ...`
3. Verificar que n8n siga conectando a la nueva URL
4. Probar que todas las herramientas funcionen con la nueva URL

### Cuando agregar nuevo usuario:

1. Insertar en la BD con role='AGENTE' (si es para MCP)
2. Las credenciales van en `mcp-server/config.yaml`
3. Probar que las herramientas funcionan con el nuevo usuario

### Cuando actualizar el flujo n8n:

1. Modificar `n8n/resumen-diario-inventario.json`
2. Importar el nuevo flujo en n8n (puede reemplazar el existente)
3. Verificar que el nodo AI Agent tiene las herramientas correctas
4. Ejecutar una prueba manual antes de depender del schedule

---

**Manual generado para contexto opencode de la casa.**
**Todas las herramientas, restricciones y estructuras están basadas en la especificación del proyecto LogiTrack IA2 y el PDF oficial "Proyecto IA2 - LogiTrack IQ".**


{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MTc4Nzg3ODUzMywiZXhwIjoxNzg3OTY0OTMzfQ.BZVeD-YWQA6ND0CapI8T-2PnQK3YHYTGOVUtGN3mfP4",
  "tokenType": "Bearer",
  "userId": 1,
  "username": "admin",
  "email": "admin@logitrack.com",
  "rol": "ADMIN"
}

{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZ2VudGVfdXNlciIsImlhdCI6MTc4Nzg3OTQxNiwiZXhwIjoxNzg3OTY1ODE2fQ.QV89YsFhe_zglnypVGkC4OaiRL1c_Kf_aH_SkUFHeHA",
  "tokenType": "Bearer",
  "userId": 5,
  "username": "agente_user",
  "email": "agente@logitrac.com",
  "rol": "AGENTE"
}