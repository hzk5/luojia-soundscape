#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

ENV_FILE="${PROJECT_ROOT}/.env"
if [[ -f "${ENV_FILE}" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "${ENV_FILE}"
  set +a
fi

SKIP_BUILD=false
SKIP_INFRA=false

for arg in "$@"; do
  case "${arg}" in
    --skip-build) SKIP_BUILD=true ;;
    --skip-infra) SKIP_INFRA=true ;;
    -h|--help)
      echo "用法: $0 [--skip-build] [--skip-infra]"
      echo "  --skip-build  使用已有 jar，跳过 Maven 打包"
      echo "  --skip-infra  不启动 Docker 基础设施"
      exit 0
      ;;
    *)
      echo "未知参数: ${arg}" >&2
      exit 2
      ;;
  esac
done

JAVA_BIN="$(find_java)"

echo "==> Luojia Soundscape 本地一键启动"
echo "项目目录: ${PROJECT_ROOT}"
echo "Java: ${JAVA_BIN}"

if [[ "${SKIP_INFRA}" == false ]]; then
  echo
  echo "==> 启动 Docker 基础设施"
  INFRA_CONTAINERS=(
    luojia_soundscape_mysql
    luojia_soundscape_redis
    luojia_soundscape_mongodb
    luojia_soundscape_elasticsearch
    luojia_soundscape_rabbitmq
    luojia_soundscape_minio
    luojia_soundscape_nacos
    luojia_soundscape_seata
    luojia_soundscape_xxl_job
  )

  for container in "${INFRA_CONTAINERS[@]}"; do
    if docker_cmd inspect "${container}" >/dev/null 2>&1; then
      running="$(docker_cmd inspect -f '{{.State.Running}}' "${container}")"
      if [[ "${running}" == "true" ]]; then
        echo "[SKIP] ${container} 已运行"
      else
        docker_cmd start "${container}" >/dev/null
        echo "[ OK ] 已启动 ${container}"
      fi
    else
      echo "[WARN] 未找到容器 ${container}"
    fi
  done

  wait_for_port "MySQL" 3306 120
  wait_for_port "Redis" 6379 60
  wait_for_port "MongoDB" 27017 60
  wait_for_port "Elasticsearch" 9200 180
  wait_for_port "RabbitMQ" 5672 120
  wait_for_port "MinIO" 9000 60
  wait_for_port "Nacos" 8848 180
  wait_for_port "Seata" 8091 120
  wait_for_port "XXL-JOB" 8080 120
fi

if [[ "${SKIP_BUILD}" == false ]]; then
  echo
  echo "==> Maven 打包（跳过测试）"
  MAVEN_BIN_RESOLVED="$(find_maven)"
  MODULES="server-gateway,service/service-album,service/service-search,service/service-user,service/service-account,service/service-order,service/service-payment,service/service-dispatch"
  (
    cd "${PROJECT_ROOT}"
    JAVA_HOME="$(cd "$(dirname "${JAVA_BIN}")/.." && pwd)" \
      "${MAVEN_BIN_RESOLVED}" -pl "${MODULES}" -am -DskipTests package
  )
fi

echo
echo "==> 启动 Java 服务"
for service in "${SERVICES[@]}"; do
  port="$(service_port "${service}")"
  jar="$(service_jar "${service}")"
  pid_file="${PID_DIR}/${service}.pid"
  log_file="${LOG_DIR}/${service}.log"

  if port_is_open "${port}"; then
    echo "[SKIP] ${service} 端口 ${port} 已被占用，不重复启动"
    continue
  fi
  if [[ ! -f "${jar}" ]]; then
    echo "[FAIL] 缺少 ${jar}，请去掉 --skip-build 后重试" >&2
    exit 1
  fi

  MANAGEMENT_ARGS=(
    "--management.endpoints.web.exposure.include=health,info,metrics,prometheus"
    "--management.endpoint.health.show-details=always"
    "--management.metrics.tags.application=$(service_application_name "${service}")"
    "--management.metrics.distribution.percentiles-histogram.http.server.requests=true"
    "--management.metrics.distribution.slo.http.server.requests=100ms,300ms,1s,2s"
    "--spring.cloud.openfeign.client.config.default.connectTimeout=500"
    "--spring.cloud.openfeign.client.config.default.readTimeout=1500"
    "--feign.client.config.default.connectTimeout=500"
    "--feign.client.config.default.readTimeout=1500"
  )
  nohup "${JAVA_BIN}" -Xms128m -Xmx512m -jar "${jar}" "${MANAGEMENT_ARGS[@]}" >"${log_file}" 2>&1 &
  pid=$!
  echo "${pid}" >"${pid_file}"
  echo "[....] ${service} PID=${pid} LOG=${log_file}"

  if ! wait_for_port "${service}" "${port}" 120; then
    echo "----- ${service} 最后 60 行日志 -----" >&2
    tail -n 60 "${log_file}" >&2 || true
    exit 1
  fi
done

echo
echo "==> 启动完成"
echo "Luojia Soundscape API: http://127.0.0.1:8500"
echo "Nacos:    http://127.0.0.1:8848/nacos"
echo "XXL-JOB:  http://127.0.0.1:8080/xxl-job-admin"
echo "日志目录: ${LOG_DIR}"
