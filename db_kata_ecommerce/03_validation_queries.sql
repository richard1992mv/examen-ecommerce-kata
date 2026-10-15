-- =====================================================================
-- 03_validation_queries.sql
-- Solo SELECT. Compatible con MySQL 5.7 (sin CTE ni funciones de ventana).
-- Se evitan restas que puedan dar negativo sobre columnas UNSIGNED.
-- =====================================================================

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
USE kata_ecommerce;

-- 1. Conteos por tabla (esperado: 4, 12, 4, 4, 9)
SELECT 'categories'  AS tabla, COUNT(*) AS filas FROM categories
UNION ALL SELECT 'products',    COUNT(*) FROM products
UNION ALL SELECT 'coupons',     COUNT(*) FROM coupons
UNION ALL SELECT 'orders',      COUNT(*) FROM orders
UNION ALL SELECT 'order_items', COUNT(*) FROM order_items;

-- 2. Productos con stock 0 (esperado: Cargador USB-C)
SELECT p.id, p.name, c.name AS categoria, p.stock
FROM products p
INNER JOIN categories c ON c.id = p.category_id
WHERE p.stock = 0;

-- 3. Cupones válidos e inválidos a la fecha actual
SELECT code, discount_pct, is_active, valid_from, valid_until,
       CASE
         WHEN is_active = 1 AND NOW() BETWEEN valid_from AND valid_until THEN 'VÁLIDO'
         WHEN is_active = 0           THEN 'INVÁLIDO: inactivo'
         WHEN NOW() < valid_from      THEN 'INVÁLIDO: aún no vigente'
         ELSE                              'INVÁLIDO: vencido'
       END AS estado
FROM coupons
ORDER BY code;

-- 4. Recalcular la cascada de cada orden y compararla con lo guardado
--    Nota: usa el discount_pct ACTUAL del cupón (orders no guarda el % del cupón).
SELECT r.order_number,
       r.s0  AS subtotal_calc, r.subtotal_original,
       r.cat AS categoria_calc, r.category_discount_amount,
       r.vol AS volumen_calc,   r.volume_discount_amount,
       r.cup AS cupon_calc,     r.coupon_discount_amount,
       r.adj AS ajuste_calc,    r.cap_adjustment_amount,
       (r.gross - r.adj)                      AS descuento_total_calc, r.discount_total,
       (r.s0 - (r.gross - r.adj))             AS total_calc,           r.total,
       ROUND((r.gross - r.adj) / NULLIF(r.s0, 0) * 100, 2) AS pct_efectivo_calc, r.effective_discount_pct,
       CASE WHEN r.adj > 0 THEN 1 ELSE 0 END  AS cap_applied_calc,     r.cap_applied,
       CASE WHEN r.s0  = r.subtotal_original
             AND r.cat = r.category_discount_amount
             AND r.vol = r.volume_discount_amount
             AND r.cup = r.coupon_discount_amount
             AND r.adj = r.cap_adjustment_amount
             AND (r.gross - r.adj) = r.discount_total
             AND (r.s0 - (r.gross - r.adj)) = r.total
             AND ROUND((r.gross - r.adj) / NULLIF(r.s0, 0) * 100, 2) = r.effective_discount_pct
             AND (CASE WHEN r.adj > 0 THEN 1 ELSE 0 END) = r.cap_applied
            THEN 'OK' ELSE 'DESCUADRE' END AS estado
FROM (
  SELECT q.*,
         (q.cat + q.vol + q.cup) AS gross,
         CASE WHEN (q.cat + q.vol + q.cup) > q.cap_limit
              THEN (q.cat + q.vol + q.cup) - q.cap_limit
              ELSE 0.00 END AS adj
  FROM (
    SELECT p.*,
           ROUND((p.s1 - p.vol) * p.cpct / 100, 2) AS cup
    FROM (
      SELECT o.id, o.order_number, o.subtotal_original, o.category_discount_amount,
             o.volume_discount_amount, o.coupon_discount_amount, o.cap_adjustment_amount,
             o.discount_total, o.effective_discount_pct, o.cap_applied, o.total,
             li.s0, li.cat,
             (li.s0 - li.cat) AS s1,
             CASE WHEN (li.s0 - li.cat) > 100.00
                  THEN ROUND((li.s0 - li.cat) * 0.05, 2) ELSE 0.00 END AS vol,
             COALESCE(c.discount_pct, 0.00) AS cpct,
             ROUND(li.s0 * o.discount_cap_pct / 100, 2) AS cap_limit
      FROM orders o
      INNER JOIN (
        SELECT order_id,
               SUM(line_subtotal)            AS s0,
               SUM(category_discount_amount) AS cat
        FROM order_items
        GROUP BY order_id
      ) li ON li.order_id = o.id
      LEFT JOIN coupons c ON c.id = o.coupon_id
    ) p
  ) q
) r
ORDER BY r.order_number;

-- 5. effective_discount_pct <= discount_cap_pct (esperado: 0 filas)
SELECT id, order_number, effective_discount_pct, discount_cap_pct
FROM orders
WHERE effective_discount_pct > discount_cap_pct;

-- 6a. Líneas descuadradas (esperado: 0 filas)
SELECT oi.id, oi.order_id, oi.product_name, oi.unit_price, oi.quantity,
       oi.line_subtotal, oi.category_discount_amount, oi.line_total
FROM order_items oi
WHERE oi.line_subtotal <> ROUND(oi.unit_price * oi.quantity, 2)
   OR oi.line_total + oi.category_discount_amount <> oi.line_subtotal;

-- 6b. Totales de la orden descuadrados (esperado: 0 filas)
SELECT o.id, o.order_number, o.subtotal_original, li.s0,
       o.category_discount_amount, li.cat, o.discount_total, o.total
FROM orders o
INNER JOIN (
  SELECT order_id,
         SUM(line_subtotal)            AS s0,
         SUM(category_discount_amount) AS cat
  FROM order_items
  GROUP BY order_id
) li ON li.order_id = o.id
WHERE o.subtotal_original <> li.s0
   OR o.category_discount_amount <> li.cat
   OR o.discount_total + o.cap_adjustment_amount
      <> o.category_discount_amount + o.volume_discount_amount + o.coupon_discount_amount
   OR o.total + o.discount_total <> o.subtotal_original
   OR (o.coupon_id IS NULL AND (o.coupon_code IS NOT NULL OR o.coupon_discount_amount <> 0.00));

-- 7. Órdenes sin líneas (esperado: 0 filas)
SELECT o.id, o.order_number
FROM orders o
LEFT JOIN order_items oi ON oi.order_id = o.id
WHERE oi.id IS NULL;
