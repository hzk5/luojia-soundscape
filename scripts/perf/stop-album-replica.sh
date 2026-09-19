#!/usr/bin/env bash

set -Eeuo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/../local/common.sh"
pid_file="${PID_DIR}/service-album-8601.pid"
[[ -f "${pid_file}" ]] || { echo "副本未记录为运行状态"; exit 0; }
pid="$(<"${pid_file}")"
kill "${pid}" 2>/dev/null || true
for _ in $(seq 1 30); do
  kill -0 "${pid}" 2>/dev/null || break
  sleep 1
done
rm -f "${pid_file}"
echo "service-album 8601 副本已停止"
