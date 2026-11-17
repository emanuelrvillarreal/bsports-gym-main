--liquibase formatted sql

-- changeset bsports-club:v22-actividades-categoria
-- comment: Categoría opcional por actividad
ALTER TABLE actividades
  ADD COLUMN categoria_id BIGINT NULL,
  ADD KEY fk_actividades_categoria (categoria_id),
  ADD CONSTRAINT fk_actividades_categoria
    FOREIGN KEY (categoria_id) REFERENCES categorias (id);
