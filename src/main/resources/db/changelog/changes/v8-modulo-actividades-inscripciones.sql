--liquibase formatted sql

-- changeset bsports-club:v8-modulo-actividades-inscripciones
-- comment: Inscripciones de socios (titular o familiar) a actividades
CREATE TABLE inscripciones_actividad (
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
  actividad_id BIGINT NOT NULL,
  member_id BIGINT NOT NULL,
  fecha_inscripcion DATE NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_inscripciones_actividad_uuid (uuid),
  KEY idx_inscripciones_act_member (actividad_id, member_id, status),
  KEY idx_inscripciones_actividad_status (actividad_id, status),
  KEY fk_inscripciones_actividad_company (company_id),
  KEY fk_inscripciones_actividad_actividad (actividad_id),
  KEY fk_inscripciones_actividad_member (member_id),
  CONSTRAINT fk_inscripciones_actividad_company FOREIGN KEY (company_id) REFERENCES companies (id),
  CONSTRAINT fk_inscripciones_actividad_actividad FOREIGN KEY (actividad_id) REFERENCES actividades (id),
  CONSTRAINT fk_inscripciones_actividad_member FOREIGN KEY (member_id) REFERENCES members (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
