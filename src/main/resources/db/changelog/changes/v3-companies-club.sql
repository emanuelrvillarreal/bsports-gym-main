--liquibase formatted sql

-- changeset bsports-club:v3-companies-club
-- comment: Campos de organización Club (B Sport Clubes complementario v1)
ALTER TABLE companies
  ADD COLUMN nombre_comercial VARCHAR(255) NULL,
  ADD COLUMN razon_social VARCHAR(255) NULL,
  ADD COLUMN domicilio VARCHAR(512) NULL,
  ADD COLUMN telefono VARCHAR(50) NULL,
  ADD COLUMN email_institucional VARCHAR(255) NULL,
  ADD COLUMN logo_url VARCHAR(512) NULL,
  ADD COLUMN color_primario VARCHAR(32) NULL,
  ADD COLUMN color_secundario VARCHAR(32) NULL,
  ADD COLUMN cuota_social_habilitada BIT(1) NULL,
  ADD COLUMN importe_cuota_social DECIMAL(12, 2) NULL,
  ADD COLUMN sysadmin_creado BIT(1) NULL;

-- changeset bsports-club:v3-companies-nombre-backfill
-- comment: Backfill nombre_comercial desde name existente
UPDATE companies SET nombre_comercial = name WHERE nombre_comercial IS NULL AND name IS NOT NULL AND name != '';
