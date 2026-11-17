--liquibase formatted sql

-- changeset bsports-club:v50-descuentos
-- comment: Catálogo de descuentos, asignación recurrente a member/inscripción de actividad, y snapshot de descuentos aplicados por cuota
CREATE TABLE descuentos (
  id BIGINT NOT NULL AUTO_INCREMENT,
  status TINYINT NOT NULL DEFAULT 0,
  uuid CHAR(36) NOT NULL,
  id_user_created BIGINT NULL,
  id_user_updated BIGINT NULL,
  id_user_deleted BIGINT NULL,
  date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  date_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  date_deleted TIMESTAMP NULL,
  company_id BIGINT NOT NULL,
  titulo VARCHAR(255) NOT NULL,
  tipo_valor VARCHAR(20) NOT NULL,
  porcentaje INT NULL,
  monto_fijo DECIMAL(12,2) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_descuentos_uuid (uuid),
  KEY fk_descuentos_company (company_id),
  CONSTRAINT fk_descuentos_company FOREIGN KEY (company_id) REFERENCES companies (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE member_descuentos (
  id BIGINT NOT NULL AUTO_INCREMENT,
  status TINYINT NOT NULL DEFAULT 0,
  uuid CHAR(36) NOT NULL,
  id_user_created BIGINT NULL,
  id_user_updated BIGINT NULL,
  id_user_deleted BIGINT NULL,
  date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  date_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  date_deleted TIMESTAMP NULL,
  member_id BIGINT NOT NULL,
  descuento_id BIGINT NOT NULL,
  fecha_desde DATE NOT NULL,
  fecha_hasta DATE NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_member_descuentos_uuid (uuid),
  KEY fk_member_descuentos_member (member_id),
  KEY fk_member_descuentos_descuento (descuento_id),
  KEY idx_member_descuentos_vigencia (member_id, status, fecha_desde, fecha_hasta),
  CONSTRAINT fk_member_descuentos_member FOREIGN KEY (member_id) REFERENCES members (id),
  CONSTRAINT fk_member_descuentos_descuento FOREIGN KEY (descuento_id) REFERENCES descuentos (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE inscripcion_actividad_descuentos (
  id BIGINT NOT NULL AUTO_INCREMENT,
  status TINYINT NOT NULL DEFAULT 0,
  uuid CHAR(36) NOT NULL,
  id_user_created BIGINT NULL,
  id_user_updated BIGINT NULL,
  id_user_deleted BIGINT NULL,
  date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  date_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  date_deleted TIMESTAMP NULL,
  inscripcion_actividad_id BIGINT NOT NULL,
  descuento_id BIGINT NOT NULL,
  fecha_desde DATE NOT NULL,
  fecha_hasta DATE NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_inscripcion_actividad_descuentos_uuid (uuid),
  KEY fk_inscripcion_actividad_descuentos_inscripcion (inscripcion_actividad_id),
  KEY fk_inscripcion_actividad_descuentos_descuento (descuento_id),
  KEY idx_inscripcion_actividad_descuentos_vigencia (inscripcion_actividad_id, status, fecha_desde, fecha_hasta),
  CONSTRAINT fk_inscripcion_actividad_descuentos_inscripcion
    FOREIGN KEY (inscripcion_actividad_id) REFERENCES inscripciones_actividad (id),
  CONSTRAINT fk_inscripcion_actividad_descuentos_descuento FOREIGN KEY (descuento_id) REFERENCES descuentos (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE cuota_descuentos (
  id BIGINT NOT NULL AUTO_INCREMENT,
  status TINYINT NOT NULL DEFAULT 0,
  uuid CHAR(36) NOT NULL,
  id_user_created BIGINT NULL,
  id_user_updated BIGINT NULL,
  id_user_deleted BIGINT NULL,
  date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  date_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  date_deleted TIMESTAMP NULL,
  cuota_id BIGINT NOT NULL,
  titulo VARCHAR(255) NOT NULL,
  valor DECIMAL(12,2) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cuota_descuentos_uuid (uuid),
  KEY fk_cuota_descuentos_cuota (cuota_id),
  CONSTRAINT fk_cuota_descuentos_cuota FOREIGN KEY (cuota_id) REFERENCES cuotas (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE cuotas
  DROP COLUMN descuento;
