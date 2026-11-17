--liquibase formatted sql

-- changeset bsports-club:v9-modulo-amenities-amenities
-- validCheckSum: ANY
-- comment: Amenities configurables por club (parrillas, etc.)
CREATE TABLE amenities (
  id BIGINT NOT NULL AUTO_INCREMENT,
  status TINYINT NOT NULL DEFAULT 0,
  uuid CHAR(36) NOT NULL,
  id_user_created BIGINT NULL,
  id_user_updated BIGINT NULL,
  id_user_deleted BIGINT NULL,
  date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  date_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  date_deleted TIMESTAMP NULL,
  company_id BIGINT NOT NULL,
  nombre VARCHAR(255) NOT NULL,
  monto_hora DECIMAL(12, 2) NOT NULL,
  porcentaje_sena DECIMAL(5, 2) NOT NULL,
  dias_semana VARCHAR(128) NOT NULL,
  hora_inicio TIME NOT NULL,
  hora_fin TIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_amenities_uuid (uuid),
  KEY fk_amenities_company (company_id),
  CONSTRAINT fk_amenities_company FOREIGN KEY (company_id) REFERENCES companies (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- changeset bsports-club:v9-modulo-amenities-reservas
-- validCheckSum: ANY
-- comment: Reservas de amenities (1:1 con slot)
CREATE TABLE reservas (
  id BIGINT NOT NULL AUTO_INCREMENT,
  status TINYINT NOT NULL DEFAULT 0,
  uuid CHAR(36) NOT NULL,
  id_user_created BIGINT NULL,
  id_user_updated BIGINT NULL,
  id_user_deleted BIGINT NULL,
  date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  date_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  date_deleted TIMESTAMP NULL,
  company_id BIGINT NOT NULL,
  amenity_id BIGINT NOT NULL,
  slot_id BIGINT NOT NULL,
  member_id BIGINT NOT NULL,
  monto_total DECIMAL(12, 2) NOT NULL,
  monto_sena DECIMAL(12, 2) NOT NULL,
  saldo_pendiente DECIMAL(12, 2) NOT NULL,
  sena_pagada_at TIMESTAMP NULL,
  vence_sena_at TIMESTAMP NOT NULL,
  estado VARCHAR(20) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_reservas_uuid (uuid),
  UNIQUE KEY uk_reservas_slot (slot_id),
  KEY fk_reservas_company (company_id),
  KEY fk_reservas_amenity (amenity_id),
  KEY fk_reservas_member (member_id),
  KEY idx_reservas_vence_sena (company_id, estado, vence_sena_at),
  KEY idx_reservas_member_estado (company_id, member_id, estado),
  CONSTRAINT fk_reservas_company FOREIGN KEY (company_id) REFERENCES companies (id),
  CONSTRAINT fk_reservas_amenity FOREIGN KEY (amenity_id) REFERENCES amenities (id),
  CONSTRAINT fk_reservas_slot FOREIGN KEY (slot_id) REFERENCES slots (id),
  CONSTRAINT fk_reservas_member FOREIGN KEY (member_id) REFERENCES members (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- changeset bsports-club:v9-modulo-amenities-slots-alter
-- comment: Slots compartidos actividad XOR amenity; actividad_id nullable
ALTER TABLE slots
  MODIFY COLUMN actividad_id BIGINT NULL;

ALTER TABLE slots
  DROP INDEX uk_slots_actividad_inicio;

ALTER TABLE slots
  ADD UNIQUE KEY uk_slots_actividad_inicio (actividad_id, inicio);

ALTER TABLE slots
  ADD CONSTRAINT fk_slots_amenity FOREIGN KEY (amenity_id) REFERENCES amenities (id);

ALTER TABLE slots
  ADD UNIQUE KEY uk_slots_amenity_inicio (amenity_id, inicio);

-- changeset bsports-club:v9-modulo-amenities-movimientos-alter
-- comment: Movimientos vinculados a reservas de amenities
ALTER TABLE movimientos
  ADD COLUMN reserva_id BIGINT NULL,
  ADD KEY fk_movimientos_reserva (reserva_id),
  ADD CONSTRAINT fk_movimientos_reserva FOREIGN KEY (reserva_id) REFERENCES reservas (id);
