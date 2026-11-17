--liquibase formatted sql

-- changeset bsports-club:v55-plan-crm-productos-crm
-- comment: Cache local del catalogo de planes y productos publicado por el CRM (blue-crm)
CREATE TABLE plan_crm (
  id BIGINT NOT NULL AUTO_INCREMENT,
  status TINYINT NOT NULL DEFAULT 0,
  uuid CHAR(36) NOT NULL,
  id_user_created BIGINT NULL,
  id_user_updated BIGINT NULL,
  id_user_deleted BIGINT NULL,
  date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  date_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  date_deleted TIMESTAMP NULL,
  codigo VARCHAR(60) NOT NULL,
  nombre VARCHAR(255) NULL,
  descripcion VARCHAR(1000) NULL,
  precio DECIMAL(12,2) NULL,
  precio_es_final BIT(1) NULL,
  alicuota_iva DECIMAL(5,2) NULL,
  periodicidad_codigo VARCHAR(60) NULL,
  productos_json VARCHAR(2000) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_plan_crm_uuid (uuid),
  UNIQUE KEY uk_plan_crm_codigo (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE productos_crm (
  id BIGINT NOT NULL AUTO_INCREMENT,
  status TINYINT NOT NULL DEFAULT 0,
  uuid CHAR(36) NOT NULL,
  id_user_created BIGINT NULL,
  id_user_updated BIGINT NULL,
  id_user_deleted BIGINT NULL,
  date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  date_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  date_deleted TIMESTAMP NULL,
  codigo VARCHAR(60) NOT NULL,
  nombre VARCHAR(255) NULL,
  descripcion VARCHAR(1000) NULL,
  precio DECIMAL(12,2) NULL,
  alicuota_iva DECIMAL(5,2) NULL,
  unidad VARCHAR(60) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_productos_crm_uuid (uuid),
  UNIQUE KEY uk_productos_crm_codigo (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
