--liquibase formatted sql

-- changeset bsports-club:v52-crm-integracion-logs
-- comment: Log append-only de las interacciones con el CRM: webhooks recibidos, ACKs enviados con la respuesta del CRM, y llamadas salientes del cliente
CREATE TABLE crm_integracion_logs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  status TINYINT NOT NULL DEFAULT 0,
  uuid CHAR(36) NOT NULL,
  id_user_created BIGINT NULL,
  id_user_updated BIGINT NULL,
  id_user_deleted BIGINT NULL,
  date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  date_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  date_deleted TIMESTAMP NULL,
  direccion VARCHAR(10) NOT NULL,
  canal VARCHAR(10) NOT NULL,
  operacion VARCHAR(120) NULL,
  tipo_evento VARCHAR(40) NULL,
  event_id VARCHAR(64) NULL,
  metodo_http VARCHAR(10) NULL,
  url VARCHAR(500) NULL,
  status_code INT NULL,
  exitoso BIT NOT NULL DEFAULT 0,
  request_payload LONGTEXT NULL,
  response_payload LONGTEXT NULL,
  error_payload LONGTEXT NULL,
  duracion_ms BIGINT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_crm_integracion_logs_uuid (uuid),
  KEY idx_crm_integracion_logs_event (event_id),
  KEY idx_crm_integracion_logs_canal_fecha (canal, date_created),
  KEY idx_crm_integracion_logs_exitoso_fecha (exitoso, date_created)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
