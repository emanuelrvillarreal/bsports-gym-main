--liquibase formatted sql

-- changeset bsports-club:v2-auth-users-profile
-- comment: Campos de perfil de persona en auth_users (B Sport Clubes)
ALTER TABLE auth_users
  ADD COLUMN nombre VARCHAR(255) NULL,
  ADD COLUMN apellido VARCHAR(255) NULL,
  ADD COLUMN dni VARCHAR(20) NULL,
  ADD COLUMN fecha_nacimiento DATE NULL,
  ADD COLUMN image_url VARCHAR(512) NULL;

-- changeset bsports-club:v2-auth-users-username-backfill
-- comment: Sincronizar username con email donde falte
UPDATE auth_users SET username = email WHERE (username IS NULL OR username = '') AND email IS NOT NULL AND email != '';
