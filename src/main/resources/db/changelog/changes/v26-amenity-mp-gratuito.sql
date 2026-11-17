--liquibase formatted sql

-- changeset bsports-club:v26-mercado-pago-cobros-reserva-amenity
-- validCheckSum: ANY
-- comment: Vincula cobros MP a reservas de amenities (sena/saldo)
ALTER TABLE mercado_pago_cobros
  ADD COLUMN reserva_id BIGINT NULL,
  ADD COLUMN reserva_cobro_tipo VARCHAR(10) NULL,
  ADD KEY fk_mp_cobros_reserva (reserva_id),
  ADD CONSTRAINT fk_mp_cobros_reserva FOREIGN KEY (reserva_id) REFERENCES reservas (id);
