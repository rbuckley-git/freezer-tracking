#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${ROOT_DIR}/.env.api"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Missing ${ENV_FILE}. Create it with DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD, PASSWORD_PEPPER." >&2
  exit 1
fi

set -a
source "${ENV_FILE}"
set +a

if [[ $# -lt 3 ]]; then
  echo "Usage: $0 <container_name> <username> <password> [house_name]" >&2
  exit 1
fi

CONTAINER_NAME="$1"
USERNAME="$2"
PASSWORD="$3"
HOUSE_NAME="${4:-Admin House}"

if [[ -z "${DATABASE_URL:-}" || -z "${DATABASE_USERNAME:-}" || -z "${DATABASE_PASSWORD:-}" ]]; then
  echo "DATABASE_URL, DATABASE_USERNAME, and DATABASE_PASSWORD must be set in ${ENV_FILE}." >&2
  exit 1
fi

if [[ -z "${PASSWORD_PEPPER:-}" ]]; then
  echo "PASSWORD_PEPPER must be set in ${ENV_FILE}." >&2
  exit 1
fi

echo "Using PASSWORD_PEPPER length: ${#PASSWORD_PEPPER}"

if [[ ${#PASSWORD} -lt 12 ]]; then
  echo "Password must be at least 12 characters." >&2
  exit 1
fi

if [[ "${DATABASE_URL}" != jdbc:postgresql://* ]]; then
  echo "DATABASE_URL must be in jdbc:postgresql://host:port/db format." >&2
  exit 1
fi

URL_NO_PREFIX="${DATABASE_URL#jdbc:postgresql://}"
HOST_PORT="${URL_NO_PREFIX%%/*}"
DB_NAME="${URL_NO_PREFIX#*/}"

DB_HOST="${HOST_PORT%%:*}"
DB_PORT="${HOST_PORT#*:}"
if [[ "${DB_HOST}" == "${DB_PORT}" ]]; then
  DB_PORT="5432"
fi

if [[ -z "${DB_HOST}" || -z "${DB_NAME}" ]]; then
  echo "Unable to parse DATABASE_URL: ${DATABASE_URL}" >&2
  exit 1
fi

if command -v uuidgen >/dev/null 2>&1; then
  HOUSE_ID="$(uuidgen | tr '[:upper:]' '[:lower:]')"
  ACCOUNT_ID="$(uuidgen | tr '[:upper:]' '[:lower:]')"
else
  HOUSE_ID="$(python3 - <<'PY'\nimport uuid\nprint(uuid.uuid4())\nPY)"
  ACCOUNT_ID="$(python3 - <<'PY'\nimport uuid\nprint(uuid.uuid4())\nPY)"
fi

SALT="$(openssl rand -hex 8)"
PASSWORD_HASH="$(printf "%s" "${SALT}${PASSWORD}${PASSWORD_PEPPER}" | shasum -a 256 | awk '{print $1}')"

echo "Using PASSWORD_SALT: ${SALT}"
echo "Using PASSWORD_HASH: ${PASSWORD_HASH}"

CONTAINER_DB_HOST="localhost"
CONTAINER_DB_PORT="5432"

PSQL="psql -h ${CONTAINER_DB_HOST} -p ${CONTAINER_DB_PORT} -U ${DATABASE_USERNAME} -d ${DB_NAME} -v ON_ERROR_STOP=1"

podman exec -e PGPASSWORD="${DATABASE_PASSWORD}" -i "${CONTAINER_NAME}" ${PSQL} \
  -v username="${USERNAME}" \
  -v password_hash="${PASSWORD_HASH}" \
  -v password_salt="${SALT}" \
  -v house_id="${HOUSE_ID}" \
  -v account_id="${ACCOUNT_ID}" \
  -v house_name="${HOUSE_NAME}" <<'SQL'
DELETE FROM accounts;
DELETE FROM houses;

WITH upsert_house AS (
  INSERT INTO houses (id, name)
  VALUES (:'house_id'::uuid, :'house_name')
  ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name
  RETURNING id
)
SELECT id AS resolved_house_id FROM upsert_house
UNION ALL
SELECT id AS resolved_house_id
FROM houses
WHERE name = :'house_name'
  AND NOT EXISTS (SELECT 1 FROM upsert_house)
LIMIT 1;
\gset

UPDATE accounts
SET password_hash = :'password_hash',
    password_salt = :'password_salt',
    failed_login_count = 0,
    lockout_until = NULL,
    is_admin = true,
    house_id = COALESCE(house_id, :'resolved_house_id'::uuid)
WHERE lower(username) = lower(:'username');

INSERT INTO accounts (
  id,
  username,
  password_hash,
  password_salt,
  failed_login_count,
  lockout_until,
  is_admin,
  house_id
)
SELECT
  :'account_id'::uuid,
  :'username',
  :'password_hash',
  :'password_salt',
  0,
  NULL,
  true,
  :'resolved_house_id'::uuid
WHERE NOT EXISTS (
  SELECT 1 FROM accounts WHERE lower(username) = lower(:'username')
);
SQL

echo "Admin user seeded for ${USERNAME}."
