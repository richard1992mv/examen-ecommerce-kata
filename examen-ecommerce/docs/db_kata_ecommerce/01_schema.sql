-- =====================================================================
-- 01_schema.sql
-- Tablas, PK, FK, UNIQUE e índices. MySQL 5.7.44 / InnoDB / utf8mb4.
-- 6 tablas: categories, products, coupons, orders, order_items y app_users (seguridad JWT).
-- Sin JSON, CHECK, columnas generadas, triggers ni procedimientos.
-- =====================================================================

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET SESSION sql_mode = 'STRICT_TRANS_TABLES,NO_ZERO_DATE,NO_ZERO_IN_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';
USE kata_ecommerce;

-- Borrado en orden inverso de dependencias (permite re-ejecutar el script).
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS coupons;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS app_users;

-- ---------------------------------------------------------------------
-- categories
-- ---------------------------------------------------------------------
CREATE TABLE categories (
  id            INT UNSIGNED      NOT NULL AUTO_INCREMENT COMMENT 'Identificador de la categoría',
  name          VARCHAR(100)      NOT NULL                COMMENT 'Nombre único de la categoría',
  discount_pct  DECIMAL(5,2) UNSIGNED NOT NULL DEFAULT 0.00 COMMENT 'Porcentaje de descuento de categoría (0.00 a 100.00)',
  created_at    DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha de creación del registro',
  updated_at    DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha de última actualización',
  PRIMARY KEY (id),
  UNIQUE KEY uk_categories_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Categorías de producto y su porcentaje de descuento de categoría';

-- ---------------------------------------------------------------------
-- products
-- ---------------------------------------------------------------------
CREATE TABLE products (
  id            INT UNSIGNED      NOT NULL AUTO_INCREMENT COMMENT 'Identificador del producto',
  name          VARCHAR(150)      NOT NULL                COMMENT 'Nombre del producto',
  unit_price    DECIMAL(12,2) UNSIGNED NOT NULL           COMMENT 'Precio unitario en USD (el backend valida > 0)',
  category_id   INT UNSIGNED      NOT NULL                COMMENT 'Categoría del producto (FK a categories.id)',
  stock         INT UNSIGNED      NOT NULL DEFAULT 0      COMMENT 'Unidades disponibles actualmente',
  created_at    DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha de creación del registro',
  updated_at    DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha de última actualización',
  PRIMARY KEY (id),
  KEY idx_products_category_id (category_id),
  CONSTRAINT fk_products_category
    FOREIGN KEY (category_id) REFERENCES categories (id)
    ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Catálogo de productos con precio y stock actual';

-- ---------------------------------------------------------------------
-- coupons
-- ---------------------------------------------------------------------
CREATE TABLE coupons (
  id            INT UNSIGNED      NOT NULL AUTO_INCREMENT COMMENT 'Identificador del cupón',
  code          VARCHAR(50)       NOT NULL                COMMENT 'Código único del cupón',
  discount_pct  DECIMAL(5,2) UNSIGNED NOT NULL            COMMENT 'Porcentaje de descuento del cupón',
  valid_from    DATETIME          NOT NULL                COMMENT 'Inicio de vigencia (inclusive)',
  valid_until   DATETIME          NOT NULL                COMMENT 'Fin de vigencia (inclusive)',
  is_active     TINYINT(1) UNSIGNED NOT NULL DEFAULT 1    COMMENT '1 = activo, 0 = inactivo',
  created_at    DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha de creación del registro',
  updated_at    DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha de última actualización',
  PRIMARY KEY (id),
  UNIQUE KEY uk_coupons_code (code),
  KEY idx_coupons_active_validity (is_active, valid_from, valid_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Cupones de descuento con porcentaje, vigencia y estado';

-- ---------------------------------------------------------------------
-- orders
-- ---------------------------------------------------------------------
CREATE TABLE orders (
  id                        INT UNSIGNED      NOT NULL AUTO_INCREMENT COMMENT 'Identificador de la orden',
  order_number              VARCHAR(30)       NOT NULL                COMMENT 'Número de orden visible y único',
  coupon_id                 INT UNSIGNED      NULL                    COMMENT 'Cupón aplicado (FK a coupons.id), NULL si no hubo',
  coupon_code               VARCHAR(50)       NULL                    COMMENT 'Snapshot del código de cupón aplicado',
  subtotal_original         DECIMAL(12,2) UNSIGNED NOT NULL           COMMENT 'Suma de unit_price x quantity sin descuentos',
  category_discount_amount  DECIMAL(12,2) UNSIGNED NOT NULL DEFAULT 0.00 COMMENT 'Total del descuento de categoría (paso 1)',
  volume_discount_amount    DECIMAL(12,2) UNSIGNED NOT NULL DEFAULT 0.00 COMMENT 'Descuento por volumen 5 % si subtotal_1 > 100.00 (paso 2)',
  coupon_discount_amount    DECIMAL(12,2) UNSIGNED NOT NULL DEFAULT 0.00 COMMENT 'Descuento del cupón sobre subtotal_2 (paso 3)',
  cap_adjustment_amount     DECIMAL(12,2) UNSIGNED NOT NULL DEFAULT 0.00 COMMENT 'Monto recortado para respetar el tope',
  discount_total            DECIMAL(12,2) UNSIGNED NOT NULL DEFAULT 0.00 COMMENT 'Descuento final aplicado (bruto - ajuste de tope)',
  effective_discount_pct    DECIMAL(5,2)  UNSIGNED NOT NULL DEFAULT 0.00 COMMENT 'discount_total / subtotal_original x 100',
  discount_cap_pct          DECIMAL(5,2)  UNSIGNED NOT NULL DEFAULT 35.00 COMMENT 'Snapshot del tope máximo de descuento vigente',
  cap_applied               TINYINT(1) UNSIGNED NOT NULL DEFAULT 0    COMMENT '1 si se aplicó el tope, 0 si no',
  total                     DECIMAL(12,2) UNSIGNED NOT NULL           COMMENT 'Total a pagar (subtotal_original - discount_total)',
  currency                  CHAR(3)           NOT NULL DEFAULT 'USD'  COMMENT 'Moneda ISO 4217',
  created_at                DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha de creación de la orden',
  PRIMARY KEY (id),
  UNIQUE KEY uk_orders_order_number (order_number),
  KEY idx_orders_coupon_id (coupon_id),
  KEY idx_orders_created_at (created_at),
  CONSTRAINT fk_orders_coupon
    FOREIGN KEY (coupon_id) REFERENCES coupons (id)
    ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Cabecera de orden con desglose histórico de descuentos';

-- ---------------------------------------------------------------------
-- order_items
-- ---------------------------------------------------------------------
CREATE TABLE order_items (
  id                        INT UNSIGNED      NOT NULL AUTO_INCREMENT COMMENT 'Identificador de la línea',
  order_id                  INT UNSIGNED      NOT NULL                COMMENT 'Orden a la que pertenece (FK a orders.id)',
  product_id                INT UNSIGNED      NOT NULL                COMMENT 'Producto vendido (FK a products.id)',
  product_name              VARCHAR(150)      NOT NULL                COMMENT 'Snapshot del nombre del producto',
  category_name             VARCHAR(100)      NOT NULL                COMMENT 'Snapshot del nombre de la categoría',
  unit_price                DECIMAL(12,2) UNSIGNED NOT NULL           COMMENT 'Snapshot del precio unitario',
  quantity                  INT UNSIGNED      NOT NULL                COMMENT 'Cantidad comprada (el backend valida > 0)',
  line_subtotal             DECIMAL(12,2) UNSIGNED NOT NULL           COMMENT 'unit_price x quantity',
  category_discount_amount  DECIMAL(12,2) UNSIGNED NOT NULL DEFAULT 0.00 COMMENT 'Descuento de categoría de la línea',
  line_total                DECIMAL(12,2) UNSIGNED NOT NULL           COMMENT 'line_subtotal - category_discount_amount',
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_items_order_product (order_id, product_id),
  KEY idx_order_items_product_id (product_id),
  CONSTRAINT fk_order_items_order
    FOREIGN KEY (order_id) REFERENCES orders (id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_order_items_product
    FOREIGN KEY (product_id) REFERENCES products (id)
    ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Líneas de la orden con valores históricos';

-- ---------------------------------------------------------------------
-- app_users (usuarios que pueden obtener un token JWT en /api/v1/auth/login)
-- ---------------------------------------------------------------------
CREATE TABLE app_users (
  id            INT UNSIGNED      NOT NULL AUTO_INCREMENT COMMENT 'Identificador del usuario',
  username      VARCHAR(50)       NOT NULL                COMMENT 'Nombre de usuario único (login)',
  password_hash VARCHAR(100)      NOT NULL                COMMENT 'Contraseña cifrada con BCrypt (nunca texto plano)',
  role          VARCHAR(30)       NOT NULL DEFAULT 'USER' COMMENT 'Rol sin prefijo ROLE_ (USER, ADMIN)',
  is_active     TINYINT(1) UNSIGNED NOT NULL DEFAULT 1    COMMENT '1 = puede iniciar sesión, 0 = bloqueado',
  created_at    DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha de creación del registro',
  updated_at    DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha de última actualización',
  PRIMARY KEY (id),
  UNIQUE KEY uk_app_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Usuarios de la API (autenticación JWT)';
