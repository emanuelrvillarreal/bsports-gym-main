--liquibase formatted sql

-- changeset bsports-club:v65-companies-dias-vencimiento-cuota
-- comment: Dias desde la generacion hasta el vencimiento de la cuota social. Separado de dias_vencimiento_moroso,
--          que queda solo para la morosidad del grupo. Los clubes existentes reciben 10 por default.
ALTER TABLE companies ADD COLUMN dias_vencimiento_cuota INT NOT NULL DEFAULT 10 AFTER dias_entre_generaciones;
