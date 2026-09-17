# 校园市场系统 · WebSocket 聊天与公告推送设计

> 日期：2026-09-17
> 状态：已批准（方案 A：原生 Spring WebSocket）

## 1. 背景与目标

校园市场系统（SpringBoot 4.1 + RuoYi 多模块 + 三端 Vue3 前端）当前缺少实时通信能力。本期新增：

1. **一对一私聊**：用户 ↔ 商家、用户 ↔ 用户，消息持久化 + 历史记录，三端接入。
2. **公告实时推送**：管理端发布公告后，实时推送给所有在线用户端/商家端，未读红点提示；历史公告仍走现有查询接口。

技术选型：**原生 Spring WebSocket**（WebSocketHandler + HandshakeInterceptor + JWT 鉴权），不引入 STOMP/第三方中间件。

## 2. 数据模型（MySQL 新增 2 张表）

### 2.1 market_chat_session（会话表）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| session_id | BIGINT PK AUTO_INCREMENT | 会话 ID |
| user_a_id | BIGINT NOT NULL | 会话双方较小 userId（规整排序） |
| user_b_id | BIGINT NOT NULL | 会话双方较大 userId |
| last_message | VARCHAR(500) | 最后一条消息摘要（会话列表展示） |
| last_time | DATETIME | 最后消息时间 |
| UNIQUE KEY uk_ab (user_a_id, user_b_id) | | 同一对用户仅一个会话 |

### 2.2 market_chat_message（消息表）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| message_id | BIGINT PK AUTO_INCREMENT | 消息 ID |
| session_id | BIGINT NOT NULL | 所属会话 |
| sender_id | BIGINT NOT NULL | 发送者 userId |
| receiver_id | BIGINT NOT NULL | 接收者 userId |
| content | VARCHAR(1000) NOT NULL | 内容 |
| msg_type | CHAR(1) DEFAULT '1' | 1 文本 / 2 商品卡片 / 3 图片 |
| status | CHAR(1) DEFAULT '0' | 0 未读 / 1 已读 |
| create_time | DATETIME DEFAULT CURRENT_TIMESTAMP | 时间 |
| KEY idx_session (session_id, message_id) | | 分页查询 |

DDL 写入 `sql/market.sql`（增量脚本或独立 `sql/chat.sql`）。

## 3. WebSocket 端点与协议

### 3.1 端点

```
ws://localhost:8080/ws?token={JWT}
```

- 握手拦截器 `ChatHandshakeInterceptor`：从 query 参数取 token，经 `TokenService.getLoginUser()` 校验；失败拒绝握手。
- 连接建立后将 `LoginUser`（userId、角色、昵称）放入 session attributes。
- 在线注册表 `ChatSessionRegistry`：`ConcurrentHashMap<Long, Set<WebSocketSession>>`，支持同一用户多端登录；注释注明多实例部署需替换为 Redis Pub/Sub。
- 断线清理：`afterConnectionClosed` 从注册表移除。

### 3.2 消息协议（JSON 文本帧）

**客户端 → 服务端：**

```json
{ "type": "CHAT_SEND", "receiverId": 2, "content": "你好", "msgType": "1" }
{ "type": "CHAT_READ", "sessionId": 5 }
{ "type": "PING" }
```

**服务端 → 客户端：**

```json
{ "type": "CHAT_MESSAGE", "data": { "message": {...}, "session": {...} } }
{ "type": "MESSAGE_ACK", "data": { "messageId": 123, "code": 0, "msg": "ok" } }
{ "type": "NOTICE_NEW", "data": { "notice": { "noticeId": 9, "noticeTitle": "..." } } }
```

`CHAT_MESSAGE` 仅推给接收者（定向推送）；`NOTICE_NEW` 推给所有在线用户（广播）。

## 4. 后端 REST 接口（MarketChatController）

鉴权：登录即可（RuoYi 默认登录鉴权，不额外加角色限制）。

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `GET /market/chat/session/list` | 会话列表 | 对方昵称/头像、最后消息、未读数、对方是否在线 |
| `POST /market/chat/session` | 创建/获取会话 | 入参 `{ userId }`（用户-用户）或 `{ shopId }`（用户-商家，后端解析商家 userId） |
| `GET /market/chat/message/list` | 历史消息 | `{ sessionId, pageNum, pageSize }`，按 message_id 倒序分页 |
| `PUT /market/chat/message/read` | 标记已读 | `{ sessionId }`，仅接收者可操作 |
| `GET /market/chat/unread/count` | 未读总数 | 导航角标 |
| `GET /market/chat/online/{userId}` | 在线状态 | 供会话列表显示在线点 |
| `GET /market/chat/admin/sessions` | 管理端全部会话（只读） | 仅 admin |
| `GET /market/chat/admin/messages` | 管理端消息记录（只读） | 仅 admin，按会话/时间筛选 |

## 5. 公告实时推送

- 现有 `SysNoticeController` 发布成功后调用 `WebSocketService.broadcast(NOTICE_NEW, noticeBrief)`。
- 在线用户端/商家端实时收到 → 前端弹未读红点；离线用户上线后查现有公告列表/已读接口补齐。
- 不改变 `SysNotice` CRUD 与 `SysNoticeRead` 已读逻辑。

## 6. 错误处理

- 发送给自己 / 对方不存在 → `MESSAGE_ACK` 携带错误码（code != 0）。
- 连接异常/服务端重启 → 客户端指数退避重连；消息已落库，重连后从历史记录补齐。
- 目标离线 → 消息落库为未读；对方上线后收未读角标 + 历史记录。
- WebSocket 心跳：客户端每 30s PING，服务端回 PONG（或被动检测），2 次无响应客户端主动重连。

## 7. 前端设计（三端独立实现，模式统一）

### 7.1 公共 WebSocket 客户端封装（每端 `src/utils/ws.ts`）

- `connect(token)`：建立 `ws(s)://host/ws?token=` 连接。
- 自动重连：断开后指数退避（1s→2s→4s→…→最大 30s）。
- 心跳：每 30s 发送 PING；连续 2 次无响应主动断开重连。
- 事件分发：`on('CHAT_MESSAGE' | 'NOTICE_NEW' | 'MESSAGE_ACK' | ...)`。
- 方法：`sendChat(receiverId, content)`、`sendRead(sessionId)`。
- 登录成功后建连；登出/401 时断开并清理。

### 7.2 用户端（campus-user）

| 位置 | 功能 |
| --- | --- |
| 商品详情页 | 新增“联系商家”按钮（商家商品显示）→ `POST /chat/session`（shopId）→ 跳消息中心并打开会话 |
| 导航栏 | 消息入口图标 + 未读角标（`GET /chat/unread/count` + WebSocket 推送刷新） |
| 新页面 `views/chat/index.vue` | 消息中心：左侧会话列表 + 右侧聊天窗口（气泡式，历史分页，自动滚动到底） |

### 7.3 商家端（campus-merchant）

- 导航栏消息中心入口（未读角标）。
- 新页面 `views/chat/index.vue`：会话列表 + 聊天窗口，回复买家咨询。

### 7.4 管理端（campus-admin）

- 公告模块：发布后广播，管理端自身不弹推送（避免自扰）。
- 新页面：聊天记录查询（只读表格，仅 admin）用于纠纷审计。
- 管理端：公告列表页面保留现有列表/已读展示，管理端自身不接收公告推送（避免自扰）。

### 7.5 未读数策略

- 会话列表：后端按会话返回未读计数。
- 导航角标：页面回到前台（`visibilitychange`）+ 收到 `CHAT_MESSAGE`/`NOTICE_NEW` 时刷新，不常驻轮询。

## 8. 测试

1. 后端 REST：curl/接口冒烟（会话创建、历史分页、已读、未读数、管理员只读）。
2. WebSocket：Python `websocket-client` 脚本模拟双端（A/B 用户）收发，验证实时性、离线补发、已读回执、公告广播。
3. E2E：Playwright 双浏览器（用户端 + 商家端同时登录）互发消息，验证实时性、历史记录、未读角标。
4. 回归：现有公告 CRUD、订单/商品主流程不受影响。

## 9. 范围与边界

- 仅一对一私聊，不做群聊/语音/视频/文件传输。
- 图片消息（msg_type=3）本期仅预留类型，前端上传走现有 `/common/upload`，不在聊天窗口内置。
- 单实例部署；多实例水平扩容（Redis Pub/Sub）不在本期实现，代码留扩展点注释。
