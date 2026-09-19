# 配置模板

本目录提供建立 Nacos Data ID 或本地 profile 所需的配置模板。

1. 复制根目录 `.env.example` 为本机 `.env`，通过环境变量注入凭据。
2. 将 `common.example.yaml` 作为公共配置，其他 YAML 分别对应各服务。
3. 交易服务同时合并 `seata-client.example.yaml`。
4. 本地演示默认启用微信支付 mock；要接入真实商户时，所有证书和密钥必须由密钥管理系统或环境变量提供。

不要把填写后的 YAML、`.env`、PEM 证书或 Nacos 配置导出文件提交到 Git。
