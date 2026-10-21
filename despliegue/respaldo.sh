#!/usr/bin/env bash
# Copia de seguridad de la base de datos y de los documentos de matricula.
# Uso: ./respaldo.sh [carpeta destino]   (por defecto /var/respaldos/plataforma)
# Guarda los ultimos 14 respaldos de cada tipo. Programarlo con cron (ver DESPLIEGUE.md).
set -euo pipefail

cd "$(dirname "$0")"
destino="${1:-/var/respaldos/plataforma}"
fecha="$(date +%Y-%m-%d_%H%M)"
mkdir -p "$destino"

# Base de datos en formato comprimido de pg_dump (se restaura con pg_restore)
docker compose exec -T postgres pg_dump -U plataforma -d plataforma -Fc > "$destino/base-$fecha.dump"

# Documentos subidos en la matricula en linea
docker compose exec -T backend tar -C /datos -czf - archivos > "$destino/archivos-$fecha.tar.gz"

# Conservar solo los 14 mas recientes de cada tipo
for tipo in base archivos; do
  ls -1t "$destino"/"$tipo"-* 2>/dev/null | tail -n +15 | xargs -r rm -f
done

echo "Respaldo listo en $destino ($fecha)"
