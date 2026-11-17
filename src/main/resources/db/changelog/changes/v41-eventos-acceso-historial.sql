--liquibase formatted sql

-- changeset bsports-club:v41-eventos-acceso-historial
-- comment: Historial de resultados de scan sobre eventos de acceso
CREATE TABLE eventos_acceso_historial (
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
  evento_acceso_id BIGINT NOT NULL,
  member_id BIGINT NULL,
  tipo VARCHAR(20) NOT NULL,
  nombre VARCHAR(255) NULL,
  resultado VARCHAR(20) NOT NULL,
  motivo TEXT NULL,
  monto DECIMAL(12,2) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_eventos_acceso_historial_uuid (uuid),
  KEY idx_eventos_acceso_historial_evento (evento_acceso_id),
  KEY idx_eventos_acceso_historial_member (member_id),
  CONSTRAINT fk_eah_company FOREIGN KEY (company_id) REFERENCES companies (id),
  CONSTRAINT fk_eah_evento_acceso FOREIGN KEY (evento_acceso_id) REFERENCES eventos_acceso (id),
  CONSTRAINT fk_eah_member FOREIGN KEY (member_id) REFERENCES members (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
