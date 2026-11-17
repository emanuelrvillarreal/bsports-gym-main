--liquibase formatted sql

-- changeset bsports-club:v82-pases-membresias
-- comment: Pases libres y membresias. Cada club configura su oferta de pases (duracion, precio, ingresos,
-- actividades, instalaciones, dias y horarios). La venta de un pase genera una cuota de tipo PASE (no hay
-- sistema de pagos aparte): la membresia queda vigente cuando esa cuota se paga. Congelamientos e
-- intentos de ingreso por QR quedan registrados. Nada se borra: bajas y reemplazos conservan el historial.
CREATE TABLE pases (
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
  nombre VARCHAR(120) NOT NULL,
  descripcion TEXT NULL,
  categoria VARCHAR(20) NOT NULL,
  modalidad VARCHAR(20) NOT NULL,
  duracion_dias INT NULL,
  precio DECIMAL(12,2) NOT NULL,
  cantidad_ingresos INT NULL,
  dias_permitidos VARCHAR(80) NULL,
  hora_desde TIME NULL,
  hora_hasta TIME NULL,
  renovacion_automatica TINYINT(1) NOT NULL DEFAULT 0,
  congelamiento_extiende TINYINT(1) NOT NULL DEFAULT 1,
  familiar TINYINT(1) NOT NULL DEFAULT 0,
  max_integrantes INT NULL,
  venta_portal TINYINT(1) NOT NULL DEFAULT 0,
  venta_desde DATE NULL,
  venta_hasta DATE NULL,
  restricciones TEXT NULL,
  activo TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_pases_uuid (uuid),
  KEY idx_pases_company (company_id, status),
  CONSTRAINT fk_pases_company FOREIGN KEY (company_id) REFERENCES companies (id)
);

CREATE TABLE pases_actividades (
  pase_id BIGINT NOT NULL,
  actividad_id BIGINT NOT NULL,
  PRIMARY KEY (pase_id, actividad_id),
  CONSTRAINT fk_pases_actividades_pase FOREIGN KEY (pase_id) REFERENCES pases (id),
  CONSTRAINT fk_pases_actividades_actividad FOREIGN KEY (actividad_id) REFERENCES actividades (id)
);

CREATE TABLE pases_amenities (
  pase_id BIGINT NOT NULL,
  amenity_id BIGINT NOT NULL,
  PRIMARY KEY (pase_id, amenity_id),
  CONSTRAINT fk_pases_amenities_pase FOREIGN KEY (pase_id) REFERENCES pases (id),
  CONSTRAINT fk_pases_amenities_amenity FOREIGN KEY (amenity_id) REFERENCES amenities (id)
);

CREATE TABLE pases_descuentos (
  pase_id BIGINT NOT NULL,
  descuento_id BIGINT NOT NULL,
  PRIMARY KEY (pase_id, descuento_id),
  CONSTRAINT fk_pases_descuentos_pase FOREIGN KEY (pase_id) REFERENCES pases (id),
  CONSTRAINT fk_pases_descuentos_descuento FOREIGN KEY (descuento_id) REFERENCES descuentos (id)
);

CREATE TABLE membresias (
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
  pase_id BIGINT NOT NULL,
  member_id BIGINT NOT NULL,
  membresia_anterior_id BIGINT NULL,
  origen VARCHAR(20) NOT NULL,
  estado VARCHAR(20) NOT NULL,
  fecha_inicio DATE NOT NULL,
  fecha_fin DATE NOT NULL,
  fecha_fin_original DATE NOT NULL,
  ingresos_permitidos INT NULL,
  ingresos_usados INT NOT NULL DEFAULT 0,
  renovacion_automatica TINYINT(1) NOT NULL DEFAULT 0,
  precio DECIMAL(12,2) NOT NULL,
  fecha_baja DATE NULL,
  motivo_baja VARCHAR(255) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_membresias_uuid (uuid),
  KEY idx_membresias_company (company_id, status, estado),
  KEY idx_membresias_member (member_id),
  KEY idx_membresias_fin (company_id, fecha_fin),
  CONSTRAINT fk_membresias_company FOREIGN KEY (company_id) REFERENCES companies (id),
  CONSTRAINT fk_membresias_pase FOREIGN KEY (pase_id) REFERENCES pases (id),
  CONSTRAINT fk_membresias_member FOREIGN KEY (member_id) REFERENCES members (id),
  CONSTRAINT fk_membresias_anterior FOREIGN KEY (membresia_anterior_id) REFERENCES membresias (id)
);

CREATE TABLE membresias_integrantes (
  membresia_id BIGINT NOT NULL,
  member_id BIGINT NOT NULL,
  PRIMARY KEY (membresia_id, member_id),
  KEY idx_membresias_integrantes_member (member_id),
  CONSTRAINT fk_membresias_integrantes_membresia FOREIGN KEY (membresia_id) REFERENCES membresias (id),
  CONSTRAINT fk_membresias_integrantes_member FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE TABLE membresias_congelamientos (
  id BIGINT NOT NULL AUTO_INCREMENT,
  status TINYINT NOT NULL DEFAULT 0,
  uuid CHAR(36) NOT NULL,
  id_user_created BIGINT NULL,
  id_user_updated BIGINT NULL,
  id_user_deleted BIGINT NULL,
  date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  date_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  date_deleted TIMESTAMP NULL,
  membresia_id BIGINT NOT NULL,
  fecha_desde DATE NOT NULL,
  fecha_hasta DATE NOT NULL,
  dias INT NOT NULL,
  motivo VARCHAR(255) NOT NULL,
  extiende_vencimiento TINYINT(1) NOT NULL,
  autorizado_por BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_membresias_congelamientos_uuid (uuid),
  KEY idx_membresias_congelamientos_membresia (membresia_id, status),
  CONSTRAINT fk_membresias_congelamientos_membresia FOREIGN KEY (membresia_id) REFERENCES membresias (id),
  CONSTRAINT fk_membresias_congelamientos_usuario FOREIGN KEY (autorizado_por) REFERENCES auth_users (id)
);

CREATE TABLE membresias_accesos (
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
  membresia_id BIGINT NULL,
  member_id BIGINT NOT NULL,
  fecha DATETIME NOT NULL,
  permitido TINYINT(1) NOT NULL,
  motivo VARCHAR(255) NULL,
  actividad_id BIGINT NULL,
  amenity_id BIGINT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_membresias_accesos_uuid (uuid),
  KEY idx_membresias_accesos_membresia (membresia_id),
  KEY idx_membresias_accesos_company_fecha (company_id, fecha),
  CONSTRAINT fk_membresias_accesos_company FOREIGN KEY (company_id) REFERENCES companies (id),
  CONSTRAINT fk_membresias_accesos_membresia FOREIGN KEY (membresia_id) REFERENCES membresias (id),
  CONSTRAINT fk_membresias_accesos_member FOREIGN KEY (member_id) REFERENCES members (id),
  CONSTRAINT fk_membresias_accesos_actividad FOREIGN KEY (actividad_id) REFERENCES actividades (id),
  CONSTRAINT fk_membresias_accesos_amenity FOREIGN KEY (amenity_id) REFERENCES amenities (id)
);

-- La venta de un pase es una cuota mas (tipo PASE) vinculada a su membresia.
ALTER TABLE cuotas ADD COLUMN membresia_id BIGINT NULL;
ALTER TABLE cuotas ADD CONSTRAINT fk_cuotas_membresia FOREIGN KEY (membresia_id) REFERENCES membresias (id);
CREATE INDEX idx_cuotas_membresia ON cuotas (membresia_id);
