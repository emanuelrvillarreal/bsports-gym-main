--liquibase formatted sql

-- changeset bsports-club:v20-mercado-pago-traza
-- comment: Trazas append-only del flujo Mercado Pago (webhook, API, liquidación)
CREATE TABLE mercado_pago_traza (
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
  intent_id BIGINT NULL,
  etapa VARCHAR(40) NOT NULL,
  nivel VARCHAR(10) NOT NULL DEFAULT 'INFO',
  external_reference CHAR(36) NULL,
  preference_id VARCHAR(64) NULL,
  payment_id BIGINT NULL,
  mensaje VARCHAR(512) NULL,
  request_raw LONGTEXT NULL,
  response_raw LONGTEXT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_mp_traza_uuid (uuid),
  KEY idx_mp_traza_company_date (company_id, date_created),
  KEY idx_mp_traza_intent (intent_id),
  KEY idx_mp_traza_company_external_ref (company_id, external_reference),
  KEY idx_mp_traza_payment (payment_id),
  CONSTRAINT fk_mp_traza_company FOREIGN KEY (company_id) REFERENCES companies (id),
  CONSTRAINT fk_mp_traza_intent FOREIGN KEY (intent_id) REFERENCES mercado_pago_cobro_intent (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
