--liquibase formatted sql

-- changeset bsports-club:v69-categorias-edades-opcionales
-- comment: La edad minima y maxima de una categoria pasan a ser opcionales. Las categorias existentes conservan sus valores.
ALTER TABLE categorias
  MODIFY COLUMN edad_minima INT NULL DEFAULT NULL,
  MODIFY COLUMN edad_maxima INT NULL DEFAULT NULL;
