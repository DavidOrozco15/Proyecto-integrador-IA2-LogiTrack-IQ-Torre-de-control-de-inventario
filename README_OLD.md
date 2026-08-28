# LogiTrack WMS - Sistema de Gestión y Auditoría de Bodegas

![LogiTrack Logo](https://img.shields.io/badge/LogiTrack-WMS-blue?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-green?style=flat-square)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=flat-square)
![JWT](https://img.shields.io/badge/JWT-Auth-red?style=flat-square)
![Swagger](https://img.shields.io/badge/Swagger-OpenAPI%203-brightgreen?style=flat-square)

---

## 📋 Descripción del Proyecto

**LogiTrack S.A.** es una empresa que administra múltiples bodegas distribuidas en distintas ciudades, encargadas de almacenar productos y gestionar movimientos de inventario (entradas, salidas y transferencias).

Este sistema backend centralizado desarrollado con **Spring Boot** permite:

- ✅ Controlar todos los movimientos entre bodegas
- ✅ Registrar automáticamente los cambios (auditorías)
- ✅ Proteger la información con autenticación JWT
- ✅ Ofrecer endpoints REST documentados y seguros
- ✅ Generar reportes auditables de los cambios realizados por cada usuario

---

## 🏗️ Arquitectura del Proyecto

```
src/
├── main/
│   ├── java/com/logitrack/
│   │   ├── config/          → Configuraciones (OpenAPI, Web, SpringContext, UserContext)
│   │   ├── controller/      → Controladores REST
│   │   ├── dto/             → Objetos de transferencia de datos
│   │   ├── exception/       → Manejo global de excepciones
│   │   ├── listener/        → Listeners JPA para auditoría automática
│   │   ├── model/           → Entidades JPA
│   │   ├── repository/      → Repositorios Spring Data JPA
│   │   ├── security/        → Seguridad JWT + Spring Security
│   │   └── service/         → Lógica de negocio
│   └── resources/
│       ├── static/          → Frontend HTML/CSS/JS
│       ├── application.properties
│       ├── schema.sql
│       └── data.sql
└── test/
```

---

## 🚀 Tecnologías Utilizadas

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| Java | 17 | Lenguaje de programación |
| Spring Boot | 3.4 | Framework principal |
| Spring Security | 6.x | Autenticación y autorización |
| Spring Data JPA | 3.x | Persistencia de datos |
| PostgreSQL | 16 | Base de datos relacional |
| JWT (jjwt) | 0.11.5 | Tokens de autenticación |
| Swagger/OpenAPI | 3.0.3 | Documentación de API |
| Lombok | Última | Reducción de código boilerplate |
| Jakarta Validation | Última | Validaciones de datos |
| HTML/CSS/JS | - | Frontend básico de prueba |

---

## ⚙️ Requisitos Previos

- **Java 17** o superior
- **Maven 3.8+** (incluye `mvnw` en el proyecto)
- **PostgreSQL 16** o superior
- Conexión a internet para descargar dependencias

---

## 🔧 Instalación y Ejecución

### 1. Clonar el repositorio

```bash
git clone https://github.com/tu-usuario/logitrack.git
cd logitrack
```

### 2. Configurar la base de datos

Editar el archivo `src/main/resources/application.properties` con tus credenciales de PostgreSQL:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/logitrack
spring.datasource.username=tu_usuario
spring.datasource.password=tu_contraseña
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.default_schema=proyecto
```

### 3. Compilar y ejecutar

```bash
# Linux/Mac
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

O también:

```bash
./mvnw clean package -DskipTests
java -jar target/logitrak-0.0.1-SNAPSHOT.jar
```

### 4. Acceder a la aplicación

| Recurso | URL |
|---------|-----|
| **Frontend** | [http://localhost:8080](http://localhost:8080) |
| **Swagger UI** | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) |
| **API Docs** | [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) |

---

## 🔐 Credenciales de Prueba

| Usuario | Contraseña | Rol |
|---------|-----------|-----|
| `admin` | `admin123` | **ADMIN** |
| `corcho` | `corcho123` | **EMPLEADO** |

> **Nota:** Las contraseñas están codificadas con BCrypt en `data.sql`. Deberás registrar nuevos usuarios o ajustar las contraseñas según corresponda.

---

## 📚 Endpoints de la API

### Autenticación (`/api/auth`)

| Método | Endpoint | Descripción | Acceso |
|--------|----------|-------------|--------|
| `POST` | `/api/auth/login` | Iniciar sesión | Público |
| `POST` | `/api/auth/register` | Registrar nuevo usuario | Público |
| `POST` | `/api/auth/register-empleado` | Registrar empleado | **ADMIN** |

### Bodegas (`/api/bodegas`)

| Método | Endpoint | Descripción | Acceso |
|--------|----------|-------------|--------|
| `GET` | `/api/bodegas` | Listar todas las bodegas | ADMIN, EMPLEADO |
| `GET` | `/api/bodegas/{id}` | Obtener bodega por ID | ADMIN, EMPLEADO |
| `GET` | `/api/bodegas/buscar?nombre=` | Buscar bodega por nombre | ADMIN, EMPLEADO |
| `GET` | `/api/bodegas/stock` | Obtener stock de todas las bodegas | ADMIN, EMPLEADO |
| `GET` | `/api/bodegas/{id}/inventario` | Obtener inventario de una bodega | ADMIN, EMPLEADO |
| `POST` | `/api/bodegas` | Crear nueva bodega | **ADMIN** |
| `PUT` | `/api/bodegas/{id}` | Actualizar bodega | ADMIN, EMPLEADO |
| `DELETE` | `/api/bodegas/{id}` | Eliminar bodega | **ADMIN** |

### Productos (`/api/productos`)

| Método | Endpoint | Descripción | Acceso |
|--------|----------|-------------|--------|
| `GET` | `/api/productos` | Listar productos (con filtros) | ADMIN, EMPLEADO |
| `GET` | `/api/productos/{id}` | Obtener producto por ID | ADMIN, EMPLEADO |
| `GET` | `/api/productos/bajo-stock?umbral=10` | Productos con stock bajo | ADMIN, EMPLEADO |
| `GET` | `/api/productos/con-inventario` | Productos con inventario por bodega | ADMIN, EMPLEADO |
| `GET` | `/api/productos/{id}/con-inventario` | Producto con inventario por ID | ADMIN, EMPLEADO |
| `POST` | `/api/productos` | Crear nuevo producto | ADMIN, EMPLEADO |
| `POST` | `/api/productos/con-inventario` | Crear producto con inventario inicial | ADMIN, EMPLEADO |
| `PUT` | `/api/productos/{id}` | Actualizar producto | ADMIN, EMPLEADO |
| `PUT` | `/api/productos/{id}/con-inventario` | Actualizar producto con inventario | ADMIN, EMPLEADO |
| `DELETE` | `/api/productos/{id}` | Eliminar producto | **ADMIN** |

### Movimientos de Inventario (`/api/movimientos`)

| Método | Endpoint | Descripción | Acceso |
|--------|----------|-------------|--------|
| `GET` | `/api/movimientos` | Listar todos los movimientos | Autenticado |
| `GET` | `/api/movimientos/{id}` | Obtener movimiento por ID | Autenticado |
| `GET` | `/api/movimientos/tipo/{tipo}` | Filtrar por tipo (ENTRADA, SALIDA, TRANSFERENCIA) | Autenticado |
| `GET` | `/api/movimientos/rango?desde=&hasta=` | Filtrar por rango de fechas | Autenticado |
| `GET` | `/api/movimientos/bodega/{bodegaId}` | Filtrar por bodega | Autenticado |
| `POST` | `/api/movimientos` | Registrar nuevo movimiento | Autenticado |

### Auditoría (`/api/auditorias`)

| Método | Endpoint | Descripción | Acceso |
|--------|----------|-------------|--------|
| `GET` | `/api/auditorias` | Listar todas las auditorías | **ADMIN** |
| `GET` | `/api/auditorias/{id}` | Obtener auditoría por ID | **ADMIN** |
| `GET` | `/api/auditorias/entidad/{entidad}` | Filtrar por entidad afectada | **ADMIN** |
| `GET` | `/api/auditorias/usuario/{usuarioId}` | Filtrar por usuario | **ADMIN** |
| `GET` | `/api/auditorias/operacion/{tipo}` | Filtrar por tipo de operación | **ADMIN** |

### Reportes (`/api/reportes`)

| Método | Endpoint | Descripción | Acceso |
|--------|----------|-------------|--------|
| `GET` | `/api/reportes/resumen?dias=30&limit=20` | Resumen general del sistema | Autenticado |

### Usuarios (`/api/usuarios`)

| Método | Endpoint | Descripción | Acceso |
|--------|----------|-------------|--------|
| `GET` | `/api/usuarios` | Listar todos los usuarios | Autenticado |

---

## 📸 Capturas de Swagger

### Vista General de la API

![Swagger - Vista General](./docs/swagger-screenshots/swaggerPrincipal.png)

### Inicio de Sesión

![Swagger - Logueo](./docs/swagger-screenshots/iniciosesion.png)

### Endpoints Protegidos con JWT

![Swagger - Autenticación JWT](./docs/swagger-screenshots/autenticacion.png)

---

## 🧪 Ejemplos de Uso

### 1. Login (obtener token JWT)

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

**Respuesta:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "userId": 1,
  "username": "admin",
  "email": "admin@logitrac.com",
  "rol": "ADMIN"
}
```

### 2. Listar bodegas (con token JWT)

```bash
curl -X GET http://localhost:8080/api/bodegas \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

### 3. Crear un producto

```bash
curl -X POST http://localhost:8080/api/productos \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d '{
    "nombre": "Mouse Inalámbrico Logitech",
    "categoria": "Perifericos",
    "stock": 50,
    "precio": 35.99
  }'
```

### 4. Registrar un movimiento de entrada

```bash
curl -X POST http://localhost:8080/api/movimientos \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d '{
    "tipoMovimiento": "ENTRADA",
    "bodegaDestino": {"id": 1},
    "detalles": [
      {
        "producto": {"id": 1},
        "cantidad": 10
      }
    ]
  }'
```

### 5. Consultar productos con stock bajo

```bash
curl -X GET "http://localhost:8080/api/productos/bajo-stock?umbral=10" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

### 6. Obtener resumen general

```bash
curl -X GET "http://localhost:8080/api/reportes/resumen?dias=30&limit=20" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

## 🗄️ Estructura de la Base de Datos

### Diagrama Entidad-Relación

```
┌─────────────┐     ┌──────────────────┐     ┌─────────────┐
│  usuarios   │     │    bodegas       │     │  productos  │
├─────────────┤     ├──────────────────┤     ├─────────────┤
│ id (PK)     │◄────│ encargado_id     │     │ id (PK)     │
│ username    │     │ id (PK)          │     │ nombre      │
│ email       │     │ nombre           │     │ categoria   │
│ password    │     │ ubicacion        │     │ stock       │
│ rol         │     │ capacidad        │     │ precio      │
└─────────────┘     └──────────────────┘     └─────────────┘
       │                     │                       │
       │                     │                       │
       ▼                     ▼                       ▼
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│   movimientos    │  │inventario_bodega │  │movimiento_detalle│
├──────────────────┤  ├──────────────────┤  ├──────────────────┤
│ id (PK)          │  │ id (PK)          │  │ id (PK)          │
│ fecha            │  │ producto_id (FK) │  │ movimiento_id(FK)│
│ tipo_movimiento  │  │ bodega_id (FK)   │  │ producto_id (FK) │
│ usuario_id (FK)  │  │ stock            │  │ cantidad         │
│ bodega_origen(FK)│  └──────────────────┘  └──────────────────┘
│ bodega_destino(FK)│
└──────────────────┘
         │
         ▼
┌──────────────────┐
│   auditorias     │
├──────────────────┤
│ id (PK)          │
│ tipo_operacion   │
│ fecha_hora       │
│ usuario_id (FK)  │
│ entidad_afectada │
│ entidad_id       │
│ valores_anteriores│
│ valores_nuevos   │
└──────────────────┘
```

### Tablas

| Tabla | Descripción |
|-------|-------------|
| `usuarios` | Usuarios del sistema (ADMIN/EMPLEADO) |
| `bodegas` | Bodegas registradas con ubicación y capacidad |
| `productos` | Catálogo de productos con stock y precio |
| `movimientos` | Movimientos de inventario (entrada, salida, transferencia) |
| `movimiento_detalles` | Detalle de productos y cantidades por movimiento |
| `inventario_bodega` | Stock de cada producto por bodega |
| `auditorias` | Registro de auditoría automática de cambios |

---

## 🔒 Seguridad

- **Autenticación:** JWT (JSON Web Tokens) con tokens de 24 horas de duración
- **Autorización:** Basada en roles (ADMIN / EMPLEADO)
- **Protección de rutas:** Endpoints protegidos según el rol del usuario
- **Contraseñas:** Encriptadas con BCrypt
- **Manejo de errores:** Respuestas JSON personalizadas para errores 400, 401, 403, 404 y 500

### Roles y Permisos

| Recurso | ADMIN | EMPLEADO |
|---------|-------|----------|
| Bodegas (GET) | ✅ | ✅ |
| Bodegas (POST) | ✅ | ❌ |
| Bodegas (DELETE) | ✅ | ❌ |
| Productos (GET) | ✅ | ✅ |
| Productos (POST) | ✅ | ✅ |
| Productos (DELETE) | ✅ | ❌ |
| Movimientos | ✅ | ✅ |
| Auditorías | ✅ | ❌ |
| Reportes | ✅ | ✅ |
| Registrar Empleados | ✅ | ❌ |

---

## 🎨 Frontend

El proyecto incluye un frontend básico en HTML/CSS/JS ubicado en `src/main/resources/static/` que permite:

- **Login** con autenticación JWT
- **Dashboard** con resumen general
- **Gestión de bodegas** (CRUD)
- **Gestión de productos** (CRUD)
- **Movimientos de inventario** (registro y consulta)
- **Auditoría** (solo ADMIN)

Para acceder: [http://localhost:8080](http://localhost:8080)

---

## 🧪 Pruebas

```bash
# Ejecutar todas las pruebas
./mvnw test

# Ejecutar pruebas específicas
./mvnw test -Dtest=LogitrackApplicationTests
```

---

## 📄 Scripts SQL

### schema.sql
Define la estructura completa de la base de datos con todas las tablas, relaciones y restricciones.

### data.sql
Contiene datos de prueba iniciales:
- 3 usuarios (1 ADMIN + 2 EMPLEADOS)
- 3 bodegas
- 5 productos
- 2 movimientos de ejemplo
- 2 registros de auditoría
- Distribución inicial de stock por bodega

---

## 🛠️ Solución de Problemas

### Error: "No autorizado" al consumir endpoints
Asegúrate de incluir el header `Authorization: Bearer <token>` en todas las peticiones.

### Error de conexión a la base de datos
Verifica que PostgreSQL esté corriendo y que las credenciales en `application.properties` sean correctas.

### Puerto 8080 en uso
Cambia el puerto en `application.properties`:
```properties
server.port=9090
```

---

## 📦 Estructura de Paquetes Detallada

```
com.logitrack
├── config/
│   ├── OpenApiConfig.java        → Configuración Swagger/OpenAPI con JWT
│   ├── SpringContext.java        → Acceso a ApplicationContext desde clases no-Spring
│   ├── UserContext.java          → ThreadLocal para usuario autenticado
│   └── WebConfig.java            → Configuración CORS y MVC
├── controller/
│   ├── AuditoriaController.java  → Endpoints de auditoría (solo ADMIN)
│   ├── AuthController.java       → Login, registro, registro de empleados
│   ├── BodegaController.java     → CRUD de bodegas
│   ├── MovimientoInventarioController.java → Movimientos de inventario
│   ├── ProductoController.java   → CRUD de productos con inventario
│   ├── ReporteController.java    → Reportes y resúmenes
│   └── UsuarioController.java    → Listado de usuarios
├── dto/
│   ├── AuthResponse.java         → Respuesta de autenticación
│   ├── LoginRequest.java         → Solicitud de login
│   ├── ProductoConInventarioDTO.java → Producto con stock por bodega
│   ├── ProductoMovidoDTO.java    → Producto más movido
│   ├── RegisterEmpleadoRequest.java → Registro de empleado
│   ├── RegisterRequest.java      → Registro de usuario
│   ├── ResumenReporteDTO.java    → Resumen general del sistema
│   └── StockPorBodegaDTO.java    → Stock total por bodega
├── exception/
│   ├── BadRequestException.java  → Excepción 400
│   ├── GlobalExceptionHandler.java → Manejador global de errores
│   └── ResourceNotFoundException.java → Excepción 404
├── listener/
│   ├── AuditEntityListener.java  → Listener JPA para auditoría automática
│   ├── AuditoriaEvent.java       → Evento de auditoría
│   └── AuditoriaEventListener.java → Manejador de eventos de auditoría
├── model/
│   ├── Auditoria.java            → Entidad de auditoría
│   ├── Bodega.java               → Entidad bodega
│   ├── InventarioBodega.java     → Stock por bodega
│   ├── MovimientoDetalle.java    → Detalle de movimiento
│   ├── MovimientoInventario.java → Entidad movimiento
│   ├── Producto.java             → Entidad producto
│   ├── Rol.java                  → Enum: ADMIN, EMPLEADO
│   ├── TipoMovimiento.java       → Enum: ENTRADA, SALIDA, TRANSFERENCIA
│   ├── TipoOperacion.java        → Enum: INSERT, UPDATE, DELETE
│   └── Usuario.java              → Entidad usuario
├── repository/
│   ├── AuditoriaRepository.java
│   ├── BodegaRepository.java
│   ├── InventarioBodegaRepository.java
│   ├── MovimientoInventarioRepository.java
│   ├── ProductoRepository.java
│   └── UsuarioRepository.java
├── security/
│   ├── JwtAuthenticationFilter.java → Filtro JWT
│   ├── JwtTokenProvider.java     → Generación/validación de tokens
│   └── SecurityConfig.java       → Configuración de seguridad
└── service/
    ├── AuditoriaService.java     → Interfaz
    ├── AuditoriaServiceImpl.java → Implementación
    ├── BodegaService.java        → Interfaz
    ├── BodegaServiceImpl.java    → Implementación
    ├── MovimientoInventarioService.java → Interfaz
    ├── MovimientoInventarioServiceImpl.java → Implementación
    ├── ProductoService.java      → Interfaz
    ├── ProductoServiceImpl.java  → Implementación
    ├── ReporteService.java       → Interfaz
    └── ReporteServiceImpl.java   → Implementación
```

---

## 👥 Autores

- **David Orozco** - *Desarrollador de Software*
- **Felipe Corzo** - *Desarrollador de Software

---

