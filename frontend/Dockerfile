# Compila el frontend y lo sirve con Caddy, que tambien da el HTTPS y envia /api al backend.
# Se construye desde despliegue/docker-compose.yml; el Caddyfile se monta desde despliegue/.
FROM node:22-alpine AS construccion
WORKDIR /fuente
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM caddy:2-alpine
COPY --from=construccion /fuente/dist /srv
