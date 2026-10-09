-- Modulo de notas segun el SIEE (art. 4.2 y 7): cada actividad evalua una dimension (Saber, Hacer o Ser)
-- dentro de una carga academica y un periodo. La nota del periodo se calcula con los pesos de cada
-- dimension y se traduce a la escala nacional con los limites de la configuracion del anio.

CREATE TABLE configuracion_evaluacion (
    id               BIGSERIAL    PRIMARY KEY,
    anio_lectivo_id  BIGINT       NOT NULL UNIQUE REFERENCES anio_lectivo (id),
    nota_minima      NUMERIC(2,1) NOT NULL,
    nota_maxima      NUMERIC(2,1) NOT NULL,
    nota_aprobatoria NUMERIC(2,1) NOT NULL,
    limite_alto      NUMERIC(2,1) NOT NULL,
    limite_superior  NUMERIC(2,1) NOT NULL,
    peso_saber       INTEGER      NOT NULL,
    peso_hacer       INTEGER      NOT NULL,
    peso_ser         INTEGER      NOT NULL,
    CONSTRAINT ck_configuracion_escala
        CHECK (nota_minima < nota_aprobatoria AND nota_aprobatoria < limite_alto
               AND limite_alto < limite_superior AND limite_superior <= nota_maxima),
    CONSTRAINT ck_configuracion_pesos
        CHECK (peso_saber >= 0 AND peso_hacer >= 0 AND peso_ser >= 0 AND peso_saber + peso_hacer + peso_ser = 100)
);

CREATE TABLE actividad_evaluativa (
    id                 BIGSERIAL    PRIMARY KEY,
    carga_academica_id BIGINT       NOT NULL REFERENCES carga_academica (id),
    periodo_id         BIGINT       NOT NULL REFERENCES periodo (id),
    dimension          VARCHAR(10)  NOT NULL,
    nombre             VARCHAR(120) NOT NULL,
    fecha              DATE,
    CONSTRAINT ck_actividad_dimension CHECK (dimension IN ('SABER', 'HACER', 'SER'))
);

CREATE INDEX ix_actividad_carga_periodo ON actividad_evaluativa (carga_academica_id, periodo_id);

CREATE TABLE nota_actividad (
    id           BIGSERIAL    PRIMARY KEY,
    actividad_id BIGINT       NOT NULL REFERENCES actividad_evaluativa (id) ON DELETE CASCADE,
    matricula_id BIGINT       NOT NULL REFERENCES matricula (id),
    valor        NUMERIC(2,1) NOT NULL,
    CONSTRAINT uk_nota_actividad UNIQUE (actividad_id, matricula_id)
);

CREATE INDEX ix_nota_actividad_matricula ON nota_actividad (matricula_id);
