--liquibase formatted sql

-- changeset bsports-club:v7-modulo-actividades-completa-bit
-- comment: actividades.completa TINYINT → BIT(1) para mapeo JPA boolean
-- preconditions onFail:MARK_RAN onError:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'actividades' AND COLUMN_NAME = 'completa' AND DATA_TYPE = 'tinyint'
ALTER TABLE actividades
  MODIFY completa BIT(1) NOT NULL DEFAULT 0;
