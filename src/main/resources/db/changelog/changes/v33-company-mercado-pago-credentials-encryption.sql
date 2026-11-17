--liquibase formatted sql

-- changeset bsports-club:v33-company-mercado-pago-credentials-encryption
-- comment: Espacio para credenciales cifradas de Mercado Pago
ALTER TABLE company_mercado_pago
  MODIFY COLUMN access_token VARCHAR(1024) NULL,
  MODIFY COLUMN webhook_secret VARCHAR(1024) NULL;
