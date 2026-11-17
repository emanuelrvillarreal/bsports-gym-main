--liquibase formatted sql

-- changeset bsports-club:v75-actividades-horarios-tablas
-- comment: Horarios por actividad (dia + franja + cupo propio) y horarios elegidos por cada inscripcion.
CREATE TABLE actividades_horarios (
  id BIGINT NOT NULL AUTO_INCREMENT,
  status TINYINT NOT NULL DEFAULT 0,
  uuid CHAR(36) NOT NULL,
  id_user_created BIGINT NULL,
  id_user_updated BIGINT NULL,
  id_user_deleted BIGINT NULL,
  date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  date_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  date_deleted TIMESTAMP NULL,
  company_id BIGINT NOT NULL,
  actividad_id BIGINT NOT NULL,
  dia_semana VARCHAR(12) NOT NULL,
  hora_inicio TIME NOT NULL,
  hora_fin TIME NOT NULL,
  cupo_maximo INT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_actividades_horarios_uuid (uuid),
  KEY idx_actividades_horarios_actividad (actividad_id, status),
  KEY fk_actividades_horarios_company (company_id),
  CONSTRAINT fk_actividades_horarios_company FOREIGN KEY (company_id) REFERENCES companies (id),
  CONSTRAINT fk_actividades_horarios_actividad FOREIGN KEY (actividad_id) REFERENCES actividades (id)
);

CREATE TABLE inscripciones_actividad_horarios (
  inscripcion_id BIGINT NOT NULL,
  horario_id BIGINT NOT NULL,
  PRIMARY KEY (inscripcion_id, horario_id),
  KEY idx_inscripciones_horarios_horario (horario_id),
  CONSTRAINT fk_inscripciones_horarios_inscripcion FOREIGN KEY (inscripcion_id) REFERENCES inscripciones_actividad (id),
  CONSTRAINT fk_inscripciones_horarios_horario FOREIGN KEY (horario_id) REFERENCES actividades_horarios (id)
);

ALTER TABLE slots
  ADD COLUMN horario_id BIGINT NULL,
  ADD KEY fk_slots_horario (horario_id),
  ADD CONSTRAINT fk_slots_horario FOREIGN KEY (horario_id) REFERENCES actividades_horarios (id);

-- changeset bsports-club:v75-actividades-horarios-migrar-datos
-- comment: Cada actividad existente pasa a tener un horario por cada dia configurado, con su franja y cupo actuales.
-- Las inscripciones existentes quedan anotadas en todos esos horarios y las clases ya generadas se vinculan
-- a su horario, de modo que nada cambia para los socios ya inscriptos.
INSERT INTO actividades_horarios (status, uuid, company_id, actividad_id, dia_semana, hora_inicio, hora_fin, cupo_maximo)
SELECT a.status, UUID(), a.company_id, a.id, d.dia, a.hora_inicio, a.hora_fin, a.cupo_maximo
FROM actividades a
JOIN (
  SELECT 'LUNES' AS dia, 1 AS orden UNION ALL SELECT 'MARTES', 2 UNION ALL SELECT 'MIERCOLES', 3
  UNION ALL SELECT 'JUEVES', 4 UNION ALL SELECT 'VIERNES', 5 UNION ALL SELECT 'SABADO', 6
  UNION ALL SELECT 'DOMINGO', 7
) d ON FIND_IN_SET(d.dia, REPLACE(UPPER(a.dias_semana), ';', ',')) > 0
ORDER BY a.id, d.orden;

INSERT INTO inscripciones_actividad_horarios (inscripcion_id, horario_id)
SELECT i.id, h.id
FROM inscripciones_actividad i
JOIN actividades_horarios h ON h.actividad_id = i.actividad_id;

-- DAYOFWEEK: 1 = domingo ... 7 = sabado
UPDATE slots s
JOIN actividades_horarios h
  ON h.actividad_id = s.actividad_id
 AND h.hora_inicio = TIME(s.inicio)
 AND h.dia_semana = ELT(DAYOFWEEK(s.inicio), 'DOMINGO', 'LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO')
SET s.horario_id = h.id
WHERE s.actividad_id IS NOT NULL;
