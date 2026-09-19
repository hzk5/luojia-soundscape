#!/usr/bin/env bash

set -Eeuo pipefail

if [[ $# -ne 2 ]]; then
  echo "用法: $0 <baseline.json> <optimized.json>" >&2
  exit 2
fi

baseline="$1"
optimized="$2"

has_authenticated_metrics() {
  jq -e '(.metrics.subscribe_duration.values["p(95)"] != null) and
         (.metrics.collect_duration.values["p(95)"] != null)' "$1" >/dev/null
}

auth_profile="public"
if has_authenticated_metrics "${baseline}" && has_authenticated_metrics "${optimized}"; then
  auth_profile="authenticated"
elif has_authenticated_metrics "${baseline}" || has_authenticated_metrics "${optimized}"; then
  echo "[拒绝] 一次包含订阅/收藏，另一次没有；不能比较不同负载场景。" >&2
  exit 2
fi

metric() {
  local file="$1"
  local expression="$2"
  jq -r "${expression} // 0" "${file}"
}

base_p95="$(metric "${baseline}" '.metrics.http_req_duration.values["p(95)"]')"
opt_p95="$(metric "${optimized}" '.metrics.http_req_duration.values["p(95)"]')"
base_p99="$(metric "${baseline}" '.metrics.http_req_duration.values["p(99)"]')"
opt_p99="$(metric "${optimized}" '.metrics.http_req_duration.values["p(99)"]')"
base_rate="$(metric "${baseline}" '.metrics.http_reqs.values.rate')"
opt_rate="$(metric "${optimized}" '.metrics.http_reqs.values.rate')"
opt_http_error="$(metric "${optimized}" '.metrics.http_req_failed.values.rate')"
opt_business_error="$(metric "${optimized}" '.metrics.business_error_rate.values.rate')"
base_detail_p95="$(metric "${baseline}" '.metrics.album_detail_duration.values["p(95)"]')"
opt_detail_p95="$(metric "${optimized}" '.metrics.album_detail_duration.values["p(95)"]')"
base_channel_p95="$(metric "${baseline}" '.metrics.channel_duration.values["p(95)"]')"
opt_channel_p95="$(metric "${optimized}" '.metrics.channel_duration.values["p(95)"]')"
base_search_p95="$(metric "${baseline}" '.metrics.search_duration.values["p(95)"]')"
opt_search_p95="$(metric "${optimized}" '.metrics.search_duration.values["p(95)"]')"
base_subscribe_p95="$(metric "${baseline}" '.metrics.subscribe_duration.values["p(95)"]')"
opt_subscribe_p95="$(metric "${optimized}" '.metrics.subscribe_duration.values["p(95)"]')"
base_collect_p95="$(metric "${baseline}" '.metrics.collect_duration.values["p(95)"]')"
opt_collect_p95="$(metric "${optimized}" '.metrics.collect_duration.values["p(95)"]')"

p95_gain="$(awk -v before="${base_p95}" -v after="${opt_p95}" 'BEGIN { if (before == 0) print 0; else printf "%.2f", (before-after)*100/before }')"
qps_gain="$(awk -v before="${base_rate}" -v after="${opt_rate}" 'BEGIN { if (before == 0) print 0; else printf "%.2f", (after-before)*100/before }')"
http_error_percent="$(awk -v value="${opt_http_error}" 'BEGIN { printf "%.2f", value*100 }')"
business_error_percent="$(awk -v value="${opt_business_error}" 'BEGIN { printf "%.2f", value*100 }')"
detail_p95_gain="$(awk -v before="${base_detail_p95}" -v after="${opt_detail_p95}" 'BEGIN { if (before == 0) print 0; else printf "%.2f", (before-after)*100/before }')"
p99_gain="$(awk -v before="${base_p99}" -v after="${opt_p99}" 'BEGIN { if (before == 0) print 0; else printf "%.2f", (before-after)*100/before }')"
channel_p95_gain="$(awk -v before="${base_channel_p95}" -v after="${opt_channel_p95}" 'BEGIN { if (before == 0) print 0; else printf "%.2f", (before-after)*100/before }')"
search_p95_gain="$(awk -v before="${base_search_p95}" -v after="${opt_search_p95}" 'BEGIN { if (before == 0) print 0; else printf "%.2f", (before-after)*100/before }')"
subscribe_p95_gain="$(awk -v before="${base_subscribe_p95}" -v after="${opt_subscribe_p95}" 'BEGIN { if (before == 0) print 0; else printf "%.2f", (before-after)*100/before }')"
collect_p95_gain="$(awk -v before="${base_collect_p95}" -v after="${opt_collect_p95}" 'BEGIN { if (before == 0) print 0; else printf "%.2f", (before-after)*100/before }')"

report="$(dirname "${optimized}")/comparison.md"
if [[ "${auth_profile}" == "authenticated" ]]; then
  report="$(dirname "${optimized}")/comparison-auth.md"
fi
apply_status="FAIL"
if awk -v p="${p95_gain}" -v d="${detail_p95_gain}" -v q="${qps_gain}" \
  -v h="${http_error_percent}" -v b="${business_error_percent}" \
  -v channel="${channel_p95_gain}" -v search="${search_p95_gain}" \
  -v subscribe="${subscribe_p95_gain}" -v collect="${collect_p95_gain}" \
  -v auth="${auth_profile}" \
  'BEGIN { exit !(p >= 40 && d >= 40 && q >= 50 && h < 1 && b < 1 &&
                    channel >= -10 && search >= -10 &&
                    (auth != "authenticated" || (subscribe >= -10 && collect >= -10))) }'; then
  apply_status="PASS"
fi

{
  echo "# 性能对比"
  echo
  echo "| 指标 | 基线 | 优化后 | 改善 |"
  echo "|---|---:|---:|---:|"
  echo "| P95 (ms) | ${base_p95} | ${opt_p95} | ${p95_gain}% |"
  echo "| P99 (ms) | ${base_p99} | ${opt_p99} | ${p99_gain}% |"
  echo "| 专辑详情 P95 (ms) | ${base_detail_p95} | ${opt_detail_p95} | ${detail_p95_gain}% |"
  echo "| 频道 P95 (ms) | ${base_channel_p95} | ${opt_channel_p95} | ${channel_p95_gain}% |"
  echo "| 搜索 P95 (ms) | ${base_search_p95} | ${opt_search_p95} | ${search_p95_gain}% |"
  if [[ "${auth_profile}" == "authenticated" ]]; then
    echo "| 订阅分页 P95 (ms) | ${base_subscribe_p95} | ${opt_subscribe_p95} | ${subscribe_p95_gain}% |"
    echo "| 收藏分页 P95 (ms) | ${base_collect_p95} | ${opt_collect_p95} | ${collect_p95_gain}% |"
  fi
  echo "| QPS | ${base_rate} | ${opt_rate} | ${qps_gain}% |"
  echo "| HTTP 错误率 | - | ${http_error_percent}% | - |"
  echo "| 业务错误率 | - | ${business_error_percent}% | - |"
  echo
  echo "验收结果：**${apply_status}**"
  echo
  echo "场景：${auth_profile}；负值表示接口变慢。按相同 VU 自循环压测时，两版实际 QPS 可能不同；若某接口回退超限，需追加固定到达率测试确定原因。"
} > "${report}"

echo "对比报告: ${report}"
