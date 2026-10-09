# Plataforma escolar IEM El Encano

Plataforma web para la Institucion Educativa Municipal El Encano: usuarios y roles, estructura academica,
estudiantes, matricula en linea, asistencia, notas segun el SIEE y boletines.

## Estructura

```
backend/    API REST en Java 21 con Spring Boot 3 (Maven)
frontend/   Aplicacion web en React 18 con TypeScript (Vite, MUI)
docker-compose.yml   PostgreSQL 16 para desarrollo
```

El backend es un monolito modular. Cada modulo es un paquete dentro de `co.edu.elencano.plataforma`:
`usuarios`, `academico`, `estudiantes`, `matricula`, `asistencia`, `notas` y `comun`.
El esquema de la base de datos se versiona con Flyway en `backend/src/main/resources/db/migration`.

## Requisitos

- Java 21
- Node.js 22
- Docker (para la base de datos local y para las pruebas del backend)

## Ejecutar en desarrollo

1. Levantar la base de datos:

   ```
   docker compose up -d
   ```

2. Levantar el backend (puerto 8080). La primera vez se crea el usuario `admin` con la contrasena
   que se pase en `ADMIN_CONTRASENA`:

   ```
   cd backend
   ADMIN_CONTRASENA=una-clave-local ./mvnw spring-boot:run
   ```

   En Windows: `set ADMIN_CONTRASENA=una-clave-local` y luego `mvnw.cmd spring-boot:run`.
   Documentacion de la API: http://localhost:8080/swagger-ui.html

3. Levantar el frontend (puerto 5173):

   ```
   cd frontend
   npm install
   npm run dev
   ```

   Abrir http://localhost:5173 e ingresar con `admin` y la contrasena de `ADMIN_CONTRASENA`.
   Las llamadas a `/api` se redirigen al backend.

## Autenticacion

- `POST /api/auth/login` abre la sesion (cookie HttpOnly) y `POST /api/auth/logout` la cierra.
- `GET /api/yo` devuelve el usuario con sesion y sus roles.
- Todas las peticiones POST, PUT y DELETE llevan el token CSRF de la cookie `XSRF-TOKEN`
  en el encabezado `X-XSRF-TOKEN`; el cliente de `frontend/src/api/cliente.ts` lo hace solo.
- Tras 5 intentos fallidos la cuenta se bloquea 15 minutos. Un administrador puede desbloquearla
  o cambiar la contrasena desde la pantalla de usuarios.
- `/api/usuarios` es solo para el rol ADMINISTRADOR.

## Estructura academica

Pantalla "Estructura academica" (roles ADMINISTRADOR y COORDINADOR_ACADEMICO). Orden sugerido para configurar un anio:

1. Sedes (solo una puede ser la principal).
2. Anio lectivo con sus periodos: por defecto tres; los porcentajes deben sumar 100 y los periodos ir en orden
   dentro de las fechas del anio. El estado solo avanza (Planeacion, Matricula, En curso, Cerrado) y solo un anio
   puede estar en curso. Un anio cerrado no admite cambios.
3. Areas y asignaturas.
4. Plan de estudios de cada grado (asignaturas e intensidad horaria). Los grados vienen cargados en la migracion V3.
5. Docentes: son los usuarios activos con rol DOCENTE; aqui se registran especialidad, escalafon y sedes.
6. Grupos (director de grupo opcional) y su carga academica (docente por asignatura del plan).

Endpoints: `/api/sedes`, `/api/anios`, `/api/grados`, `/api/areas`, `/api/asignaturas`, `/api/plan-estudio`,
`/api/docentes`, `/api/grupos` y `/api/grupos/{id}/carga`. Consultar requiere sesion; modificar requiere uno de
los dos roles anteriores.

## Estudiantes y matricula

Pantalla "Estudiantes" (consulta: administrador, rector, coordinador academico y secretaria; modificar: administrador
y secretaria). Los docentes no ven datos de estudiantes por ahora porque son menores de edad; tendran una consulta
limitada a sus grupos; por ahora en asistencia solo ven nombres y apellidos de los estudiantes de sus clases.

- Ficha del estudiante: documento (se puede cambiar de TI a CC), datos personales, EPS, discapacidad, PIAR y otras condiciones.
- Acudientes: si el documento ya existe se reutiliza la persona (por ejemplo, la madre de dos hermanos). Solo un acudiente
  es el principal; el primero que se agrega queda como principal.
- Matricula: una por estudiante y anio, con grupo opcional. Respeta el cupo del grupo. Al retirar se guarda el motivo;
  si el estudiante vuelve el mismo anio se reactiva la misma matricula.

Endpoints: `/api/estudiantes`, `/api/estudiantes/{id}/acudientes`, `/api/personas/por-documento`,
`/api/estudiantes/{id}/matriculas`, `/api/matriculas`, `/api/matriculas/{id}/grupo`, `/api/matriculas/{id}/retirar`
y `/api/grupos/{id}/estudiantes`.

## Asistencia

Pantalla "Asistencia" con dos pestanas:

- Tomar asistencia (docente de la clase, administrador y coordinador academico): se elige la clase (grupo y asignatura
  de la carga academica), la fecha y las horas dictadas. Todos quedan como "Asistio" y se marcan solo las novedades:
  falta, retardo o permiso. Se puede corregir despues; no se aceptan fechas futuras, fechas fuera de los periodos
  ni anios o periodos cerrados. Un docente solo ve sus propias clases.
- Consolidado por grupo (directivos, secretaria y el director del grupo): horas sin justificar por asignatura en el anio
  y su porcentaje frente a las horas de la asignatura (intensidad semanal por semanas lectivas). En rojo quien pasa
  el maximo del SIEE (15 %). Desde aqui se justifican las faltas de un dia.

Reglas del SIEE que se usan (articulo 5): se pierde la asignatura por inasistencia injustificada mayor al 15 % de su
intensidad horaria anual, y la falta se justifica dentro de los 3 dias habiles siguientes (sin contar sabados ni
domingos; los festivos aun no se descuentan). Pasado el plazo solo el administrador o el coordinador la pueden
justificar. El permiso cuenta como inasistencia justificada y el retardo no cuenta como inasistencia.
Los valores estan en `application.yml` (`plataforma.asistencia`).

Endpoints: `/api/asistencia/cargas`, `/api/asistencia/cargas/{id}?fecha=`, `/api/asistencia/grupos`,
`/api/asistencia/grupos/{id}/resumen`, `/api/asistencia/matriculas/{id}/novedades` y
`/api/asistencia/matriculas/{id}/justificacion`.

Inicio: `/api/asistencia/pendientes` entrega lo pendiente de cada usuario: sus clases sin asistencia de hoy
(docente), las faltas sin justificar que siguen en plazo (director de grupo, secretaria, coordinador, administrador)
y los estudiantes que superan el maximo o ya pasaron dos tercios de el. El inicio no repite el menu.

## Notas

Segun el SIEE (art. 4.2 y 7), cada actividad evalua una dimension: Saber, Hacer o Ser.

- El docente crea las actividades del periodo en su clase y registra las notas en la planilla (`/notas/planilla`).
- Promedio de cada dimension: promedio simple de las notas que tiene el estudiante. Una actividad sin nota no cuenta;
  si no la presento, el docente registra la nota minima.
- Nota del periodo: promedio ponderado de las dimensiones con los pesos del anio. Si falta una dimension, la nota es
  parcial (se marca con *).
- Nota del anio: promedio ponderado de los periodos segun su porcentaje.
- Desempeno: Bajo por debajo de la nota aprobatoria, luego Basico, Alto y Superior segun los limites del anio.
- La escala y los pesos se guardan por anio (`configuracion_evaluacion`). El SIEE no fija numeros; por defecto se
  usan 1.0 a 5.0, aprueba con 3.0, Alto desde 4.0, Superior desde 4.6 y pesos 40/40/20, hasta que el consejo
  academico los confirme. Los cambia administrador o coordinacion en `/notas/escala`.
- Los grados con evaluacion cualitativa (Transicion) no tienen planilla numerica.
- Consolidado por grupo (`/notas/consolidado`): directivos, secretaria y director del grupo.

Endpoints bajo `/api/notas`: `configuracion/{anioId}`, `cargas`, `cargas/{id}/periodos/{periodoId}` (GET y PUT),
`cargas/{id}/periodos/{periodoId}/actividades`, `actividades/{id}` (PUT y DELETE) y `grupos/{id}/consolidado`.

## Estilo visual del frontend

- Colores del escudo y la bandera en `frontend/src/tema.ts` (`COLORES`): azul laguna, verde parcela y ocre del sol.
  Tipografias Source Sans 3 (texto) y Source Serif 4 (titulos), incluidas en el proyecto, sin depender de internet.
- Cada pantalla empieza con `componentes/Encabezado` (titulo, descripcion corta y accion principal).
- Botones: uno solo `contained` por pantalla o dialogo para la accion principal; `outlined` para acciones secundarias
  (Cancelar en los dialogos);
  texto simple para acciones de fila; `color="error"` solo para retirar o quitar.
- Pestañas para cambiar entre secciones de un mismo modulo (estructura academica, asistencia); tarjetas con borde
  para agrupar bloques de informacion de un mismo registro (ficha del estudiante). Sin franjas de color.
- Estados (activo, bloqueado, falta, etc.) con `componentes/Estado`: punto de color y texto, sin fondo.
- Todo texto visible lleva tildes y ene (año, contraseña, matrícula). Los nombres en el codigo siguen sin tildes.

## Pruebas

```
cd backend && ./mvnw verify      # requiere Docker en ejecucion (Testcontainers)
cd frontend && npm test
```

GitHub Actions ejecuta las mismas pruebas en cada push y pull request.

## Variables de entorno del backend

| Variable | Valor por defecto | Uso |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/plataforma` | Conexion a PostgreSQL |
| `DB_USUARIO` | `plataforma` | Usuario de la base de datos |
| `DB_CONTRASENA` | `plataforma` | Contrasena de la base de datos |
| `ADMIN_USUARIO` | `admin` | Nombre del primer administrador |
| `ADMIN_CONTRASENA` | (vacio) | Si esta vacio no se crea el primer administrador |
| `plataforma.seguridad.maximo-intentos` | `5` | Intentos fallidos antes de bloquear la cuenta |
| `plataforma.seguridad.minutos-bloqueo` | `15` | Minutos que dura el bloqueo |
| `plataforma.asistencia.porcentaje-maximo-inasistencia` | `15` | Inasistencia injustificada maxima por asignatura |
| `plataforma.asistencia.dias-habiles-justificacion` | `3` | Dias habiles para justificar una falta |
| `plataforma.asistencia.semanas-lectivas` | `40` | Semanas del anio para calcular las horas anuales |
