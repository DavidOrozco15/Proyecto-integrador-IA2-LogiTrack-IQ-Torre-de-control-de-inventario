-- LogiTrack S.A. - Datos Sinteticos Consistentes
-- Solo se mantienen los usuarios existentes (ids 1-4)

SET search_path TO proyecto;

-- ============================================
-- LIMPIEZA COMPLETA (excepto usuarios)
-- ============================================
DELETE FROM movimiento_detalles;
DELETE FROM movimientos;
DELETE FROM auditorias;
DELETE FROM resumen_panel;
DELETE FROM ordenes_compra;
DELETE FROM inventario_bodega;
DELETE FROM productos;
DELETE FROM proveedores;
DELETE FROM bodegas;

ALTER SEQUENCE IF EXISTS bodegas_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS proveedores_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS productos_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS inventario_bodega_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS movimientos_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS movimiento_detalles_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS auditorias_id_seq RESTART WITH 1;

-- ============================================
-- PROVEEDORES (6)
-- ============================================
INSERT INTO proveedores (id, nombre, contacto, telefono, dias_entrega) VALUES
(1, 'TechParts Colombia', 'Carlos Mendez', '310-555-1001', 5),
(2, 'Distribuidora Nacional', 'Ana Rodriguez', '315-555-1002', 7),
(3, 'Importaciones Globales', 'Luis Fernandez', '320-555-1003', 10),
(4, 'Mayorista Office Plus', 'Maria Lopez', '300-555-1004', 3),
(5, 'Electronica Express', 'Pedro Sanchez', '318-555-1005', 14),
(6, 'Suministros Rapid', 'Laura Gutierrez', '301-555-1006', 2);

-- ============================================
-- BODEGAS (5) - Bodega Central CRITICA
-- ============================================
INSERT INTO bodegas (id, nombre, ubicacion, capacidad, encargado_id) VALUES
(1, 'Bodega Central Bogota', 'Calle 26 #68-10, Bogota D.C.', 380, 1),
(2, 'Centro Distribucion Medellin', 'Carrera 48 #10-45, Medellin', 350, 2),
(3, 'Bodega Norte Cali', 'Avenida 6N #22-00, Cali', 400, 3),
(4, 'Bodega Sur Barranquilla', 'Calle 72 #54-30, Barranquilla', 350, 1),
(5, 'Bodega Occidente Pereira', 'Carrera 7 #35-12, Pereira', 400, 2);

-- ============================================
-- PRODUCTOS (30)
-- stock = suma de inventario en todas las bodegas
-- ============================================
INSERT INTO productos (id, nombre, categoria, stock, precio, proveedor_principal_id) VALUES
(1,  'Laptop Lenovo ThinkPad T14',       'Electronica',    48,  4500000, 1),
(2,  'Tablet iPad Air M2',               'Electronica',    25,  2900000, 5),
(3,  'Monitor Samsung 27" 4K',           'Electronica',    62,  1800000, 1),
(4,  'Monitor Dell UltraSharp 24"',      'Electronica',    38,  1200000, 1),
(5,  'Laptop HP EliteBook 840',          'Electronica',    30,  3800000, 2),
(6,  'Chromebook Lenovo 14"',            'Electronica',    45,  1500000, 2),
(7,  'Teclado Mecanico Logitech MX',     'Perifericos',    85,  350000,  2),
(8,  'Mouse Inalambrico Logitech MX',    'Perifericos',    60,  280000,  2),
(9,  'Webcam Logitech Brio 4K',          'Perifericos',    35,  650000,  2),
(10, 'Mouse Gamer RGB Corsair',          'Perifericos',    3,   180000,  1),
(11, 'Teclado Mini Mecanico Keychron',   'Perifericos',    2,   420000,  5),
(12, 'Mousepad XL SteelSeries',          'Perifericos',    90,  85000,   4),
(13, 'Monitor Portatil ASUS 15.6"',      'Perifericos',    18,  950000,  3),
(14, 'SSD Kingston 1TB NVMe',            'Almacenamiento', 120, 350000,  2),
(15, 'Disco Duro Externo SSD 2TB',       'Almacenamiento', 75,  450000,  1),
(16, 'Memoria RAM DDR5 16GB',            'Almacenamiento', 95,  320000,  2),
(17, 'Memoria RAM DDR4 8GB',             'Almacenamiento', 140, 150000,  4),
(18, 'Pendrive USB 128GB',               'Almacenamiento', 200, 45000,   4),
(19, 'Fuente de Poder Corsair 750W',     'Componentes',    42,  520000,  3),
(20, 'Tarjeta Grafica RTX 4060',         'Componentes',    4,   1800000, 5),
(21, 'Procesador AMD Ryzen 7 7800X',     'Componentes',    8,   1650000, 5),
(22, 'Case Torre ATX Mid-Tower',         'Componentes',    28,  380000,  3),
(23, 'Switch Cisco 24 Puertos',          'Redes',          25,  850000,  3),
(24, 'Router Ubiquiti UniFi AP',         'Redes',          15,  1200000, 3),
(25, 'Cable UTP Cat6 3m (Caja 100)',     'Redes',          50,  180000,  4),
(26, 'Audifonos Sony WH-1000XM5',       'Audio',          20,  1200000, 5),
(27, 'Parlante JBL Charge 5',            'Audio',          30,  450000,  6),
(28, 'Microfono Blue Yeti USB',          'Audio',          1,   580000,  5),
(29, 'Silla Ergonomica Herman Miller',   'Mobiliario',     10,  2800000, 3),
(30, 'Escritorio Executive 1.6m',        'Mobiliario',     6,   1200000, 3);

-- ============================================
-- INVENTARIO POR BODEGA
-- Verificado: suma de stock = producto.stock
-- ============================================

-- Bodega Central Bogota (cap: 380) ~350/380 = 92.1% CRITICA
INSERT INTO inventario_bodega (id, producto_id, bodega_id, stock) VALUES
(1,  1,  1, 31),
(2,  2,  1, 1),
(3,  4,  1, 20),
(4,  6,  1, 22),
(5,  7,  1, 32),
(6,  8,  1, 7),
(7,  9,  1, 12),
(8,  13, 1, 1),
(9,  14, 1, 37),
(10, 16, 1, 39),
(11, 18, 1, 80),
(12, 19, 1, 20),
(13, 22, 1, 12),
(14, 23, 1, 12),
(15, 25, 1, 5),
(16, 27, 1, 17),
(17, 30, 1, 2);

-- Centro Distribucion Medellin (cap: 350) ~255/350 = 72.9%
INSERT INTO inventario_bodega (id, producto_id, bodega_id, stock) VALUES
(18, 3,  2, 59),
(19, 5,  2, 6),
(20, 8,  2, 21),
(21, 9,  2, 3),
(22, 10, 2, 2),
(23, 12, 2, 10),
(24, 14, 2, 33),
(25, 15, 2, 33),
(26, 17, 2, 46),
(27, 18, 2, 5),
(28, 20, 2, 2),
(29, 21, 2, 4),
(30, 23, 2, 13),
(31, 24, 2, 9),
(32, 28, 2, 1),
(33, 29, 2, 4),
(34, 30, 2, 4);

-- Bodega Norte Cali (cap: 400) ~242/400 = 60.5%
INSERT INTO inventario_bodega (id, producto_id, bodega_id, stock) VALUES
(35, 1,  3, 17),
(36, 5,  3, 19),
(37, 7,  3, 17),
(38, 8,  3, 27),
(39, 11, 3, 2),
(40, 12, 3, 30),
(41, 13, 3, 8),
(42, 15, 3, 9),
(43, 16, 3, 37),
(44, 17, 3, 48),
(45, 20, 3, 1),
(46, 21, 3, 2),
(47, 22, 3, 13),
(48, 26, 3, 10),
(49, 29, 3, 2);

-- Bodega Sur Barranquilla (cap: 350) ~246/350 = 70.3%
INSERT INTO inventario_bodega (id, producto_id, bodega_id, stock) VALUES
(50, 4,  4, 6),
(51, 5,  4, 5),
(52, 6,  4, 23),
(53, 7,  4, 11),
(54, 9,  4, 20),
(55, 10, 4, 1),
(56, 12, 4, 8),
(57, 13, 4, 1),
(58, 14, 4, 27),
(59, 18, 4, 86),
(60, 19, 4, 22),
(61, 20, 4, 1),
(62, 25, 4, 22),
(63, 26, 4, 10),
(64, 29, 4, 3);

-- Bodega Occidente Pereira (cap: 400) ~317/400 = 79.2%
INSERT INTO inventario_bodega (id, producto_id, bodega_id, stock) VALUES
(65, 2,  5, 24),
(66, 3,  5, 3),
(67, 4,  5, 12),
(68, 7,  5, 25),
(69, 8,  5, 5),
(70, 12, 5, 42),
(71, 13, 5, 8),
(72, 14, 5, 23),
(73, 15, 5, 33),
(74, 16, 5, 19),
(75, 17, 5, 46),
(76, 18, 5, 29),
(77, 21, 5, 2),
(78, 22, 5, 3),
(79, 24, 5, 6),
(80, 25, 5, 23),
(81, 27, 5, 13),
(82, 29, 5, 1);

-- ============================================
-- MOVIMIENTOS (30 dias)
-- ============================================
INSERT INTO movimientos (id, fecha, tipo_movimiento, usuario_id, bodega_origen_id, bodega_destino_id) VALUES
(1,  '2026-08-27 08:00:00', 'ENTRADA',       1, NULL, 1),
(2,  '2026-08-27 09:00:00', 'ENTRADA',       1, NULL, 2),
(3,  '2026-08-27 10:00:00', 'SALIDA',        2, 1,    NULL),
(4,  '2026-08-27 11:00:00', 'SALIDA',        3, 2,    NULL),
(5,  '2026-08-27 14:00:00', 'TRANSFERENCIA', 1, 1,    3),
(6,  '2026-08-27 15:00:00', 'ENTRADA',       1, NULL, 4),
(7,  '2026-08-27 16:00:00', 'SALIDA',        2, 3,    NULL),
(8,  '2026-08-26 09:00:00', 'ENTRADA',       1, NULL, 1),
(9,  '2026-08-26 10:00:00', 'SALIDA',        2, 1,    NULL),
(10, '2026-08-26 14:00:00', 'TRANSFERENCIA', 3, 2,    1),
(11, '2026-08-25 08:00:00', 'ENTRADA',       1, NULL, 2),
(12, '2026-08-25 10:00:00', 'SALIDA',        2, 1,    NULL),
(13, '2026-08-25 14:00:00', 'SALIDA',        3, 4,    NULL),
(14, '2026-08-23 09:00:00', 'ENTRADA',       1, NULL, 3),
(15, '2026-08-23 10:00:00', 'SALIDA',        2, 2,    NULL),
(16, '2026-08-23 14:00:00', 'TRANSFERENCIA', 1, 1,    4),
(17, '2026-08-21 08:00:00', 'ENTRADA',       1, NULL, 1),
(18, '2026-08-21 10:00:00', 'SALIDA',        2, 3,    NULL),
(19, '2026-08-21 11:00:00', 'ENTRADA',       1, NULL, 2),
(20, '2026-08-18 09:00:00', 'SALIDA',        2, 1,    NULL),
(21, '2026-08-18 10:00:00', 'ENTRADA',       1, NULL, 3),
(22, '2026-08-18 14:00:00', 'TRANSFERENCIA', 3, 2,    4),
(23, '2026-08-14 08:00:00', 'ENTRADA',       1, NULL, 1),
(24, '2026-08-14 10:00:00', 'SALIDA',        2, 1,    NULL),
(25, '2026-08-14 11:00:00', 'SALIDA',        3, 2,    NULL),
(26, '2026-08-08 09:00:00', 'ENTRADA',       1, NULL, 2),
(27, '2026-08-08 10:00:00', 'SALIDA',        2, 4,    NULL),
(28, '2026-08-08 14:00:00', 'TRANSFERENCIA', 1, 3,    1),
(29, '2026-08-03 09:00:00', 'ENTRADA',       1, NULL, 1),
(30, '2026-08-03 10:00:00', 'SALIDA',        2, 1,    NULL),
(31, '2026-07-31 09:00:00', 'SALIDA',        2, 1,    NULL),
(32, '2026-07-31 11:00:00', 'SALIDA',        3, 2,    NULL);

-- ============================================
-- DETALLES DE MOVIMIENTOS
-- EN RIESGO: Mouse Gamer(10), Teclado Keychron(11),
-- GPU RTX(20), Microfono Blue(28)
-- ============================================
INSERT INTO movimiento_detalles (id, movimiento_id, producto_id, cantidad) VALUES
-- AYER
(1, 1, 3, 10),    (2, 1, 7, 15),    (3, 2, 8, 12),    (4, 2, 14, 20),
(5, 3, 10, 1),    (6, 3, 11, 1),    (7, 3, 20, 1),    (8, 4, 28, 1),
(9, 4, 10, 1),    (10, 4, 20, 1),   (11, 5, 19, 4),   (12, 5, 22, 3),
(13, 6, 29, 2),   (14, 7, 3, 2),    (15, 7, 20, 1),   (16, 7, 10, 1),
(17, 7, 28, 1),
-- HACE 2 DIAS
(18, 8, 5, 5),    (19, 8, 9, 8),    (20, 9, 15, 4),   (21, 9, 27, 3),
(22, 10, 6, 6),
-- HACE 3 DIAS
(23, 11, 20, 2),  (24, 11, 21, 2),  (25, 12, 23, 2),  (26, 12, 24, 1),
(27, 13, 26, 1),
-- HACE 5 DIAS
(28, 14, 12, 10), (29, 14, 25, 8),  (30, 15, 29, 1),  (31, 15, 30, 1),
(32, 16, 4, 4),
-- HACE 7 DIAS
(33, 17, 2, 8),   (34, 17, 26, 4),  (35, 18, 10, 1),  (36, 18, 11, 1),
(37, 19, 13, 5),
-- HACE 10 DIAS
(38, 20, 1, 2),   (39, 20, 5, 3),   (40, 21, 19, 5),  (41, 21, 28, 1),
(42, 22, 22, 4),
-- HACE 14 DIAS
(43, 23, 15, 10), (44, 23, 17, 15), (45, 24, 1, 4),   (46, 24, 8, 6),
(47, 25, 20, 1),  (48, 25, 27, 2),
-- HACE 20 DIAS
(49, 26, 6, 8),   (50, 26, 28, 1),  (51, 27, 26, 2),  (52, 27, 13, 2),
(53, 28, 16, 10),
-- HACE 25 DIAS
(54, 29, 18, 30), (55, 29, 12, 15), (56, 30, 14, 5),  (57, 30, 23, 3),
-- HACE 28 DIAS
(58, 31, 10, 1),  (59, 31, 11, 1),  (60, 31, 20, 1),  (61, 32, 28, 1),
(62, 32, 10, 1);

-- ============================================
-- AUDITORIAS
-- ============================================
INSERT INTO auditorias (id, tipo_operacion, fecha_hora, usuario_id, entidad_afectada, entidad_id, valores_anteriores, valores_nuevos) VALUES
(1, 'INSERT', '2026-08-03 09:00:00', 1, 'Bodega', 1, NULL, '{"nombre":"Bodega Central Bogota","capacidad":380}'),
(2, 'INSERT', '2026-08-03 09:05:00', 1, 'Bodega', 2, NULL, '{"nombre":"Centro Distribucion Medellin","capacidad":350}'),
(3, 'INSERT', '2026-08-03 09:10:00', 1, 'Producto', 1, NULL, '{"nombre":"Laptop Lenovo ThinkPad T14","stock":48}');
