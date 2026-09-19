# 本地一键启停

这些脚本用于 Ubuntu 本地演示环境，复用已按项目环境文档创建的 Docker 容器。

## 首次启动

先在 IDEA 中停止所有珞珈声场微服务，再执行：

```bash
./scripts/local/start-all.sh
```

脚本会：

1. 启动已有 Docker 基础设施容器。
2. 等待必要端口就绪。
3. 使用 Maven 打包后端。
4. 启动 7 个业务服务和网关。
5. 将 PID 和日志写入 `.local-run/`。

## 快速二次启动

代码没有修改时可以跳过 Maven 打包：

```bash
./scripts/local/start-all.sh --skip-build
```

Docker 基础设施已经启动时：

```bash
./scripts/local/start-all.sh --skip-build --skip-infra
```

## 查看状态

```bash
./scripts/local/status.sh
```

## 停止服务

仅停止脚本启动的 Java 服务，保留 Docker 基础设施：

```bash
./scripts/local/stop-all.sh
```

同时停止 Java 服务和 Docker 基础设施：

```bash
./scripts/local/stop-all.sh --all
```

## 日志

```text
.local-run/logs/
```

某个端口如果已被 IDEA 或其他进程占用，启动脚本会跳过该服务，不会自动结束外部进程。

