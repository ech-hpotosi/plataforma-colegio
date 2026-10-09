-- Peso relativo de cada actividad dentro de su dimension: 1 es normal, 2 vale el doble, hasta 5.
ALTER TABLE actividad_evaluativa ADD COLUMN peso INTEGER NOT NULL DEFAULT 1;
ALTER TABLE actividad_evaluativa ADD CONSTRAINT ck_actividad_peso CHECK (peso BETWEEN 1 AND 5);
