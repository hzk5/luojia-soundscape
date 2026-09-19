#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

echo "Java 服务状态"
for service in "${SERVICES[@]}"; do
  port="$(service_port "${service}")"
  pid_file="${PID_DIR}/${service}.pid"
  pid="-"
  [[ -f "${pid_file}" ]] && pid="$(<"${pid_file}")"
  if port_is_open "${port}"; then
    printf '  %-20s RUNNING  port=%s pid=%s\n' "${service}" "${port}" "${pid}"
  else
    printf '  %-20s STOPPED  port=%s pid=%s\n' "${service}" "${port}" "${pid}"
  fi
done

echo
echo "Docker 容器状态"
if docker_cmd ps -a --format '  {{.Names}}\t{{.Status}}' 2>/dev/null; then
  :
else
  echo "  无法读取 Docker 状态"
fi

