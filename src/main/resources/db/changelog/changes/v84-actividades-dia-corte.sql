--liquibase formatted sql

-- changeset bsports-club:v84-actividades-dia-corte
-- comment: Dia de corte de inscripcion (solo actividades con dia fijo de vencimiento). Si alguien se inscribe
-- despues de ese dia, no se le cobra el mes en curso: su primera cuota es la del mes siguiente.
ALTER TABLE actividades ADD COLUMN dia_corte_inscripcion INT NULL;
-- rollback ALTER TABLE actividades DROP COLUMN dia_corte_inscripcion;
