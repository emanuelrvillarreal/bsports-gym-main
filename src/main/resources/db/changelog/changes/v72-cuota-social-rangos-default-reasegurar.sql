--liquibase formatted sql

-- changeset bsports-club:v72-cuota-social-rangos-default-reasegurar
-- comment: Inserta el rango catch-all "General" (es_default = 1) en las companies activas que no tienen uno activo
INSERT INTO cuotas_sociales_rangos
  (uuid, status, nombre, descripcion, edad_hasta, importe, es_default, company_id, date_created, date_updated)
SELECT UUID(), 0, 'General', NULL, 100, 0.00, 1, c.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM companies c
WHERE c.status = 0
  AND NOT EXISTS (
    SELECT 1 FROM cuotas_sociales_rangos r
    WHERE r.company_id = c.id AND r.status = 0 AND r.es_default = 1
  );
