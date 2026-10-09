-- Lo que el docente aporta al boletin de cada periodo (SIEE art. 14):
-- el concepto descriptivo de cada desempeno en su clase, y por estudiante la valoracion del comportamiento
-- (el boletin muestra el promedio de todos los docentes) y una observacion.

CREATE TABLE descriptor_desempeno (
    id                 BIGSERIAL    PRIMARY KEY,
    carga_academica_id BIGINT       NOT NULL REFERENCES carga_academica (id),
    periodo_id         BIGINT       NOT NULL REFERENCES periodo (id),
    desempeno          VARCHAR(10)  NOT NULL,
    descripcion        VARCHAR(600) NOT NULL,
    CONSTRAINT ck_descriptor_desempeno CHECK (desempeno IN ('BAJO', 'BASICO', 'ALTO', 'SUPERIOR')),
    CONSTRAINT uk_descriptor UNIQUE (carga_academica_id, periodo_id, desempeno)
);

CREATE TABLE informe_estudiante (
    id                 BIGSERIAL    PRIMARY KEY,
    carga_academica_id BIGINT       NOT NULL REFERENCES carga_academica (id),
    periodo_id         BIGINT       NOT NULL REFERENCES periodo (id),
    matricula_id       BIGINT       NOT NULL REFERENCES matricula (id),
    comportamiento     NUMERIC(2,1),
    observacion        VARCHAR(500),
    registrado_por     BIGINT       NOT NULL REFERENCES usuario (id),
    fecha_registro     TIMESTAMP    NOT NULL,
    CONSTRAINT uk_informe_estudiante UNIQUE (carga_academica_id, periodo_id, matricula_id)
);

CREATE INDEX ix_informe_estudiante_matricula ON informe_estudiante (matricula_id);
