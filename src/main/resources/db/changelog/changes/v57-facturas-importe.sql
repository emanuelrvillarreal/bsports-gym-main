--liquibase formatted sql

-- changeset bsports-club:v57-facturas-importe
-- comment: Importe de la factura, resuelto desde el precio del plan y/o los productos
ALTER TABLE facturas ADD COLUMN importe DECIMAL(12,2) NULL;
