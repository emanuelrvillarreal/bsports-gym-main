--liquibase formatted sql

-- changeset bsports-club:v6-modulo-actividades-profesores
-- comment: Profesores del club (vínculo user_id, patrón members)
CREATE TABLE profesores (
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
  user_id BIGINT NOT NULL,
  estado VARCHAR(20) NOT NULL,
  fecha_alta DATE NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_profesores_uuid (uuid),
  UNIQUE KEY uk_profesores_company_user (company_id, user_id),
  KEY fk_profesores_company (company_id),
  KEY fk_profesores_user (user_id),
  CONSTRAINT fk_profesores_company FOREIGN KEY (company_id) REFERENCES companies (id),
  CONSTRAINT fk_profesores_user FOREIGN KEY (user_id) REFERENCES auth_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- changeset bsports-club:v6-modulo-actividades-actividades
-- comment: Actividades (comisiones) con dias_semana y horario único
CREATE TABLE actividades (
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
  nombre VARCHAR(255) NOT NULL,
  ubicacion VARCHAR(255) NOT NULL,
  cupo_maximo INT NOT NULL,
  dias_semana VARCHAR(128) NOT NULL,
  hora_inicio TIME NOT NULL,
  hora_fin TIME NOT NULL,
  importe_cuota DECIMAL(12, 2) NOT NULL,
  completa TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_actividades_uuid (uuid),
  KEY fk_actividades_company (company_id),
  CONSTRAINT fk_actividades_company FOREIGN KEY (company_id) REFERENCES companies (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- changeset bsports-club:v6-modulo-actividades-profesores-mn
-- comment: Asignación M:N actividad ↔ profesor
CREATE TABLE actividades_profesores (
  actividad_id BIGINT NOT NULL,
  profesor_id BIGINT NOT NULL,
  PRIMARY KEY (actividad_id, profesor_id),
  KEY fk_ap_profesor (profesor_id),
  CONSTRAINT fk_ap_actividad FOREIGN KEY (actividad_id) REFERENCES actividades (id),
  CONSTRAINT fk_ap_profesor FOREIGN KEY (profesor_id) REFERENCES profesores (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- changeset bsports-club:v6-modulo-actividades-slots
-- comment: Slots genéricos de calendario (actividad_id; amenity_id reservado MVP 2)
CREATE TABLE slots (
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
  inicio DATETIME NOT NULL,
  fin DATETIME NOT NULL,
  estado VARCHAR(20) NOT NULL,
  amenity_id BIGINT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_slots_uuid (uuid),
  UNIQUE KEY uk_slots_actividad_inicio (actividad_id, inicio),
  KEY idx_slots_company_inicio (company_id, inicio),
  KEY fk_slots_company (company_id),
  KEY fk_slots_actividad (actividad_id),
  CONSTRAINT fk_slots_company FOREIGN KEY (company_id) REFERENCES companies (id),
  CONSTRAINT fk_slots_actividad FOREIGN KEY (actividad_id) REFERENCES actividades (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- changeset bsports-club:v6-modulo-actividades-cuotas-fk
-- comment: FK cuotas.actividad_id → actividades
ALTER TABLE cuotas
  ADD CONSTRAINT fk_cuotas_actividad FOREIGN KEY (actividad_id) REFERENCES actividades (id);
