-- Recuperaciones (superacion) segun el SIEE art. 9 y 11. Con periodo_id es la recuperacion de ese periodo;
-- sin periodo es la recuperacion final del anio. La nota definitiva es la mayor entre la calculada y la
-- recuperacion, sin pasar del tope de recuperacion de la configuracion del anio.

ALTER TABLE configuracion_evaluacion ADD COLUMN tope_recuperacion NUMERIC(2,1);
UPDATE configuracion_evaluacion SET tope_recuperacion = nota_aprobatoria;
ALTER TABLE configuracion_evaluacion ALTER COLUMN tope_recuperacion SET NOT NULL;
ALTER TABLE configuracion_evaluacion ADD CONSTRAINT ck_configuracion_tope
    CHECK (tope_recuperacion >= nota_aprobatoria AND tope_recuperacion <= nota_maxima);

CREATE TABLE recuperacion (
    id                 BIGSERIAL    PRIMARY KEY,
    carga_academica_id BIGINT       NOT NULL REFERENCES carga_academica (id),
    periodo_id         BIGINT       REFERENCES periodo (id),
    matricula_id       BIGINT       NOT NULL REFERENCES matricula (id),
    nota               NUMERIC(2,1) NOT NULL,
    observacion        VARCHAR(300),
    registrado_por     BIGINT       NOT NULL REFERENCES usuario (id),
    fecha_registro     TIMESTAMP    NOT NULL
);

CREATE UNIQUE INDEX uk_recuperacion_periodo ON recuperacion (carga_academica_id, periodo_id, matricula_id)
    WHERE periodo_id IS NOT NULL;
CREATE UNIQUE INDEX uk_recuperacion_final ON recuperacion (carga_academica_id, matricula_id)
    WHERE periodo_id IS NULL;
CREATE INDEX ix_recuperacion_matricula ON recuperacion (matricula_id);
