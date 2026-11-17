--liquibase formatted sql

-- changeset bsports-club:v32-mp-cobro-intent-external-ref-index
-- comment: Índice para buscar el intent por external reference de Mercado Pago
CREATE INDEX idx_mp_cobro_intent_external_reference ON mercado_pago_cobro_intent (external_reference);
