--liquibase formatted sql

-- changeset bsports-club:v68-actividades-categorias
-- comment: Una actividad pasa a tener muchas categorias (muchos a muchos). Se migran las asignaciones actuales
--          de actividades.categoria_id a la tabla intermedia y se elimina la columna.
CREATE TABLE actividades_categorias (
  actividad_id BIGINT NOT NULL,
  categoria_id BIGINT NOT NULL,
  PRIMARY KEY (actividad_id, categoria_id),
  KEY fk_actividades_categorias_categoria (categoria_id),
  CONSTRAINT fk_actividades_categorias_actividad FOREIGN KEY (actividad_id) REFERENCES actividades (id),
  CONSTRAINT fk_actividades_categorias_categoria FOREIGN KEY (categoria_id) REFERENCES categorias (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO actividades_categorias (actividad_id, categoria_id)
SELECT id, categoria_id
FROM actividades
WHERE categoria_id IS NOT NULL;

ALTER TABLE actividades DROP FOREIGN KEY fk_actividades_categoria;
ALTER TABLE actividades DROP INDEX fk_actividades_categoria;
ALTER TABLE actividades DROP COLUMN categoria_id;
