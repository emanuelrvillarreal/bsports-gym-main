--liquibase formatted sql

-- changeset bsports-club:v30-certificados-medicos
-- comment: Certificados médicos de usuarios, con índice por fecha de expiración
CREATE TABLE certificados_medicos (
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
  fecha_carga DATE NOT NULL,
  fecha_expiracion DATE NOT NULL,
  nombre_archivo VARCHAR(255) NOT NULL,
  url_archivo VARCHAR(512) NOT NULL,
  estado VARCHAR(30) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_certificados_medicos_uuid (uuid),
  KEY fk_certificados_medicos_user (user_id),
  KEY idx_certificados_medicos_fecha_expiracion (fecha_expiracion),
  CONSTRAINT fk_certificados_medicos_user FOREIGN KEY (user_id) REFERENCES auth_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
