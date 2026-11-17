--liquibase formatted sql

-- changeset bsports-club:v38-auth-users-dni-unique-backfill-eliminados
-- comment: Antes de poner UNIQUE en dni, anonimizar los auth_users ya eliminados (status = -1)
--          para que no colisionen entre si ni con altas nuevas.
UPDATE auth_users
SET
  email = CONCAT('deleted-', id, '-', email),
  username = CONCAT('deleted-', id, '-', username),
  dni = CASE WHEN dni IS NOT NULL AND dni != '' THEN CONCAT('deleted-', id, '-', dni) ELSE dni END,
  nombre = 'Usuario',
  apellido = 'Eliminado'
WHERE status = -1
  AND email NOT LIKE 'deleted-%';

-- changeset bsports-club:v38-auth-users-dni-unique
-- comment: dni unico a nivel DB. Requiere que no existan duplicados entre usuarios ACTIVOS/PENDIENTES
--          antes de correr esto (el backfill de arriba ya libero los eliminados). NULLs multiples
--          estan permitidos por MySQL/MariaDB en columnas UNIQUE.
--          Si falla por duplicados reales, correr antes:
--          SELECT dni, COUNT(*) FROM auth_users WHERE status <> -1 AND dni IS NOT NULL AND dni != '' GROUP BY dni HAVING COUNT(*) > 1;
ALTER TABLE auth_users
  ADD UNIQUE KEY uk_auth_users_dni (dni);
