--liquibase formatted sql

-- changeset bsports-club:v36-company-path-backfill
-- comment: path nacio nullable en v1, asi que los clubes previos lo tienen en NULL. Hay que
--          completarlos antes del NOT NULL: MySQL en modo estricto aborta el ALTER con el
--          error 1265 "Data truncated for column 'path'" si encuentra NULLs.
--          club-{id} es unico por construccion y cumple el patron ^[a-zA-Z0-9_-]+$ que valida
--          ClubProfileFieldsDTO. Cada club puede cambiarlo despues via PUT /companies/profile.
UPDATE companies
SET path = CONCAT('club-', id)
WHERE path IS NULL OR TRIM(path) = '';

-- changeset bsports-club:v36-company-path-index-unique
-- comment: Add unique constraint on companies.path
-- Nota: MODIFY ... UNIQUE ya crea el indice unico; un CREATE INDEX adicional sobre la misma
--       columna seria redundante.
ALTER TABLE companies
  MODIFY COLUMN path VARCHAR(256) UNIQUE NOT NULL;
