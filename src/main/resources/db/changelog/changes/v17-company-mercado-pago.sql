--liquibase formatted sql

-- changeset bsports-club:v17-company-mercado-pago
-- comment: Credenciales Mercado Pago Checkout Pro por club (1:1 con companies)
CREATE TABLE company_mercado_pago (
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
  habilitado BIT(1) NOT NULL DEFAULT 0,
  access_token VARCHAR(512) NULL,
  public_key VARCHAR(512) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_company_mercado_pago_uuid (uuid),
  UNIQUE KEY uk_company_mercado_pago_company (company_id),
  CONSTRAINT fk_company_mercado_pago_company FOREIGN KEY (company_id) REFERENCES companies (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
