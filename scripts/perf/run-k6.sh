#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
RESULT_DIR="${SCRIPT_DIR}/results"
mkdir -p "${RESULT_DIR}"
source "${PROJECT_ROOT}/scripts/local/common.sh"

label="${1:-run}"
timestamp="$(date +%Y%m%d-%H%M%S)"
summary_file="${RESULT_DIR}/${label}-${timestamp}.json"

export BASE_URL="${BASE_URL:-http://127.0.0.1:8500}"
export ALBUM_ID="${ALBUM_ID:-1}"
export CATEGORY1_ID="${CATEGORY1_ID:-1}"
export KEYWORD="${KEYWORD:-儿童}"
export TOKEN="${TOKEN:-}"
export THINK_TIME="${THINK_TIME:-0}"
export FIXED_RATE="${FIXED_RATE:-0}"
export SERVER_COMMIT="${SERVER_COMMIT:-$(git -C "${PROJECT_ROOT}" rev-parse HEAD)}"
export SUMMARY_PATH="/work/results/$(basename "${summary_file}")"

warmup_requests="${WARMUP_REQUESTS:-20}"
if (( warmup_requests > 0 )); then
  echo "==> 预热 ${warmup_requests} 轮（不计入 k6 统计）"
  search_body="{\"keyword\":\"${KEYWORD}\",\"category1Id\":0,\"category2Id\":0,\"category3Id\":0,\"attributeList\":[],\"order\":\"1:desc\",\"pageNo\":1,\"pageSize\":10}"

  warmup_get() {
    local url="$1"
    shift
    if ! curl --fail --silent --show-error --connect-timeout 2 --max-time 10 \
      --output /dev/null "$@" "$url"; then
      echo "[FAIL] 预热请求失败: ${url}" >&2
      echo "请先执行 ./scripts/local/status.sh，确认相关服务均为 RUNNING。" >&2
      exit 1
    fi
  }

  warmup_post_json() {
    local url="$1"
    local body="$2"
    if ! curl --fail --silent --show-error --connect-timeout 2 --max-time 10 \
      --output /dev/null -H 'Content-Type: application/json' -d "$body" "$url"; then
      echo "[FAIL] 预热请求失败: ${url}" >&2
      echo "请先执行 ./scripts/local/status.sh，确认相关服务均为 RUNNING。" >&2
      exit 1
    fi
  }

  for ((i = 1; i <= warmup_requests; i++)); do
    warmup_get "${BASE_URL}/api/search/albumInfo/${ALBUM_ID}"
    warmup_get "${BASE_URL}/api/search/albumInfo/channel/${CATEGORY1_ID}"
    warmup_post_json "${BASE_URL}/api/search/albumInfo" "${search_body}"
    if [[ -n "${TOKEN}" ]]; then
      warmup_get "${BASE_URL}/api/user/userInfo/findUserSubscribePage/1/20" \
        -H "token: ${TOKEN}"
      warmup_get "${BASE_URL}/api/user/userInfo/findUserCollectPage/1/20" \
        -H "token: ${TOKEN}"
    fi
  done
fi

{
  java_bin="$(find_java)"
  echo "server_commit=${SERVER_COMMIT}"
  echo "java=$(${java_bin} -version 2>&1 | head -n 1)"
  echo "cpu=$(nproc)"
  echo "memory=$(LC_ALL=C free -h | awk '/^Mem:/ {print $2}')"
  echo "jvm_args=-Xms128m -Xmx512m"
  echo "k6_version=2.1.0"
  echo "base_url=${BASE_URL}"
  echo "album_id=${ALBUM_ID}"
  echo "think_time=${THINK_TIME}"
  echo "fixed_rate=${FIXED_RATE}"
  echo "trace_mode=${PERF_TRACE_MODE:-default}"
  echo "warmup_requests=${warmup_requests}"
} > "${summary_file%.json}.env"

run_status=0
if command -v k6 >/dev/null 2>&1; then
  SUMMARY_PATH="${summary_file}" k6 run "${SCRIPT_DIR}/mixed-workload.js" || run_status=$?
else
  docker_cmd run --rm --network host \
    --user "$(id -u):$(id -g)" \
    -e BASE_URL -e ALBUM_ID -e CATEGORY1_ID -e KEYWORD -e TOKEN -e THINK_TIME -e FIXED_RATE -e SUMMARY_PATH \
    -v "${SCRIPT_DIR}:/work" -w /work \
    grafana/k6:2.1.0 run /work/mixed-workload.js || run_status=$?
fi

if [[ ! -s "${summary_file}" ]]; then
  echo "[FAIL] k6 未生成结果文件：${summary_file}" >&2
  exit 1
fi

echo "k6 结果: ${summary_file}"
if (( run_status != 0 )); then
  echo "[WARN] k6 退出码 ${run_status}；请检查结果中的阈值、丢弃请求和业务错误。" >&2
fi
exit "${run_status}"
