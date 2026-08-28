-- LogiTrack S.A. - Datos Iniciales de Prueba (PostgreSQL)

SET search_path TO proyecto;

-- Usuarios (Contraseña codificada con BCrypt: "admin123")
INSERT INTO usuarios (id, username, email, password, rol) VALUES
(1, 'admin', 'admin@logitrac.com', '$2a$10$EblZqNptyYvcLm/VwDCVAu.23Q.4xZ41rE24R9Y/x/c.d1e2f3g4', 'ADMIN'),
(2, 'jdoe', 'j.doe@logitrac.com', '$2a$10$EblZqNptyYvcLm/VwDCVAu.23Q.4xZ41rE24R9Y/x/c.d1e2f3g4', 'EMPLEADO'),
(3, 'mgarcia', 'm.garcia@logitrac.com', '$2a$10$EblZqNptyYvcLm/VwDCVAu.23Q.4xZ41rE24R9Y/x/c.d1e2f3g4', 'EMPLEADO'),
(4, 'agente_user', 'agente@logitrac.com', '$2a$10$EblZqNptyYvcLm/VwDCVAu.23Q.4xZ41rE24R9Y/x/c.d1e2f3g4', 'AGENTE')
ON CONFLICT (id) DO NOTHING;

-- Bodegas
INSERT INTO bodegas (id, nombre, ubicacion, capacidad, encargado_id) VALUES
(1, 'Bodega Central Bogota', 'Calle 26 # 68-10, Bogota', 50000, 1),
(2, 'Centro Distribucion Medellin', 'Carrera 48 # 10-45, Medellin', 35000, 2),
(3, 'Bodega Norte Cali', 'Avenida 6N # 22-00, Cali', 20000, 3)
ON CONFLICT (id) DO NOTHING;

-- Productos
INSERT INTO productos (id, nombre, categoria, stock, precio) VALUES
(1, 'Laptop Lenovo ThinkPad T14', 'Electronica', 145, 1200.00),
(2, 'Monitor Dell UltraSharp 27"', 'Perifericos', 8, 450.50),
(3, 'Teclado Mecanico Logitech MX', 'Perifericos', 65, 120.00),
(4, 'Silla Ergonomica Herman Miller', 'Mobiliario', 4, 950.00),
(5, 'Disco Duro Externo SSD 2TB', 'Almacenamiento', 200, 180.00)
ON CONFLICT (id) DO NOTHING;

-- Movimientos Iniciales
INSERT INTO movimientos (id, fecha, tipo_movimiento, usuario_id, bodega_origen_id, bodega_destino_id) VALUES
(1, CURRENT_TIMESTAMP, 'ENTRADA', 1, NULL, 1),
(2, CURRENT_TIMESTAMP, 'TRANSFERENCIA', 2, 1, 2)
ON CONFLICT (id) DO NOTHING;

-- Detalle de Movimientos
INSERT INTO movimiento_detalles (id, movimiento_id, producto_id, cantidad) VALUES
(1, 1, 1, 50),
(2, 1, 3, 20),
(3, 2, 2, 5)
ON CONFLICT (id) DO NOTHING;

-- Auditorias
INSERT INTO auditorias (id, tipo_operacion, fecha_hora, usuario_id, entidad_afectada, entidad_id, valores_anteriores, valores_nuevos) VALUES
(1, 'INSERT', CURRENT_TIMESTAMP, 1, 'Bodega', 1, NULL, '{"id": 1, "nombre": "Bodega Central Bogota", "capacidad": 50000}'),
(2, 'INSERT', CURRENT_TIMESTAMP, 1, 'Producto', 1, NULL, '{"id": 1, "nombre": "Laptop Lenovo ThinkPad T14", "stock": 145}')
ON CONFLICT (id) DO NOTHING;

-- Distribución inicial de stock por bodega (coincide con productos.stock)
INSERT INTO inventario_bodega (id, producto_id, bodega_id, stock) VALUES
(1, 1, 1, 145),   -- Laptop en Bodega Central Bogota
(2, 2, 2, 8),     -- Monitor en Centro Distribucion Medellin
(3, 3, 1, 65),    -- Teclado en Bodega Central Bogota
(4, 4, 3, 4),     -- Silla en Bodega Norte Cali
(5, 5, 1, 200)    -- Disco Duro en Bodega Central Bogota
ON CONFLICT (id) DO NOTHING;