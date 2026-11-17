--liquibase formatted sql

-- changeset bsports-club:v79-evaluaciones-fisicas
-- comment: Evaluaciones fisicas sucesivas por socio (historial completo, baja logica). El IMC se guarda
-- calculado al registrar. Modulo opcional por club: la activacion vive en configurations
-- (evaluaciones.config). No son evaluaciones clinicas.
CREATE TABLE evaluaciones_fisicas (
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
  member_id BIGINT NOT NULL,
  registrada_por BIGINT NOT NULL,
  profesional VARCHAR(120) NULL,
  fecha DATE NOT NULL,
  peso_kg DECIMAL(5,2) NULL,
  altura_cm DECIMAL(5,1) NULL,
  imc DECIMAL(5,2) NULL,
  grasa_corporal_pct DECIMAL(4,1) NULL,
  masa_muscular_kg DECIMAL(5,2) NULL,
  cintura_cm DECIMAL(5,1) NULL,
  cadera_cm DECIMAL(5,1) NULL,
  pecho_cm DECIMAL(5,1) NULL,
  brazo_cm DECIMAL(5,1) NULL,
  muslo_cm DECIMAL(5,1) NULL,
  pantorrilla_cm DECIMAL(5,1) NULL,
  observaciones TEXT NULL,
  recomendaciones TEXT NULL,
  proxima_evaluacion DATE NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_evaluaciones_fisicas_uuid (uuid),
  KEY idx_evaluaciones_fisicas_member (company_id, member_id, status, fecha),
  CONSTRAINT fk_evaluaciones_fisicas_company FOREIGN KEY (company_id) REFERENCES companies (id),
  CONSTRAINT fk_evaluaciones_fisicas_member FOREIGN KEY (member_id) REFERENCES members (id),
  CONSTRAINT fk_evaluaciones_fisicas_user FOREIGN KEY (registrada_por) REFERENCES auth_users (id)
);
