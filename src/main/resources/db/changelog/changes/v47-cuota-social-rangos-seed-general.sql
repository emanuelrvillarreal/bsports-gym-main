--liquibase formatted sql

-- changeset bsports-club:v47-cuota-social-rangos-seed-general
-- comment: Rango catch-all "General" (hasta 100 años, importe 0) para companies sin rangos
INSERT INTO cuotas_sociales_rangos
  (uuid, status, nombre, descripcion, edad_hasta, importe, company_id, date_created, date_updated)
SELECT UUID(), 0, 'General', NULL, 100, 0.00, c.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM companies c
WHERE c.status = 0
  AND NOT EXISTS (
    SELECT 1 FROM cuotas_sociales_rangos r
    WHERE r.company_id = c.id AND r.status = 0
  );
