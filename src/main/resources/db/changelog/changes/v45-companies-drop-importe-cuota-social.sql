--liquibase formatted sql

-- changeset bsports-club:v45-companies-drop-importe-cuota-social
-- comment: El importe de la cuota social ahora vive por rango etario (cuotas_sociales_rangos)
ALTER TABLE companies
  DROP COLUMN importe_cuota_social;
