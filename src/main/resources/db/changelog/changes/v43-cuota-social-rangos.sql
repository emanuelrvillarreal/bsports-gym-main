--liquibase formatted sql

-- changeset bsports-club:v43-cuota-social-rangos
-- comment: Rangos etarios con importe propio para la cuota social
CREATE TABLE cuotas_sociales_rangos (
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
  descripcion VARCHAR(512) NULL,
  edad_minima INT NOT NULL,
  edad_maxima INT NOT NULL,
  importe DECIMAL(12,2) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cuotas_sociales_rangos_uuid (uuid),
  UNIQUE KEY uk_cuotas_sociales_rangos_company_nombre (company_id, nombre),
  KEY fk_cuotas_sociales_rangos_company (company_id),
  CONSTRAINT fk_cuotas_sociales_rangos_company FOREIGN KEY (company_id) REFERENCES companies (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
