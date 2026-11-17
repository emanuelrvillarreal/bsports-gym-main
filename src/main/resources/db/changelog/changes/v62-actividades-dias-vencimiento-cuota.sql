--liquibase formatted sql

-- changeset bsports-club:v62-actividades-dias-vencimiento-cuota
-- comment: Dias de vencimiento de cuota por actividad; las actividades existentes reciben 30 por default
ALTER TABLE actividades ADD COLUMN dias_vencimiento_cuota INT NOT NULL DEFAULT 30 AFTER dias_generacion_cuota;
