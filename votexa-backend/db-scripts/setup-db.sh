#!/usr/bin/env bash
# Usage:
#   ./setup-db.sh            -> apply V1 schema + V2 demo seed
#   ./setup-db.sh --reset    -> wipe schema 'public' (and local ledger/keys-independent files) first
# Password: `export PGPASSWORD='...'` first, or psql will prompt.
set -euo pipefail

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-votexadb}"
DB_USER="${DB_USER:-shoeb114}"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DIR="$ROOT/src/main/resources/db/migration"
PSQL=(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1)

if [[ "${1:-}" == "--reset" ]]; then
  echo ">> Resetting schema 'public' in $DB_NAME ..."
  "${PSQL[@]}" -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
  rm -f "$ROOT/ledger/local-ledger.jsonl"   # keep local ledger in sync with the empty audit table
fi

echo ">> Applying V1__schema.sql and V2__seed_demo_data.sql ..."
"${PSQL[@]}" -f "$DIR/V1__schema.sql" -f "$DIR/V2__seed_demo_data.sql"

echo ">> Done. Row counts:"
"${PSQL[@]}" -c "
SELECT 'users' AS tbl, count(*) FROM users
UNION ALL SELECT 'voters', count(*) FROM voters
UNION ALL SELECT 'polling_stations', count(*) FROM polling_stations
UNION ALL SELECT 'voter_assignments', count(*) FROM voter_assignments
UNION ALL SELECT 'voting_authorizations', count(*) FROM voting_authorizations
ORDER BY 1;"
