--liquibase formatted sql

-- changeset bsports-club:v12-evento-actividad
-- comment: Eventos puntuales de actividad (tabla propia, independiente de slots)
CREATE TABLE evento_actividad (
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
  titulo VARCHAR(255) NOT NULL,
  descripcion TEXT NOT NULL,
  ubicacion VARCHAR(255) NOT NULL,
  inicio DATETIME NOT NULL,
  fin DATETIME NOT NULL,
  estado VARCHAR(20) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_evento_actividad_uuid (uuid),
  KEY fk_evento_actividad_company (company_id),
  KEY fk_evento_actividad_actividad (actividad_id),
  KEY idx_evento_actividad_company_inicio (company_id, inicio),
  KEY idx_evento_actividad_actividad (actividad_id),
  CONSTRAINT fk_evento_actividad_company FOREIGN KEY (company_id) REFERENCES companies (id),
  CONSTRAINT fk_evento_actividad_actividad FOREIGN KEY (actividad_id) REFERENCES actividades (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
