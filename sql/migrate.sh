#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="$PROJECT_ROOT/.env"

load_env() {
	if [[ ! -f "$ENV_FILE" ]]; then
		return
	fi
	while IFS= read -r line || [[ -n "$line" ]]; do
		line="${line#"${line%%[![:space:]]*}"}"
		line="${line%"${line##*[![:space:]]}"}"
		[[ -z "$line" || "$line" == \#* ]] && continue
		export "$line"
	done < "$ENV_FILE"
}

load_env

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_USERNAME="${DB_USERNAME:-root}"
DB_PASSWORD="${DB_PASSWORD:-}"

SQL_FILE="${1:-$SCRIPT_DIR/migrate-all.sql}"

if [[ ! -f "$SQL_FILE" ]]; then
	echo "SQL file not found: $SQL_FILE" >&2
	exit 1
fi

MYSQL_ARGS=(-h "$DB_HOST" -P "$DB_PORT" -u "$DB_USERNAME")

if [[ -n "$DB_PASSWORD" ]]; then
	export MYSQL_PWD="$DB_PASSWORD"
fi

echo "Running $(basename "$SQL_FILE") against $DB_HOST:$DB_PORT as $DB_USERNAME..."
mysql "${MYSQL_ARGS[@]}" < "$SQL_FILE"
echo "Done."
