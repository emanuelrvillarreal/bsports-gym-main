-- liquibase formatted sql

-- changeset bsports-club:v14-actividades-profesores-status
-- comment: Baja lógica en asignación actividad-profesor
ALTER TABLE actividades_profesores
  ADD COLUMN status TINYINT NOT NULL DEFAULT 0 AFTER profesor_id;
