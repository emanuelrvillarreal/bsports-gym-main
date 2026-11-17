--liquibase formatted sql

-- changeset bsports-club:v59-cuotas-idx-actividad-periodo
-- comment: La UK plana (company, persona, actividad, anio, mes) de v37 tambien servia de indice al
--          NOT EXISTS del job y al exists del alta por inscripcion. Como la UK pasa a la columna
--          generada, se deja el mismo prefijo como indice comun para no perder esos accesos.
CREATE INDEX idx_cuotas_actividad_periodo ON cuotas (company_id, persona_id, actividad_id, anio, mes);

-- changeset bsports-club:v59-cuotas-actividad-periodo-uid
-- comment: La cuota anulada deja de ocupar el periodo: se genera NULL para las ANULADA (y para las
--          SOCIAL, donde actividad_id es NULL) y MySQL admite NULLs repetidos en UNIQUE. Asi conviven
--          N anuladas con una sola cuota vigente del mismo (club, socio, actividad, periodo), que es
--          lo que permite volver a inscribir al socio despues de anularle la cuota del mes. Es STORED
--          y no VIRTUAL porque la indexa una UK; al anular, el UPDATE de estado la recalcula a NULL.
ALTER TABLE cuotas
  ADD COLUMN actividad_periodo_uid VARCHAR(64) GENERATED ALWAYS AS (
    IF(tipo = 'ACTIVIDAD' AND actividad_id IS NOT NULL AND estado <> 'ANULADA',
       CONCAT(company_id, '-', persona_id, '-', actividad_id, '-', anio, '-', mes), NULL)
  ) STORED;

-- changeset bsports-club:v59-cuotas-uk-actividad-periodo
-- comment: Reemplaza la UK plana de v37. No puede haber duplicados al crearla: la UK anterior era mas
--          estricta (no miraba el estado), asi que toda fila vigente ya era unica en su periodo.
ALTER TABLE cuotas DROP INDEX uk_cuotas_actividad_periodo;

ALTER TABLE cuotas
  ADD UNIQUE KEY uk_cuotas_actividad_periodo (actividad_periodo_uid);
