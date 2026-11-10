# 游戏服运营接口契约 v1

所有接口使用 JSON。后台发送以下签名头：

- `X-App-Id`：区服配置的应用 ID。
- `X-Timestamp`：Unix 秒；游戏服应拒绝超过 300 秒的请求。
- `X-Nonce`：单次请求 UUID；同一 App ID 下不得重复使用。
- `X-Signature`：小写十六进制 HMAC-SHA256。

签名原文按换行拼接：

```text
timestamp
nonce
HTTP_METHOD
/原始请求路径
原始请求体（GET 请求为空字符串）
```

游戏服必须同时校验时间窗口、Nonce 防重放和常量时间签名比较。

## 玩家查询

`GET /ops-api/v1/players/{playerId}`

```json
{
  "playerName": "Alice",
  "level": 42,
  "status": "ONLINE",
  "assets": { "gold": 1000, "diamond": 88 }
}
```

## 奖励发放

`POST /ops-api/v1/reward-grants`

```json
{
  "requestId": "sha256-idempotency-key",
  "serverCode": "S1",
  "playerId": "10001",
  "mailTitle": "维护补偿",
  "mailContent": "感谢支持",
  "items": [{ "itemCode": "gold", "quantity": 100 }]
}
```

游戏服必须以 `requestId` 做永久或满足业务保留期的唯一约束，并对重复请求返回第一次处理结果。

```json
{ "status": "SUCCESS", "externalTxnId": "game-txn-123", "message": "accepted" }
```

业务拒绝返回 4xx；临时不可用返回 5xx。后台只自动重试连接失败、超时和 5xx。

## 活动发布

`PUT /ops-api/v1/activities/{activityCode}/versions/{version}`

请求包含 `requestId`、活动编码、版本号和完整不可变 `snapshot`。游戏服应按活动编码与版本幂等接收，不允许低版本覆盖高版本，显式回退请求除外。

```json
{ "status": "SUCCESS", "externalVersion": "42" }
```

## 活动下线

`POST /ops-api/v1/activities/{activityCode}/offline`

```json
{ "requestId": "uuid" }
```

## 健康检查

`GET /ops-api/v1/health` 返回任意 2xx JSON 响应即视为可用。
