--liquibase formatted sql

-- changeset bsports-club:v87-certificado-medico-configurable
-- comment: Vigencia del certificado medico configurable por club (FECHA_FIJA: todos renuevan el dia/mes indicado;
-- DURACION: vence N meses despues de la carga) y exigencia por actividad. Los valores por defecto reproducen el
-- comportamiento anterior: vence el 31/12 (renovacion el 1/1) y ninguna actividad lo exige.
ALTER TABLE companies ADD COLUMN certificado_vigencia_modo VARCHAR(20) NOT NULL DEFAULT 'FECHA_FIJA';
ALTER TABLE companies ADD COLUMN certificado_vigencia_meses INT NOT NULL DEFAULT 12;
ALTER TABLE companies ADD COLUMN certificado_renovacion_dia INT NOT NULL DEFAULT 1;
ALTER TABLE companies ADD COLUMN certificado_renovacion_mes INT NOT NULL DEFAULT 1;
ALTER TABLE actividades ADD COLUMN requiere_certificado BOOLEAN NOT NULL DEFAULT 0;
-- rollback ALTER TABLE actividades DROP COLUMN requiere_certificado;
-- rollback ALTER TABLE companies DROP COLUMN certificado_renovacion_mes;
-- rollback ALTER TABLE companies DROP COLUMN certificado_renovacion_dia;
-- rollback ALTER TABLE companies DROP COLUMN certificado_vigencia_meses;
-- rollback ALTER TABLE companies DROP COLUMN certificado_vigencia_modo;
