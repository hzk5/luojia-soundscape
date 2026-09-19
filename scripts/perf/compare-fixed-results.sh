#!/usr/bin/env bash

set -Eeuo pipefail

if [[ $# -ne 2 ]]; then
  echo "用法: $0 <baseline-fixed.json> <optimized-fixed.json>" >&2
  exit 2
fi

baseline="$1"
optimized="$2"

for file in "$baseline" "$optimized"; do
  if [[ ! -s "$file" ]] || ! jq -e '.metrics' "$file" >/dev/null; then
    echo "[拒绝] 不是有效的 k6 summary JSON: $file" >&2
    exit 2
  fi
done

metric() {
  local file="$1"
  local expression="$2"
  jq -r "$expression // 0" "$file"
}

percent_gain() {
  awk -v before="$1" -v after="$2" \
    'BEGIN { if (before == 0) print "0.00"; else printf "%.2f", (before-after)*100/before }'
}

base_requests="$(metric "$baseline" '.metrics.http_reqs.values.count')"
opt_requests="$(metric "$optimized" '.metrics.http_reqs.values.count')"
base_dropped="$(metric "$baseline" '.metrics.dropped_iterations.values.count')"
opt_dropped="$(metric "$optimized" '.metrics.dropped_iterations.values.count')"
opt_http_error="$(metric "$optimized" '.metrics.http_req_failed.values.rate')"
opt_business_error="$(metric "$optimized" '.metrics.business_error_rate.values.rate')"

declare -a keys=(
  http_req_duration
  album_detail_duration
  channel_duration
  search_duration
  subscribe_duration
  collect_duration
)
declare -a labels=(
  "整体 P95"
  "专辑详情 P95"
  "频道 P95"
  "搜索 P95"
  "订阅分页 P95"
  "收藏分页 P95"
)

declare -a base_values=()
declare -a opt_values=()
declare -a gains=()
for i in "${!keys[@]}"; do
  base_value="$(metric "$baseline" ".metrics.${keys[$i]}.values[\"p(95)\"]")"
  opt_value="$(metric "$optimized" ".metrics.${keys[$i]}.values[\"p(95)\"]")"
  base_values+=("$base_value")
  opt_values+=("$opt_value")
  gains+=("$(percent_gain "$base_value" "$opt_value")")
done

http_error_percent="$(awk -v value="$opt_http_error" 'BEGIN { printf "%.2f", value*100 }')"
business_error_percent="$(awk -v value="$opt_business_error" 'BEGIN { printf "%.2f", value*100 }')"

status="PASS"
reason="固定到达率下无丢弃、错误率低于 1%，且各读接口回退未超过 10%。"
if ! awk -v requests="$opt_requests" -v dropped="$opt_dropped" \
  -v http="$http_error_percent" -v business="$business_error_percent" \
  -v overall="${gains[0]}" -v detail="${gains[1]}" \
  -v channel="${gains[2]}" -v search="${gains[3]}" \
  -v subscribe="${gains[4]}" -v collect="${gains[5]}" \
  'BEGIN { exit !(requests >= 35990 && dropped == 0 && http < 1 && business < 1 &&
                    overall >= -10 && detail >= -10 && channel >= -10 && search >= -10 &&
                    subscribe >= -10 && collect >= -10) }'; then
  status="FAIL"
  reason="存在请求丢弃、错误率超限，或至少一个读接口 P95 回退超过 10%。"
fi

report="$(dirname "$optimized")/comparison-fixed.md"
{
  echo "# 固定 600 RPS 性能对比"
  echo
  echo "> 本报告用于同到达率下的延迟回退检查；吞吐提升使用阶梯 VU 报告验证。"
  echo
  echo "| 指标 | 基线 | 优化后 | 改善 |"
  echo "|---|---:|---:|---:|"
  for i in "${!keys[@]}"; do
    echo "| ${labels[$i]} (ms) | ${base_values[$i]} | ${opt_values[$i]} | ${gains[$i]}% |"
  done
  echo "| 完成请求数 | ${base_requests} | ${opt_requests} | - |"
  echo "| 丢弃迭代数 | ${base_dropped} | ${opt_dropped} | - |"
  echo "| HTTP 错误率 | - | ${http_error_percent}% | - |"
  echo "| 业务错误率 | - | ${business_error_percent}% | - |"
  echo
  echo "回退检查：**${status}**。${reason}"
} > "$report"

echo "固定速率对比报告: $report"
echo "回退检查: $status"
