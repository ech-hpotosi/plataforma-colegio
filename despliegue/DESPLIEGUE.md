# Despliegue en el servidor del colegio

Guia para instalar la plataforma en un servidor VPS con un dominio propio. Al terminar, la plataforma queda en
`https://<dominio>` con certificado HTTPS automatico, la base de datos en el mismo servidor y copias de seguridad
diarias.

Todo corre con Docker en tres contenedores:

| Contenedor | Que hace |
|---|---|
| `postgres` | Base de datos PostgreSQL 16. No se expone a internet. |
| `backend` | Aplicacion Spring Boot (Java 21). Crea y actualiza las tablas sola al arrancar (Flyway). Guarda los documentos de matricula en el volumen `archivos`. No se expone a internet. |
| `web` | Caddy: sirve la aplicacion web, envia `/api` al backend y obtiene y renueva el certificado HTTPS de Let's Encrypt. Es el unico que abre puertos (80 y 443). |

## 1. Requisitos

- Servidor Linux (Ubuntu 22.04 o 24.04 recomendado) con al menos 2 GB de RAM, 2 nucleos y 20 GB de disco.
  Para unos 1000 estudiantes y 50 docentes es suficiente.
- Un dominio o subdominio con un registro DNS tipo A que apunte a la IP publica del servidor
  (por ejemplo `plataforma.iemelencano.edu.co`). Debe estar apuntando antes del paso 4, porque Caddy lo
  necesita para pedir el certificado.
- Puertos 80 y 443 abiertos en el firewall del proveedor del VPS.
- Acceso por SSH con un usuario que pueda usar `sudo`.

## 2. Instalar Docker

```
sudo apt update
sudo apt install -y ca-certificates curl git
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER
```

Cerrar la sesion SSH y volver a entrar para que el grupo `docker` tome efecto. Comprobar con
`docker compose version`.

## 3. Descargar el codigo

```
sudo mkdir -p /opt/plataforma && sudo chown $USER /opt/plataforma
git clone https://github.com/ech-hpotosi/plataforma-colegio.git /opt/plataforma
cd /opt/plataforma/despliegue
```

Si el repositorio es privado, clonarlo con un token de acceso o copiar la carpeta al servidor con `scp`.
Se despliega la rama `main`.

## 4. Configurar y arrancar

```
cp .env.ejemplo .env
nano .env
```

Completar:

| Variable | Valor |
|---|---|
| `DOMINIO` | El dominio del paso 1, sin `https://` |
| `DB_CONTRASENA` | Una contrasena larga y aleatoria. Se puede generar con `openssl rand -base64 24` |
| `ADMIN_USUARIO` | Usuario del primer administrador (por defecto `admin`) |
| `ADMIN_CONTRASENA` | Contrasena del primer administrador |

El archivo `.env` contiene contrasenas: no se sube al repositorio ni se comparte. Conviene protegerlo con
`chmod 600 .env`.

Arrancar:

```
docker compose up -d --build
```

La primera vez tarda varios minutos porque compila el backend y el frontend. Revisar que todo este arriba:

```
docker compose ps
docker compose logs -f backend      # Ctrl+C para salir
```

En el log del backend debe aparecer `Administrador inicial 'admin' creado` y `Started PlataformaApplication`.
Luego abrir `https://<dominio>` en el navegador.

## 5. Primer ingreso

1. Ingresar con `ADMIN_USUARIO` y `ADMIN_CONTRASENA`.
2. Con "Cambiar contraseña" (abajo en el menu lateral), poner una contrasena nueva para el administrador.
3. Borrar el valor de `ADMIN_CONTRASENA` en `.env` (dejar `ADMIN_CONTRASENA=`). El administrador inicial solo se
   crea cuando la base de datos no tiene usuarios, asi que despues de este primer arranque ya no se usa.
4. Configurar en este orden: sedes, ano lectivo y periodos, grados (evaluacion cualitativa de Transicion), areas y
   asignaturas, plan de estudios, docentes, grupos y carga academica. Despues, importar los estudiantes desde Excel
   (Estudiantes, Importar desde Excel) y crear las cuentas de los funcionarios.

## 6. Copias de seguridad

El script `respaldo.sh` guarda la base de datos (`pg_dump`) y los documentos de matricula y conserva los ultimos 14
de cada uno.

```
sudo mkdir -p /var/respaldos/plataforma && sudo chown $USER /var/respaldos/plataforma
./respaldo.sh                       # probarlo una vez
crontab -e
```

Agregar esta linea para que corra todos los dias a las 2:00 a.m.:

```
0 2 * * * /opt/plataforma/despliegue/respaldo.sh >> /var/respaldos/plataforma/respaldo.log 2>&1
```

Los respaldos quedan en el mismo servidor. Para no perderlos si el servidor falla, copiar la carpeta
`/var/respaldos/plataforma` a otro lugar con regularidad (por ejemplo con `rsync` o `rclone` a un Drive del
colegio).

### Restaurar

```
cd /opt/plataforma/despliegue
docker compose stop backend
# Base de datos
docker compose exec -T postgres pg_restore -U plataforma -d plataforma --clean --if-exists < /var/respaldos/plataforma/base-AAAA-MM-DD_HHMM.dump
# Documentos
docker compose run --rm --no-deps -T --entrypoint tar backend -C /datos -xzf - < /var/respaldos/plataforma/archivos-AAAA-MM-DD_HHMM.tar.gz
docker compose start backend
```

## 7. Actualizar a una nueva version

```
cd /opt/plataforma
./despliegue/respaldo.sh
git pull
cd despliegue
docker compose up -d --build
```

Las migraciones de la base de datos se aplican solas al arrancar el backend.

## 8. Operacion diaria

| Tarea | Comando (en `/opt/plataforma/despliegue`) |
|---|---|
| Ver el estado | `docker compose ps` |
| Ver los logs | `docker compose logs --tail 200 backend` |
| Reiniciar | `docker compose restart` |
| Detener todo | `docker compose down` (los datos quedan en los volumenes) |
| Espacio en disco | `df -h` y `docker system df` |
| Limpiar imagenes viejas despues de actualizar | `docker image prune -f` |

Los contenedores se reinician solos si el servidor se reinicia (`restart: unless-stopped`).

Nunca ejecutar `docker compose down -v`: la opcion `-v` borra los volumenes, es decir la base de datos y los
documentos.

## 9. Seguridad

- Solo los puertos 80 y 443 quedan abiertos; la base de datos y el backend no se ven desde internet.
- El backend corre con el perfil `produccion`
  (`backend/src/main/resources/application-produccion.yml`): cookies de sesion seguras, la IP real del usuario
  para el limite de registros y la documentacion de la API (Swagger) desactivada.
- Las contrasenas se guardan cifradas con BCrypt. Tras 5 intentos fallidos la cuenta se bloquea 15 minutos.
- Mantener el sistema al dia con `sudo apt update && sudo apt upgrade` y activar el firewall:
  `sudo ufw allow OpenSSH && sudo ufw allow 80 && sudo ufw allow 443 && sudo ufw enable`.
- La consulta de notas de un acudiente se activa cuando secretaria aprueba su matricula en linea o desde la ficha
  del estudiante. Secretaria solo debe dar el acceso despues de verificar el documento de la persona.

## 10. Problemas frecuentes

| Sintoma | Causa y solucion |
|---|---|
| El navegador dice que el certificado no es valido | El DNS todavia no apunta al servidor o el puerto 80 esta cerrado. Revisar `docker compose logs web`, corregir y ejecutar `docker compose restart web`. |
| No se puede ingresar con el administrador | `ADMIN_CONTRASENA` estaba vacia en el primer arranque. Completarla en `.env` y, solo si la base de datos esta recien creada y vacia, ejecutar `docker compose down -v && docker compose up -d` para empezar de cero. |
| El backend se reinicia una y otra vez | Ver `docker compose logs backend`. Suele ser una `DB_CONTRASENA` distinta a la que se uso la primera vez: la base de datos guarda la contrasena del primer arranque. |
| Las horas de asistencia salen con otra fecha | Los contenedores usan la zona horaria `America/Bogota`; revisar que no se haya cambiado en `docker-compose.yml`. |
