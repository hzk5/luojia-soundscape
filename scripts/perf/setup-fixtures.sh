#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/../local/common.sh"

: "${MYSQL_PASSWORD:?请通过环境变量 MYSQL_PASSWORD 提供本地 MySQL root 密码}"
: "${SOURCE_ALBUM_ID:?请提供 SOURCE_ALBUM_ID}"
: "${SOURCE_TRACK_ID:?请提供 SOURCE_TRACK_ID}"

PERF_USER_ID="${PERF_USER_ID:-1}"
# Must match spring.data.mongodb.database (Nacos common.yaml), not the MySQL schema.
MONGO_DATABASE="${MONGO_DATABASE:-luojia-soundscape}"
GATEWAY_URL="${GATEWAY_URL:-http://127.0.0.1:8500}"
PERF_RUN_ID="${PERF_RUN_ID:-$(date +%Y%m%d%H%M%S)}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-luojia_soundscape_mysql}"
MONGO_CONTAINER="${MONGO_CONTAINER:-luojia_soundscape_mongo}"
ALBUM_FIRST=9000000001
ALBUM_LAST=9000000200
TRACK_FIRST=9100000001
TRACK_LAST=9100000200

MANIFEST_DIR="${SCRIPT_DIR}/manifests"
MANIFEST="${MANIFEST_DIR}/${PERF_RUN_ID}.env"
mkdir -p "${MANIFEST_DIR}"

if docker_cmd exec "${MONGO_CONTAINER}" mongosh --version >/dev/null 2>&1; then
  MONGO_SHELL=mongosh
elif docker_cmd exec "${MONGO_CONTAINER}" mongo --version >/dev/null 2>&1; then
  MONGO_SHELL=mongo
else
  echo "[拒绝] MongoDB 容器中未找到 mongosh 或 mongo，未写入任何 PERF 数据。" >&2
  exit 1
fi

album_ids="$(seq -s, "${ALBUM_FIRST}" "${ALBUM_LAST}")"
track_ids="$(seq -s, "${TRACK_FIRST}" "${TRACK_LAST}")"

existing="$(docker_cmd exec "${MYSQL_CONTAINER}" mysql -N -uroot "-p${MYSQL_PASSWORD}" -e \
  "SELECT COUNT(*) FROM luojia_soundscape_album.album_info WHERE id BETWEEN ${ALBUM_FIRST} AND ${ALBUM_LAST};")"
if [[ "${existing}" != "0" ]]; then
  echo "[拒绝] PERF 保留专辑 ID 区间已被占用；请先使用对应 manifest 清理。" >&2
  exit 1
fi

source_count="$(docker_cmd exec "${MYSQL_CONTAINER}" mysql -N -uroot "-p${MYSQL_PASSWORD}" -e \
  "SELECT (SELECT COUNT(*) FROM luojia_soundscape_album.album_info WHERE id=${SOURCE_ALBUM_ID}) + (SELECT COUNT(*) FROM luojia_soundscape_album.track_info WHERE id=${SOURCE_TRACK_ID});")"
if [[ "${source_count}" != "2" ]]; then
  echo "[拒绝] 源专辑或源声音不存在，未写入任何 PERF 数据。" >&2
  exit 1
fi

cat >"${MANIFEST}" <<EOF
PERF_RUN_ID=${PERF_RUN_ID}
PERF_USER_ID=${PERF_USER_ID}
MONGO_DATABASE=${MONGO_DATABASE}
ALBUM_IDS=${album_ids}
TRACK_IDS=${track_ids}
EOF
chmod 600 "${MANIFEST}"

values=""
for n in $(seq 1 200); do
  [[ -n "${values}" ]] && values+=","
  values+="(${n},$((9000000000 + n)),$((9100000000 + n)))"
done

docker_cmd exec -i "${MYSQL_CONTAINER}" mysql --default-character-set=utf8mb4 -uroot "-p${MYSQL_PASSWORD}" luojia_soundscape_album <<SQL
START TRANSACTION;
CREATE TEMPORARY TABLE perf_ids (n INT PRIMARY KEY, album_id BIGINT, track_id BIGINT);
INSERT INTO perf_ids VALUES ${values};

INSERT INTO album_info
(id,user_id,album_title,category3_id,album_intro,cover_url,include_track_count,is_finished,
 estimated_track_count,album_rich_intro,quality_score,pay_type,price_type,price,discount,
 vip_discount,tracks_for_free,seconds_for_free,buy_notes,selling_point,is_open,status,
 create_time,update_time,is_deleted)
SELECT p.album_id,${PERF_USER_ID},CONCAT('[PERF][${PERF_RUN_ID}] 专辑 ',p.n),a.category3_id,
       CONCAT('[PERF] run=', '${PERF_RUN_ID}'),a.cover_url,1,a.is_finished,1,a.album_rich_intro,
       a.quality_score,a.pay_type,a.price_type,a.price,a.discount,a.vip_discount,a.tracks_for_free,
       a.seconds_for_free,a.buy_notes,a.selling_point,'1','0301',NOW(),NOW(),0
FROM perf_ids p JOIN album_info a ON a.id=${SOURCE_ALBUM_ID};

INSERT INTO album_stat (album_id,stat_type,stat_num,create_time,update_time,is_deleted)
SELECT p.album_id,d.stat_type,p.n * d.weight,NOW(),NOW(),0
FROM perf_ids p
JOIN (SELECT '0401' stat_type,11 weight UNION ALL SELECT '0402',7 UNION ALL
      SELECT '0403',5 UNION ALL SELECT '0404',3) d;

INSERT INTO album_attribute_value (album_id,attribute_id,value_id,create_time,update_time,is_deleted)
SELECT p.album_id,a.attribute_id,a.value_id,NOW(),NOW(),0
FROM perf_ids p JOIN album_attribute_value a ON a.album_id=${SOURCE_ALBUM_ID} AND a.is_deleted=0;

INSERT INTO track_info
(id,user_id,album_id,track_title,order_num,track_intro,track_rich_intro,cover_url,media_duration,
 media_file_id,media_url,media_size,media_type,source,is_open,status,
 create_time,update_time,is_deleted)
SELECT p.track_id,${PERF_USER_ID},p.album_id,CONCAT('[PERF][${PERF_RUN_ID}] 声音 ',p.n),1,
       t.track_intro,t.track_rich_intro,t.cover_url,t.media_duration,
       CONCAT('perf-', '${PERF_RUN_ID}', '-', p.n),t.media_url,t.media_size,t.media_type,
       t.source,'1','0501',NOW(),NOW(),0
FROM perf_ids p JOIN track_info t ON t.id=${SOURCE_TRACK_ID};

INSERT INTO track_stat (track_id,stat_type,stat_num,create_time,update_time,is_deleted)
SELECT p.track_id,d.stat_type,p.n * d.weight,NOW(),NOW(),0
FROM perf_ids p
JOIN (SELECT '0701' stat_type,11 weight UNION ALL SELECT '0702',7 UNION ALL
      SELECT '0703',5 UNION ALL SELECT '0704',3) d;
COMMIT;
SQL

docker_cmd exec "${MONGO_CONTAINER}" "${MONGO_SHELL}" --quiet "${MONGO_DATABASE}" --eval "
const run='${PERF_RUN_ID}', uid=NumberLong('${PERF_USER_ID}');
const albums=Array.from({length:100},(_,i)=>({userId:uid,albumId:NumberLong(String(9000000001+i)),createTime:new Date(),perfRunId:run}));
const tracks=Array.from({length:100},(_,i)=>({userId:uid,trackId:NumberLong(String(9100000001+i)),createTime:new Date(),perfRunId:run}));
db.getCollection('userSubscribe_${PERF_USER_ID}').insertMany(albums);
db.getCollection('userCollect_${PERF_USER_ID}').insertMany(tracks);
"

for album_id in $(seq "${ALBUM_FIRST}" "${ALBUM_LAST}"); do
  curl --fail --silent --show-error "${GATEWAY_URL}/api/search/albumInfo/upperAlbum/${album_id}" >/dev/null
done

echo "[OK] PERF 数据已生成，manifest: ${MANIFEST}"
