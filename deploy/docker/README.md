# Docker 基础设施示例

`docker-compose.infrastructure.example.yml` 是面向新环境的参考模板，包含 MySQL、Redis、MongoDB、RabbitMQ、MinIO、Elasticsearch、Nacos、Seata 和 XXL-JOB。

使用前需要：

1. 在仓库根目录将 `.env.example` 复制为 `.env` 并替换所有 `change-me`。
2. 核对宿主机端口、CPU/内存和镜像版本。
3. 确认 `deploy/sql` 的脚本只会在全新 MySQL volume 初始化时自动执行。
4. 完成 Nacos 管理用户、XXL-JOB 管理用户和 MinIO bucket 的安全初始化。

该开发示例默认用 MySQL root 连接多个 schema，所以 `.env` 中的 `MYSQL_PASSWORD` 必须与 `MYSQL_ROOT_PASSWORD` 相同；非本地环境请分配按服务隔离且仅有必需权限的数据库用户。

用于生产时请锁定 MinIO 等使用 `latest` 的镜像版本，并配置 TLS、持久化备份和密钥管理。
