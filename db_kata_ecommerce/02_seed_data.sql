-- =====================================================================
-- 02_seed_data.sql
-- Datos de ejemplo en orden de dependencias:
-- categories -> products -> coupons -> orders -> order_items
-- Redondeo HALF_UP a 2 decimales al final de cada paso.
-- Nota: el stock de products representa el estado ACTUAL (posterior a las
-- órdenes históricas de ejemplo).
-- =====================================================================

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET SESSION sql_mode = 'STRICT_TRANS_TABLES,NO_ZERO_DATE,NO_ZERO_IN_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';
USE kata_ecommerce;

START TRANSACTION;

-- ---------------------------------------------------------------------
-- categories (Tecnología 10 %, resto 0 %)
-- ---------------------------------------------------------------------
INSERT INTO categories (id, name, discount_pct) VALUES
  (1, 'Tecnología', 10.00),
  (2, 'Hogar',       0.00),
  (3, 'Libros',      0.00),
  (4, 'Deportes',    0.00);

-- ---------------------------------------------------------------------
-- products (12; 6 de Tecnología; id 5 con stock 0; id 7 con stock 2)
-- ---------------------------------------------------------------------
INSERT INTO products (id, name, unit_price, category_id, stock) VALUES
  ( 1, 'Audífonos Bluetooth',     45.00, 1, 25),
  ( 2, 'Mouse inalámbrico',       20.00, 1, 40),
  ( 3, 'Teclado mecánico',        80.00, 1, 15),
  ( 4, 'Monitor 24 pulgadas',    150.00, 1, 10),
  ( 5, 'Cargador USB-C',          12.50, 1,  0),
  ( 6, 'Lámpara de escritorio',   30.00, 2, 20),
  ( 7, 'Juego de sartenes',       55.00, 2,  2),
  ( 8, 'Novela clásica',          15.00, 3, 50),
  ( 9, 'Libro de programación',   40.00, 3, 30),
  (10, 'Balón de fútbol',         25.00, 4, 35),
  (11, 'Esterilla de yoga',       18.00, 4, 22),
  (12, 'Smartwatch',             120.00, 1,  8);

-- ---------------------------------------------------------------------
-- coupons
-- ---------------------------------------------------------------------
INSERT INTO coupons (id, code, discount_pct, valid_from, valid_until, is_active) VALUES
  (1, 'WELCOME2026', 15.00, '2026-01-01 00:00:00', '2026-12-31 23:59:59', 1),
  (2, 'EXPIRED2025', 20.00, '2025-01-01 00:00:00', '2025-12-31 23:59:59', 1), -- vencido
  (3, 'INACTIVE10',  10.00, '2026-01-01 00:00:00', '2026-12-31 23:59:59', 0), -- inactivo
  (4, 'TESTCAP30',   30.00, '2026-01-01 00:00:00', '2026-12-31 23:59:59', 1); -- prueba del tope (HU4)

-- ---------------------------------------------------------------------
-- orders
-- ---------------------------------------------------------------------
-- (a) ORD-2026-000001: subtotal < 100, sin cupón
--   Líneas: Novela clásica 15.00 x 2 = 30.00 (0 %)   -> cat 0.00
--           Mouse inalámbrico 20.00 x 1 = 20.00 (10 %) -> cat 2.00
--   subtotal_original = 50.00
--   categoría = 2.00 -> subtotal_1 = 48.00
--   volumen: 48.00 <= 100.00 -> 0.00 -> subtotal_2 = 48.00
--   cupón = 0.00
--   bruto = 2.00 ; tope = 50.00 x 0.35 = 17.50 -> no aplica
--   discount_total = 2.00 ; total = 48.00 ; efectivo = 2.00/50.00 x 100 = 4.00 %
--
-- (b) ORD-2026-000002: subtotal_1 > 100, con volumen, sin cupón
--   Líneas: Teclado mecánico 80.00 x 1 = 80.00 (10 %) -> cat 8.00
--           Lámpara de escritorio 30.00 x 1 = 30.00 (0 %) -> cat 0.00
--   subtotal_original = 110.00
--   categoría = 8.00 -> subtotal_1 = 102.00
--   volumen: 102.00 > 100.00 -> 102.00 x 0.05 = 5.10 -> subtotal_2 = 96.90
--   cupón = 0.00
--   bruto = 13.10 ; tope = 110.00 x 0.35 = 38.50 -> no aplica
--   discount_total = 13.10 ; total = 96.90
--   efectivo = 13.10/110.00 x 100 = 11.9090... -> 11.91 %
--
-- (c) ORD-2026-000003: con WELCOME2026 (15 %)
--   Líneas: Audífonos Bluetooth 45.00 x 2 = 90.00 (10 %) -> cat 9.00
--           Libro de programación 40.00 x 1 = 40.00 (0 %) -> cat 0.00
--   subtotal_original = 130.00
--   categoría = 9.00 -> subtotal_1 = 121.00
--   volumen: 121.00 x 0.05 = 6.05 -> subtotal_2 = 114.95
--   cupón: 114.95 x 0.15 = 17.2425 -> 17.24
--   bruto = 9.00 + 6.05 + 17.24 = 32.29 ; tope = 130.00 x 0.35 = 45.50 -> no aplica
--   discount_total = 32.29 ; total = 97.71
--   efectivo = 32.29/130.00 x 100 = 24.8384... -> 24.84 %
--
-- (d) ORD-2026-000004: con TESTCAP30 (30 %) y tope aplicado
--   Líneas: Monitor 24 pulgadas 150.00 x 1 = 150.00 (10 %) -> cat 15.00
--           Smartwatch 120.00 x 1 = 120.00 (10 %)          -> cat 12.00
--           Balón de fútbol 25.00 x 2 = 50.00 (0 %)        -> cat 0.00
--   subtotal_original = 320.00
--   categoría = 27.00 -> subtotal_1 = 293.00
--   volumen: 293.00 x 0.05 = 14.65 -> subtotal_2 = 278.35
--   cupón: 278.35 x 0.30 = 83.505 -> 83.51 (HALF_UP)
--   bruto = 27.00 + 14.65 + 83.51 = 125.16
--   tope = 320.00 x 0.35 = 112.00 -> 125.16 > 112.00 -> aplica
--   cap_adjustment = 125.16 - 112.00 = 13.16 ; cap_applied = 1
--   discount_total = 112.00 ; total = 208.00 ; efectivo = 35.00 %
-- ---------------------------------------------------------------------
INSERT INTO orders (
  id, order_number, coupon_id, coupon_code,
  subtotal_original, category_discount_amount, volume_discount_amount,
  coupon_discount_amount, cap_adjustment_amount, discount_total,
  effective_discount_pct, discount_cap_pct, cap_applied, total, currency, created_at
) VALUES
  (1, 'ORD-2026-000001', NULL, NULL,          50.00,  2.00,  0.00,  0.00,  0.00,   2.00,  4.00, 35.00, 0,  48.00, 'USD', '2026-10-01 10:15:00'),
  (2, 'ORD-2026-000002', NULL, NULL,         110.00,  8.00,  5.10,  0.00,  0.00,  13.10, 11.91, 35.00, 0,  96.90, 'USD', '2026-10-03 16:40:00'),
  (3, 'ORD-2026-000003', 1,    'WELCOME2026', 130.00,  9.00,  6.05, 17.24,  0.00,  32.29, 24.84, 35.00, 0,  97.71, 'USD', '2026-10-05 12:05:00'),
  (4, 'ORD-2026-000004', 4,    'TESTCAP30',   320.00, 27.00, 14.65, 83.51, 13.16, 112.00, 35.00, 35.00, 1, 208.00, 'USD', '2026-10-08 19:30:00');

-- ---------------------------------------------------------------------
-- order_items (line_total = line_subtotal - category_discount_amount)
-- ---------------------------------------------------------------------
INSERT INTO order_items (
  id, order_id, product_id, product_name, category_name,
  unit_price, quantity, line_subtotal, category_discount_amount, line_total
) VALUES
  -- Orden 1
  (1, 1,  8, 'Novela clásica',        'Libros',      15.00, 2,  30.00,  0.00,  30.00),
  (2, 1,  2, 'Mouse inalámbrico',     'Tecnología',  20.00, 1,  20.00,  2.00,  18.00),
  -- Orden 2
  (3, 2,  3, 'Teclado mecánico',      'Tecnología',  80.00, 1,  80.00,  8.00,  72.00),
  (4, 2,  6, 'Lámpara de escritorio', 'Hogar',       30.00, 1,  30.00,  0.00,  30.00),
  -- Orden 3
  (5, 3,  1, 'Audífonos Bluetooth',   'Tecnología',  45.00, 2,  90.00,  9.00,  81.00),
  (6, 3,  9, 'Libro de programación', 'Libros',      40.00, 1,  40.00,  0.00,  40.00),
  -- Orden 4
  (7, 4,  4, 'Monitor 24 pulgadas',   'Tecnología', 150.00, 1, 150.00, 15.00, 135.00),
  (8, 4, 12, 'Smartwatch',            'Tecnología', 120.00, 1, 120.00, 12.00, 108.00),
  (9, 4, 10, 'Balón de fútbol',       'Deportes',    25.00, 2,  50.00,  0.00,  50.00);

COMMIT;
