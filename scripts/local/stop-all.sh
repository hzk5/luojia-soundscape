#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

STOP_INFRA=false
if [[ "${1:-}" == "--infra" || "${1:-}" == "--all" ]]; then
  STOP_INFRA=true
elif [[ -n "${1:-}" ]]; then
  echo "用法: $0 [--infra|--all]" >&2
  exit 2
fi

echo "==> 停止由启动脚本创建的 Java 进程"
stopping_services=()
for ((index=${#SERVICES[@]}-1; index>=0; index--)); do
  service="${SERVICES[$index]}"
  pid_file="${PID_DIR}/${service}.pid"
  if [[ ! -f "${pid_file}" ]]; then
    echo "[SKIP] ${service} 没有 PID 记录"
    continue
  fi

  pid="$(<"${pid_file}")"
  expected_jar="$(service_jar "${service}")"
  cmdline=""
  if [[ -r "/proc/${pid}/cmdline" ]]; then
    cmdline="$(tr '\0' ' ' <"/proc/${pid}/cmdline")"
  fi

  if kill -0 "${pid}" 2>/dev/null && [[ "${cmdline}" == *"${expected_jar}"* ]]; then
    kill "${pid}"
    stopping_services+=("${service}")
    echo "[ OK ] 已请求停止 ${service} PID=${pid}"
  else
    echo "[SKIP] ${service} PID 已失效或不属于该服务"
  fi
  rm -f "${pid_file}"
done

# SIGTERM is asynchronous; do not return while a just-stopped service still
# occupies its port, or an immediate start-all.sh can silently skip that jar.
for service in "${stopping_services[@]}"; do
  port="$(service_port "${service}")"
  for ((attempt=0; attempt<30; attempt++)); do
    if ! port_is_open "${port}"; then
      break
    fi
    sleep 1
  done
  if port_is_open "${port}"; then
    echo "[FAIL] ${service} 的端口 ${port} 在停止后仍被占用；请核对进程再启动。" >&2
    exit 1
  fi
done

if [[ "${STOP_INFRA}" == true ]]; then
  echo
  echo "==> 停止 Docker 基础设施"
  INFRA_CONTAINERS=(
    luojia_soundscape_xxl_job_admin
    seata-server
    luojia_soundscape_nacos
    luojia_soundscape_minio
    luojia_soundscape_rabbitmq
    luojia_soundscape_elasticsearch
    luojia_soundscape_mongo
    luojia_soundscape_redis
    luojia_soundscape_mysql
  )
  for container in "${INFRA_CONTAINERS[@]}"; do
    if docker_cmd inspect "${container}" >/dev/null 2>&1; then
      docker_cmd stop "${container}" >/dev/null || true
      echo "[ OK ] 已停止 ${container}"
    fi
  done
fi

echo "==> 停止命令执行完成"
