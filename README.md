# 游戏运营管理后台

基于 RuoYi-Vue-Plus 5.6.2、Plus UI 5.6.1、Java 21、Vue 3、MySQL、Redis 与 Quartz 的单游戏多区服运营后台。

## 一键启动

1. 复制环境变量：`cp .env.example .env`，至少修改数据库、Redis 和 `GAME_MASTER_KEY`。
2. 启动：`docker compose up -d --build`。
3. 查看状态：`docker compose ps`。
4. 浏览器打开 `http://localhost:8088`，本地初始化账号为 `admin` / `admin123`，首次登录后立即修改密码。

首次启动会导入 RuoYi 基础表，随后由 Flyway 创建游戏业务表、Quartz 表、菜单和运营角色。Compose 同时启动 WireMock；默认 `S1` 区服可用于玩家查询、发奖和活动发布演示。

## 主要能力

- 区服配置、用户区服数据权限、实时玩家查询和 HMAC 游戏服适配器。
- 单人、批量、CSV 与预约发奖；批量/预约双人复核、目标级幂等、失败分类与重试。
- 签到、累计登录、充值、排行榜和自定义活动模板；不可变版本、逐区服发布、重试和回退。
- Quartz JDBC 集群任务，支持蓝绿实例并行期间的单次执行、暂停、恢复、立即执行和审计日志。
- GitHub Actions 构建、测试、GHCR 镜像、10%/50%/100% 灰度和槽位回滚。

## 开发命令

```bash
cd server
mvn -pl ruoyi-admin -am -DskipTests=false package

cd ../web
corepack enable
pnpm install
pnpm exec eslint src/views/game src/api/game
pnpm build:prod
```

游戏服接口与签名协议见 [docs/game-server-api.md](docs/game-server-api.md)。生产发布见 [docs/deployment.md](docs/deployment.md)。

## 安全约束

- 生产环境不得使用 `.env.example` 中的示例值，也不得提交 `.env`、证书或真实区服密钥。
- 区服密钥以 AES-256-GCM 密文保存，主密钥只通过环境变量提供。
- 游戏后台不直连玩家数据库；所有资产变更必须经过带幂等键的游戏服管理接口。
- 数据库迁移只允许向前兼容；应用回滚不会自动回滚数据库。
