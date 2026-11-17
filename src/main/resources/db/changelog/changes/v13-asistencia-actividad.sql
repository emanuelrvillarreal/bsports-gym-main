--liquibase formatted sql

-- changeset bsports-club:v13-asistencia-actividad
-- comment: Asistencia a clases recurrentes (slot + member)
CREATE TABLE asistencia_actividad (
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
  slot_id BIGINT NOT NULL,
  actividad_id BIGINT NOT NULL,
  member_id BIGINT NOT NULL,
  fecha DATE NOT NULL,
  estado VARCHAR(20) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_asistencia_actividad_uuid (uuid),
  UNIQUE KEY uk_asistencia_slot_member (slot_id, member_id),
  KEY idx_asistencia_company_fecha (company_id, fecha),
  KEY idx_asistencia_company_member (company_id, member_id),
  KEY fk_asistencia_actividad_company (company_id),
  KEY fk_asistencia_actividad_slot (slot_id),
  KEY fk_asistencia_actividad_actividad (actividad_id),
  KEY fk_asistencia_actividad_member (member_id),
  CONSTRAINT fk_asistencia_actividad_company FOREIGN KEY (company_id) REFERENCES companies (id),
  CONSTRAINT fk_asistencia_actividad_slot FOREIGN KEY (slot_id) REFERENCES slots (id),
  CONSTRAINT fk_asistencia_actividad_actividad FOREIGN KEY (actividad_id) REFERENCES actividades (id),
  CONSTRAINT fk_asistencia_actividad_member FOREIGN KEY (member_id) REFERENCES members (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
