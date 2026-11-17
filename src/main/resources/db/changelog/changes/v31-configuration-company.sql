--liquibase formatted sql

-- changeset bsports-club:v31-configuration-company
-- comment: Agregar relación de configurations con companies
ALTER TABLE configurations
  ADD COLUMN company_id BIGINT NULL,
  ADD CONSTRAINT fk_configurations_company FOREIGN KEY (company_id) REFERENCES companies (id);
