#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/../local/common.sh"

: "${REDIS_PASSWORD:?请通过环境变量 REDIS_PASSWORD 提供本地 Redis 密码}"
REDIS_CONTAINER="${REDIS_CONTAINER:-luojia_soundscape_redis}"

# Only read-path cache keys shared by the baseline and optimized builds are
# removed. Login tokens, account/order state, rankings and business data stay.
patterns=(
  'album:info:*'
  'album:detail:*'
  'albuminfo:stat:*'
  'search:channel:*'
  'search:item:*'
  'search:query:*'
  'category:*'
  'user:userinfovo:*'
)

deleted=0
for pattern in "${patterns[@]}"; do
  mapfile -t keys < <(docker_cmd exec -e REDISCLI_AUTH="${REDIS_PASSWORD}" \
    "${REDIS_CONTAINER}" redis-cli --no-auth-warning --raw --scan --pattern "${pattern}")
  if (( ${#keys[@]} == 0 )); then
    continue
  fi
  docker_cmd exec -e REDISCLI_AUTH="${REDIS_PASSWORD}" "${REDIS_CONTAINER}" \
    redis-cli --no-auth-warning UNLINK "${keys[@]}" >/dev/null
  deleted=$((deleted + ${#keys[@]}))
done

echo "[OK] 已清除 ${deleted} 个读链路缓存 Key；登录 token 和业务数据未删除。"
