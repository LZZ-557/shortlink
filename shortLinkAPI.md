# ShortLink 短链接服务接口文档

## 通用说明

- 基础路径：`http://<host>:<port>`（服务名 `shortlink`）
- 返回格式（除跳转接口外）统一为 `Result` 对象：

```json
{
  "code": 0,        // 0 = 成功；1 = 失败
  "msg": "success", // 提示信息
  "data": {}        // 业务数据
}
```

- 鉴权：所有非 GET 请求需在请求头携带 X-API-Key，值为 <见 application.yml 中 API_KEY 配置>
  - 鉴权拦截范围：`/api/shortlink/**`
- 短码规则：6 位字符，字符集为 `0-9 a-z A-Z`（Base62）。

---

## 1. 生成短链接

- **URL**：`POST /api/shortlink`
- **鉴权**：需要 `X-API-Key`
- **请求体**：

```json
{ "longUrl": "https://example.com/very/long/path" }
```

- **成功响应**：`data` 为生成的短码

```json
{ "code": 0, "msg": "success", "data": "Ab3xYz" }
```

- **说明**：长链以 MD5 哈希去重，已存在时直接返回原短码。

---

## 2. 按短码查询信息（查数据库）

- **URL**：`GET /api/shortlink/{shortCode}`
- **鉴权**：无（GET 放行）
- **成功响应**：`data` 为 `ShortLinkInformation`

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "id": 1,
    "shortCode": "Ab3xYz",
    "longUrl": "https://example.com/very/long/path",
    "urlHash": "…",
    "status": 1,
    "createTime": "2025-01-01T00:00:00",
    "expireTime": null
  }
}
```

- **失败响应**：`{ "code": 1, "msg": "短码已失效", "data": null }`

---

## 3. 按短码查询长链（缓存优先）

- **URL**：`GET /api/shortlink/{shortCode}/cached`
- **鉴权**：无
- **逻辑**：先查 Redis（key `shortLink:{shortCode}`，TTL 7 天），未命中再查数据库并回填缓存。
- **成功响应**：`data` 为长链字符串

```json
{ "code": 0, "msg": "success", "data": "https://example.com/very/long/path" }
```

---

## 4. 删除短链接（软删除）

- **URL**：`DELETE /api/shortlink/{shortCode}/delete`
- **鉴权**：需要 `X-API-Key`
- **逻辑**：将 `status` 置为 `0`，并同步删除缓存（写时删缓存）。
- **成功响应**：

```json
{ "code": 0, "msg": "success", "data": "已删除：Ab3xYz" }
```

---

## 5. 重新启用短链接

- **URL**：`POST /api/shortlink/{shortCode}/enable`
- **鉴权**：需要 `X-API-Key`
- **逻辑**：将 `status` 置为 `1`。
- **成功响应**：

```json
{ "code": 0, "msg": "success", "data": "重新启用：Ab3xYz" }
```

---

## 6. 短链接访问统计

- **URL**：`GET /api/shortlink/{shortCode}/stats`
- **鉴权**：无
- **成功响应**：`data` 为 `StatsData`

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "shortCode": "Ab3xYz",
    "visitCount": 128,
    "lastVisitTime": "2025-01-02T10:30:00"
  }
}
```

- **失败响应**：`{ "code": 1, "msg": "短码无效", "data": null }`

---

## 7. 短链跳转（302）

- **URL**：`GET /{shortCode}`
- **鉴权**：无
- **逻辑**：查缓存/数据库获取长链；命中后异步记录访问日志，返回 `302 Found` 并携带 `Location` 头跳转。
- **成功**：`HTTP 302`，响应头 `Location: <longUrl>`
- **失败**：`HTTP 404`（短码不存在或已失效）

---

## 附：状态码约定

| status | 含义 |
|--------|------|
| 1 | 正常/启用 |
| 0 | 已删除/失效 |

## 附：错误码约定

| code | 含义 |
|------|------|
| 0 | 成功 |
| 1 | 失败（业务错误或未授权） |