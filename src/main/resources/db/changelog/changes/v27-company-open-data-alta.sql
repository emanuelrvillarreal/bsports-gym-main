--liquibase formatted sql

-- changeset bsports-club:v27-company-domicilio-estructurado
-- comment: Domicilio institucional estructurado (dirección, CP, localidad)
ALTER TABLE companies
  CHANGE COLUMN domicilio domicilio_direccion VARCHAR(512) NULL,
  ADD COLUMN domicilio_codigo_postal VARCHAR(16) NULL,
  ADD COLUMN domicilio_localidad VARCHAR(128) NULL;

-- changeset bsports-club:v27-company-domicilios-facturacion
-- comment: Domicilios de facturación y entrega de facturas
ALTER TABLE companies
  ADD COLUMN domicilio_facturacion_direccion VARCHAR(512) NULL,
  ADD COLUMN domicilio_facturacion_codigo_postal VARCHAR(16) NULL,
  ADD COLUMN domicilio_facturacion_localidad VARCHAR(128) NULL,
  ADD COLUMN domicilio_entrega_facturas_direccion VARCHAR(512) NULL,
  ADD COLUMN domicilio_entrega_facturas_codigo_postal VARCHAR(16) NULL,
  ADD COLUMN domicilio_entrega_facturas_localidad VARCHAR(128) NULL;

-- changeset bsports-club:v27-company-open-data-escalares
-- comment: Datos de facturación y contacto Open Data
ALTER TABLE companies
  ADD COLUMN fax VARCHAR(50) NULL,
  ADD COLUMN telefono_pago_proveedores VARCHAR(50) NULL,
  ADD COLUMN responsable_pago_proveedores_nombre VARCHAR(255) NULL,
  ADD COLUMN responsable_pago_proveedores_apellido VARCHAR(255) NULL,
  ADD COLUMN responsable_pago_proveedores_cargo VARCHAR(128) NULL,
  ADD COLUMN responsable_pago_proveedores_email VARCHAR(255) NULL,
  ADD COLUMN dias_atencion_pago_proveedores VARCHAR(255) NULL,
  ADD COLUMN email_factura_electronica VARCHAR(255) NULL;

-- changeset bsports-club:v27-company-personas-autorizadas-open-data
-- comment: Personas autorizadas a operar con Blue Open Data
CREATE TABLE company_personas_autorizadas_open_data (
  company_id BIGINT NOT NULL,
  nombre VARCHAR(255) NOT NULL,
  apellido VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL,
  CONSTRAINT fk_company_personas_autorizadas_company FOREIGN KEY (company_id) REFERENCES companies (id)
);

CREATE INDEX idx_company_personas_autorizadas_company ON company_personas_autorizadas_open_data (company_id);
