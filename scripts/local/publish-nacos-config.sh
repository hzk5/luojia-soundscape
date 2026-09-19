#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

if [[ -f "${PROJECT_ROOT}/.env" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "${PROJECT_ROOT}/.env"
  set +a
fi

command -v curl >/dev/null 2>&1 || {
  echo "未找到 curl，请先安装后重试。" >&2
  exit 1
}

NACOS_BASE="${NACOS_SERVER_ADDR:-127.0.0.1:8848}"
if [[ "${NACOS_BASE}" != http://* && "${NACOS_BASE}" != https://* ]]; then
  NACOS_BASE="http://${NACOS_BASE}"
fi
NACOS_API="${NACOS_BASE%/}/nacos/v1/cs/configs"
CONFIG_DIR="${PROJECT_ROOT}/deploy/config"
TEMP_DIR="$(mktemp -d)"
trap 'rm -rf -- "${TEMP_DIR}"' EXIT

publish_config() {
  local data_id="$1"
  local config_file="$2"
  local response

  response="$(curl -fsS -X POST "${NACOS_API}" \
    --data-urlencode "dataId=${data_id}" \
    --data-urlencode "group=DEFAULT_GROUP" \
    --data-urlencode "content@${config_file}")"
  if [[ "${response}" != "true" ]]; then
    echo "发布失败: ${data_id}（Nacos 返回 ${response}）" >&2
    exit 1
  fi
  echo "[OK] ${data_id}"
}

publish_config "common.yaml" "${CONFIG_DIR}/common.example.yaml"

SERVICES=(gateway album search user account order payment dispatch)
for service in "${SERVICES[@]}"; do
  source_file="${CONFIG_DIR}/service-${service}.example.yaml"
  [[ "${service}" == "gateway" ]] && source_file="${CONFIG_DIR}/gateway.example.yaml"
  publish_file="${source_file}"

  case "${service}" in
    user|account|order|payment)
      publish_file="${TEMP_DIR}/${service}.yaml"
      {
        cat "${source_file}"
        printf '\n'
        cat "${CONFIG_DIR}/seata-client.example.yaml"
      } >"${publish_file}"
      ;;
  esac

  publish_config "luojia-soundscape-${service}-dev.yaml" "${publish_file}"
done

echo "Nacos 配置发布完成。"
