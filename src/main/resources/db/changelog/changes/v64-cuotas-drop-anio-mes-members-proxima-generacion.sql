--liquibase formatted sql

-- changeset bsports-club:v64-members-proxima-generacion-cuota
-- comment: Fecha en la que le toca la proxima cuota social al socio. El job genera cuando llega y la avanza
--          companies.dias_entre_generaciones en la misma transaccion, igual que inscripciones_actividad.proxima_generacion.
ALTER TABLE members ADD COLUMN proxima_generacion_cuota DATE NULL AFTER fecha_alta;

-- Ultima cuota social del socio (las anuladas cuentan: el club decidio no cobrar ese tramo) mas los dias del club.
-- Sin cuotas se toma el mayor entre la fecha de alta y hoy, para no facturar hacia atras a socios de clubes que
-- todavia no usan cuota social.
UPDATE members m
  JOIN companies co ON co.id = m.company_id
   SET m.proxima_generacion_cuota = COALESCE(
         (SELECT DATE_ADD(MAX(c.fecha_generacion), INTERVAL co.dias_entre_generaciones DAY)
            FROM cuotas c
           WHERE c.company_id = m.company_id
             AND c.persona_id = m.id
             AND c.tipo = 'SOCIAL'
             AND c.status = 0),
         GREATEST(m.fecha_alta, CURDATE()));

ALTER TABLE members MODIFY COLUMN proxima_generacion_cuota DATE NOT NULL;

CREATE INDEX idx_members_proxima_generacion_cuota
  ON members (company_id, status, proxima_generacion_cuota);

-- changeset bsports-club:v64-cuotas-drop-anio-mes
-- comment: Las cuotas dejan de ser mensuales: se generan cada N dias segun el puntero de la inscripcion o del socio.
--          Se quitan las UK e indices por periodo y las columnas anio/mes.
ALTER TABLE cuotas DROP INDEX uk_cuotas_actividad_periodo;
ALTER TABLE cuotas DROP INDEX uk_cuotas_social_periodo;
ALTER TABLE cuotas DROP INDEX idx_cuotas_actividad_periodo;
ALTER TABLE cuotas DROP INDEX idx_cuotas_periodo;

ALTER TABLE cuotas
  DROP COLUMN actividad_periodo_uid,
  DROP COLUMN social_periodo_uid;

ALTER TABLE cuotas
  DROP COLUMN anio,
  DROP COLUMN mes;

-- changeset bsports-club:v64-cuotas-uk-fecha-generacion
-- comment: Red de seguridad contra duplicados: el puntero ya evita generar dos veces la misma fecha, esta UK lo
--          garantiza en la base. Las ANULADA quedan en NULL (MySQL admite NULLs repetidos en UNIQUE), igual que v59.
--          No puede haber duplicados al crearla: las UK anteriores eran por mes, mas estrictas que por fecha.
ALTER TABLE cuotas
  ADD COLUMN actividad_fecha_gen_uid VARCHAR(80) GENERATED ALWAYS AS (
    IF(tipo = 'ACTIVIDAD' AND actividad_id IS NOT NULL AND estado <> 'ANULADA',
       CONCAT(company_id, '-', persona_id, '-', actividad_id, '-', fecha_generacion), NULL)
  ) STORED,
  ADD COLUMN social_fecha_gen_uid VARCHAR(64) GENERATED ALWAYS AS (
    IF(tipo = 'SOCIAL' AND estado <> 'ANULADA',
       CONCAT(company_id, '-', persona_id, '-', fecha_generacion), NULL)
  ) STORED;

ALTER TABLE cuotas
  ADD UNIQUE KEY uk_cuotas_actividad_fecha_gen (actividad_fecha_gen_uid),
  ADD UNIQUE KEY uk_cuotas_social_fecha_gen (social_fecha_gen_uid);

-- changeset bsports-club:v64-cuotas-idx-company-fecha-generacion
-- comment: Sirve a sumMontoPorCobrarByPeriodo, que ahora filtra por club y rango de fecha_generacion.
CREATE INDEX idx_cuotas_company_fecha_generacion ON cuotas (company_id, fecha_generacion);
