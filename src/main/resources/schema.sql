-- LogiTrack S.A. - Schema de Base de Datos PostgreSQL

CREATE SCHEMA IF NOT EXISTS proyecto;
SET search_path TO proyecto;

CREATE TABLE IF NOT EXISTS usuarios (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS bodegas (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    ubicacion VARCHAR(150) NOT NULL,
    capacidad INT NOT NULL,
    encargado_id BIGINT,
    FOREIGN KEY (encargado_id) REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS productos (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    categoria VARCHAR(50),
    stock INT NOT NULL DEFAULT 0,
    precio DECIMAL(10, 2) NOT NULL
);

CREATE TABLE IF NOT EXISTS movimientos (
    id BIGSERIAL PRIMARY KEY,
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tipo_movimiento VARCHAR(20) NOT NULL,
    usuario_id BIGINT NOT NULL,
    bodega_origen_id BIGINT,
    bodega_destino_id BIGINT,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    FOREIGN KEY (bodega_origen_id) REFERENCES bodegas(id),
    FOREIGN KEY (bodega_destino_id) REFERENCES bodegas(id)
);

CREATE TABLE IF NOT EXISTS movimiento_detalles (
    id BIGSERIAL PRIMARY KEY,
    movimiento_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    cantidad INT NOT NULL,
    FOREIGN KEY (movimiento_id) REFERENCES movimientos(id) ON DELETE CASCADE,
    FOREIGN KEY (producto_id) REFERENCES productos(id)
);

CREATE TABLE IF NOT EXISTS proveedores (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    contacto VARCHAR(100),
    telefono VARCHAR(20),
    dias_entrega INT NOT NULL
);

ALTER TABLE productos ADD COLUMN IF NOT EXISTS proveedor_principal_id BIGINT;
ALTER TABLE productos ADD FOREIGN KEY (proveedor_principal_id) REFERENCES proveedores(id) ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS ordenes_compra (
    id BIGSERIAL PRIMARY KEY,
    producto_id BIGINT NOT NULL,
    proveedor_id BIGINT NOT NULL,
    bodega_destino_id BIGINT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(10, 2) NOT NULL,
    total DECIMAL(10, 2) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    pdf_bytes BYTEA,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (producto_id) REFERENCES productos(id),
    FOREIGN KEY (proveedor_id) REFERENCES proveedores(id),
    FOREIGN KEY (bodega_destino_id) REFERENCES bodegas(id)
);

CREATE TABLE IF NOT EXISTS resumen_panel (
    id BIGSERIAL PRIMARY KEY,
    fecha DATE NOT NULL,
    narrativa VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS alertas_panel (
    id BIGSERIAL PRIMARY KEY,
    resumen_id BIGINT NOT NULL,
    severidad VARCHAR(10) NOT NULL,
    titulo VARCHAR(100) NOT NULL,
    detalle VARCHAR(500) NOT NULL,
    producto_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (resumen_id) REFERENCES resumen_panel(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS acciones_sugeridas_panel (
    id BIGSERIAL PRIMARY KEY,
    resumen_id BIGINT NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    descripcion VARCHAR(500) NOT NULL,
    orden_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (resumen_id) REFERENCES resumen_panel(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS auditorias (
    id BIGSERIAL PRIMARY KEY,
    tipo_operacion VARCHAR(20) NOT NULL,
    fecha_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_id BIGINT,
    entidad_afectada VARCHAR(50) NOT NULL,
    entidad_id BIGINT,
    valores_anteriores TEXT,
    valores_nuevos TEXT,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE SET NULL
);

-- Tabla: inventario_bodega (stock de cada producto por bodega)
CREATE TABLE IF NOT EXISTS inventario_bodega (
    id BIGSERIAL PRIMARY KEY,
    producto_id BIGINT NOT NULL,
    bodega_id BIGINT NOT NULL,
    stock INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (producto_id) REFERENCES productos(id) ON DELETE CASCADE,
    FOREIGN KEY (bodega_id) REFERENCES bodegas(id) ON DELETE CASCADE,
    UNIQUE (producto_id, bodega_id)
);
