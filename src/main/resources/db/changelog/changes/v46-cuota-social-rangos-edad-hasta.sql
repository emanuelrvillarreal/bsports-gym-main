--liquibase formatted sql

-- changeset bsports-club:v46-cuota-social-rangos-edad-hasta
-- comment: CuotaSocialRango pasa a modelarse como tramos por edad_hasta (sin edad_minima)
ALTER TABLE cuotas_sociales_rangos
  DROP COLUMN edad_minima,
  CHANGE COLUMN edad_maxima edad_hasta INT NOT NULL,
  ADD UNIQUE KEY uk_cuotas_sociales_rangos_company_edad_hasta (company_id, edad_hasta);
