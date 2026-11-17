--liquibase formatted sql

-- changeset bsports-club:v48-members-backfill-cuota-social-rango
-- comment: Asigna el rango de cuota social a los socios existentes que aún no tienen uno,
-- según su edad actual (mismo criterio que MemberCuotaSocialRangoRecalculoService: el rango
-- con menor edad_hasta que sea >= la edad del socio).
UPDATE members m
JOIN auth_users u ON u.id = m.user_id
SET m.cuota_social_rango_id = (
  SELECT r.id
  FROM cuotas_sociales_rangos r
  WHERE r.company_id = m.company_id
    AND r.status = 0
    AND r.edad_hasta >= (YEAR(CURDATE()) - YEAR(u.fecha_nacimiento))
  ORDER BY r.edad_hasta ASC
  LIMIT 1
)
WHERE m.status = 0
  AND m.cuota_social_rango_id IS NULL
  AND u.fecha_nacimiento IS NOT NULL;
