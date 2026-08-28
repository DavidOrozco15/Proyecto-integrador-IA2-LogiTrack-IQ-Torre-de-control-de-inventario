# LogiTrack IQ

> Torre de Control de Inventario — Gestión Inteligente de Inventarios para LogiTrack WMS

LogiTrack IQ es un sistema completo de gestión de inventarios construido sobre la plataforma LogiTrack WMS existente. Agrega capacidades de inteligencia artificial para detectar productos en riesgo de quiebre de stock, gestionar órdenes de compra automáticas y generar reportes diarios automatizados.

**Desarrollado por:** David Orozco

---

## Arquitectura General

```
┌─────────────────────────────────────────────────────────────┐
│                     Frontend (HTML/CSS/JS)                   │
│              Servido desde Spring Boot :8080                 │
├─────────────────────────────────────────────────────────────┤
│              Backend Spring Boot (Java 17)                  │
│              REST API + JWT Auth + JPA                       │
│              Puerto: 8080                                   │
├──────────┬──────────────────────────────┬──────────────────┤
│          │                              │                  │
│  PostgreSQL (Supabase)         MCP Server (Python)    n8n (Automatización)
│  Base de datos remota          Puerto: 8081           Puerto: 5678
│  Esquema: proyecto             FastAPI + JWT           Workflow diario 6AM
│                                                        Agente IA
└─────────────────────────────────────────────────────────────┘
```

### Flujo de Datos

1. **Frontend → Backend**: El usuario interactúa con la interfaz web. Las peticiones HTTP incluyen el token JWT en el header `Authorization`.
2. **Backend → PostgreSQL**: Spring Boot ejecuta queries JPA contra Supabase (PostgreSQL托管).
3. **n8n → MCP → Backend**: Cada día a las 6:00 AM, n8n dispara un workflow que consulta el MCP server para obtener KPIs, productos en riesgo y bodegas críticas. El MCP se autentica como `agente_user` contra el backend.
4. **n8n → Backend**: El agente IA en n8n decide crear órdenes de compra y publicar un resumen en el panel.

---

## Stack Tecnológico

| Capa | Tecnología | Versión | Propósito |
|------|-----------|---------|-----------|
| **Backend** | Java + Spring Boot | 17 / 3.2.4 | REST API, JWT Auth, JPA/Hibernate |
| **Base de Datos** | PostgreSQL (Supabase) | - | Almacenamiento persistente |
| **Frontend** | HTML, CSS, JavaScript | Vanilla | Interfaz de usuario (SPA-like) |
| **MCP Server** | Python + FastAPI | 3.11 | Intermediario para agente IA |
| **Automatización** | n8n | latest | Workflow diario de inventario |
| **Contenedores** | Docker + Docker Compose | - | Orquestación de servicios |
| **Documentación** | Swagger/OpenAPI | 3.0 | API docs en `/swagger-ui.html` |

---

## Requisitos Previos

- Docker y Docker Compose instalados
- Conexión a internet (base de datos en Supabase)
- Puerto 8080, 8081 y 5678 disponibles

---

## Ejecución

### Iniciar todos los servicios

```bash
docker-compose up -d --build
```

Esto levantará 3 contenedores:

| Servicio | URL | Descripción |
|----------|-----|-------------|
| **Backend + Frontend** | http://localhost:8080 | API REST y panel web |
| **Swagger UI** | http://localhost:8080/swagger-ui.html | Documentación interactiva de la API |
| **MCP Server** | http://localhost:8081 | Herramientas para el agente IA |
| **n8n** | http://localhost:5678 | Automatización de workflows |

### Credenciales

| Usuario | Contraseña | Rol |
|---------|-----------|-----|
| `admin` | `admin123` | Administrador |
| `agente_user` | `admin123` | Agente IA (usado por n8n/MCP) |

### Verificar el estado

```bash
# Ver contenedores corriendo
docker-compose ps

# Ver logs del backend
docker-compose logs -f backend

# Ver logs del MCP server
docker-compose logs -f mcp

# Ver logs de n8n
docker-compose logs -f n8n
```

### Detener servicios

```bash
docker-compose down
```

---

## Estructura del Proyecto

```
Proyecto_SpringBoot_LogiTrack/
├── src/
│   └── main/
│       ├── java/com/logitrack/
│       │   ├── config/          # Configuración (CORS, OpenAPI, UserContext)
│       │   ├── controller/      # 10 controladores REST
│       │   ├── dto/             # 12 Data Transfer Objects
│       │   ├── exception/       # Manejo de excepciones globales
│       │   ├── listener/        # Event-driven audit (JPA listeners)
│       │   ├── model/           # 18 entidades JPA + enums
│       │   ├── repository/      # 10 repositorios Spring Data
│       │   ├── security/        # JWT filter, token provider, SecurityConfig
│       │   ├── service/         # 17 servicios (interfaces + implementaciones)
│       │   └── util/            # Utilidades (fechas Bogotá)
│       └── resources/
│           ├── application.properties   # Config Spring Boot
│           ├── data.sql                 # Datos semilla (30 productos, 5 bodegas)
│           ├── schema.sql               # DDL
│           └── static/                  # Frontend servido por Spring Boot
│               ├── css/styles.css       # Estilos dark-theme
│               ├── js/app.js            # Lógica del frontend
│               ├── dashboard.html       # Dashboard principal
│               ├── login.html           # Página de login
│               └── ...                  # Otras páginas
├── frontend/                     # Frontend standalone (copia para desarrollo)
├── mcp-server/
│   ├── server.py                 # FastAPI con 9 herramientas MCP
│   ├── config.yaml               # Configuración del MCP server
│   └── Dockerfile
├── n8n/
│   └── resumen-diario-inventario.json  # Workflow n8n exportado
├── docs/                         # Documentación del proyecto
│   ├── documento-arquitectura.md
│   └── sdd/                      # Software Design Document
├── docker-compose.yml            # Orquestación de 3 servicios
├── Dockerfile                    # Multi-stage build (backend)
├── pom.xml                       # Dependencias Maven
└── skills/
    └── operacion-logitrack/
        └── SKILL.md              # Definición del skill del agente IA
```

---

## Modelo de Datos

### Entidades Principales

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Usuario    │     │   Bodega     │     │  Producto    │
├──────────────┤     ├──────────────┤     ├──────────────┤
│ id           │     │ id           │     │ id           │
│ username     │     │ nombre       │     │ nombre       │
│ password     │     │ direccion    │     │ sku          │
│ rol          │     │ capacidadMax │     │ precioUnit   │
│ email        │     │ activa       │     │ stockTotal   │
│ creadoEn     │     │ creadoEn     │     │ ptoreorden   │
│              │     │              │     │ categoria    │
└──────────────┘     └──────────────┘     │ activo       │
                                          └──────┬───────┘
                                                 │
                    ┌──────────────┐     ┌────────┴────────┐
                    │   Proveedor  │     │InventarioBodega │
                    ├──────────────┤     ├─────────────────┤
                    │ id           │     │ id              │
                    │ nombre       │     │ producto (FK)   │
                    │ nit          │     │ bodega (FK)     │
│ email          │     │ stock         │
│ telefono       │     │ fechaActualiz │
│ direccion      │     └─────────────────┘
│ contacto       │
│ activo         │     ┌──────────────────────┐
└──────────────┘     │ MovimientoInventario  │
                     ├──────────────────────┤
                     │ id                   │
                     │ tipo (ENTRADA/SALIDA/│
                     │       TRASLADO)      │
                     │ bodegaOrigen (FK)    │
                     │ bodegaDestino (FK)   │
                     │ usuario (FK)         │
                     │ observaciones        │
                     │ creadoEn             │
                     │ fechaMovimiento      │
                     └──────────┬───────────┘
                                │ 1:N
                     ┌──────────┴───────────┐
                     │  MovimientoDetalle   │
                     ├──────────────────────┤
                     │ id                   │
                     │ movimiento (FK)      │
                     │ producto (FK)        │
                     │ cantidad             │
                     │ precioUnitario       │
                     └──────────────────────┘

┌──────────────────────┐     ┌──────────────────────┐
│    OrdenCompra       │     │    ResumenPanel      │
├──────────────────────┤     ├──────────────────────┤
│ id                   │     │ id                   │
│ estado (BORRADOR/    │     │ titulo               │
│   PENDIENTE/APROBADA/│     │ contenido            │
│   RECIBIDA/CANCELADA)│     │ contenidoJson        │
│ proveedor (FK)       │     │ tipoResumen          │
│ usuarioSolicitante   │     │ autor                │
│ totalEstimado        │     │ creadoEn             │
│ observaciones        │     │                      │
│ creadoPor            │     └──────────────────────┘
│ pdfRuta              │
│ pdfFechaGeneracion   │     ┌──────────────────────┐
│ creadoEn             │     │    Auditoria         │
└──────────┬───────────┘     ├──────────────────────┤
           │ 1:N             │ id                   │
┌──────────┴───────────┐     │ usuario (FK)         │
│ OrdenCompraDetalle   │     │ tipoOperacion        │
├──────────────────────┤     │ entidad              │
│ id                   │     │ entidadId            │
│ orden (FK)           │     │ detalles             │
│ producto (FK)        │     │ direccionIp          │
│ cantidad             │     │ creadoEn             │
│ precioUnitario       │     └──────────────────────┘
└──────────────────────┘
```

### Enums

| Enum | Valores |
|------|---------|
| **Rol** | `ADMIN`, `EMPLEADO`, `AGENTE` |
| **TipoMovimiento** | `ENTRADA`, `SALIDA`, `TRASLADO` |
| **TipoOperacion** | `CREACION`, `ACTUALIZACION`, `ELIMINACION`, `LOGIN`, `CONSULTA` |
| **EstadoOrdenCompra** | `BORRADOR`, `PENDIENTE`, `APROBADA`, `RECIBIDA`, `CANCELADA` |
| **SeveridadAlerta** | `BAJA`, `MEDIA`, `ALTA`, `CRITICA` |
| **TipoAccionSugerida** | `REABASTECER`, `TRASLADAR`, `CANCELAR` |

---

## API REST

### Autenticación

```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Respuesta: { "token": "eyJ...", "rol": "ADMIN", "username": "admin" }

# Usar token en peticiones subsecuentes
curl http://localhost:8080/api/productos \
  -H "Authorization: Bearer eyJ..."
```

### Endpoints Principales

| Método | Endpoint | Descripción | Roles |
|--------|----------|-------------|-------|
| POST | `/api/auth/login` | Iniciar sesión | Público |
| POST | `/api/auth/registrar` | Registrar usuario | ADMIN |
| GET | `/api/usuarios` | Listar usuarios | ADMIN |
| GET | `/api/productos` | Listar productos | Todos |
| GET | `/api/productos/{id}` | Producto por ID | Todos |
| POST | `/api/productos` | Crear producto | ADMIN |
| PUT | `/api/productos/{id}` | Actualizar producto | ADMIN |
| GET | `/api/bodegas` | Listar bodegas | Todos |
| GET | `/api/bodegas/stock` | Stock por bodega | Todos |
| GET | `/api/bodegas/{id}/inventario` | Inventario de bodega | Todos |
| POST | `/api/movimientos` | Crear movimiento | ADMIN, EMPLEADO |
| GET | `/api/movimientos` | Listar movimientos | Todos |
| GET | `/api/movimientos/{id}/pdf` | Descargar PDF de movimiento | Todos |
| POST | `/api/ordenes-compra` | Crear orden de compra | ADMIN, EMPLEADO |
| GET | `/api/ordenes-compra` | Listar órdenes | Todos |
| GET | `/api/ordenes-compra/{id}` | Orden por ID | Todos |
| PUT | `/api/ordenes-compra/{id}/aprobar` | Aprobar orden | ADMIN |
| PUT | `/api/ordenes-compra/{id}/recibir` | Recibir orden | ADMIN |
| PUT | `/api/ordenes-compra/{id}/cancelar` | Cancelar orden | ADMIN |
| GET | `/api/ordenes-compra/{id}/pdf` | Descargar PDF de orden | Todos |
| GET | `/api/proveedores` | Listar proveedores | Todos |
| POST | `/api/proveedores` | Crear proveedor | ADMIN |
| GET | `/api/kpis` | KPIs del dashboard | Todos |
| GET | `/api/kpis/riesgos` | Productos en riesgo | Todos |
| GET | `/api/kpis/bodegas-criticas` | Bodegas con occupancy >=90% | Todos |
| GET | `/api/panel-resumen` | Resumen del panel | Todos |
| POST | `/api/panel-resumen` | Publicar resumen | AGENTE, ADMIN |
| GET | `/api/auditoria` | Log de auditoría | ADMIN |

### Ejemplos con curl

```bash
# Listar productos
curl http://localhost:8080/api/productos \
  -H "Authorization: Bearer $TOKEN"

# Crear movimiento de entrada
curl -X POST http://localhost:8080/api/movimientos \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "tipo": "ENTRADA",
    "bodegaOrigenId": 1,
    "observaciones": "Ingreso de mercadería",
    "detalles": [
      {"productoId": 1, "cantidad": 50, "precioUnitario": 15000}
    ]
  }'

# Crear orden de compra
curl -X POST http://localhost:8080/api/ordenes-compra \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "proveedorId": 1,
    "observaciones": "Reabastecimiento urgente",
    "detalles": [
      {"productoId": 1, "cantidad": 100, "precioUnitario": 15000}
    ]
  }'

# Aprobar orden de compra
curl -X PUT http://localhost:8080/api/ordenes-compra/1/aprobar \
  -H "Authorization: Bearer $TOKEN"

# Obtener KPIs
curl http://localhost:8080/api/kpis \
  -H "Authorization: Bearer $TOKEN"
```

---

## Datos Semilla (data.sql)

El sistema se inicializa automáticamente con datos de prueba consistentes:

| Entidad | Cantidad | Detalles |
|---------|----------|----------|
| **Productos** | 30 | 6 categorías (Electrónica, Periféricos, Almacenamiento, Redes, Impresión, Audio) |
| **Bodegas** | 5 | Central (92.1% - CRÍTICA), Norte, Sur, Este, Oeste |
| **Proveedores** | 6 | TechSupply, Importadora Global, etc. |
| **Usuarios** | 3 | admin (ADMIN), agente_user (AGENTE), empleado1 (EMPLEADO) |
| **Movimientos** | 32 | 7 de ayer, 25 de hoy (varias horas) |
| **Detalles de movimiento** | 62 | Stock consistente con inventario |
| **Órdenes de compra** | 1 | En estado BORRADOR |
| **Inventarios por bodega** | 150 | Stock distribuido en 5 bodegas × 30 productos |

### Bodegas Críticas

| Bodega | Capacidad | Stock | Ocupación | Estado |
|--------|-----------|-------|-----------|--------|
| Bodega Central | 380 | 350 | **92.1%** | CRÍTICA |
| Bodega Norte | 200 | 74 | 37.0% | Normal |
| Bodega Sur | 150 | 78 | 52.0% | Normal |
| Bodega Este | 100 | 38 | 38.0% | Normal |
| Bodega Oeste | 170 | 74 | 43.5% | Normal |

### Productos en Riesgo (stock <= punto de reorden)

| Producto | SKU | Stock Total | Pto. Reorden | Estado |
|----------|-----|-------------|--------------|--------|
| Teclado Keychron K2 | KBD-K2-BT | 2 | 3 | EN RIESGO |
| Micrófono Blue Yeti | MIC-BLU-YETI | 1 | 3 | EN RIESGO |

---

## Frontend

### Páginas Disponibles

| Página | URL | Descripción |
|--------|-----|-------------|
| Login | `/login.html` | Inicio de sesión |
| Dashboard | `/dashboard.html` | Panel principal con KPIs, bodegas, resumen |
| Órdenes de Compra | `/dashboard.html#ordenes` | Gestión de órdenes con filtros y acciones |
| Productos en Riesgo | `/dashboard.html#riesgo` | Productos por debajo del punto de reorden |
| Todos los Productos | `/dashboard.html#todos` | Catálogo completo con filtros |
| Bodegas | `/dashboard.html#bodegas` | Estado de las 5 bodegas con inventario |
| Auditoría | `/html/auditoria.html` | Log de actividades (solo ADMIN) |

### Funcionalidades del Frontend

- **Dark theme** con efectos glassmorphism y animaciones
- **Contadores animados** en las tarjetas KPI
- **Filtros** por categoría, nombre, ordenamiento y nivel de stock
- **Órdenes de compra**: Chips de filtrado (Borrador/Aprobadas/Recibidas/Canceladas/Todas) + botones de acción (Aprobar, Recibir, Cancelar)
- **PDF generation**: POST genera el PDF → GET lo descarga
- **Modal de inventario por bodega**: Muestra 5 columnas (Producto, Stock Bodega, Stock Total, Precio, Categoría)
- **Responsive**: Se adapta a diferentes tamaños de pantalla

---

## MCP Server (Model Context Protocol)

El MCP server es un intermediario FastAPI que permite al agente IA de n8n interactuar con el backend de forma segura.

### Herramientas Disponibles (9)

| # | Herramienta | Método | Descripción |
|---|------------|--------|-------------|
| 1 | `consultar_stock_producto` | GET | Consultar stock de un producto por ID |
| 2 | `consultar_bodegas_criticas` | GET | Bodegas con ocupación >= 90% |
| 3 | `consultar_productos_en_riesgo` | GET | Productos por debajo del punto de reorden |
| 4 | `consultar_proveedores` | GET | Listar todos los proveedores activos |
| 5 | `consultar_bodega_sugerida/{productoId}` | GET | Bodega con menor stock para un producto |
| 6 | `consultar_kpis` | GET | KPIs del dashboard |
| 7 | `crear_orden_borrador` | POST | Crear orden de compra en estado BORRADOR |
| 8 | `publicar_resumen` | POST | Publicar resumen en el panel |
| 9 | `consultar_ordenes_borrador` | GET | Listar órdenes en estado BORRADOR |

### Autenticación del MCP

El MCP server se autentica como `agente_user` contra el backend:

```python
# Internamente hace login y obtiene JWT
response = requests.post(f"{API_BASE_URL}/auth/login", json={
    "username": "agente_user",
    "password": "admin123"
})
token = response.json()["token"]
```

---

## n8n - Automatización Diaria

### Workflow: Resumen Diario de Inventario

- **Schedule**: Diario a las 6:00 AM (America/Bogota)
- **Archivo**: `n8n/resumen-diario-inventario.json`

### Flujo del Workflow

1. **Schedule Trigger** → Se ejecuta diario a las 6:00 AM
2. **Config MCP** → Establece la URL base del MCP server
3. **GET KPIs** → Consulta los KPIs actuales del dashboard
4. **GET Productos en Riesgo** → Obtiene la lista de productos con stock bajo
5. **GET Bodegas Críticas** → Identifica bodegas con ocupación >= 90%
6. **Loop por cada producto en riesgo** → Para cada producto:
   - Consulta el stock actual
   - Consulta la bodega sugerida (menor stock)
   - Crea una orden de compra BORRADOR si es necesario
7. **Publish Summary** → Publica un resumen estructurado en el panel

### Configuración de n8n

Acceder a n8n: http://localhost:5678

Para importar el workflow:
1. Ir a Workflows → Import from File
2. Seleccionar `n8n/resumen-diario-inventario.json`
3. Activar el workflow

---

## Seguridad

### JWT Authentication

- **Filtro**: `JwtAuthenticationFilter` se ejecuta antes de cada petición
- **Token Provider**: `JwtTokenProvider` genera y valida tokens
- **Secret**: Configurado en `application.properties` (`jwt.secret`)
- **Expiración**: 24 horas (`jwt.expiration-ms=86400000`)
- **UserContext**: `ThreadLocal` almacena el usuario actual desde el JWT

### Roles y Permisos

| Recurso | ADMIN | EMPLEADO | AGENTE |
|---------|-------|----------|--------|
| Lectura (GET) | ✅ | ✅ | ✅ |
| Crear productos | ✅ | ❌ | ❌ |
| Crear movimientos | ✅ | ✅ | ❌ |
| Aprobar/Recibir/Cancelar órdenes | ✅ | ❌ | ❌ |
| Registrar usuarios | ✅ | ❌ | ❌ |
| Publicar resumen | ✅ | ❌ | ✅ |
| Auditoría | ✅ | ❌ | ❌ |

### Auditoría Automática

Cada operación de escritura en la base de datos genera automáticamente un registro de auditoría mediante JPA EntityListeners:

- `AuditEntityListener` → Captura eventos de creación/actualización/eliminación
- `AuditoriaEventListener` → Procesa el evento y guarda el registro
- Tabla `auditoria` almacena: usuario, tipo operación, entidad, ID, detalles, IP, fecha

---

## Documentación Adicional

| Documento | Ubicación | Contenido |
|-----------|-----------|-----------|
| Arquitectura | `docs/documento-arquitectura.md` | Diagramas de clases, arquitectura en capas, flujo JWT |
| SDD - Propuesta | `docs/sdd/01-propuesta.md` | Propuesta del proyecto |
| SDD - Especificación | `docs/sdd/02-especificacion.md` | Especificación detallada |
| SDD - Diseño | `docs/sdd/03-diseno.md` | Diseño del sistema |
| SDD - Tareas | `docs/sdd/04-tareas.md` | Desglose de tareas |
| Manual MCP+n8n | `MCP-n8n-Manual.md` | Guía completa de configuración MCP y n8n |
| Skill del Agente | `skills/operacion-logitrack/SKILL.md` | Definición del skill para el agente IA |
| Evidencia MCP | `mcp-server/evidencia-herramientas.md` | Tabla de evidencia de las herramientas MCP |

---

## Solución de Problemas

### El backend no inicia

```bash
# Ver logs
docker-compose logs backend

# Rebuild completo
docker-compose down
docker-compose up -d --build
```

### Error de conexión a la base de datos

La aplicación usa Supabase (PostgreSQL remoto). Verificar:
1. Conexión a internet
2. Las credenciales en `application.properties` son válidas
3. El esquema `proyecto` existe en la base de datos

### El frontend no muestra datos

1. Verificar que el backend esté corriendo: `curl http://localhost:8080/api/productos`
2. Verificar el token JWT en la consola del navegador
3. Verificar los logs del backend para errores 401/403

### n8n no ejecuta el workflow

1. Acceder a http://localhost:5678
2. Verificar que el workflow esté activo (toggle activado)
3. Verificar que el MCP server esté corriendo: `curl http://localhost:8081/health`
4. Revisar logs de n8n: `docker-compose logs n8n`

### MCP server error 500

```bash
# Ver logs del MCP
docker-compose logs mcp

# Verificar que el backend esté accesible desde el MCP
docker-compose exec mcp curl http://backend:8080/api/kpis
```

---

## Tecnologías y Versiones

| Componente | Versión |
|-----------|---------|
| Java | 17 |
| Spring Boot | 3.2.4 |
| Spring Data JPA | 3.2.4 |
| Spring Security | 6.2.1 |
| Hibernate | 6.4.0 |
| PostgreSQL Driver | 42.7.1 |
|jjwt (JWT) | 0.12.3 |
| SpringDoc OpenAPI | 2.3.0 |
| Python | 3.11 |
| FastAPI | 0.109.0 |
| Uvicorn | 0.27.0 |
| n8n | latest |
| Docker | Multi-stage build |
| Maven | Wrapper (mvnw) |

---

## Licencia

Proyecto académico — Universidad Distrital Francisco José de Caldas

---

*Desarrollado por David Orozco — LogiTrack IQ 2025*
