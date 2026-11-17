--liquibase formatted sql

-- changeset bsports-club:v18-company-mercado-pago-webhook-secret
-- comment: Clave secreta de webhook Mercado Pago por club (1:1 con company_mercado_pago)
ALTER TABLE company_mercado_pago
  ADD COLUMN webhook_secret VARCHAR(512) NULL;
