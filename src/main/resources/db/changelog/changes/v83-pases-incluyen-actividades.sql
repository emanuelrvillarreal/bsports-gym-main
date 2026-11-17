--liquibase formatted sql

-- changeset bsports-club:v83-pases-incluyen-actividades
-- comment: Un pase puede incluir la cuota de las actividades. Si lo incluye, mientras el socio tenga ese pase
-- vigente no se le generan cuotas de las actividades que el pase cubre (todas, si el pase no tiene actividades).
ALTER TABLE pases ADD COLUMN incluye_cuota_actividades BOOLEAN NOT NULL DEFAULT FALSE;
-- rollback ALTER TABLE pases DROP COLUMN incluye_cuota_actividades;
