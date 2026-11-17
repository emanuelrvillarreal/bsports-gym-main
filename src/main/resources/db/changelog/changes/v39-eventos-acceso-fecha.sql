--liquibase formatted sql

-- changeset bsports-club:v39-eventos-acceso-fecha
-- comment: Add fecha field to eventos_acceso table
ALTER TABLE eventos_acceso
ADD COLUMN fecha DATETIME NULL;
