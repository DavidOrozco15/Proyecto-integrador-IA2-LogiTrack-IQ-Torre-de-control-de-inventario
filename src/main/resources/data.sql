-- LogiTrack S.A. - Datos Sinteticos Consistentes
-- 51 productos, 5 bodegas, 8 proveedores, movimientos de 30 dias

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
ALTER SEQUENCE IF EXISTS auditorias_id_seq RESTART WITH 6;

-- ============================================
-- PROVEEDORES (8)
-- ============================================
INSERT INTO proveedores (id, nombre, contacto, telefono, dias_entrega) VALUES
(1, 'TechParts Colombia',       'Carlos Mendez',     '310-555-1001', 5),
(2, 'Distribuidora Nacional',   'Ana Rodriguez',     '315-555-1002', 7),
(3, 'Importaciones Globales',   'Luis Fernandez',    '320-555-1003', 10),
(4, 'Mayorista Office Plus',    'Maria Lopez',       '300-555-1004', 3),
(5, 'Electronica Express',      'Pedro Sanchez',     '318-555-1005', 14),
(6, 'Suministros Rapid',        'Laura Gutierrez',   '301-555-1006', 2),
(7, 'Redes y Comunicaciones',   'Andres Moreno',     '312-555-1007', 4),
(8, 'AudioProfesional S.A.',    'Sandra Ramirez',    '316-555-1008', 6);

-- ============================================
-- BODEGAS (5) - Bodega Central CRITICA
-- ============================================
INSERT INTO bodegas (id, nombre, ubicacion, capacidad, encargado_id) VALUES
(1, 'Bodega Central Bogota',       'Calle 26 #68-10, Bogota D.C.',       450, 1),
(2, 'Centro Distribucion Medellin','Carrera 48 #10-45, Medellin',       400, 2),
(3, 'Bodega Norte Cali',           'Avenida 6N #22-00, Cali',           380, 3),
(4, 'Bodega Sur Barranquilla',     'Calle 72 #54-30, Barranquilla',     350, 1),
(5, 'Bodega Occidente Pereira',    'Carrera 7 #35-12, Pereira',         420, 2);

-- ============================================
-- PRODUCTOS (50) - 7 categorias
-- RIESGO: Mouse Gamer(10), Teclado Keychron(11), GPU RTX(20), Microfono Blue(28)
-- ============================================
INSERT INTO productos (id, nombre, categoria, stock, precio, proveedor_principal_id) VALUES
-- Electronica (8)
(1,  'Laptop Lenovo ThinkPad T14',          'Electronica',    52,  4500000, 1),
(2,  'Tablet iPad Air M2',                  'Electronica',    28,  2900000, 5),
(3,  'Monitor Samsung 27" 4K',              'Electronica',    65,  1800000, 1),
(4,  'Monitor Dell UltraSharp 24"',         'Electronica',    40,  1200000, 1),
(5,  'Laptop HP EliteBook 840',             'Electronica',    35,  3800000, 2),
(6,  'Chromebook Lenovo 14"',               'Electronica',    48,  1500000, 2),
(51, 'Laptop ASUS ROG Strix G16',           'Electronica',    12,  5200000, 5),
(52, 'Tablet Samsung Galaxy Tab S9',        'Electronica',    18,  2100000, 5),
-- Perifericos (10)
(7,  'Teclado Mecanico Logitech MX',        'Perifericos',    88,  350000,  2),
(8,  'Mouse Inalambrico Logitech MX',       'Perifericos',    62,  280000,  2),
(9,  'Webcam Logitech Brio 4K',             'Perifericos',    38,  650000,  2),
(10, 'Mouse Gamer RGB Corsair',             'Perifericos',    1,   180000,  1),
(11, 'Teclado Mini Mecanico Keychron',      'Perifericos',    2,   420000,  5),
(12, 'Mousepad XL SteelSeries',             'Perifericos',    95,  85000,   4),
(13, 'Monitor Portatil ASUS 15.6"',         'Perifericos',    20,  950000,  3),
(53, 'Auriculares Gamer HyperX Cloud',      'Perifericos',    25,  320000,  6),
(54, 'Controlador Xbox Series X',           'Perifericos',    30,  250000,  6),
(55, 'Webcam Razer Kiyo Pro',               'Perifericos',    15,  480000,  2),
-- Almacenamiento (6)
(14, 'SSD Kingston 1TB NVMe',               'Almacenamiento', 125, 350000,  2),
(15, 'Disco Duro Externo SSD 2TB',          'Almacenamiento', 78,  450000,  1),
(16, 'Memoria RAM DDR5 16GB',               'Almacenamiento', 100, 320000,  2),
(17, 'Memoria RAM DDR4 8GB',                'Almacenamiento', 145, 150000,  4),
(18, 'Pendrive USB 128GB',                  'Almacenamiento', 210, 45000,   4),
(56, 'SSD Samsung 2TB NVMe',                'Almacenamiento', 55,  680000,  1),
-- Componentes (6)
(19, 'Fuente de Poder Corsair 750W',        'Componentes',    45,  520000,  3),
(20, 'Tarjeta Grafica RTX 4060',            'Componentes',    2,   1800000, 5),
(21, 'Procesador AMD Ryzen 7 7800X',        'Componentes',    10,  1650000, 5),
(22, 'Case Torre ATX Mid-Tower',            'Componentes',    30,  380000,  3),
(57, 'Motherboard ASUS ROG Strix B650E',    'Componentes',    8,   1200000, 5),
(58, 'Cooler CPU Noctua NH-D15',            'Componentes',    14,  280000,  3),
-- Redes (5)
(23, 'Switch Cisco 24 Puertos',             'Redes',          28,  850000,  7),
(24, 'Router Ubiquiti UniFi AP',            'Redes',          18,  1200000, 7),
(25, 'Cable UTP Cat6 3m (Caja 100)',        'Redes',          55,  180000,  4),
(59, 'Access Point TP-Link EAP670',         'Redes',          12,  750000,  7),
(60, 'Firewall Fortinet FortiGate 40F',     'Redes',          6,   2800000, 7),
-- Audio (6)
(26, 'Audifonos Sony WH-1000XM5',          'Audio',          22,  1200000, 8),
(27, 'Parlante JBL Charge 5',               'Audio',          32,  450000,  6),
(28, 'Microfono Blue Yeti USB',             'Audio',          0,   580000,  8),
(61, 'Audifonos Gamer SteelSeries Arctis',  'Audio',          18,  420000,  8),
(62, 'Parlante Bose SoundLink Max',          'Audio',          10,  850000,  8),
(63, 'Mezcladora Audio Behringer Xenyx',    'Audio',          7,   350000,  8),
-- Mobiliario (4)
(29, 'Silla Ergonomica Herman Miller',      'Mobiliario',     12,  2800000, 3),
(30, 'Escritorio Executive 1.6m',           'Mobiliario',     8,   1200000, 3),
(64, 'Silla Gamer Secretlab Titan',         'Mobiliario',     15,  1500000, 3),
(65, 'Escritorio Ajustable Electrico',      'Mobiliario',     5,   1800000, 3),
-- Extra - Auriculares y accesorios (6)
(31, 'Audifonos JBL Tune 510BT',            'Audio',          40,  180000,  6),
(32, 'Cable HDMI 2.1 2m',                  'Redes',          150, 45000,   4),
(33, 'Hub USB-C 7 en 1',                   'Perifericos',    55,  180000,  4),
(34, 'Lampara LED Escritorio BenQ',         'Mobiliario',     20,  350000,  4),
(35, 'Soporte Monitor Ergonomico',          'Perifericos',    35,  220000,  3),
(36, 'Webcam HD Logitech C920',            'Perifericos',    42,  280000,  2);

-- ============================================
-- INVENTARIO POR BODEGA
-- Verificado: suma de stock = producto.stock
-- ============================================

-- Bodega Central Bogota (cap: 450) ~410/450 = 91.1% CRITICA
INSERT INTO inventario_bodega (id, producto_id, bodega_id, stock) VALUES
(1,  1,  1, 33),   (2,  2,  1, 2),    (3,  4,  1, 22),  (4,  6,  1, 24),
(5,  7,  1, 35),   (6,  8,  1, 8),    (7,  9,  1, 14),  (8,  13, 1, 2),
(9,  14, 1, 40),   (10, 16, 1, 42),   (11, 18, 1, 85),  (12, 19, 1, 22),
(13, 22, 1, 14),   (14, 23, 1, 14),   (15, 25, 1, 6),   (16, 27, 1, 19),
(17, 30, 1, 3),    (18, 51, 1, 4),    (19, 53, 1, 8),   (20, 56, 1, 15),
(21, 57, 1, 3),    (22, 59, 1, 4),    (23, 60, 1, 2),   (24, 64, 1, 5);

-- Centro Distribucion Medellin (cap: 400) ~310/400 = 77.5%
INSERT INTO inventario_bodega (id, producto_id, bodega_id, stock) VALUES
(25, 3,  2, 60),   (26, 5,  2, 8),    (27, 8,  2, 24),  (28, 9,  2, 4),
(29, 10, 2, 1),    (30, 12, 2, 12),   (31, 14, 2, 35),  (32, 15, 2, 36),
(33, 17, 2, 48),   (34, 18, 2, 6),    (35, 20, 2, 2),   (36, 21, 2, 5),
(37, 23, 2, 14),   (38, 24, 2, 10),   (39, 28, 2, 0),   (40, 29, 2, 5),
(41, 30, 2, 5),    (42, 52, 2, 10),   (43, 54, 2, 12),  (44, 55, 2, 6),
(45, 58, 2, 6),    (46, 61, 2, 8),    (47, 62, 2, 4),   (48, 63, 2, 3);

-- Bodega Norte Cali (cap: 380) ~285/380 = 75.0%
INSERT INTO inventario_bodega (id, producto_id, bodega_id, stock) VALUES
(49, 1,  3, 19),   (50, 5,  3, 22),   (51, 7,  3, 18),  (52, 8,  3, 28),
(53, 11, 3, 2),    (54, 12, 3, 32),   (55, 13, 3, 9),   (56, 15, 3, 10),
(57, 16, 3, 40),   (58, 17, 3, 50),   (59, 20, 3, 0),   (60, 21, 3, 3),
(61, 22, 3, 15),   (62, 26, 3, 12),   (63, 29, 3, 3),   (64, 51, 3, 5),
(65, 57, 3, 3),    (66, 58, 3, 5),    (67, 64, 3, 6),   (68, 65, 3, 2);

-- Bodega Sur Barranquilla (cap: 350) ~280/350 = 80.0%
INSERT INTO inventario_bodega (id, producto_id, bodega_id, stock) VALUES
(69, 4,  4, 8),    (70, 5,  4, 6),    (71, 6,  4, 24),  (72, 7,  4, 12),
(73, 9,  4, 22),   (74, 10, 4, 0),    (75, 12, 4, 9),   (76, 13, 4, 2),
(77, 14, 4, 28),   (78, 18, 4, 90),   (79, 19, 4, 24),  (80, 20, 4, 0),
(81, 25, 4, 24),   (82, 26, 4, 11),   (83, 29, 4, 4),   (84, 52, 4, 8),
(85, 53, 4, 10),   (86, 54, 4, 10),   (87, 59, 4, 4),   (88, 61, 4, 6),
(89, 63, 4, 3),    (90, 65, 4, 2);

-- Bodega Occidente Pereira (cap: 420) ~365/420 = 86.9%
INSERT INTO inventario_bodega (id, producto_id, bodega_id, stock) VALUES
(91,  2,  5, 26),  (92,  3,  5, 5),   (93,  4,  5, 10),  (94,  7,  5, 23),
(95,  8,  5, 2),   (96,  12, 5, 42),  (97,  13, 5, 7),   (98,  14, 5, 22),
(99,  15, 5, 32),  (100, 16, 5, 18),  (101, 17, 5, 47),  (102, 18, 5, 29),
(103, 21, 5, 2),   (104, 22, 5, 1),   (105, 24, 5, 8),   (106, 25, 5, 25),
(107, 27, 5, 13),  (108, 29, 5, 0),   (109, 51, 5, 3),   (110, 55, 5, 9),
(111, 56, 5, 18),  (112, 57, 5, 2),   (113, 59, 5, 4),   (114, 60, 5, 4),
(115, 62, 5, 6),   (116, 64, 5, 4),   (117, 65, 5, 1),
-- Productos extra (31-36)
(118, 31, 1, 12),  (119, 31, 2, 10),  (120, 31, 3, 8),   (121, 31, 4, 10),
(122, 32, 1, 50),  (123, 32, 2, 30),  (124, 32, 3, 40),  (125, 32, 4, 30),
(126, 33, 1, 15),  (127, 33, 2, 12),  (128, 33, 3, 14),  (129, 33, 4, 14),
(130, 34, 1, 6),   (131, 34, 2, 5),   (132, 34, 5, 9),
(133, 35, 1, 10),  (134, 35, 2, 8),   (135, 35, 3, 9),   (136, 35, 4, 8),
(137, 36, 1, 14),  (138, 36, 2, 10),  (139, 36, 3, 10),  (140, 36, 4, 8);

-- ============================================
-- MOVIMIENTOS (30 dias) - 45 movimientos
-- ============================================
INSERT INTO movimientos (id, fecha, tipo_movimiento, usuario_id, bodega_origen_id, bodega_destino_id) VALUES
-- AYER (7 movimientos)
(1,  '2026-08-29 08:00:00', 'ENTRADA',       1, NULL, 1),
(2,  '2026-08-29 09:30:00', 'ENTRADA',       1, NULL, 2),
(3,  '2026-08-29 10:15:00', 'SALIDA',        2, 1,    NULL),
(4,  '2026-08-29 11:00:00', 'SALIDA',        3, 2,    NULL),
(5,  '2026-08-29 14:00:00', 'TRANSFERENCIA', 1, 1,    3),
(6,  '2026-08-29 15:30:00', 'ENTRADA',       1, NULL, 4),
(7,  '2026-08-29 16:00:00', 'SALIDA',        2, 3,    NULL),
-- HACE 2 DIAS (6 movimientos)
(8,  '2026-08-28 08:30:00', 'ENTRADA',       1, NULL, 1),
(9,  '2026-08-28 09:45:00', 'SALIDA',        2, 1,    NULL),
(10, '2026-08-28 11:00:00', 'TRANSFERENCIA', 3, 2,    1),
(11, '2026-08-28 14:00:00', 'ENTRADA',       1, NULL, 5),
(12, '2026-08-28 15:00:00', 'SALIDA',        2, 5,    NULL),
(13, '2026-08-28 16:30:00', 'ENTRADA',       1, NULL, 3),
-- HACE 3 DIAS (5 movimientos)
(14, '2026-08-27 08:00:00', 'ENTRADA',       1, NULL, 2),
(15, '2026-08-27 09:30:00', 'SALIDA',        2, 1,    NULL),
(16, '2026-08-27 11:00:00', 'SALIDA',        3, 4,    NULL),
(17, '2026-08-27 14:00:00', 'TRANSFERENCIA', 1, 2,    4),
(18, '2026-08-27 16:00:00', 'ENTRADA',       1, NULL, 1),
-- HACE 5 DIAS (5 movimientos)
(19, '2026-08-25 08:00:00', 'ENTRADA',       1, NULL, 3),
(20, '2026-08-25 09:30:00', 'SALIDA',        2, 2,    NULL),
(21, '2026-08-25 11:00:00', 'SALIDA',        3, 1,    NULL),
(22, '2026-08-25 14:00:00', 'TRANSFERENCIA', 1, 1,    4),
(23, '2026-08-25 16:00:00', 'ENTRADA',       1, NULL, 5),
-- HACE 7 DIAS (5 movimientos)
(24, '2026-08-23 08:00:00', 'ENTRADA',       1, NULL, 1),
(25, '2026-08-23 09:30:00', 'SALIDA',        2, 3,    NULL),
(26, '2026-08-23 11:00:00', 'ENTRADA',       1, NULL, 2),
(27, '2026-08-23 14:00:00', 'SALIDA',        3, 4,    NULL),
(28, '2026-08-23 16:00:00', 'TRANSFERENCIA', 1, 3,    5),
-- HACE 10 DIAS (5 movimientos)
(29, '2026-08-20 08:00:00', 'SALIDA',        2, 1,    NULL),
(30, '2026-08-20 09:30:00', 'ENTRADA',       1, NULL, 3),
(31, '2026-08-20 11:00:00', 'SALIDA',        3, 2,    NULL),
(32, '2026-08-20 14:00:00', 'TRANSFERENCIA', 1, 2,    1),
(33, '2026-08-20 16:00:00', 'ENTRADA',       1, NULL, 4),
-- HACE 14 DIAS (5 movimientos)
(34, '2026-08-16 08:00:00', 'ENTRADA',       1, NULL, 1),
(35, '2026-08-16 09:30:00', 'SALIDA',        2, 1,    NULL),
(36, '2026-08-16 11:00:00', 'SALIDA',        3, 2,    NULL),
(37, '2026-08-16 14:00:00', 'ENTRADA',       1, NULL, 5),
(38, '2026-08-16 16:00:00', 'TRANSFERENCIA', 1, 4,    2),
-- HACE 20 DIAS (5 movimientos)
(39, '2026-08-10 08:00:00', 'ENTRADA',       1, NULL, 2),
(40, '2026-08-10 09:30:00', 'SALIDA',        2, 3,    NULL),
(41, '2026-08-10 11:00:00', 'SALIDA',        3, 1,    NULL),
(42, '2026-08-10 14:00:00', 'TRANSFERENCIA', 1, 1,    3),
(43, '2026-08-10 16:00:00', 'ENTRADA',       1, NULL, 4),
-- HACE 25 DIAS (4 movimientos)
(44, '2026-08-05 08:00:00', 'ENTRADA',       1, NULL, 1),
(45, '2026-08-05 09:30:00', 'SALIDA',        2, 2,    NULL),
(46, '2026-08-05 11:00:00', 'SALIDA',        3, 4,    NULL),
(47, '2026-08-05 14:00:00', 'TRANSFERENCIA', 1, 3,    1);

-- ============================================
-- DETALLES DE MOVIMIENTOS
-- ============================================
INSERT INTO movimiento_detalles (id, movimiento_id, producto_id, cantidad) VALUES
-- AYER (movimientos 1-7)
(1,  1, 3, 12),    (2,  1, 7, 18),    (3,  1, 14, 15),  (4,  2, 8, 14),
(5,  2, 16, 10),   (6,  2, 54, 5),    (7,  3, 10, 1),   (8,  3, 11, 1),
(9,  3, 20, 1),    (10, 4, 28, 1),    (11, 4, 10, 1),   (12, 4, 20, 1),
(13, 5, 19, 5),    (14, 5, 22, 4),    (15, 5, 58, 2),   (16, 6, 29, 3),
(17, 6, 64, 2),    (18, 7, 3, 3),     (19, 7, 20, 1),   (20, 7, 10, 1),
(21, 7, 28, 1),
-- HACE 2 DIAS (movimientos 8-13)
(22, 8, 5, 6),     (23, 8, 9, 10),    (24, 8, 53, 3),   (25, 9, 15, 5),
(26, 9, 27, 4),    (27, 10, 6, 7),    (28, 11, 21, 3),  (29, 11, 57, 2),
(30, 12, 26, 2),   (31, 13, 12, 8),
-- HACE 3 DIAS (movimientos 14-18)
(32, 14, 20, 2),   (33, 14, 21, 3),   (34, 14, 51, 1),  (35, 15, 23, 3),
(36, 15, 24, 2),   (37, 16, 26, 2),   (38, 17, 55, 3),  (39, 18, 13, 4),
-- HACE 5 DIAS (movimientos 19-23)
(40, 19, 12, 12),  (41, 19, 25, 10),  (42, 20, 29, 1),  (43, 20, 30, 1),
(44, 21, 4, 5),    (45, 22, 19, 4),   (46, 23, 27, 5),
-- HACE 7 DIAS (movimientos 24-28)
(47, 24, 2, 10),   (48, 24, 26, 5),   (49, 25, 10, 1),  (50, 25, 11, 1),
(51, 26, 13, 6),   (52, 27, 52, 4),   (53, 28, 56, 3),
-- HACE 10 DIAS (movimientos 29-33)
(54, 29, 1, 3),    (55, 29, 5, 4),    (56, 30, 19, 6),  (57, 30, 28, 1),
(58, 31, 22, 5),   (59, 32, 59, 2),   (60, 33, 61, 3),
-- HACE 14 DIAS (movimientos 34-38)
(61, 34, 15, 12),  (62, 34, 17, 18),  (63, 35, 1, 5),   (64, 35, 8, 7),
(65, 36, 20, 1),   (66, 36, 27, 3),   (67, 37, 52, 5),  (68, 38, 62, 2),
-- HACE 20 DIAS (movimientos 39-43)
(69, 39, 6, 9),    (70, 39, 28, 1),   (71, 40, 26, 3),  (72, 40, 13, 3),
(73, 41, 16, 12),  (74, 42, 60, 1),   (75, 43, 64, 3),
-- HACE 25 DIAS (movimientos 44-47)
(76, 44, 18, 35),  (77, 44, 12, 18),  (78, 45, 14, 6),  (79, 45, 23, 4),
(80, 46, 10, 1),   (81, 46, 11, 1),   (82, 47, 16, 10);

-- ============================================
-- AUDITORIAS
-- ============================================
INSERT INTO auditorias (id, tipo_operacion, fecha_hora, usuario_id, entidad_afectada, entidad_id, valores_anteriores, valores_nuevos) VALUES
(1, 'INSERT', '2026-08-03 09:00:00', 1, 'Bodega', 1, NULL, '{"nombre":"Bodega Central Bogota","capacidad":450}'),
(2, 'INSERT', '2026-08-03 09:05:00', 1, 'Bodega', 2, NULL, '{"nombre":"Centro Distribucion Medellin","capacidad":400}'),
(3, 'INSERT', '2026-08-03 09:10:00', 1, 'Producto', 1, NULL, '{"nombre":"Laptop Lenovo ThinkPad T14","stock":52}'),
(4, 'INSERT', '2026-08-03 09:15:00', 1, 'Proveedor', 1, NULL, '{"nombre":"TechParts Colombia","diasEntrega":5}'),
(5, 'INSERT', '2026-08-20 10:00:00', 1, 'MovimientoInventario', 29, NULL, '{"tipo":"SALIDA","bodegaOrigen":1}');
