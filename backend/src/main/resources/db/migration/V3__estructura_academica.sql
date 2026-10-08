-- Modulo academico: sedes, anios lectivos, periodos, grados, areas, asignaturas,
-- plan de estudios, docentes, grupos y carga academica.

CREATE TABLE sede (
    id          BIGSERIAL    PRIMARY KEY,
    codigo_dane VARCHAR(20),
    nombre      VARCHAR(120) NOT NULL,
    direccion   VARCHAR(200),
    principal   BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_sede_nombre UNIQUE (nombre),
    CONSTRAINT uk_sede_codigo_dane UNIQUE (codigo_dane)
);

CREATE TABLE anio_lectivo (
    id           BIGSERIAL   PRIMARY KEY,
    anio         INT         NOT NULL,
    fecha_inicio DATE        NOT NULL,
    fecha_fin    DATE        NOT NULL,
    estado       VARCHAR(15) NOT NULL,
    CONSTRAINT uk_anio_lectivo UNIQUE (anio),
    CONSTRAINT ck_anio_fechas CHECK (fecha_fin > fecha_inicio),
    CONSTRAINT ck_anio_estado CHECK (estado IN ('PLANEACION', 'MATRICULA', 'EN_CURSO', 'CERRADO'))
);

CREATE TABLE periodo (
    id              BIGSERIAL    PRIMARY KEY,
    anio_lectivo_id BIGINT       NOT NULL REFERENCES anio_lectivo (id) ON DELETE CASCADE,
    numero          INT          NOT NULL,
    fecha_inicio    DATE         NOT NULL,
    fecha_fin       DATE         NOT NULL,
    porcentaje      NUMERIC(5,2) NOT NULL,
    cerrado         BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_periodo UNIQUE (anio_lectivo_id, numero),
    CONSTRAINT ck_periodo_fechas CHECK (fecha_fin > fecha_inicio),
    CONSTRAINT ck_periodo_porcentaje CHECK (porcentaje > 0 AND porcentaje <= 100)
);

CREATE TABLE grado (
    id                     BIGSERIAL   PRIMARY KEY,
    nombre                 VARCHAR(40) NOT NULL,
    nivel                  VARCHAR(20) NOT NULL,
    orden                  INT         NOT NULL,
    evaluacion_cualitativa BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_grado_nombre UNIQUE (nombre),
    CONSTRAINT uk_grado_orden UNIQUE (orden),
    CONSTRAINT ck_grado_nivel CHECK (nivel IN ('PREESCOLAR', 'BASICA_PRIMARIA', 'BASICA_SECUNDARIA', 'MEDIA'))
);

-- Grados del sistema educativo colombiano. Transicion se evalua de forma cualitativa (SIEE art. 4.3.1).
INSERT INTO grado (nombre, nivel, orden, evaluacion_cualitativa) VALUES
    ('Transicion', 'PREESCOLAR', 0, TRUE),
    ('Primero', 'BASICA_PRIMARIA', 1, FALSE),
    ('Segundo', 'BASICA_PRIMARIA', 2, FALSE),
    ('Tercero', 'BASICA_PRIMARIA', 3, FALSE),
    ('Cuarto', 'BASICA_PRIMARIA', 4, FALSE),
    ('Quinto', 'BASICA_PRIMARIA', 5, FALSE),
    ('Sexto', 'BASICA_SECUNDARIA', 6, FALSE),
    ('Septimo', 'BASICA_SECUNDARIA', 7, FALSE),
    ('Octavo', 'BASICA_SECUNDARIA', 8, FALSE),
    ('Noveno', 'BASICA_SECUNDARIA', 9, FALSE),
    ('Decimo', 'MEDIA', 10, FALSE),
    ('Undecimo', 'MEDIA', 11, FALSE);

CREATE TABLE area (
    id     BIGSERIAL    PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    CONSTRAINT uk_area_nombre UNIQUE (nombre)
);

CREATE TABLE asignatura (
    id      BIGSERIAL    PRIMARY KEY,
    area_id BIGINT       NOT NULL REFERENCES area (id),
    nombre  VARCHAR(120) NOT NULL,
    CONSTRAINT uk_asignatura_nombre UNIQUE (nombre)
);

CREATE TABLE plan_estudio (
    id                 BIGSERIAL PRIMARY KEY,
    anio_lectivo_id    BIGINT    NOT NULL REFERENCES anio_lectivo (id),
    grado_id           BIGINT    NOT NULL REFERENCES grado (id),
    asignatura_id      BIGINT    NOT NULL REFERENCES asignatura (id),
    intensidad_horaria INT       NOT NULL,
    CONSTRAINT uk_plan_estudio UNIQUE (anio_lectivo_id, grado_id, asignatura_id),
    CONSTRAINT ck_plan_intensidad CHECK (intensidad_horaria BETWEEN 1 AND 40)
);

-- Datos propios de una persona que es docente. La clave es la misma de persona (relacion 1 a 1).
CREATE TABLE docente (
    persona_id   BIGINT       PRIMARY KEY REFERENCES persona (id),
    especialidad VARCHAR(150),
    escalafon    VARCHAR(30)
);

CREATE TABLE docente_sede (
    docente_id BIGINT NOT NULL REFERENCES docente (persona_id) ON DELETE CASCADE,
    sede_id    BIGINT NOT NULL REFERENCES sede (id),
    PRIMARY KEY (docente_id, sede_id)
);

CREATE TABLE grupo (
    id              BIGSERIAL   PRIMARY KEY,
    anio_lectivo_id BIGINT      NOT NULL REFERENCES anio_lectivo (id),
    sede_id         BIGINT      NOT NULL REFERENCES sede (id),
    grado_id        BIGINT      NOT NULL REFERENCES grado (id),
    director_id     BIGINT      REFERENCES docente (persona_id),
    nombre          VARCHAR(20) NOT NULL,
    jornada         VARCHAR(10) NOT NULL,
    cupo            INT         NOT NULL,
    CONSTRAINT uk_grupo UNIQUE (anio_lectivo_id, sede_id, grado_id, nombre),
    CONSTRAINT ck_grupo_jornada CHECK (jornada IN ('MANANA', 'TARDE', 'UNICA')),
    CONSTRAINT ck_grupo_cupo CHECK (cupo BETWEEN 1 AND 60)
);

CREATE TABLE carga_academica (
    id            BIGSERIAL PRIMARY KEY,
    grupo_id      BIGINT    NOT NULL REFERENCES grupo (id) ON DELETE CASCADE,
    asignatura_id BIGINT    NOT NULL REFERENCES asignatura (id),
    docente_id    BIGINT    NOT NULL REFERENCES docente (persona_id),
    CONSTRAINT uk_carga_academica UNIQUE (grupo_id, asignatura_id)
);

CREATE INDEX ix_grupo_anio ON grupo (anio_lectivo_id);
CREATE INDEX ix_carga_docente ON carga_academica (docente_id);

-- Las personas que ya tienen rol DOCENTE quedan registradas como docentes
INSERT INTO docente (persona_id)
SELECT u.persona_id FROM usuario u JOIN usuario_rol r ON r.usuario_id = u.id WHERE r.rol = 'DOCENTE';
