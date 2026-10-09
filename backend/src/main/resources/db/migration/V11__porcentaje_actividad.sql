-- El peso de 1 a 5 se reemplaza por un porcentaje dentro de la dimension, que es como lo entienden los docentes.
-- Sin porcentaje, la actividad se reparte en partes iguales lo que falte para llegar a 100 %.
ALTER TABLE actividad_evaluativa ADD COLUMN porcentaje INTEGER;
ALTER TABLE actividad_evaluativa ADD CONSTRAINT ck_actividad_porcentaje CHECK (porcentaje BETWEEN 1 AND 100);

-- Las dimensiones donde algun peso era mayor que 1 pasan a porcentajes equivalentes (redondeados hacia abajo).
UPDATE actividad_evaluativa a
SET porcentaje = GREATEST(1, FLOOR(a.peso * 100.0 / t.total))
FROM (SELECT carga_academica_id, periodo_id, dimension, SUM(peso) AS total, MAX(peso) AS mayor
      FROM actividad_evaluativa
      GROUP BY carga_academica_id, periodo_id, dimension) t
WHERE t.carga_academica_id = a.carga_academica_id
  AND t.periodo_id = a.periodo_id
  AND t.dimension = a.dimension
  AND t.mayor > 1;

ALTER TABLE actividad_evaluativa DROP COLUMN peso;
