--liquibase formatted sql

-- changeset bsports-club:v25-movimientos-evento-acceso
-- comment: Vincula movimientos a eventos de acceso (cobro invitado)
ALTER TABLE movimientos
  ADD COLUMN evento_acceso_id BIGINT NULL,
  ADD KEY fk_movimientos_evento_acceso (evento_acceso_id),
  ADD CONSTRAINT fk_movimientos_evento_acceso FOREIGN KEY (evento_acceso_id) REFERENCES eventos_acceso (id);

-- changeset bsports-club:v25-mercado-pago-cobros-evento-acceso
-- comment: Vincula cobros MP a eventos de acceso
ALTER TABLE mercado_pago_cobros
  ADD COLUMN evento_acceso_id BIGINT NULL,
  ADD KEY fk_mp_cobros_evento_acceso (evento_acceso_id),
  ADD CONSTRAINT fk_mp_cobros_evento_acceso FOREIGN KEY (evento_acceso_id) REFERENCES eventos_acceso (id);
