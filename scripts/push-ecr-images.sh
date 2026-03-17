#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
FORCE_PUSH=false

TERRAFORM_DIR="${TERRAFORM_DIR:-${REPO_ROOT}/infra/terraform}"
API_LOCAL_IMAGE="${API_LOCAL_IMAGE:-freezer-tracking-api}"
WEB_LOCAL_IMAGE="${WEB_LOCAL_IMAGE:-freezer-tracking-web}"

usage() {
  cat <<'EOF'
Usage: scripts/push-ecr-images.sh [--force]

Options:
  --force  Push API and web images even when versions are unchanged from HEAD~1.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --force)
      FORCE_PUSH=true
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

read_version() {
  local file="$1"
  local pattern="$2"
  sed -nE "${pattern}" "${file}" | head -n1
}

read_previous_version() {
  local path="$1"
  local pattern="$2"
  git -C "${REPO_ROOT}" show "HEAD~1:${path}" 2>/dev/null | sed -nE "${pattern}" | head -n1
}

api_version="$(read_version "${REPO_ROOT}/apps/api/build.gradle" "s/^version = '([0-9]+\\.[0-9]+\\.[0-9]+)'.*/\\1/p")"
web_version="$(read_version "${REPO_ROOT}/apps/web/package.json" "s/^[[:space:]]*\\\"version\\\": \\\"([0-9]+\\.[0-9]+\\.[0-9]+)\\\",.*/\\1/p")"
previous_api_version="$(read_previous_version "apps/api/build.gradle" "s/^version = '([0-9]+\\.[0-9]+\\.[0-9]+)'.*/\\1/p")"
previous_web_version="$(read_previous_version "apps/web/package.json" "s/^[[:space:]]*\\\"version\\\": \\\"([0-9]+\\.[0-9]+\\.[0-9]+)\\\",.*/\\1/p")"

if [[ -z "${api_version}" || -z "${web_version}" ]]; then
  echo "Failed to determine API/web semantic versions from source files." >&2
  exit 1
fi

API_LOCAL_TAG="${API_LOCAL_TAG:-${api_version}}"
WEB_LOCAL_TAG="${WEB_LOCAL_TAG:-${web_version}}"
API_IMAGE_TAG="${API_IMAGE_TAG:-${IMAGE_TAG:-${api_version}}}"
WEB_IMAGE_TAG="${WEB_IMAGE_TAG:-${IMAGE_TAG:-${web_version}}}"
should_push_api=true
should_push_web=true

if [[ "${FORCE_PUSH}" == "false" ]]; then
  if [[ -n "${previous_api_version}" && "${previous_api_version}" == "${api_version}" ]]; then
    should_push_api=false
  fi

  if [[ -n "${previous_web_version}" && "${previous_web_version}" == "${web_version}" ]]; then
    should_push_web=false
  fi

  if [[ "${should_push_api}" == "false" && "${should_push_web}" == "false" ]]; then
    echo "No ECR pushes required. API/web versions are unchanged from HEAD~1."
    exit 0
  fi
fi

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

require_cmd terraform
require_cmd aws
require_cmd podman

if [[ ! -d "${TERRAFORM_DIR}" ]]; then
  echo "Terraform directory not found: ${TERRAFORM_DIR}" >&2
  exit 1
fi

api_repo_url="$(terraform -chdir="${TERRAFORM_DIR}" output -raw api_ecr_repository_url)"
web_repo_url="$(terraform -chdir="${TERRAFORM_DIR}" output -raw web_ecr_repository_url)"

if [[ -z "${api_repo_url}" || -z "${web_repo_url}" ]]; then
  echo "Failed to read ECR repository URLs from Terraform outputs." >&2
  exit 1
fi

login_registry() {
  local repo_url="$1"
  local registry
  local region

  registry="${repo_url%%/*}"
  region="$(awk -F. '{print $4}' <<<"${registry}")"

  if [[ -z "${region}" ]]; then
    echo "Unable to determine AWS region from ECR registry: ${registry}" >&2
    exit 1
  fi

  echo "Logging in to ${registry} (${region})"
  aws ecr get-login-password --region "${region}" | podman login --username AWS --password-stdin "${registry}"
}

if [[ "${should_push_api}" == "true" ]]; then
  login_registry "${api_repo_url}"
fi

if [[ "${should_push_web}" == "true" && ("${should_push_api}" == "false" || "${web_repo_url%%/*}" != "${api_repo_url%%/*}") ]]; then
  login_registry "${web_repo_url}"
fi

api_target="${api_repo_url}:${API_IMAGE_TAG}"
web_target="${web_repo_url}:${WEB_IMAGE_TAG}"

if [[ "${FORCE_PUSH}" == "true" ]]; then
  echo "Force pushing API and web images."
fi

if [[ "${should_push_api}" == "true" ]]; then
  echo "Tagging ${API_LOCAL_IMAGE}:${API_LOCAL_TAG} -> ${api_target}"
  podman tag "${API_LOCAL_IMAGE}:${API_LOCAL_TAG}" "${api_target}"
fi

if [[ "${should_push_web}" == "true" ]]; then
  echo "Tagging ${WEB_LOCAL_IMAGE}:${WEB_LOCAL_TAG} -> ${web_target}"
  podman tag "${WEB_LOCAL_IMAGE}:${WEB_LOCAL_TAG}" "${web_target}"
fi

if [[ "${should_push_api}" == "true" ]]; then
  echo "Pushing ${api_target}"
  podman push "${api_target}"
fi

if [[ "${should_push_web}" == "true" ]]; then
  echo "Pushing ${web_target}"
  podman push "${web_target}"
fi

echo "Done."
