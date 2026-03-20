.DEFAULT_GOAL := help

CONTAINER_RUNTIME ?= docker
PROJECT_DIR := $(CURDIR)
M2_DIR ?= $(HOME)/.m2
MAVEN_IMAGE ?= docker.io/maven:3.9.7-eclipse-temurin-21-alpine
MAVEN_ARGS ?= -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn clean verify

.PHONY: help test-in-container test-in-container-docker test-in-container-podman

help:
	@printf '%s\n' \
	  'Available targets:' \
	  '  test-in-container         Run integration tests in a container (set CONTAINER_RUNTIME=docker|podman)' \
	  '  test-in-container-docker  Run integration tests in a Docker container' \
	  '  test-in-container-podman  Run integration tests in a rootless Podman container'

test-in-container:
	@set -eu; \
	if ! command -v "$(CONTAINER_RUNTIME)" >/dev/null 2>&1; then \
	  echo "Container runtime '$(CONTAINER_RUNTIME)' is not installed or not on PATH." >&2; \
	  exit 1; \
	fi; \
	mkdir -p "$(M2_DIR)"; \
	SOCKET_PATH=/var/run/docker.sock; \
	RYUK_ENV=""; \
	if [ "$(CONTAINER_RUNTIME)" = podman ]; then \
	  SOCKET_PATH="$${XDG_RUNTIME_DIR:-/run/user/$$(id -u)}/podman/podman.sock"; \
	  if [ ! -S "$$SOCKET_PATH" ]; then \
	    echo "podman socket not found at: $$SOCKET_PATH" >&2; \
	    echo >&2; \
	    echo "Start the rootless socket first, for example:" >&2; \
	    echo "  systemctl --user enable --now podman.socket" >&2; \
	    exit 1; \
	  fi; \
	  RYUK_ENV='-e TESTCONTAINERS_RYUK_DISABLED=true'; \
	fi; \
	HOST_OVERRIDE_ENV=""; \
	if [ -n "$${TESTCONTAINERS_HOST_OVERRIDE:-}" ]; then \
	  HOST_OVERRIDE_ENV="-e TESTCONTAINERS_HOST_OVERRIDE=$${TESTCONTAINERS_HOST_OVERRIDE}"; \
	fi; \
	exec "$(CONTAINER_RUNTIME)" run --rm \
	  -v "$(PROJECT_DIR):$(PROJECT_DIR)" \
	  -w "$(PROJECT_DIR)" \
	  -v "$$SOCKET_PATH:/var/run/docker.sock" \
	  -v "$(M2_DIR):/root/.m2" \
	  -e DOCKER_HOST=unix:///var/run/docker.sock \
	  $$HOST_OVERRIDE_ENV \
	  $$RYUK_ENV \
	  "$(MAVEN_IMAGE)" \
	  /bin/sh -lc 'apk add --no-cache git >/dev/null && mvn -B $(MAVEN_ARGS)'

test-in-container-docker:
	@$(MAKE) test-in-container CONTAINER_RUNTIME=docker

test-in-container-podman:
	@$(MAKE) test-in-container CONTAINER_RUNTIME=podman
