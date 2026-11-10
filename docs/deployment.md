# 生产部署与回滚

## 主机准备

- 安装 Docker Engine 与 Compose 插件，在部署目录放置本仓库和生产 `.env`。
- `.env` 必须设置 `API_IMAGE`、`WEB_IMAGE`、`BLUE_TAG`、`GREEN_TAG`、数据库/Redis 密码、Actuator 的 `MONITOR_PASSWORD` 及随机 `GAME_MASTER_KEY`。
- 为 GitHub `production` Environment 配置审批规则，以及 `DEPLOY_HOST`、`DEPLOY_USER`、`DEPLOY_SSH_KEY`、`DEPLOY_PATH`、`GHCR_TOKEN` Secrets。

初始化生产栈：

```bash
docker compose -f compose.prod.yaml up -d mysql redis api-blue web-blue api-green web-green edge
```

## 灰度发布

流水线将同一 Git SHA 的 API/Web 镜像推送至 GHCR，然后在非活动槽位执行：

```bash
deploy/scripts/deploy.sh <git-sha> green
```

脚本等待新 API 健康后，按 10%、50%、100% 调整流量。灰度使用客户端 IP 与 User-Agent 的稳定哈希，同一用户的 HTML、静态资源与 API 会进入同一槽位。`GRAY_OBSERVATION_SECONDS` 默认 300 秒，可由运维环境覆盖。

## 回滚

```bash
deploy/scripts/rollback.sh blue
```

回滚只切换到已验证的旧槽位，不重新构建镜像。数据库迁移遵守 expand/contract，绝不由回滚脚本反向执行。删除旧字段必须至少跨一个完整发布周期。

## 故障检查

```bash
docker compose -f compose.prod.yaml ps
docker compose -f compose.prod.yaml logs --tail=200 api-blue api-green edge
docker compose -f compose.prod.yaml exec edge nginx -t
```

如果新槽位健康检查失败，保持旧槽位 100% 流量并检查 Flyway、数据库连接、Redis 和 Quartz 日志。
