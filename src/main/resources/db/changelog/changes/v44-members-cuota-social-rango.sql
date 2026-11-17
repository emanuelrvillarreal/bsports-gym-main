--liquibase formatted sql

-- changeset bsports-club:v44-members-cuota-social-rango
-- comment: Rango etario de cuota social asignado al socio
ALTER TABLE members
  ADD COLUMN cuota_social_rango_id BIGINT NULL,
  ADD KEY fk_members_cuota_social_rango (cuota_social_rango_id),
  ADD CONSTRAINT fk_members_cuota_social_rango
    FOREIGN KEY (cuota_social_rango_id) REFERENCES cuotas_sociales_rangos (id);
