--liquibase formatted sql

-- changeset bsports-club:v85-recargo-cuotas-vencidas
-- comment: Recargo opcional por club para cuotas impagas despues del vencimiento + dias de gracia. Se cobra una unica vez
-- por cuota (cuotas.recargo_aplicado_at marca que ya se aplico). Solo alcanza a cuotas con vencimiento posterior a
-- companies.recargo_habilitado_desde, para no recargar deuda anterior a la activacion.
ALTER TABLE companies ADD COLUMN recargo_habilitado BOOLEAN NOT NULL DEFAULT 0;
ALTER TABLE companies ADD COLUMN recargo_tipo VARCHAR(20) NULL;
ALTER TABLE companies ADD COLUMN recargo_valor DECIMAL(12,2) NULL;
ALTER TABLE companies ADD COLUMN recargo_dias_gracia INT NOT NULL DEFAULT 0;
ALTER TABLE companies ADD COLUMN recargo_habilitado_desde DATE NULL;
ALTER TABLE cuotas ADD COLUMN recargo DECIMAL(12,2) NOT NULL DEFAULT 0;
ALTER TABLE cuotas ADD COLUMN recargo_aplicado_at DATE NULL;
-- rollback ALTER TABLE cuotas DROP COLUMN recargo_aplicado_at;
-- rollback ALTER TABLE cuotas DROP COLUMN recargo;
-- rollback ALTER TABLE companies DROP COLUMN recargo_habilitado_desde;
-- rollback ALTER TABLE companies DROP COLUMN recargo_dias_gracia;
-- rollback ALTER TABLE companies DROP COLUMN recargo_valor;
-- rollback ALTER TABLE companies DROP COLUMN recargo_tipo;
-- rollback ALTER TABLE companies DROP COLUMN recargo_habilitado;
