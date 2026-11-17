--liquibase formatted sql

-- changeset bsports-club:v76-actividades-dia-vencimiento-fijo
-- comment: Dia del mes (1 a 28) en que vencen las cuotas de la actividad. NULL mantiene el modo actual:
-- la cuota vence dias_vencimiento_cuota dias despues de generada.
ALTER TABLE actividades
  ADD COLUMN dia_vencimiento_fijo INT NULL;
