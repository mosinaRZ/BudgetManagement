#!/bin/sh
# Periodic compressed MongoDB dumps with simple retention.
# Runs inside the `backup` service of docker-compose.yml.
set -eu

: "${MONGO_URI:?MONGO_URI is required}"
: "${MONGO_DB_NAME:?MONGO_DB_NAME is required}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"
INTERVAL="${BACKUP_INTERVAL_SECONDS:-86400}"
DIR=/backups

while true; do
  ts="$(date -u +%Y%m%dT%H%M%SZ)"
  tmp="$DIR/.cidna-$ts.part"
  final="$DIR/cidna-$ts.archive.gz"
  if mongodump --uri="$MONGO_URI" --db="$MONGO_DB_NAME" --archive="$tmp" --gzip --quiet; then
    mv "$tmp" "$final"
    echo "backup ok: $final"
    find "$DIR" -name 'cidna-*.archive.gz' -mtime "+$RETENTION_DAYS" -delete
  else
    rm -f "$tmp"
    echo "backup FAILED at $ts" >&2
  fi
  sleep "$INTERVAL"
done
