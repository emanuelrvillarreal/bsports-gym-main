--liquibase formatted sql

-- changeset backend-multiempresa:v1-initial-schema
-- comment: Esquema inicial (MySQL 8 / InnoDB). Semillas RBAC y empresas uuid 1/2 en DatabaseInitializerService.
CREATE TABLE companies (
  id BIGINT NOT NULL AUTO_INCREMENT,
  status TINYINT NOT NULL DEFAULT 0,
  uuid CHAR(36) NOT NULL,
  id_user_created BIGINT NULL,
  id_user_updated BIGINT NULL,
  id_user_deleted BIGINT NULL,
  date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  date_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  date_deleted TIMESTAMP NULL,
  name VARCHAR(255) NULL,
  path VARCHAR(255) NULL,
  description VARCHAR(255) NULL,
  code VARCHAR(255) NULL,
  balance DOUBLE NULL,
  cuit VARCHAR(255) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_companies_uuid (uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE auth_permissions (
  id BIGINT NOT NULL AUTO_INCREMENT,
  status TINYINT NOT NULL DEFAULT 0,
  uuid CHAR(36) NOT NULL,
  id_user_created BIGINT NULL,
  id_user_updated BIGINT NULL,
  id_user_deleted BIGINT NULL,
  date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  date_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  date_deleted TIMESTAMP NULL,
  name VARCHAR(255) NULL,
  description VARCHAR(255) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_auth_permissions_uuid (uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE auth_users (
  id BIGINT NOT NULL AUTO_INCREMENT,
  status TINYINT NOT NULL DEFAULT 0,
  uuid CHAR(36) NOT NULL,
  id_user_created BIGINT NULL,
  id_user_updated BIGINT NULL,
  id_user_deleted BIGINT NULL,
  date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  date_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  date_deleted TIMESTAMP NULL,
  username VARCHAR(255) NULL,
  password VARCHAR(255) NULL,
  sso_uuid VARCHAR(255) NULL,
  malvinas_legajo VARCHAR(255) NULL,
  name VARCHAR(255) NULL,
  department_name VARCHAR(255) NULL,
  group_name VARCHAR(255) NULL,
  email VARCHAR(255) NULL,
  phone VARCHAR(255) NULL,
  verification_code VARCHAR(255) NULL,
  verification_code_expiry DATETIME(6) NULL,
  verification_code_last_sent DATETIME(6) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_auth_users_uuid (uuid),
  UNIQUE KEY uk_auth_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE configurations (
  id BIGINT NOT NULL AUTO_INCREMENT,
  status TINYINT NOT NULL DEFAULT 0,
  uuid CHAR(36) NOT NULL,
  id_user_created BIGINT NULL,
  id_user_updated BIGINT NULL,
  id_user_deleted BIGINT NULL,
  date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  date_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  date_deleted TIMESTAMP NULL,
  name VARCHAR(255) NOT NULL,
  value TEXT NOT NULL,
  `global` BIT(1) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_configurations_uuid (uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE auth_roles (
  id BIGINT NOT NULL AUTO_INCREMENT,
  status TINYINT NOT NULL DEFAULT 0,
  uuid CHAR(36) NOT NULL,
  id_user_created BIGINT NULL,
  id_user_updated BIGINT NULL,
  id_user_deleted BIGINT NULL,
  date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  date_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  date_deleted TIMESTAMP NULL,
  name VARCHAR(255) NULL,
  company_id BIGINT NULL,
  is_default TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_auth_roles_uuid (uuid),
  KEY fk_auth_roles_company (company_id),
  CONSTRAINT fk_auth_roles_company FOREIGN KEY (company_id) REFERENCES companies (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE auth_users_companies (
  id BIGINT NOT NULL AUTO_INCREMENT,
  status TINYINT NOT NULL DEFAULT 0,
  uuid CHAR(36) NOT NULL,
  id_user_created BIGINT NULL,
  id_user_updated BIGINT NULL,
  id_user_deleted BIGINT NULL,
  date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  date_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  date_deleted TIMESTAMP NULL,
  user_id BIGINT NULL,
  company_id BIGINT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_auth_users_companies_uuid (uuid),
  KEY fk_uc_user (user_id),
  KEY fk_uc_company (company_id),
  CONSTRAINT fk_uc_user FOREIGN KEY (user_id) REFERENCES auth_users (id),
  CONSTRAINT fk_uc_company FOREIGN KEY (company_id) REFERENCES companies (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE auth_roles_permissions (
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  PRIMARY KEY (role_id, permission_id),
  KEY fk_arp_permission (permission_id),
  CONSTRAINT fk_arp_role FOREIGN KEY (role_id) REFERENCES auth_roles (id),
  CONSTRAINT fk_arp_permission FOREIGN KEY (permission_id) REFERENCES auth_permissions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE auth_user_company_roles (
  user_company_role_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_company_role_id, role_id),
  KEY fk_aucr_role (role_id),
  CONSTRAINT fk_aucr_uc FOREIGN KEY (user_company_role_id) REFERENCES auth_users_companies (id),
  CONSTRAINT fk_aucr_role FOREIGN KEY (role_id) REFERENCES auth_roles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE security_code (
  id BIGINT NOT NULL AUTO_INCREMENT,
  status TINYINT NOT NULL DEFAULT 0,
  uuid CHAR(36) NOT NULL,
  id_user_created BIGINT NULL,
  id_user_updated BIGINT NULL,
  id_user_deleted BIGINT NULL,
  date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  date_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  date_deleted TIMESTAMP NULL,
  code VARCHAR(255) NULL,
  created_at DATETIME(6) NULL,
  purpose VARCHAR(255) NULL,
  used TINYINT(1) NULL,
  user_id BIGINT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_security_code_uuid (uuid),
  KEY fk_security_code_user (user_id),
  CONSTRAINT fk_security_code_user FOREIGN KEY (user_id) REFERENCES auth_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE auth_user_access_tokens (
  id BIGINT NOT NULL AUTO_INCREMENT,
  status TINYINT NOT NULL DEFAULT 0,
  uuid CHAR(36) NOT NULL,
  id_user_created BIGINT NULL,
  id_user_updated BIGINT NULL,
  id_user_deleted BIGINT NULL,
  date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  date_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  date_deleted TIMESTAMP NULL,
  user_id BIGINT NULL,
  name VARCHAR(255) NULL,
  code VARCHAR(255) NULL,
  active INT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_auth_user_access_tokens_uuid (uuid),
  KEY fk_auat_user (user_id),
  CONSTRAINT fk_auat_user FOREIGN KEY (user_id) REFERENCES auth_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE auth_jwt_refresh_tokens (
  id BIGINT NOT NULL AUTO_INCREMENT,
  status TINYINT NOT NULL DEFAULT 0,
  uuid CHAR(36) NOT NULL,
  id_user_created BIGINT NULL,
  id_user_updated BIGINT NULL,
  id_user_deleted BIGINT NULL,
  date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  date_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  date_deleted TIMESTAMP NULL,
  user_id BIGINT NOT NULL,
  token VARCHAR(1024) NOT NULL,
  expiry_date DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_auth_jwt_refresh_tokens_uuid (uuid),
  UNIQUE KEY uk_auth_jwt_refresh_tokens_token (token(767)),
  KEY fk_jrt_user (user_id),
  CONSTRAINT fk_jrt_user FOREIGN KEY (user_id) REFERENCES auth_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
