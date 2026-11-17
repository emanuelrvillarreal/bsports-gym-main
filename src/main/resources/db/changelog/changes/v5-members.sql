--liquibase formatted sql

-- changeset bsports-club:v5-members-categorias
-- comment: Categorías de socios por club
CREATE TABLE categorias (
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
  PRIMARY KEY (id),
  UNIQUE KEY uk_categorias_uuid (uuid),
  UNIQUE KEY uk_categorias_company_nombre (company_id, nombre),
  KEY fk_categorias_company (company_id),
  CONSTRAINT fk_categorias_company FOREIGN KEY (company_id) REFERENCES companies (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- changeset bsports-club:v5-members-table
-- comment: Socios titulares y familiares (grupo familiar vía titular_id auto-referenciado)
CREATE TABLE members (
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
  titular_id BIGINT NULL,
  parentesco VARCHAR(30) NULL,
  numero_socio VARCHAR(20) NULL,
  categoria_id BIGINT NULL,
  estado VARCHAR(20) NOT NULL,
  grupo_inhabilitado_at TIMESTAMP NULL,
  fecha_alta DATE NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_members_uuid (uuid),
  UNIQUE KEY uk_members_user_company (company_id, user_id),
  UNIQUE KEY uk_members_numero_socio (company_id, numero_socio),
  KEY fk_members_company (company_id),
  KEY fk_members_user (user_id),
  KEY fk_members_titular (titular_id),
  KEY fk_members_categoria (categoria_id),
  KEY idx_members_titular (company_id, titular_id),
  KEY idx_members_estado (company_id, estado),
  CONSTRAINT fk_members_company FOREIGN KEY (company_id) REFERENCES companies (id),
  CONSTRAINT fk_members_user FOREIGN KEY (user_id) REFERENCES auth_users (id),
  CONSTRAINT fk_members_titular FOREIGN KEY (titular_id) REFERENCES members (id),
  CONSTRAINT fk_members_categoria FOREIGN KEY (categoria_id) REFERENCES categorias (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- changeset bsports-club:v5-member-conversiones
-- comment: Auditoría de conversión familiar a socio titular (doc §3.5)
CREATE TABLE member_conversiones (
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
  titular_anterior_id BIGINT NOT NULL,
  nuevo_numero_socio VARCHAR(20) NOT NULL,
  ejecutado_por BIGINT NOT NULL,
  fecha TIMESTAMP NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_member_conversiones_uuid (uuid),
  KEY fk_member_conversiones_company (company_id),
  KEY fk_member_conversiones_member (member_id),
  KEY fk_member_conversiones_titular_anterior (titular_anterior_id),
  KEY fk_member_conversiones_ejecutado_por (ejecutado_por),
  CONSTRAINT fk_member_conversiones_company FOREIGN KEY (company_id) REFERENCES companies (id),
  CONSTRAINT fk_member_conversiones_member FOREIGN KEY (member_id) REFERENCES members (id),
  CONSTRAINT fk_member_conversiones_titular_anterior FOREIGN KEY (titular_anterior_id) REFERENCES members (id),
  CONSTRAINT fk_member_conversiones_ejecutado_por FOREIGN KEY (ejecutado_por) REFERENCES auth_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- changeset bsports-club:v5-tesoreria-fk-members
-- comment: FKs de cuotas y movimientos hacia members
ALTER TABLE cuotas
  ADD CONSTRAINT fk_cuotas_persona FOREIGN KEY (persona_id) REFERENCES members (id),
  ADD CONSTRAINT fk_cuotas_titular FOREIGN KEY (titular_id) REFERENCES members (id);

ALTER TABLE movimientos
  ADD CONSTRAINT fk_movimientos_persona FOREIGN KEY (persona_id) REFERENCES members (id),
  ADD CONSTRAINT fk_movimientos_titular FOREIGN KEY (titular_id) REFERENCES members (id);
