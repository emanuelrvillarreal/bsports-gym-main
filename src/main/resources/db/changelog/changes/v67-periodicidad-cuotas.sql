--liquibase formatted sql

-- changeset bsports-club:v67-periodicidad-cuotas
-- comment: Las cuotas dejan de generarse cada N dias y pasan a una periodicidad de calendario (SEMANAL, MENSUAL,
--          BIMESTRAL, TRIMESTRAL, CUATRIMESTRAL, SEMESTRAL, ANUAL). Clubes y actividades existentes quedan en MENSUAL.
--          Los punteros members.proxima_generacion_cuota e inscripciones_actividad.proxima_generacion no se tocan:
--          desde su fecha actual pasan a avanzar por periodo.
ALTER TABLE companies ADD COLUMN periodicidad_cuota_social VARCHAR(20) NOT NULL DEFAULT 'MENSUAL' AFTER dias_entre_generaciones;
ALTER TABLE companies DROP COLUMN dias_entre_generaciones;

ALTER TABLE actividades ADD COLUMN periodicidad_cuota VARCHAR(20) NOT NULL DEFAULT 'MENSUAL' AFTER dias_generacion_cuota;
ALTER TABLE actividades DROP COLUMN dias_generacion_cuota;
