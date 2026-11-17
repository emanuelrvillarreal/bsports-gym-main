--liquibase formatted sql

-- changeset bsports-club:v77-inscripciones-fecha-baja
-- comment: Historial de inscripciones. La baja ya era logica (status = -1) y la reinscripcion crea una fila
-- nueva; ahora cada periodo guarda cuando termino y por que. Las bajas anteriores toman la fecha de su
-- ultima modificacion, que es cuando se dieron de baja.
ALTER TABLE inscripciones_actividad
  ADD COLUMN fecha_baja DATE NULL,
  ADD COLUMN motivo_baja VARCHAR(255) NULL;

UPDATE inscripciones_actividad
SET fecha_baja = DATE(COALESCE(date_deleted, date_updated, date_created)),
    date_updated = date_updated
WHERE status = -1 AND fecha_baja IS NULL;
