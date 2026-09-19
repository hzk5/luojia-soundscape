# 性能压测

## 1. 启动项目与监控

```bash
./scripts/local/start-all.sh

read -rsp '设置本地 Grafana 管理员密码: ' GRAFANA_ADMIN_PASSWORD
echo
export GRAFANA_ADMIN_PASSWORD
./scripts/perf/start-observability.sh
unset GRAFANA_ADMIN_PASSWORD
```

- Prometheus: <http://127.0.0.1:9090>
- Grafana: <http://127.0.0.1:3001>（3000 已由 YApi 使用）

## 2. 运行压测

首次压测前可生成可精确清理的 PERF 数据。密码只通过环境变量传入，不会写入仓库：

```bash
MYSQL_PASSWORD='your-password' \
SOURCE_ALBUM_ID=1 SOURCE_TRACK_ID=3 PERF_USER_ID=1 \
./scripts/perf/setup-fixtures.sh
```

脚本会复制 200 个专辑和声音，生成 100 条订阅和收藏，并把 manifest 写入已忽略的 `scripts/perf/manifests/`。清理时必须显式指定该 manifest：

MongoDB 默认使用 Nacos `common.yaml` 中的 `spring.data.mongodb.database=luojia_soundscape`；`luojia_soundscape_user` 是 MySQL 库名，不是此处的 MongoDB 库名。如本机配置不同，在生成数据时用 `MONGO_DATABASE` 指定实际 MongoDB 库。

```bash
MYSQL_PASSWORD='your-password' \
./scripts/perf/cleanup-fixtures.sh scripts/perf/manifests/<run-id>.env
```

脚本会校验 PERF 保留 ID，不接受标题模糊匹配删除。

TOKEN 可选；不传时只压测公开读接口。
脚本默认在正式计时前预热 20 轮，并且不添加人为思考时间，用于测量服务在相同 VU 下的实际吞吐上限。
切换基线版和优化版时，两个版本的 Redis 缓存序列化格式不同。每次启动待测版本后先运行 `REDIS_PASSWORD=... ./scripts/perf/reset-read-cache.sh`，只删除读链路缓存并保留登录 token 与业务数据；带 token 时预热会同时覆盖订阅和收藏分页。

```bash
SERVER_COMMIT="$(git rev-parse perf-baseline)" \
ALBUM_ID=1 CATEGORY1_ID=1 WARMUP_REQUESTS=20 THINK_TIME=0 \
./scripts/perf/run-k6.sh baseline

SERVER_COMMIT="$(git rev-parse HEAD)" \
TOKEN='your-token' ALBUM_ID=1 CATEGORY1_ID=1 \
WARMUP_REQUESTS=20 THINK_TIME=0 \
./scripts/perf/run-k6.sh optimized
```

`SERVER_COMMIT` 必须填写当前实际运行的服务版本；从另一个 worktree 运行基线服务时尤其需要显式指定。
需要模拟用户思考时间时，可例如设置 `THINK_TIME=0.1`；此模式不用于验收最大吞吐提升。
用于诊断固定 VU 下某条接口回退时，两版可使用相同的 `FIXED_RATE=600`（每秒 600 次混合请求）；此模式固定运行 60 秒，必须确认 `dropped_iterations=0`、HTTP 与业务错误率均低于 1%，并从性能版目录运行同一脚本。固定到达率的诊断结果不能代替原来的峰值吞吐压测。

本机未安装 k6 时，脚本会使用 `grafana/k6:2.1.0` Docker 镜像。

按需执行双实例缓存一致性测试：

```bash
./scripts/perf/start-album-replica.sh
# 验证 8501/8601 后
./scripts/perf/stop-album-replica.sh
```

## 3. 生成对比报告

```bash
./scripts/perf/compare-results.sh \
  scripts/perf/results/baseline.json \
  scripts/perf/results/optimized.json

# 固定到达率的回退检查
./scripts/perf/compare-fixed-results.sh \
  scripts/perf/results/baseline-fixed.json \
  scripts/perf/results/optimized-fixed.json
```

报告同时检查 HTTP 错误率和响应体中的业务错误率，防止 HTTP 200 包裹业务失败被误判为成功。
带登录结果额外对比订阅与收藏分页，并检查搜索等接口是否超过 10% 回退上限，写入 `results/comparison-auth.md`；公开读结果仍写入 `results/comparison.md`。两次测试必须使用相同 token 状态和同一批测试数据。
固定到达率报告写入 `results/comparison-fixed.md`，要求完成约 36,000 次请求、无调度丢弃、错误率低于 1%，且每个受测读接口的 P95 回退不超过 10%。它用于验证同负载延迟，不替代阶梯 VU 的吞吐提升报告。
原始结果不会被自动提交；确认数据真实后，可将选定的结果和 `comparison.md` 加入仓库。
