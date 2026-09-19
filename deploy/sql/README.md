# 数据库初始化

本目录只包含结构、索引、约束和最小必需的基础字典，不包含用户、专辑、订单、支付、日志、PERF 或 Nacos 业务配置数据。

| 顺序 | 文件 | 用途 |
| --- | --- | --- |
| 1 | `01-schema.sql` | account、album、dispatch、live、order、payment、system、user 业务库 DDL |
| 2 | `02-base-data.sql` | 内容分类、属性和 VIP 套餐字典 |
| 3 | `03-seata.sql` | Seata Server 表、`undo_log` 和 TCC fence 表 |
| 4 | `04-nacos.sql` | Nacos 持久化表，不含真实配置和默认账号 |
| 5 | `05-xxl-job.sql` | XXL-JOB 表及调度锁，不含默认管理员和示例任务 |

按表中顺序使用 MySQL 8 执行。Nacos 和 XXL-JOB 管理账号应在部署环境中按官方方式单独初始化，避免仓库内出现通用密码哈希。

脚本不包含业务数据、用户账号、云密钥或其他运行时凭据。
