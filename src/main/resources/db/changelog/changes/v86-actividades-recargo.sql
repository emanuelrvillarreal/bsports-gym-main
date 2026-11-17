--liquibase formatted sql

-- changeset bsports-club:v86-actividades-recargo
-- comment: Recargo por pago fuera de termino propio de cada actividad (mismas columnas recargo_* que companies,
-- que pasa a regir solo la cuota social). Apagado por defecto.
ALTER TABLE actividades ADD COLUMN recargo_habilitado BOOLEAN NOT NULL DEFAULT 0;
ALTER TABLE actividades ADD COLUMN recargo_tipo VARCHAR(20) NULL;
ALTER TABLE actividades ADD COLUMN recargo_valor DECIMAL(12,2) NULL;
ALTER TABLE actividades ADD COLUMN recargo_dias_gracia INT NOT NULL DEFAULT 0;
ALTER TABLE actividades ADD COLUMN recargo_habilitado_desde DATE NULL;
-- rollback ALTER TABLE actividades DROP COLUMN recargo_habilitado_desde;
-- rollback ALTER TABLE actividades DROP COLUMN recargo_dias_gracia;
-- rollback ALTER TABLE actividades DROP COLUMN recargo_valor;
-- rollback ALTER TABLE actividades DROP COLUMN recargo_tipo;
-- rollback ALTER TABLE actividades DROP COLUMN recargo_habilitado;
