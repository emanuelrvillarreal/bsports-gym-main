--liquibase formatted sql

-- changeset bsports-club:v15-accesos-club
-- comment: Log de ingresos y egresos al club (append-only)
CREATE TABLE accesos_club (
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
  tipo VARCHAR(20) NOT NULL,
  fecha TIMESTAMP NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_accesos_club_uuid (uuid),
  KEY idx_accesos_club_company_fecha (company_id, fecha),
  KEY idx_accesos_club_company_user_fecha (company_id, user_id, fecha),
  KEY fk_accesos_club_company (company_id),
  KEY fk_accesos_club_user (user_id),
  CONSTRAINT fk_accesos_club_company FOREIGN KEY (company_id) REFERENCES companies (id),
  CONSTRAINT fk_accesos_club_user FOREIGN KEY (user_id) REFERENCES auth_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- changeset bsports-club:v15-club-presencia
-- comment: Estado en tiempo real de presencia en el club por usuario
CREATE TABLE club_presencia (
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
  presente TINYINT(1) NOT NULL DEFAULT 0,
  ultimo_acceso_at TIMESTAMP NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_club_presencia_uuid (uuid),
  UNIQUE KEY uk_club_presencia_company_user (company_id, user_id),
  KEY idx_club_presencia_company_presente (company_id, presente),
  KEY fk_club_presencia_company (company_id),
  KEY fk_club_presencia_user (user_id),
  CONSTRAINT fk_club_presencia_company FOREIGN KEY (company_id) REFERENCES companies (id),
  CONSTRAINT fk_club_presencia_user FOREIGN KEY (user_id) REFERENCES auth_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
