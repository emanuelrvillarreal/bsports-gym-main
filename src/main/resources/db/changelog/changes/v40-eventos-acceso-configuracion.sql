--liquibase formatted sql

-- changeset bsports-club:v40-eventos-acceso-configuracion
-- comment: Add permiteMorosos, permiteInvitados, montoMorosos, montoInvitados to eventos_acceso
ALTER TABLE eventos_acceso ADD COLUMN permite_morosos TINYINT(1) NOT NULL DEFAULT 1;
ALTER TABLE eventos_acceso ADD COLUMN permite_invitados TINYINT(1) NOT NULL DEFAULT 1;
ALTER TABLE eventos_acceso ADD COLUMN monto_morosos DECIMAL(12,2) NULL;
ALTER TABLE eventos_acceso ADD COLUMN monto_invitados DECIMAL(12,2) NULL;

UPDATE eventos_acceso SET monto_morosos = monto, monto_invitados = monto WHERE monto_morosos IS NULL;

ALTER TABLE eventos_acceso MODIFY COLUMN monto_morosos DECIMAL(12,2) NOT NULL;
ALTER TABLE eventos_acceso MODIFY COLUMN monto_invitados DECIMAL(12,2) NOT NULL;
