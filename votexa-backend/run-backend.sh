#!/usr/bin/env bash
# Loads ./.env (if present) and starts the Spring Boot backend.
set -euo pipefail
cd "$(dirname "$0")"
if [[ -f .env ]]; then set -a; source .env; set +a; fi
if [[ -z "${DB_PASSWORD:-}" ]]; then read -rsp "PostgreSQL password for ${DB_USER:-shoeb114}: " DB_PASSWORD; echo; export DB_PASSWORD; fi
exec mvn spring-boot:run "$@"
