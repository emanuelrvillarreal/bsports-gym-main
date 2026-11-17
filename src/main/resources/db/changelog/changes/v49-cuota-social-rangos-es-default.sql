--liquibase formatted sql

-- changeset bsports-club:v49-cuota-social-rangos-es-default
-- comment: Marca el rango catch-all sembrado por v47 como no eliminable
ALTER TABLE cuotas_sociales_rangos
  ADD COLUMN es_default TINYINT(1) NOT NULL DEFAULT 0;

UPDATE cuotas_sociales_rangos
SET es_default = 1
WHERE nombre = 'General'
  AND descripcion IS NULL
  AND edad_hasta = 100
  AND importe = 0.00;
