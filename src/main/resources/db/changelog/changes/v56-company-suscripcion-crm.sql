--liquibase formatted sql

-- changeset bsports-club:v56-company-suscripcion-crm
-- comment: Suscripcion del club a su plan del CRM (1:1 con companies), con la respuesta de la suscripcion
CREATE TABLE company_suscripcion_crm (
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
  plan_crm_id BIGINT NOT NULL,
  uuid_suscripcion_crm VARCHAR(60) NULL,
  uuid_factura_inicial_crm VARCHAR(60) NULL,
  estado VARCHAR(30) NULL,
  fecha_proxima_facturacion DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_company_suscripcion_crm_uuid (uuid),
  UNIQUE KEY uk_company_suscripcion_crm_company (company_id),
  CONSTRAINT fk_company_suscripcion_crm_company FOREIGN KEY (company_id) REFERENCES companies (id),
  CONSTRAINT fk_company_suscripcion_crm_plan FOREIGN KEY (plan_crm_id) REFERENCES plan_crm (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
