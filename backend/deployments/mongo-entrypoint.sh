#!/bin/sh
# Generates the replica-set keyfile on first start and persists it in a volume,
# so no secret has to be committed to Git or copied onto the server by hand.
# A single-node replica set is required: the API uses multi-document transactions.
set -eu

KEYFILE=/keyfile/mongo-keyfile

if [ ! -s "$KEYFILE" ]; then
  umask 077
  head -c 512 /dev/urandom | base64 | tr -d '\n' > "$KEYFILE"
fi
chown mongodb:mongodb "$KEYFILE"
chmod 400 "$KEYFILE"

exec docker-entrypoint.sh "$@"
