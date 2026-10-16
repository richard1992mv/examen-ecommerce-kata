-- =====================================================================
-- 04_auth_users.sql
-- Migración para una base kata_ecommerce YA existente: crea app_users y
-- carga los usuarios de prueba SIN borrar ni tocar las demás tablas.
-- (En una instalación nueva basta con 00, 01 y 02, que ya incluyen app_users.)
-- Re-ejecutable: CREATE TABLE IF NOT EXISTS + INSERT ... ON DUPLICATE KEY.
-- =====================================================================

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET SESSION sql_mode = 'STRICT_TRANS_TABLES,NO_ZERO_DATE,NO_ZERO_IN_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';
USE kata_ecommerce;

-- ---------------------------------------------------------------------
-- app_users (usuarios que pueden obtener un token JWT en /api/v1/auth/login)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS app_users (
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

-- ---------------------------------------------------------------------
-- app_users (contraseñas cifradas con BCrypt, costo 10)
--   admin / admin123         -> ADMIN, activo
--   cliente / cliente123     -> USER,  activo
--   bloqueado / bloqueado123 -> USER,  inactivo (login debe responder 401)
-- ---------------------------------------------------------------------
INSERT INTO app_users (id, username, password_hash, role, is_active) VALUES
  (1, 'admin',     '$2a$10$M6EUsAoIsb.FqakWs3EA9Oy0Xk6buEkxEvmuyWxKLqebYGBPBI1uS', 'ADMIN', 1),
  (2, 'cliente',   '$2a$10$xVl5n.O4TQQDhvFT6xO4vO3EEPxULKUelFlzONTjEH027z8Yort0m', 'USER',  1),
  (3, 'bloqueado', '$2a$10$mOYFj9adPRvGIoO2grdl8eMjsGhidhPV83hGpANiJGriJXo.dgSDi', 'USER',  0)
ON DUPLICATE KEY UPDATE
  password_hash = VALUES(password_hash),
  role          = VALUES(role),
  is_active     = VALUES(is_active);

SELECT id, username, role, is_active FROM app_users ORDER BY id;
