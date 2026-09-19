#!/usr/bin/env bash

set -Eeuo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/../local/common.sh"

jar="${PROJECT_ROOT}/service/service-album/target/service-album.jar"
[[ -f "${jar}" ]] || { echo "缺少 ${jar}，请先打包" >&2; exit 1; }
port_is_open 8601 && { echo "8601 已在监听"; exit 0; }

java_bin="$(find_java)"
runtime_config="${RUNTIME_DIR}/service-album-replica.yml"
nacos_base="${NACOS_URL:-http://127.0.0.1:8848/nacos}"

# Bootstrap 阶段的 Nacos 属性源会覆盖普通命令行参数中的 server.port。
# 副本启动时将当前公共配置与专辑配置复制到忽略目录，移除远端端口后禁用
# bootstrap，既保持数据源等配置一致，也不会修改 Nacos 或提交其中的密钥。
curl --fail --silent --show-error \
  "${nacos_base}/v1/cs/configs?dataId=common.yaml&group=DEFAULT_GROUP" \
  >"${runtime_config}"
printf '\n---\n' >>"${runtime_config}"
curl --fail --silent --show-error \
  "${nacos_base}/v1/cs/configs?dataId=luojia-soundscape-album-dev.yaml&group=DEFAULT_GROUP" \
  | sed '/^server:$/ { N; /\n[[:space:]]*port:[[:space:]]*8501[[:space:]]*$/d; }' \
  >>"${runtime_config}"
chmod 600 "${runtime_config}"

nohup "${java_bin}" -Xms128m -Xmx512m \
  -Dspring.cloud.bootstrap.enabled=false -jar "${jar}" \
  --spring.application.name=luojia-soundscape-album-replica \
  --spring.cloud.nacos.config.enabled=false \
  --spring.config.additional-location="file:${runtime_config}" \
  --server.port=8601 \
  --spring.cloud.nacos.discovery.port=8601 \
  --management.endpoints.web.exposure.include=health,info,metrics,prometheus \
  --management.metrics.tags.application=luojia-soundscape-album-replica \
  >"${LOG_DIR}/service-album-8601.log" 2>&1 &
echo "$!" >"${PID_DIR}/service-album-8601.pid"
wait_for_port luojia-soundscape-album-replica 8601 120
