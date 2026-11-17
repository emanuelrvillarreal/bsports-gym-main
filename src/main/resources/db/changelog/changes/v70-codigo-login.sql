--liquibase formatted sql

-- changeset bsports-club:v70-codigo-login
-- comment: Codigos de un solo uso para el login sin contraseña. Se guarda solo el hash SHA-256 del codigo
CREATE TABLE codigo_login (
  id BIGINT NOT NULL AUTO_INCREMENT,
  status TINYINT NOT NULL DEFAULT 0,
  uuid CHAR(36) NOT NULL,
  id_user_created BIGINT NULL,
  id_user_updated BIGINT NULL,
  id_user_deleted BIGINT NULL,
  date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  date_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  date_deleted TIMESTAMP NULL,
  user_id BIGINT NOT NULL,
  codigo_hash CHAR(64) NOT NULL,
  fecha_vencimiento DATETIME(6) NOT NULL,
  fecha_uso DATETIME(6) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_codigo_login_uuid (uuid),
  UNIQUE KEY uk_codigo_login_codigo_hash (codigo_hash),
  KEY idx_codigo_login_user_status (user_id, status),
  CONSTRAINT fk_codigo_login_user FOREIGN KEY (user_id) REFERENCES auth_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
