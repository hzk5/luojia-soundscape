#!/usr/bin/env bash

set -Eeuo pipefail

LOCAL_SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${LOCAL_SCRIPT_DIR}/../.." && pwd)"
RUNTIME_DIR="${PROJECT_ROOT}/.local-run"
PID_DIR="${RUNTIME_DIR}/pids"
LOG_DIR="${RUNTIME_DIR}/logs"

mkdir -p "${PID_DIR}" "${LOG_DIR}"

SERVICES=(
  service-album
  service-search
  service-user
  service-account
  service-order
  service-payment
  service-dispatch
  server-gateway
)

service_port() {
  case "$1" in
    server-gateway) echo 8500 ;;
    service-album) echo 8501 ;;
    service-search) echo 8502 ;;
    service-user) echo 8503 ;;
    service-order) echo 8504 ;;
    service-account) echo 8505 ;;
    service-payment) echo 8506 ;;
    service-dispatch) echo 8509 ;;
    *) return 1 ;;
  esac
}

service_application_name() {
  case "$1" in
    server-gateway) echo "luojia-soundscape-gateway" ;;
    service-*) echo "luojia-soundscape-${1#service-}" ;;
    *) return 1 ;;
  esac
}

service_jar() {
  case "$1" in
    server-gateway) echo "${PROJECT_ROOT}/server-gateway/target/server-gateway.jar" ;;
    *) echo "${PROJECT_ROOT}/service/$1/target/$1.jar" ;;
  esac
}

port_is_open() {
  local port="$1"
  timeout 1 bash -c "</dev/tcp/127.0.0.1/${port}" >/dev/null 2>&1
}

find_java() {
  if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]]; then
    echo "${JAVA_HOME}/bin/java"
    return
  fi

  local java_candidate
  for java_candidate in "${HOME:-}/.jdks"/temurin-17*/bin/java; do
    if [[ -x "${java_candidate}" ]]; then
      echo "${java_candidate}"
      return
    fi
  done

  command -v java || {
    echo "未找到 Java，请安装 JDK 17 或设置 JAVA_HOME。" >&2
    return 1
  }
}

find_maven() {
  if [[ -n "${MAVEN_BIN:-}" && -x "${MAVEN_BIN}" ]]; then
    echo "${MAVEN_BIN}"
    return
  fi
  if command -v mvn >/dev/null 2>&1; then
    command -v mvn
    return
  fi

  local maven_candidate
  for maven_candidate in /opt/idea/idea-*/plugins/maven/lib/maven3/bin/mvn; do
    if [[ -x "${maven_candidate}" ]]; then
      echo "${maven_candidate}"
      return
    fi
  done

  echo "未找到 Maven，请安装 Maven 或通过 MAVEN_BIN 指定可执行文件。" >&2
  return 1
}

docker_cmd() {
  if docker info >/dev/null 2>&1; then
    docker "$@"
  else
    sudo docker "$@"
  fi
}

wait_for_port() {
  local name="$1"
  local port="$2"
  local timeout_seconds="${3:-120}"
  local waited=0

  while ! port_is_open "${port}"; do
    if (( waited >= timeout_seconds )); then
      echo "[FAIL] ${name} 在 ${timeout_seconds}s 内未就绪（端口 ${port}）" >&2
      return 1
    fi
    sleep 2
    waited=$((waited + 2))
  done
  echo "[ OK ] ${name} 已就绪（端口 ${port}）"
}
