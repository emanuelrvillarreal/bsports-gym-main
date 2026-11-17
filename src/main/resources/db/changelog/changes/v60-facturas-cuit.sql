--liquibase formatted sql

-- changeset bsports-club:v60-facturas-cuit
-- comment: CUIT de la factura (solo digitos). Permite registrar la deuda del CRM aunque el club todavia no exista; company_id pasa a ser opcional y se completa al dar de alta el club
ALTER TABLE facturas ADD COLUMN cuit VARCHAR(20) NULL AFTER company_id;

UPDATE facturas f
  JOIN companies c ON c.id = f.company_id
   SET f.cuit = REPLACE(REPLACE(REPLACE(c.cuit, '-', ''), ' ', ''), '.', '');

ALTER TABLE facturas MODIFY COLUMN cuit VARCHAR(20) NOT NULL;
ALTER TABLE facturas MODIFY COLUMN company_id BIGINT NULL;
CREATE INDEX idx_facturas_cuit ON facturas (cuit);
