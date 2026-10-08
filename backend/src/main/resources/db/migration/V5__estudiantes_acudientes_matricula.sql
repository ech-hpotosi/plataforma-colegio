-- Modulo de estudiantes: ficha del estudiante, acudientes y su relacion,
-- y matricula de cada estudiante en un anio lectivo (con su grupo).

-- Datos propios del estudiante. La clave es la misma de persona (relacion 1 a 1).
CREATE TABLE estudiante (
    persona_id             BIGINT       PRIMARY KEY REFERENCES persona (id),
    codigo                 VARCHAR(20),
    fecha_nacimiento       DATE         NOT NULL,
    genero                 VARCHAR(10)  NOT NULL,
    direccion              VARCHAR(200),
    eps                    VARCHAR(100),
    grupo_sanguineo        VARCHAR(5),
    condicion_discapacidad VARCHAR(150),
    tiene_piar             BOOLEAN      NOT NULL DEFAULT FALSE,
    condiciones_especiales VARCHAR(500),
    estado                 VARCHAR(15)  NOT NULL,
    CONSTRAINT uk_estudiante_codigo UNIQUE (codigo),
    CONSTRAINT ck_estudiante_genero CHECK (genero IN ('FEMENINO', 'MASCULINO')),
    CONSTRAINT ck_estudiante_estado CHECK (estado IN ('ASPIRANTE', 'ACTIVO', 'RETIRADO', 'GRADUADO'))
);

-- Datos propios del acudiente. Una persona puede ser acudiente y tambien docente o funcionario.
CREATE TABLE acudiente (
    persona_id BIGINT       PRIMARY KEY REFERENCES persona (id),
    ocupacion  VARCHAR(100)
);

CREATE TABLE estudiante_acudiente (
    estudiante_id BIGINT      NOT NULL REFERENCES estudiante (persona_id) ON DELETE CASCADE,
    acudiente_id  BIGINT      NOT NULL REFERENCES acudiente (persona_id),
    parentesco    VARCHAR(15) NOT NULL,
    principal     BOOLEAN     NOT NULL DEFAULT FALSE,
    PRIMARY KEY (estudiante_id, acudiente_id),
    CONSTRAINT ck_parentesco CHECK (parentesco IN ('MADRE', 'PADRE', 'ABUELO', 'TIO', 'HERMANO', 'TUTOR', 'OTRO'))
);

-- Matricula de un estudiante en un anio lectivo. El grupo puede quedar pendiente.
-- La relacion con la solicitud de matricula en linea se agrega en el modulo de matricula.
CREATE TABLE matricula (
    id              BIGSERIAL    PRIMARY KEY,
    estudiante_id   BIGINT       NOT NULL REFERENCES estudiante (persona_id),
    anio_lectivo_id BIGINT       NOT NULL REFERENCES anio_lectivo (id),
    grupo_id        BIGINT       REFERENCES grupo (id),
    fecha_matricula DATE         NOT NULL,
    estado          VARCHAR(10)  NOT NULL,
    fecha_retiro    DATE,
    motivo_retiro   VARCHAR(300),
    CONSTRAINT uk_matricula UNIQUE (estudiante_id, anio_lectivo_id),
    CONSTRAINT ck_matricula_estado CHECK (estado IN ('ACTIVA', 'RETIRADA'))
);

CREATE INDEX ix_matricula_grupo ON matricula (grupo_id);
