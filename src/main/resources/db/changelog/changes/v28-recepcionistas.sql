--liquibase formatted sql

-- changeset bsports-club:v28-recepcionistas
-- comment: Recepcionistas del club (vínculo user_id, patrón profesores)
CREATE TABLE recepcionistas (
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
  UNIQUE KEY uk_recepcionistas_uuid (uuid),
  UNIQUE KEY uk_recepcionistas_company_user (company_id, user_id),
  KEY fk_recepcionistas_company (company_id),
  KEY fk_recepcionistas_user (user_id),
  CONSTRAINT fk_recepcionistas_company FOREIGN KEY (company_id) REFERENCES companies (id),
  CONSTRAINT fk_recepcionistas_user FOREIGN KEY (user_id) REFERENCES auth_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
