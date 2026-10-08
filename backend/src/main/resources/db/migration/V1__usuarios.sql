-- Modulo usuarios: personas, cuentas de acceso y roles.
-- Las tablas funcionario, docente y docente_sede se agregan con el modulo academico (dependen de sede).

CREATE TABLE persona (
    id               BIGSERIAL    PRIMARY KEY,
    tipo_documento   VARCHAR(5)   NOT NULL,
    numero_documento VARCHAR(20)  NOT NULL,
    nombres          VARCHAR(100) NOT NULL,
    apellidos        VARCHAR(100) NOT NULL,
    telefono         VARCHAR(20),
    correo           VARCHAR(150),
    creado_en        TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uk_persona_documento UNIQUE (tipo_documento, numero_documento),
    CONSTRAINT ck_persona_tipo_documento CHECK (tipo_documento IN ('RC', 'TI', 'CC', 'CE', 'PPT'))
);

CREATE TABLE usuario (
    id                BIGSERIAL    PRIMARY KEY,
    persona_id        BIGINT       NOT NULL REFERENCES persona (id),
    nombre_usuario    VARCHAR(50)  NOT NULL,
    contrasena_hash   VARCHAR(100) NOT NULL,
    activo            BOOLEAN      NOT NULL DEFAULT TRUE,
    intentos_fallidos INT          NOT NULL DEFAULT 0,
    ultimo_acceso     TIMESTAMP,
    CONSTRAINT uk_usuario_nombre UNIQUE (nombre_usuario),
    CONSTRAINT uk_usuario_persona UNIQUE (persona_id)
);

CREATE TABLE usuario_rol (
    usuario_id BIGINT      NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    rol        VARCHAR(30) NOT NULL,
    PRIMARY KEY (usuario_id, rol),
    CONSTRAINT ck_usuario_rol CHECK (rol IN ('ADMINISTRADOR', 'RECTOR', 'COORDINADOR_ACADEMICO',
                                             'SECRETARIA', 'DOCENTE', 'ESTUDIANTE', 'ACUDIENTE'))
);
