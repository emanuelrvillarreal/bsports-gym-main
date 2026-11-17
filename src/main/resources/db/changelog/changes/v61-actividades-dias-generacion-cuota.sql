--liquibase formatted sql

-- changeset bsports-club:v61-actividades-dias-generacion-cuota
-- comment: Dias de generacion de cuota por actividad; las actividades existentes reciben 30 por default
ALTER TABLE actividades ADD COLUMN dias_generacion_cuota INT NOT NULL DEFAULT 30 AFTER importe_cuota;
