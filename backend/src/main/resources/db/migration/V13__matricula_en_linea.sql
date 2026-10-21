-- Matricula en linea: el acudiente crea su cuenta, diligencia la solicitud con los datos del estudiante,
-- sube los documentos y la secretaria la revisa. Al aprobarla se crea o actualiza la ficha del estudiante,
-- se vincula al acudiente y se registra la matricula.

-- Documentos que se piden en la matricula. Los configura secretaria o administracion.
CREATE TABLE tipo_documento_requerido (
    id          BIGSERIAL    PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL,
    descripcion VARCHAR(300),
    obligatorio BOOLEAN      NOT NULL DEFAULT TRUE,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    orden       INT          NOT NULL DEFAULT 0,
    CONSTRAINT uk_tipo_documento_requerido UNIQUE (nombre)
);

-- Lista inicial, a confirmar con la secretaria del colegio
INSERT INTO tipo_documento_requerido (nombre, descripcion, obligatorio, orden) VALUES
    ('Documento de identidad del estudiante', 'Registro civil, tarjeta de identidad o cédula, por ambas caras', TRUE, 1),
    ('Documento de identidad del acudiente', 'Cédula del padre, madre o acudiente, por ambas caras', TRUE, 2),
    ('Certificado de afiliación a salud', 'Certificado de la EPS o del SISBÉN', TRUE, 3),
    ('Certificado de estudios del año anterior', 'Solo para estudiantes que vienen de otra institución', FALSE, 4),
    ('Foto del estudiante', 'Foto reciente tipo documento, de frente y con fondo claro', FALSE, 5);

CREATE TABLE solicitud_matricula (
    id                     BIGSERIAL    PRIMARY KEY,
    anio_lectivo_id        BIGINT       NOT NULL REFERENCES anio_lectivo (id),
    sede_id                BIGINT       NOT NULL REFERENCES sede (id),
    grado_id               BIGINT       NOT NULL REFERENCES grado (id),
    solicitante_id         BIGINT       NOT NULL REFERENCES usuario (id),
    parentesco             VARCHAR(15)  NOT NULL,
    tipo                   VARCHAR(10)  NOT NULL,
    estado                 VARCHAR(20)  NOT NULL,
    -- Datos del estudiante tal como los escribe el acudiente. Pasan a la ficha solo al aprobar.
    tipo_documento         VARCHAR(5)   NOT NULL,
    numero_documento       VARCHAR(20)  NOT NULL,
    nombres                VARCHAR(100) NOT NULL,
    apellidos              VARCHAR(100) NOT NULL,
    fecha_nacimiento       DATE         NOT NULL,
    genero                 VARCHAR(10)  NOT NULL,
    direccion              VARCHAR(200),
    telefono               VARCHAR(20),
    correo                 VARCHAR(150),
    eps                    VARCHAR(100),
    grupo_sanguineo        VARCHAR(5),
    condicion_discapacidad VARCHAR(150),
    condiciones_especiales VARCHAR(500),
    institucion_anterior   VARCHAR(150),
    autoriza_datos         BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha_autorizacion     TIMESTAMP,
    fecha_creacion         TIMESTAMP    NOT NULL,
    fecha_envio            TIMESTAMP,
    observacion            VARCHAR(500),
    estudiante_id          BIGINT       REFERENCES estudiante (persona_id),
    matricula_id           BIGINT       REFERENCES matricula (id),
    CONSTRAINT ck_solicitud_tipo CHECK (tipo IN ('NUEVO', 'ANTIGUO')),
    CONSTRAINT ck_solicitud_estado CHECK (estado IN ('BORRADOR', 'ENVIADA', 'CON_OBSERVACIONES', 'APROBADA', 'RECHAZADA')),
    CONSTRAINT ck_solicitud_parentesco CHECK (parentesco IN ('MADRE', 'PADRE', 'ABUELO', 'TIO', 'HERMANO', 'TUTOR', 'OTRO')),
    CONSTRAINT ck_solicitud_tipo_documento CHECK (tipo_documento IN ('RC', 'TI', 'CC', 'CE', 'PPT')),
    CONSTRAINT ck_solicitud_genero CHECK (genero IN ('FEMENINO', 'MASCULINO'))
);

CREATE INDEX ix_solicitud_solicitante ON solicitud_matricula (solicitante_id);
CREATE INDEX ix_solicitud_anio_estado ON solicitud_matricula (anio_lectivo_id, estado);

-- Un archivo por tipo de documento en cada solicitud; si se vuelve a subir, se reemplaza.
CREATE TABLE documento_adjunto (
    id                  BIGSERIAL    PRIMARY KEY,
    solicitud_id        BIGINT       NOT NULL REFERENCES solicitud_matricula (id) ON DELETE CASCADE,
    tipo_documento_id   BIGINT       NOT NULL REFERENCES tipo_documento_requerido (id),
    nombre_original     VARCHAR(200) NOT NULL,
    ruta_almacenamiento VARCHAR(300) NOT NULL,
    tipo_mime           VARCHAR(50)  NOT NULL,
    tamano_bytes        BIGINT       NOT NULL,
    fecha_carga         TIMESTAMP    NOT NULL,
    estado              VARCHAR(10)  NOT NULL,
    observacion         VARCHAR(300),
    CONSTRAINT ck_documento_estado CHECK (estado IN ('PENDIENTE', 'APROBADO', 'RECHAZADO')),
    CONSTRAINT uk_documento_solicitud_tipo UNIQUE (solicitud_id, tipo_documento_id)
);

-- Historial de cambios de estado de cada solicitud
CREATE TABLE revision_solicitud (
    id              BIGSERIAL    PRIMARY KEY,
    solicitud_id    BIGINT       NOT NULL REFERENCES solicitud_matricula (id) ON DELETE CASCADE,
    usuario_id      BIGINT       NOT NULL REFERENCES usuario (id),
    fecha           TIMESTAMP    NOT NULL,
    estado_anterior VARCHAR(20)  NOT NULL,
    estado_nuevo    VARCHAR(20)  NOT NULL,
    comentario      VARCHAR(500)
);

CREATE INDEX ix_revision_solicitud ON revision_solicitud (solicitud_id);

-- Solo los acudientes con este permiso ven notas y asistencia del estudiante. Se activa al aprobar
-- una solicitud hecha por ese acudiente, o lo marca secretaria en la ficha del estudiante.
ALTER TABLE estudiante_acudiente ADD COLUMN acceso_consulta BOOLEAN NOT NULL DEFAULT FALSE;
