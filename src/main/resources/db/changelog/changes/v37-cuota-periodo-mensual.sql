--liquibase formatted sql

-- changeset bsports-club:v37-cuotas-drop-uk-fecha-generacion
-- comment: La idempotencia vuelve al período (anio, mes); fecha_generacion pasa a ser solo el ancla del ciclo
ALTER TABLE cuotas DROP INDEX uk_cuotas_social_fecha_gen;
ALTER TABLE cuotas DROP INDEX uk_cuotas_actividad_fecha_gen;

ALTER TABLE cuotas
  DROP COLUMN social_fecha_gen_uid,
  DROP COLUMN actividad_fecha_gen_uid;

-- changeset bsports-club:v37-cuotas-uk-periodo
-- comment: Unique por período. Actividad usa columnas planas: en las filas SOCIAL actividad_id es NULL
--          y MySQL admite NULLs repetidos en UNIQUE, asi que la clave se auto-limita a las de actividad
--          y ademas sirve al NOT EXISTS del job. Social necesita columna generada porque una UK plana
--          con tipo restringiria tambien las de actividad y romperia al socio inscripto en dos el mismo mes.
ALTER TABLE cuotas
  ADD COLUMN social_periodo_uid VARCHAR(64) GENERATED ALWAYS AS (
    IF(tipo = 'SOCIAL', CONCAT(company_id, '-', persona_id, '-', anio, '-', mes), NULL)
  ) STORED;

ALTER TABLE cuotas
  ADD UNIQUE KEY uk_cuotas_social_periodo (social_periodo_uid),
  ADD UNIQUE KEY uk_cuotas_actividad_periodo (company_id, persona_id, actividad_id, anio, mes);

-- changeset bsports-club:v37-cuotas-drop-origen
-- comment: Ya no se distingue alta/inscripcion de ciclo mensual
ALTER TABLE cuotas DROP COLUMN origen;

-- changeset bsports-club:v37-cuotas-descuento-columna
-- comment: Descuento porcentual sobre el importe de lista
ALTER TABLE cuotas
  ADD COLUMN descuento TINYINT UNSIGNED NOT NULL DEFAULT 0;

-- changeset bsports-club:v37-cuotas-descuento-check
-- comment: El rango 0-100 tambien lo valida el factory Cuota.deActividad
ALTER TABLE cuotas
  ADD CONSTRAINT ck_cuotas_descuento CHECK (descuento BETWEEN 0 AND 100);

-- changeset bsports-club:v37-cuotas-idx-periodo
-- comment: Sirve a sumMontoPorCobrarByPeriodo, que filtra por club y período sin persona.
--          El NOT EXISTS del job no lo necesita: lo resuelve uk_cuotas_actividad_periodo.
--          No hay columna monto_final: tanto ROUND() como la división '/' fueron rechazados
--          dentro de GENERATED ALWAYS AS por MariaDB (error 1901, la determinación del generated
--          column no acepta expresiones dependientes de div_precision_increment). El neto se
--          calcula en tiempo de consulta en sumMontoPorCobrarByPeriodo, donde esa restricción
--          no aplica, y en Java en Cuota.montoFinal().
CREATE INDEX idx_cuotas_periodo ON cuotas (company_id, tipo, anio, mes);

-- changeset bsports-club:v37-cuotas-descuento-tipo
-- comment: descuento quedo TINYINT UNSIGNED en v37-cuotas-descuento-columna; Hibernate mapea
--          Integer a INTEGER (SQL INT) y la validacion de esquema (ddl-auto=validate) rechaza
--          el tipo TINYINT en el arranque. INT desperdicia unos bytes por fila frente a TINYINT,
--          pero evita forzar el campo Java a un tipo no estandar solo para ahorrarlos; el rango
--          0-100 lo sigue garantizando el CHECK y el factory Cuota.deActividad. Se dropea y
--          recrea el CHECK para no depender de si MariaDB lo preserva solo al cambiar el tipo.
ALTER TABLE cuotas DROP CONSTRAINT ck_cuotas_descuento;

ALTER TABLE cuotas MODIFY COLUMN descuento INT NOT NULL DEFAULT 0;

ALTER TABLE cuotas
  ADD CONSTRAINT ck_cuotas_descuento CHECK (descuento BETWEEN 0 AND 100);
