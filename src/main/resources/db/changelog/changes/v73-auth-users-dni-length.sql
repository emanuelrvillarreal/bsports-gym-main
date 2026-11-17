--liquibase formatted sql

-- changeset bsports-club:v73-auth-users-dni-length
-- comment: Ampliar dni de VARCHAR(20) a VARCHAR(64) para soportar el prefijo de anonimizacion
--          'deleted-<id>-' que se agrega al rechazar/eliminar usuarios (MemberRegistroAprobacionService).
--          Se mantiene el UNIQUE KEY uk_auth_users_dni.
ALTER TABLE auth_users
  MODIFY COLUMN dni VARCHAR(64) NULL;
