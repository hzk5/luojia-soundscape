#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
source "${PROJECT_ROOT}/scripts/local/common.sh"

: "${GRAFANA_ADMIN_PASSWORD:?请先设置 GRAFANA_ADMIN_PASSWORD，密码不会写入仓库}"

cd "${PROJECT_ROOT}/ops/observability"
export PERF_UID="$(id -u)" PERF_GID="$(id -g)"
mkdir -p data/prometheus data/grafana
for data_dir in data/prometheus data/grafana; do
  if [[ ! -w "${data_dir}" ]]; then
    echo "[FAIL] ${PWD}/${data_dir} 不可写，请修正目录所有者后再启动观测容器。" >&2
    exit 1
  fi
done
docker_cmd compose up -d
echo "Prometheus: http://127.0.0.1:9090"
echo "Grafana:    http://127.0.0.1:3001 (默认用户 admin，密码来自 GRAFANA_ADMIN_PASSWORD)"
