--liquibase formatted sql

-- changeset bsports-club:v42-evento-acceso-mercado-pago-cobro-datos
-- comment: Datos de socio/invitado asociados a un cobro MP de evento de acceso, para loguear el historial al confirmarse el pago
CREATE TABLE evento_acceso_mercado_pago_cobro_datos (
  id BIGINT NOT NULL AUTO_INCREMENT,
  status TINYINT NOT NULL DEFAULT 0,
  uuid CHAR(36) NOT NULL,
  id_user_created BIGINT NULL,
  id_user_updated BIGINT NULL,
  id_user_deleted BIGINT NULL,
  date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  date_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  date_deleted TIMESTAMP NULL,
  mercado_pago_cobro_id BIGINT NOT NULL,
  member_id BIGINT NULL,
  tipo VARCHAR(20) NOT NULL,
  nombre VARCHAR(255) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_eamp_cobro_datos_uuid (uuid),
  UNIQUE KEY uk_eamp_cobro_datos_cobro (mercado_pago_cobro_id),
  KEY idx_eamp_cobro_datos_member (member_id),
  CONSTRAINT fk_eamp_cobro_datos_cobro FOREIGN KEY (mercado_pago_cobro_id) REFERENCES mercado_pago_cobros (id),
  CONSTRAINT fk_eamp_cobro_datos_member FOREIGN KEY (member_id) REFERENCES members (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
