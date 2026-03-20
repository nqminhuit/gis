#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Run gis integration tests from inside a container using the Testcontainers
"docker wormhole" pattern.

Usage:
  ./scripts/test-in-container.sh [docker|podman] [maven args...]

Examples:
  ./scripts/test-in-container.sh docker
  ./scripts/test-in-container.sh podman -Dtest=GisIntTest verify
USAGE
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

runtime="${1:-docker}"
if [[ $# -gt 0 ]]; then
  shift
fi

case "$runtime" in
  docker)
    socket_path="/var/run/docker.sock"
    ryuk_env=()
    ;;
  podman)
    socket_path="${XDG_RUNTIME_DIR:-/run/user/$(id -u)}/podman/podman.sock"
    if [[ ! -S "$socket_path" ]]; then
      cat >&2 <<EOF_ERR
podman socket not found at: $socket_path

Start the rootless socket first, for example:
  systemctl --user enable --now podman.socket
EOF_ERR
      exit 1
    fi
    ryuk_env=(-e TESTCONTAINERS_RYUK_DISABLED=true)
    ;;
  *)
    echo "Unsupported runtime: $runtime" >&2
    usage >&2
    exit 1
    ;;
esac

if ! command -v "$runtime" >/dev/null 2>&1; then
  echo "Container runtime '$runtime' is not installed or not on PATH." >&2
  exit 1
fi

project_dir="$(pwd)"
m2_dir="${HOME}/.m2"
mkdir -p "$m2_dir"

maven_image="docker.io/maven:3.9.7-eclipse-temurin-21-alpine"
maven_args=("$@")
if [[ ${#maven_args[@]} -eq 0 ]]; then
  maven_args=(
    -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn
    clean
    verify
  )
fi

host_override_args=()
if [[ -n "${TESTCONTAINERS_HOST_OVERRIDE:-}" ]]; then
  host_override_args=(-e "TESTCONTAINERS_HOST_OVERRIDE=${TESTCONTAINERS_HOST_OVERRIDE}")
fi

exec "$runtime" run --rm \
  -v "$project_dir:$project_dir" \
  -w "$project_dir" \
  -v "$socket_path:/var/run/docker.sock" \
  -v "$m2_dir:/root/.m2" \
  -e DOCKER_HOST=unix:///var/run/docker.sock \
  "${host_override_args[@]}" \
  "${ryuk_env[@]}" \
  "$maven_image" \
  /bin/sh -lc 'apk add --no-cache git >/dev/null && mvn -B "$@"' -- "${maven_args[@]}"
