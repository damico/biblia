#!/usr/bin/env bash
# Applies the numbered SQL scripts in db/migrations/ that have not run yet.
#
# Each script runs in its own transaction together with its record in
# schema_migration, so a failing script leaves no trace. Scripts must not
# contain BEGIN/COMMIT. An applied script must never be edited: its checksum
# is stored and a mismatch aborts the run. Put fixes in a new script.
#
# Connection uses the standard libpq variables (PGHOST, PGPORT, PGDATABASE,
# PGUSER) and ~/.pgpass for the password.
#
# Usage: db/migrate.sh [--status]

set -euo pipefail

export PGHOST="${PGHOST:-localhost}"
export PGPORT="${PGPORT:-5432}"
export PGDATABASE="${PGDATABASE:-biblia}"
export PGUSER="${PGUSER:-biblia}"
export PGOPTIONS="${PGOPTIONS:--c client_min_messages=warning}"

MIGRATIONS_DIR="$(cd "$(dirname "$0")" && pwd)/migrations"
NAME_PATTERN='^[0-9]{3}_[a-z0-9_]+\.sql$'

run_psql() {
    psql -X -q -v ON_ERROR_STOP=1 "$@"
}

run_psql -c "CREATE TABLE IF NOT EXISTS schema_migration (
    version     TEXT PRIMARY KEY,
    checksum    TEXT NOT NULL,
    applied_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);"

if [[ "${1:-}" == "--status" ]]; then
    run_psql -c "SELECT version, applied_at FROM schema_migration ORDER BY version;"
    exit 0
fi

applied=0
shopt -s nullglob
for file in "$MIGRATIONS_DIR"/*.sql; do
    name="$(basename "$file")"
    if [[ ! "$name" =~ $NAME_PATTERN ]]; then
        echo "ERROR: invalid migration name '$name' (expected NNN_name.sql)" >&2
        exit 1
    fi

    checksum="$(sha256sum "$file" | cut -d' ' -f1)"
    stored="$(run_psql -At -c "SELECT checksum FROM schema_migration WHERE version = '$name';")"

    if [[ -n "$stored" ]]; then
        if [[ "$stored" != "$checksum" ]]; then
            echo "ERROR: $name was edited after being applied. Create a new script instead." >&2
            exit 1
        fi
        continue
    fi

    echo "Applying $name"
    run_psql --single-transaction \
        -f "$file" \
        -c "INSERT INTO schema_migration (version, checksum) VALUES ('$name', '$checksum');"
    applied=$((applied + 1))
done

echo "Done. $applied migration(s) applied."
