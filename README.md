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

   Abrir http://localhost:5173. Las llamadas a `/api` se redirigen al backend.

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
