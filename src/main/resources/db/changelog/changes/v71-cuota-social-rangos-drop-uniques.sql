--liquibase formatted sql

-- changeset bsports-club:v71-cuota-social-rangos-drop-uniques
-- comment: Quita las unique por nombre y edad_hasta (contaban los dados de baja); la unicidad entre activos la valida la aplicación
ALTER TABLE cuotas_sociales_rangos
  DROP INDEX uk_cuotas_sociales_rangos_company_nombre,
  DROP INDEX uk_cuotas_sociales_rangos_company_edad_hasta;
