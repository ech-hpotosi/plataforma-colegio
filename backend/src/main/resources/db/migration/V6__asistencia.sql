-- Modulo de asistencia: cada registro es una clase dictada (carga academica y fecha)
-- y cada detalle es el estado de un estudiante en esa clase.

CREATE TABLE registro_asistencia (
    id                 BIGSERIAL PRIMARY KEY,
    carga_academica_id BIGINT    NOT NULL REFERENCES carga_academica (id),
    periodo_id         BIGINT    NOT NULL REFERENCES periodo (id),
    fecha              DATE      NOT NULL,
    horas              INTEGER   NOT NULL,
    registrado_por     BIGINT    NOT NULL REFERENCES usuario (id),
    fecha_registro     TIMESTAMP NOT NULL,
    CONSTRAINT uk_registro_asistencia UNIQUE (carga_academica_id, fecha),
    CONSTRAINT ck_registro_horas CHECK (horas BETWEEN 1 AND 10)
);

CREATE INDEX ix_registro_asistencia_periodo ON registro_asistencia (periodo_id);

CREATE TABLE detalle_asistencia (
    id                  BIGSERIAL    PRIMARY KEY,
    registro_id         BIGINT       NOT NULL REFERENCES registro_asistencia (id) ON DELETE CASCADE,
    matricula_id        BIGINT       NOT NULL REFERENCES matricula (id),
    estado              VARCHAR(20)  NOT NULL,
    observacion         VARCHAR(300),
    justificacion       VARCHAR(500),
    fecha_justificacion TIMESTAMP,
    justificado_por     BIGINT       REFERENCES usuario (id),
    CONSTRAINT uk_detalle_asistencia UNIQUE (registro_id, matricula_id),
    CONSTRAINT ck_detalle_estado CHECK (estado IN ('ASISTIO', 'FALTA', 'FALTA_JUSTIFICADA', 'RETARDO', 'PERMISO'))
);

CREATE INDEX ix_detalle_asistencia_matricula ON detalle_asistencia (matricula_id);
