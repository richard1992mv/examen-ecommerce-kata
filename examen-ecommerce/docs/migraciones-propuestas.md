# Migraciones propuestas (NO aplicadas)

El backend funciona con el esquema actual. Estas migraciones solo se aplicarían si el negocio aprueba las funcionalidades correspondientes. No hay Flyway/Liquibase: se ejecutarían manualmente y luego se ajustaría el mapeo JPA (validate).

## M1 · Cancelación de órdenes

Hoy las órdenes son inmutables: no hay `status` y `order_items` usa FK `RESTRICT`.

```sql
-- MySQL 5.7.44
ALTER TABLE orders
  ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED'
      COMMENT 'CONFIRMED | CANCELLED' AFTER currency,
  ADD COLUMN cancelled_at DATETIME NULL COMMENT 'Fecha de cancelación' AFTER status,
  ADD KEY idx_orders_status (status);
```

Uso previsto: `POST /api/v1/orders/{id}/cancel` → en una transacción, `status = 'CANCELLED'`, `cancelled_at = NOW()` y devolución de stock con `UPDATE products SET stock = stock + :qty WHERE id = :id`. Nunca `DELETE` (se conserva el histórico).

## M2 · Secuencia de número de orden (alta concurrencia)

```sql
CREATE TABLE order_number_sequences (
  year      SMALLINT UNSIGNED NOT NULL,
  last_value INT UNSIGNED     NOT NULL,
  PRIMARY KEY (year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- Obtención: UPDATE ... SET last_value = LAST_INSERT_ID(last_value + 1) WHERE year = :y; SELECT LAST_INSERT_ID();
```

Elimina los reintentos por colisión del UNIQUE.

## M3 · Límite de usos del cupón

```sql
ALTER TABLE coupons
  ADD COLUMN max_uses INT UNSIGNED NULL COMMENT 'NULL = ilimitado' AFTER is_active,
  ADD COLUMN used_count INT UNSIGNED NOT NULL DEFAULT 0 AFTER max_uses;
```

Consumo atómico: `UPDATE coupons SET used_count = used_count + 1 WHERE id = :id AND (max_uses IS NULL OR used_count < max_uses)`.
