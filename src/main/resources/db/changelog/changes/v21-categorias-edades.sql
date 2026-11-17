--liquibase formatted sql

-- changeset bsports-club:v21-categorias-edades
-- comment: Rango de edades reglamentario por categoría
ALTER TABLE categorias
  ADD COLUMN edad_minima INT NOT NULL DEFAULT 0,
  ADD COLUMN edad_maxima INT NOT NULL DEFAULT 99;
