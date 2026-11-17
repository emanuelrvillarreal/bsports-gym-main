--liquibase formatted sql

-- changeset bsports-club:v10-reserva-cancelacion-auditoria
-- comment: Auditoría cancelación manual de reserva amenity (patrón Movimiento.anuladaPor)
ALTER TABLE reservas
  ADD COLUMN cancelada_por BIGINT NULL,
  ADD COLUMN cancelada_at TIMESTAMP NULL;

ALTER TABLE reservas
  ADD KEY fk_reservas_cancelada_por (cancelada_por),
  ADD CONSTRAINT fk_reservas_cancelada_por FOREIGN KEY (cancelada_por) REFERENCES auth_users (id);
