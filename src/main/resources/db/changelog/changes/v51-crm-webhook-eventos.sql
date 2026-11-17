--liquibase formatted sql

-- changeset bsports-club:v51-crm-webhook-eventos
-- comment: Registro de los webhooks recibidos del CRM para garantizar idempotencia: un evento ya procesado no vuelve a ejecutar la logica, se le responde el ACK guardado
CREATE TABLE crm_webhook_eventos (
  id BIGINT NOT NULL AUTO_INCREMENT,
  status TINYINT NOT NULL DEFAULT 0,
  uuid CHAR(36) NOT NULL,
  id_user_created BIGINT NULL,
  id_user_updated BIGINT NULL,
  id_user_deleted BIGINT NULL,
  date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  date_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  date_deleted TIMESTAMP NULL,
  event_id VARCHAR(64) NOT NULL,
  estado VARCHAR(20) NOT NULL,
  tipo VARCHAR(40) NOT NULL,
  numero_factura VARCHAR(100) NULL,
  motivo_error TEXT NULL,
  webhook_raw TEXT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_crm_webhook_eventos_uuid (uuid),
  UNIQUE KEY uk_crm_webhook_eventos_event_id (event_id),
  KEY idx_crm_webhook_eventos_estado (estado, date_updated)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
