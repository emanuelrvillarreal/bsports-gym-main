--liquibase formatted sql

-- changeset bsports-club:v54-company-bloqueo-acceso
-- comment: Estado de bloqueo de acceso a la plataforma por facturacion vencida (1:1 con companies)
CREATE TABLE company_bloqueo_acceso (
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
  bloqueado BIT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_company_bloqueo_acceso_uuid (uuid),
  UNIQUE KEY uk_company_bloqueo_acceso_company (company_id),
  CONSTRAINT fk_company_bloqueo_acceso_company FOREIGN KEY (company_id) REFERENCES companies (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
