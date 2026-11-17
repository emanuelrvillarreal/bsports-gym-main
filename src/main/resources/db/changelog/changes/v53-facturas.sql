--liquibase formatted sql

-- changeset bsports-club:v53-facturas
-- comment: Facturas del club generadas a partir del evento DEUDA_GENERADA del CRM. uuid_factura_crm es unico y da la idempotencia frente a los reintentos del CRM
CREATE TABLE facturas (
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
  uuid_factura_crm VARCHAR(64) NOT NULL,
  codigo_plan VARCHAR(60) NULL,
  codigos_productos VARCHAR(500) NULL,
  fecha_generacion DATE NOT NULL,
  fecha_vencimiento DATE NOT NULL,
  estado VARCHAR(20) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_facturas_uuid (uuid),
  UNIQUE KEY uk_facturas_uuid_factura_crm (uuid_factura_crm),
  KEY idx_facturas_company_estado (company_id, estado),
  CONSTRAINT fk_facturas_company FOREIGN KEY (company_id) REFERENCES companies (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
