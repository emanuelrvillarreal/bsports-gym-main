--liquibase formatted sql

-- changeset bsports-club:v11-companies-plazos-cuota
-- comment: Parámetros rolling de generación, vencimiento/moroso e inhabilitación por club
ALTER TABLE companies
  ADD COLUMN dias_entre_generaciones INT NOT NULL DEFAULT 30,
  ADD COLUMN dias_vencimiento_moroso INT NOT NULL DEFAULT 31,
  ADD COLUMN dias_inhabilitacion INT NOT NULL DEFAULT 91;

-- changeset bsports-club:v11-cuotas-fecha-generacion
-- comment: Fecha de negocio ancla para rolling, vencimiento y morosidad
ALTER TABLE cuotas
  ADD COLUMN fecha_generacion DATE NOT NULL DEFAULT (CURRENT_DATE);

-- changeset bsports-club:v11-cuotas-drop-uk-mes-anio
-- comment: Idempotencia rolling por fecha_generacion en lugar de mes/anio
ALTER TABLE cuotas DROP INDEX uk_cuotas_social_ciclo;
ALTER TABLE cuotas DROP INDEX uk_cuotas_actividad_ciclo;

-- changeset bsports-club:v11-cuotas-backfill-fecha-generacion
-- comment: Derivar fecha_generacion del ciclo mes/anio existente (no CURRENT_DATE masivo)
UPDATE cuotas
SET fecha_generacion = STR_TO_DATE(CONCAT(anio, '-', LPAD(mes, 2, '0'), '-01'), '%Y-%m-%d')
WHERE mes IS NOT NULL AND anio IS NOT NULL;

UPDATE cuotas
SET fecha_generacion = DATE(date_created)
WHERE mes IS NULL OR anio IS NULL;

-- changeset bsports-club:v11-cuotas-uk-fecha-generacion
-- comment: Unique parcial social y actividad vía columnas generadas (varias ACTIVIDAD por socio no colisionan en social)
ALTER TABLE cuotas
  ADD COLUMN social_fecha_gen_uid VARCHAR(64) GENERATED ALWAYS AS (
    IF(tipo = 'SOCIAL', CONCAT(company_id, '-', persona_id, '-', fecha_generacion), NULL)
  ) STORED,
  ADD COLUMN actividad_fecha_gen_uid VARCHAR(80) GENERATED ALWAYS AS (
    IF(tipo = 'ACTIVIDAD' AND actividad_id IS NOT NULL,
       CONCAT(company_id, '-', persona_id, '-', actividad_id, '-', fecha_generacion),
       NULL)
  ) STORED;

ALTER TABLE cuotas
  ADD UNIQUE KEY uk_cuotas_social_fecha_gen (social_fecha_gen_uid),
  ADD UNIQUE KEY uk_cuotas_actividad_fecha_gen (actividad_fecha_gen_uid);

-- changeset bsports-club:v11-cuotas-idx-fecha-generacion
-- comment: Índices para jobs rolling y recálculo de deuda social grupal
CREATE INDEX idx_cuotas_fecha_generacion ON cuotas (company_id, persona_id, tipo, fecha_generacion);
CREATE INDEX idx_cuotas_social_impaga_titular ON cuotas (company_id, titular_id, tipo, estado, fecha_generacion);
