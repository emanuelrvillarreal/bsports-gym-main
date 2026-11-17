--liquibase formatted sql

-- changeset bsports-club:v74-auth-users-sexo
-- comment: Agregar sexo (MASCULINO / FEMENINO / OTRO) al perfil del usuario. Nullable para usuarios existentes.
ALTER TABLE auth_users
  ADD COLUMN sexo VARCHAR(20) NULL;
