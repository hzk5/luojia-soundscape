#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/../local/common.sh"

: "${MYSQL_PASSWORD:?请通过环境变量 MYSQL_PASSWORD 提供本地 MySQL root 密码}"
MANIFEST="${1:-}"
if [[ -z "${MANIFEST}" || ! -f "${MANIFEST}" ]]; then
  echo "用法: MYSQL_PASSWORD=... $0 scripts/perf/manifests/<run-id>.env" >&2
  exit 2
fi

source "${MANIFEST}"
if [[ ! "${PERF_RUN_ID}" =~ ^[0-9]{14}$ ]] || [[ ! "${ALBUM_IDS}" =~ ^[0-9]+(,[0-9]+)*$ ]] \
  || [[ ! "${TRACK_IDS}" =~ ^[0-9]+(,[0-9]+)*$ ]]; then
  echo "[拒绝] manifest 格式不合法，未执行任何删除。" >&2
  exit 3
fi
if [[ ! "${PERF_USER_ID}" =~ ^[0-9]+$ ]] || [[ ! "${MONGO_DATABASE}" =~ ^[a-zA-Z0-9_]+$ ]] \
  || [[ -n "${MONGO_LEGACY_DATABASE:-}" && ! "${MONGO_LEGACY_DATABASE}" =~ ^[a-zA-Z0-9_]+$ ]]; then
  echo "[拒绝] manifest 的用户 ID 或 MongoDB 数据库名不合法，未执行任何删除。" >&2
  exit 3
fi
if [[ "${ALBUM_IDS%%,*}" != "9000000001" || "${ALBUM_IDS##*,}" != "9000000200" \
  || "${TRACK_IDS%%,*}" != "9100000001" || "${TRACK_IDS##*,}" != "9100000200" ]]; then
  echo "[拒绝] manifest 不属于 PERF 保留 ID 区间。" >&2
  exit 3
fi

MYSQL_CONTAINER="${MYSQL_CONTAINER:-luojia_soundscape_mysql}"
MONGO_CONTAINER="${MONGO_CONTAINER:-luojia_soundscape_mongo}"
GATEWAY_URL="${GATEWAY_URL:-http://127.0.0.1:8500}"

if docker_cmd exec "${MONGO_CONTAINER}" mongosh --version >/dev/null 2>&1; then
  MONGO_SHELL=mongosh
elif docker_cmd exec "${MONGO_CONTAINER}" mongo --version >/dev/null 2>&1; then
  MONGO_SHELL=mongo
else
  echo "[拒绝] MongoDB 容器中未找到 mongosh 或 mongo，未执行清理。" >&2
  exit 1
fi

for album_id in ${ALBUM_IDS//,/ }; do
  # 下架是辅助步骤；即使搜索服务暂时不可用，也不能阻塞后续精确的
  # MySQL/Mongo 清理。请求设置上限，避免脚本无限等待。
  curl --silent --connect-timeout 2 --max-time 5 \
    "${GATEWAY_URL}/api/search/albumInfo/lowerAlbum/${album_id}" >/dev/null || true
done

docker_cmd exec -i "${MYSQL_CONTAINER}" mysql -uroot "-p${MYSQL_PASSWORD}" luojia_soundscape_album <<SQL
START TRANSACTION;
DELETE FROM track_stat WHERE track_id IN (${TRACK_IDS});
DELETE FROM track_info WHERE id IN (${TRACK_IDS});
DELETE FROM album_attribute_value WHERE album_id IN (${ALBUM_IDS});
DELETE FROM album_stat WHERE album_id IN (${ALBUM_IDS});
DELETE FROM album_info WHERE id IN (${ALBUM_IDS});
COMMIT;
SQL

docker_cmd exec "${MONGO_CONTAINER}" "${MONGO_SHELL}" --quiet "${MONGO_DATABASE}" --eval "
db.getCollection('userSubscribe_${PERF_USER_ID}').deleteMany({perfRunId:'${PERF_RUN_ID}'});
db.getCollection('userCollect_${PERF_USER_ID}').deleteMany({perfRunId:'${PERF_RUN_ID}'});
"

# A repaired run may also have records in the old, incorrect MongoDB database.
if [[ -n "${MONGO_LEGACY_DATABASE:-}" && "${MONGO_LEGACY_DATABASE}" != "${MONGO_DATABASE}" ]]; then
  docker_cmd exec "${MONGO_CONTAINER}" "${MONGO_SHELL}" --quiet "${MONGO_LEGACY_DATABASE}" --eval "
db.getCollection('userSubscribe_${PERF_USER_ID}').deleteMany({perfRunId:'${PERF_RUN_ID}'});
db.getCollection('userCollect_${PERF_USER_ID}').deleteMany({perfRunId:'${PERF_RUN_ID}'});
"
fi

echo "[OK] 只清理了 manifest 中登记的 PERF 数据；manifest 保留用于审计：${MANIFEST}"
