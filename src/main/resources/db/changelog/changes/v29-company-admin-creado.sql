--liquibase formatted sql

-- changeset bsports-club:v29-company-admin-creado
-- comment: Renombrar sysadmin_creado a admin_creado
ALTER TABLE companies
  CHANGE COLUMN sysadmin_creado admin_creado BIT(1) NULL;
