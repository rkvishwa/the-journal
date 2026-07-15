#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

COMPOSE=(docker compose)

die() {
	echo "ERROR: $*" >&2
	exit 1
}

load_env() {
	local env_file="$1"
	while IFS= read -r line || [[ -n "$line" ]]; do
		line="${line#"${line%%[![:space:]]*}"}"
		line="${line%"${line##*[![:space:]]}"}"
		[[ -z "$line" || "$line" == \#* ]] && continue
		[[ "$line" == export\ * ]] && line="${line#export }"
		[[ "$line" == *"="* ]] || continue
		export "$line"
	done < "$env_file"
}

require_command() {
	command -v "$1" >/dev/null 2>&1 || die "$1 is not installed or is not in PATH."
}

require_env() {
	local name="$1"
	local value="${!name:-}"
	[[ -n "$value" ]] || die "$name must be set in .env."
	[[ "$value" != replace-with-* ]] || die "$name still has a placeholder value in .env."
}

require_command git
require_command docker

git rev-parse --is-inside-work-tree >/dev/null 2>&1 || die "Run this script inside the cloned Git repository."

if [[ ! -f .env ]]; then
	cp .env.production.example .env
	die ".env was created from .env.production.example. Fill in the production secrets, then run ./deploy.sh again."
fi

load_env .env

require_env DB_ROOT_PASSWORD
require_env DB_PASSWORD
require_env BLOG_ADMIN_PASSWORD
require_env BLOG_BASE_URL

[[ "${BLOG_BASE_URL}" == "https://seinnlae.me" ]] || die "BLOG_BASE_URL must be https://seinnlae.me for this deployment."
[[ "${DB_HOST:-db}" == "db" ]] || die "DB_HOST must be db when using the Docker Compose database."
[[ "${DB_NAME:-blog_engine}" == "blog_engine" ]] || die "DB_NAME must be blog_engine because sql/migrate-all.sql creates that database."

echo "Pulling latest code..."
git pull --ff-only

echo "Starting database..."
"${COMPOSE[@]}" up -d db

echo "Waiting for database health..."
for _ in {1..60}; do
	if "${COMPOSE[@]}" exec -T -e MYSQL_PWD="${DB_ROOT_PASSWORD}" db \
			mariadb-admin ping -h 127.0.0.1 -u root --silent >/dev/null 2>&1; then
		break
	fi
	sleep 2
done

if ! "${COMPOSE[@]}" exec -T -e MYSQL_PWD="${DB_ROOT_PASSWORD}" db \
		mariadb-admin ping -h 127.0.0.1 -u root --silent >/dev/null 2>&1; then
	"${COMPOSE[@]}" logs --tail=80 db >&2
	die "Database did not become healthy."
fi

echo "Applying database schema..."
"${COMPOSE[@]}" exec -T -e MYSQL_PWD="${DB_ROOT_PASSWORD}" db mariadb \
	-h 127.0.0.1 \
	-u root < sql/migrate-all.sql

echo "Applying incremental schema updates..."
POST_COVER_COLUMN=$("${COMPOSE[@]}" exec -T -e MYSQL_PWD="${DB_ROOT_PASSWORD}" db mariadb \
	-h 127.0.0.1 \
	-u root \
	-N -B \
	-e "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = 'blog_engine' AND TABLE_NAME = 'posts' AND COLUMN_NAME = 'cover_image_url';")

if [[ "${POST_COVER_COLUMN}" == "0" ]]; then
	echo "Adding posts.cover_image_url column..."
	"${COMPOSE[@]}" exec -T -e MYSQL_PWD="${DB_ROOT_PASSWORD}" db mariadb \
		-h 127.0.0.1 \
		-u root < sql/006_post_cover_image.sql
else
	echo "posts.cover_image_url already exists; skipping cover image migration."
fi

echo "Building application image and running tests..."
"${COMPOSE[@]}" build app

echo "Starting application and HTTPS proxy..."
"${COMPOSE[@]}" up -d app caddy

echo
echo "Deployment complete."
"${COMPOSE[@]}" ps
echo
echo "Open: https://seinnlae.me"
