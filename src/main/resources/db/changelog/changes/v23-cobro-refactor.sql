--liquibase formatted sql

-- changeset bsports-club:v23-cobro-refactor
-- comment: Modelo MercadoPagoCobro; elimina mercado_pago_cobro_intent_cuota
CREATE TABLE mercado_pago_cobros (
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
  monto_total DECIMAL(12, 2) NOT NULL,
  registrado_por BIGINT NOT NULL,
  tipo VARCHAR(20) NOT NULL,
  titulo VARCHAR(255) NULL,
  detalle VARCHAR(512) NULL,
  mercado_pago_cobro_intent_id BIGINT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_mp_cobros_uuid (uuid),
  UNIQUE KEY uk_mp_cobros_intent (mercado_pago_cobro_intent_id),
  KEY fk_mp_cobros_company (company_id),
  KEY fk_mp_cobros_registrado_por (registrado_por),
  CONSTRAINT fk_mp_cobros_company FOREIGN KEY (company_id) REFERENCES companies (id),
  CONSTRAINT fk_mp_cobros_registrado_por FOREIGN KEY (registrado_por) REFERENCES auth_users (id),
  CONSTRAINT fk_mp_cobros_intent FOREIGN KEY (mercado_pago_cobro_intent_id) REFERENCES mercado_pago_cobro_intent (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE mercado_pago_cobro_items (
  id BIGINT NOT NULL AUTO_INCREMENT,
  status TINYINT NOT NULL DEFAULT 0,
  uuid CHAR(36) NOT NULL,
  id_user_created BIGINT NULL,
  id_user_updated BIGINT NULL,
  id_user_deleted BIGINT NULL,
  date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  date_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  date_deleted TIMESTAMP NULL,
  mercado_pago_cobro_id BIGINT NOT NULL,
  cuota_id BIGINT NULL,
  monto DECIMAL(12, 2) NOT NULL,
  titulo VARCHAR(255) NULL,
  detalle VARCHAR(512) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_mp_cobro_items_uuid (uuid),
  KEY fk_mp_cobro_items_cobro (mercado_pago_cobro_id),
  KEY fk_mp_cobro_items_cuota (cuota_id),
  CONSTRAINT fk_mp_cobro_items_cobro FOREIGN KEY (mercado_pago_cobro_id) REFERENCES mercado_pago_cobros (id),
  CONSTRAINT fk_mp_cobro_items_cuota FOREIGN KEY (cuota_id) REFERENCES cuotas (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO mercado_pago_cobros (
  status,
  uuid,
  id_user_created,
  id_user_updated,
  id_user_deleted,
  date_created,
  date_updated,
  date_deleted,
  company_id,
  monto_total,
  registrado_por,
  tipo,
  titulo,
  detalle,
  mercado_pago_cobro_intent_id
)
SELECT
  i.status,
  UUID(),
  i.id_user_created,
  i.id_user_updated,
  i.id_user_deleted,
  i.date_created,
  i.date_updated,
  i.date_deleted,
  i.company_id,
  i.monto_total,
  i.registrado_por,
  'CUOTA',
  NULL,
  NULL,
  i.id
FROM mercado_pago_cobro_intent i;

INSERT INTO mercado_pago_cobro_items (
  status,
  uuid,
  id_user_created,
  id_user_updated,
  id_user_deleted,
  date_created,
  date_updated,
  date_deleted,
  mercado_pago_cobro_id,
  cuota_id,
  monto,
  titulo,
  detalle
)
SELECT
  ic.status,
  UUID(),
  ic.id_user_created,
  ic.id_user_updated,
  ic.id_user_deleted,
  ic.date_created,
  ic.date_updated,
  ic.date_deleted,
  c.id,
  ic.cuota_id,
  cu.monto,
  NULL,
  cu.concepto
FROM mercado_pago_cobro_intent_cuota ic
JOIN mercado_pago_cobros c ON c.mercado_pago_cobro_intent_id = ic.intent_id
JOIN cuotas cu ON cu.id = ic.cuota_id;

ALTER TABLE mercado_pago_cobro_intent
  DROP FOREIGN KEY fk_mp_cobro_intent_registrado_por;

ALTER TABLE mercado_pago_cobro_intent
  DROP COLUMN monto_total,
  DROP COLUMN registrado_por;

DROP TABLE mercado_pago_cobro_intent_cuota;
