--liquibase formatted sql

-- Migracion de nomenclatura: quita la enne de columnas, indices y valores de enum del modulo amenities.
-- En bases nuevas v9 ya crea las columnas sin enne, por eso cada changeset lleva una precondicion
-- que lo marca como ejecutado (MARK_RAN) cuando no hay nada que renombrar.

-- changeset bsports-club:v34-amenities-porcentaje-sena
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'amenities' AND BINARY column_name = 'porcentaje_seña'
-- comment: Renombra amenities.porcentaje_seña a porcentaje_sena
ALTER TABLE amenities RENAME COLUMN `porcentaje_seña` TO `porcentaje_sena`;

-- changeset bsports-club:v34-reservas-monto-sena
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'reservas' AND BINARY column_name = 'monto_seña'
-- comment: Renombra reservas.monto_seña a monto_sena
ALTER TABLE reservas RENAME COLUMN `monto_seña` TO `monto_sena`;

-- changeset bsports-club:v34-reservas-sena-pagada-at
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'reservas' AND BINARY column_name = 'seña_pagada_at'
-- comment: Renombra reservas.seña_pagada_at a sena_pagada_at
ALTER TABLE reservas RENAME COLUMN `seña_pagada_at` TO `sena_pagada_at`;

-- changeset bsports-club:v34-reservas-vence-sena-at
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'reservas' AND BINARY column_name = 'vence_seña_at'
-- comment: Renombra reservas.vence_seña_at a vence_sena_at
ALTER TABLE reservas RENAME COLUMN `vence_seña_at` TO `vence_sena_at`;

-- changeset bsports-club:v34-reservas-indice-sena
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'reservas' AND BINARY index_name = 'idx_reservas_vence_seña'
-- comment: Renombra el indice idx_reservas_vence_seña a idx_reservas_vence_sena
ALTER TABLE reservas RENAME INDEX `idx_reservas_vence_seña` TO `idx_reservas_vence_sena`;

-- changeset bsports-club:v34-datos-enum-sena
-- comment: Migra los valores de enum persistidos como texto que contenian enne (PENDIENTE_SENA y SENA)
UPDATE reservas SET estado = 'PENDIENTE_SENA' WHERE estado = 'PENDIENTE_SEÑA';
UPDATE mercado_pago_cobros SET reserva_cobro_tipo = 'SENA' WHERE reserva_cobro_tipo = 'SEÑA';
