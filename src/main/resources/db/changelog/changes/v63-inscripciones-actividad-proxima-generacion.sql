--liquibase formatted sql

-- changeset bsports-club:v63-inscripciones-actividad-proxima-generacion
-- comment: Fecha en la que le toca la proxima cuota a la inscripcion. El job genera cuando llega y la avanza
--          dias_generacion_cuota en la misma transaccion, asi una segunda corrida no la vuelve a encontrar.
--          version habilita el optimistic locking para que dos procesos no generen la misma cuota a la vez.
ALTER TABLE inscripciones_actividad
  ADD COLUMN proxima_generacion DATE NULL AFTER fecha_inscripcion,
  ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Se toma la ultima cuota de la propia inscripcion (fecha_generacion >= fecha_inscripcion), no la de una
-- inscripcion anterior del mismo socio a la misma actividad. Las anuladas cuentan: el club decidio no cobrar
-- ese tramo y el job no debe reponerlo. Sin cuotas, la proxima generacion es la fecha de inscripcion.
UPDATE inscripciones_actividad i
  JOIN actividades a ON a.id = i.actividad_id
   SET i.proxima_generacion = COALESCE(
         (SELECT DATE_ADD(MAX(c.fecha_generacion), INTERVAL a.dias_generacion_cuota DAY)
            FROM cuotas c
           WHERE c.company_id = i.company_id
             AND c.persona_id = i.member_id
             AND c.actividad_id = i.actividad_id
             AND c.tipo = 'ACTIVIDAD'
             AND c.status = 0
             AND c.fecha_generacion >= i.fecha_inscripcion),
         i.fecha_inscripcion);

ALTER TABLE inscripciones_actividad MODIFY COLUMN proxima_generacion DATE NOT NULL;

CREATE INDEX idx_inscripciones_actividad_proxima_generacion
  ON inscripciones_actividad (company_id, status, proxima_generacion);
