--liquibase formatted sql

-- changeset bsports-club:v58-companies-drop-fax
-- comment: El alta/perfil de club ya no pide ni persiste fax
ALTER TABLE companies
  DROP COLUMN fax;
