#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
FORCE_BUILD=false

usage() {
  cat <<'EOF'
Usage: scripts/build-images.sh [--force]

Options:
  --force  Rebuild API and web images even when versions are unchanged, using a no-cache build.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --force)
      FORCE_BUILD=true
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

api_version="$(read_version "${REPO_ROOT}/apps/api/build.gradle" "s/^version = '([0-9]+\.[0-9]+\.[0-9]+)'.*/\1/p")"
web_version="$(read_version "${REPO_ROOT}/apps/web/package.json" "s/^[[:space:]]*\"version\": \"([0-9]+\.[0-9]+\.[0-9]+)\",.*/\1/p")"
IMAGE_PLATFORM="${IMAGE_PLATFORM:-linux/arm64}"
previous_api_version="$(read_previous_version "apps/api/build.gradle" "s/^version = '([0-9]+\.[0-9]+\.[0-9]+)'.*/\1/p")"
previous_web_version="$(read_previous_version "apps/web/package.json" "s/^[[:space:]]*\"version\": \"([0-9]+\.[0-9]+\.[0-9]+)\",.*/\1/p")"

if [[ -z "${api_version}" || -z "${web_version}" ]]; then
  echo "Failed to determine API/web semantic versions from source files." >&2
  exit 1
fi

should_build_api=true
should_build_web=true
build_args=(--platform "${IMAGE_PLATFORM}")

if [[ "${FORCE_BUILD}" == "false" ]]; then
  if [[ -n "${previous_api_version}" && "${previous_api_version}" == "${api_version}" ]]; then
    should_build_api=false
  fi

  if [[ -n "${previous_web_version}" && "${previous_web_version}" == "${web_version}" ]]; then
    should_build_web=false
  fi

  if [[ "${should_build_api}" == "false" && "${should_build_web}" == "false" ]]; then
    echo "No image builds required. API/web versions are unchanged from HEAD~1."
    exit 0
  fi
else
  build_args+=(--no-cache)
fi

if [[ "${FORCE_BUILD}" == "true" ]]; then
  echo "Force rebuilding API and web images for ${IMAGE_PLATFORM}"
else
  echo "Building changed images for ${IMAGE_PLATFORM}"
fi

if [[ "${should_build_api}" == "true" ]]; then
  echo "Building freezer-tracking-api:${api_version}"
  podman build "${build_args[@]}" -t "freezer-tracking-api:${api_version}" -f "${REPO_ROOT}/apps/api/Dockerfile" "${REPO_ROOT}"
fi

if [[ "${should_build_web}" == "true" ]]; then
  echo "Building freezer-tracking-web:${web_version}"
  podman build "${build_args[@]}" -t "freezer-tracking-web:${web_version}" -f "${REPO_ROOT}/apps/web/Dockerfile" "${REPO_ROOT}"
fi

# Keep latest tags for local workflows that expect them.
if [[ "${should_build_api}" == "true" ]]; then
  podman tag "freezer-tracking-api:${api_version}" "freezer-tracking-api:latest"
fi

if [[ "${should_build_web}" == "true" ]]; then
  podman tag "freezer-tracking-web:${web_version}" "freezer-tracking-web:latest"
fi

echo "Built images:"
if [[ "${should_build_api}" == "true" ]]; then
  echo "  freezer-tracking-api:${api_version}"
fi
if [[ "${should_build_web}" == "true" ]]; then
  echo "  freezer-tracking-web:${web_version}"
fi
echo "Updated local :latest tags for built images only."
