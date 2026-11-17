--liquibase formatted sql

-- changeset bsports-club:v19-mercado-pago-cobro-intent
-- comment: Intent de cobro MP (preferencia + cuotas) para liquidar vía webhook
CREATE TABLE mercado_pago_cobro_intent (
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
  external_reference CHAR(36) NOT NULL,
  preference_id VARCHAR(64) NOT NULL,
  payment_id BIGINT NULL,
  estado VARCHAR(20) NOT NULL,
  monto_total DECIMAL(12, 2) NOT NULL,
  registrado_por BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_mp_cobro_intent_uuid (uuid),
  UNIQUE KEY uk_mp_cobro_intent_external_ref (company_id, external_reference),
  KEY idx_mp_cobro_intent_estado (company_id, estado),
  KEY fk_mp_cobro_intent_registrado_por (registrado_por),
  CONSTRAINT fk_mp_cobro_intent_company FOREIGN KEY (company_id) REFERENCES companies (id),
  CONSTRAINT fk_mp_cobro_intent_registrado_por FOREIGN KEY (registrado_por) REFERENCES auth_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE mercado_pago_cobro_intent_cuota (
  id BIGINT NOT NULL AUTO_INCREMENT,
  status TINYINT NOT NULL DEFAULT 0,
  uuid CHAR(36) NOT NULL,
  id_user_created BIGINT NULL,
  id_user_updated BIGINT NULL,
  id_user_deleted BIGINT NULL,
  date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  date_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  date_deleted TIMESTAMP NULL,
  intent_id BIGINT NOT NULL,
  cuota_id BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_mp_cobro_intent_cuota_uuid (uuid),
  UNIQUE KEY uk_mp_cobro_intent_cuota (intent_id, cuota_id),
  KEY fk_mp_cobro_intent_cuota_cuota (cuota_id),
  CONSTRAINT fk_mp_cobro_intent_cuota_intent FOREIGN KEY (intent_id) REFERENCES mercado_pago_cobro_intent (id),
  CONSTRAINT fk_mp_cobro_intent_cuota_cuota FOREIGN KEY (cuota_id) REFERENCES cuotas (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
