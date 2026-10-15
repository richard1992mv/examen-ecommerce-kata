-- =====================================================================
-- 00_create_database.sql
-- KATA: Core E-Commerce con descuentos acumulativos
-- Motor objetivo: MySQL 5.7.44
-- Crea la base de datos con utf8mb4 / utf8mb4_unicode_ci y fija sql_mode.
-- =====================================================================

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- sql_mode estricto para la sesión actual.
-- NO_ENGINE_SUBSTITUTION evita que MySQL cambie InnoDB por otro motor en silencio.
SET SESSION sql_mode = 'STRICT_TRANS_TABLES,NO_ZERO_DATE,NO_ZERO_IN_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- Recomendado a nivel servidor (requiere privilegio SUPER o hacerlo en my.cnf):
-- SET GLOBAL sql_mode = 'STRICT_TRANS_TABLES,NO_ZERO_DATE,NO_ZERO_IN_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';
-- my.cnf -> [mysqld]
--   sql_mode = STRICT_TRANS_TABLES,NO_ZERO_DATE,NO_ZERO_IN_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION
--   character-set-server = utf8mb4
--   collation-server     = utf8mb4_unicode_ci
--   default-storage-engine = InnoDB

CREATE DATABASE IF NOT EXISTS kata_ecommerce
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE kata_ecommerce;

-- Verificación rápida de la configuración de la sesión.
SELECT @@SESSION.sql_mode            AS sql_mode_sesion,
       @@character_set_database      AS charset_bd,
       @@collation_database          AS collation_bd;
