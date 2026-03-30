#!/bin/sh
set -eu

if [ -n "${DB_URL:-}" ] && [ "${DB_URL#postgresql://}" != "${DB_URL}" ]; then
  export DB_URL="jdbc:${DB_URL}"
fi

exec java -jar /app/app.jar
