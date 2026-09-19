#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${PROJECT_ROOT}"

patterns=(
  'AKID''[0-9A-Za-z]{13,}'
  'AKIA''[0-9A-Z]{16}'
  'ASIA''[0-9A-Z]{16}'
  'gh''[pousr]_[0-9A-Za-z]{20,}'
  'github_pat_''[0-9A-Za-z_]{20,}'
  'sk-''[0-9A-Za-z]{20,}'
  'wx''[0-9a-fA-F]{16}'
  '-----BEGIN ''(RSA |EC |OPENSSH )?PRIVATE KEY-----'
)

sensitive_name_pattern='(^|/)(id_(rsa|ed25519)|project\.private\.config\.json|\.env|\.env\.(local|prod|production|dev|development)|.*\.(pem|p12|pfx|jks|keystore|key))$'
failed=0

report_tree() {
  local revision="$1"
  local label="$2"
  local pattern paths

  for pattern in "${patterns[@]}"; do
    paths="$(git grep -Il -E -e "${pattern}" "${revision}" -- . 2>/dev/null || true)"
    if [[ -n "${paths}" ]]; then
      failed=1
      while IFS= read -r path; do
        echo "[HIGH] ${label}: ${path#*:}"
      done <<< "${paths}"
    fi
  done

  while IFS= read -r path; do
    [[ -z "${path}" ]] && continue
    failed=1
    echo "[HIGH] ${label}: 敏感文件名 ${path}"
  done < <(git ls-tree -r --name-only "${revision}" | grep -Ei "${sensitive_name_pattern}" || true)

  return 0
}

for revision in $(git rev-list --all); do
  report_tree "${revision}" "commit $(git rev-parse --short "${revision}")"
done

# Also inspect tracked working-tree content, including staged but uncommitted edits.
for pattern in "${patterns[@]}"; do
  while IFS= read -r path; do
    [[ -z "${path}" ]] && continue
    failed=1
    echo "[HIGH] working tree: ${path}"
  done < <(git grep -Il -E -e "${pattern}" -- . 2>/dev/null || true)
  true
done

if (( failed != 0 )); then
  echo "[FAIL] 检测到高置信度凭据或私钥文件；输出仅包含位置，不包含凭据值。" >&2
  exit 1
fi

echo "[OK] 当前源码及全部可达 Git 历史未发现高置信度密钥、访问令牌或私钥文件。"
