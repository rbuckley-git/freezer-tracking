#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

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

echo "Building changed images for ${IMAGE_PLATFORM}"

if [[ "${should_build_api}" == "true" ]]; then
  echo "Building freezer-tracking-api:${api_version}"
  podman build --platform "${IMAGE_PLATFORM}" -t "freezer-tracking-api:${api_version}" -f "${REPO_ROOT}/apps/api/Dockerfile" "${REPO_ROOT}"
fi

if [[ "${should_build_web}" == "true" ]]; then
  echo "Building freezer-tracking-web:${web_version}"
  podman build --platform "${IMAGE_PLATFORM}" -t "freezer-tracking-web:${web_version}" -f "${REPO_ROOT}/apps/web/Dockerfile" "${REPO_ROOT}"
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
