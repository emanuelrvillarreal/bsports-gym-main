--liquibase formatted sql

-- changeset bsports-club:v66-alta-parcial-company
-- comment: Alta parcial de Company desde ALTA_CLIENTE (uuid del cliente en el CRM + onboarding). Se eliminan los datos de pago a proveedores, personas autorizadas y domicilio de entrega de facturas. Las facturas guardan uuid_cliente y cuit pasa a opcional.
ALTER TABLE companies ADD COLUMN uuid_cliente VARCHAR(64) NULL;
CREATE UNIQUE INDEX uk_companies_uuid_cliente ON companies (uuid_cliente);

ALTER TABLE companies ADD COLUMN onboarding_finalizado BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE companies SET onboarding_finalizado = TRUE;

DROP TABLE company_personas_autorizadas_open_data;

ALTER TABLE companies DROP COLUMN telefono_pago_proveedores;
ALTER TABLE companies DROP COLUMN responsable_pago_proveedores_nombre;
ALTER TABLE companies DROP COLUMN responsable_pago_proveedores_apellido;
ALTER TABLE companies DROP COLUMN responsable_pago_proveedores_cargo;
ALTER TABLE companies DROP COLUMN responsable_pago_proveedores_email;
ALTER TABLE companies DROP COLUMN dias_atencion_pago_proveedores;
ALTER TABLE companies DROP COLUMN domicilio_entrega_facturas_direccion;
ALTER TABLE companies DROP COLUMN domicilio_entrega_facturas_codigo_postal;
ALTER TABLE companies DROP COLUMN domicilio_entrega_facturas_localidad;

ALTER TABLE facturas ADD COLUMN uuid_cliente VARCHAR(64) NULL AFTER cuit;
CREATE INDEX idx_facturas_uuid_cliente ON facturas (uuid_cliente);
ALTER TABLE facturas MODIFY COLUMN cuit VARCHAR(20) NULL;
