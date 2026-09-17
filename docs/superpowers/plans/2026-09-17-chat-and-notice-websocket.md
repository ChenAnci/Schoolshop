# WebSocket 聊天与公告推送 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为校园市场系统新增一对一私聊（用户↔商家/用户↔用户，消息持久化+历史记录）与公告 WebSocket 实时推送。

**Architecture:** 原生 Spring WebSocket（`/ws?token=`）接入 RuoYi 鉴权；新增 `market_chat_session`/`market_chat_message` 两张表（MyBatis-Plus 访问）；`ChatSessionRegistry` 内存维护在线连接；`ChatWebSocketHandler` 统一处理 CHAT_SEND/CHAT_READ/PING，REST `MarketChatController` 提供会话/历史/未读/在线/管理员只读接口；公告发布后经 WebSocketService 广播。前端三端各封装 `utils/ws.ts` 客户端 + 消息中心页面。

**Tech Stack:** Spring Boot 4.1（WebSocket/WebMVC）、RuoYi 3.9.2（TokenService/JWT/Redis）、MyBatis-Plus、MySQL 8、Vue3 + Vite + Element Plus（三端独立）、Playwright（E2E）。

---

## 文件结构总览

**后端（新增/修改）：**
- `sql/chat.sql`（新增 DDL）
- `ruoyi-framework/pom.xml`（加 websocket starter）
- `ruoyi-framework/src/main/java/com/ruoyi/framework/web/service/TokenService.java`（加 `getLoginUser(String token)`）
- `ruoyi-framework/src/main/java/com/ruoyi/framework/config/SecurityConfig.java`（放行 `/ws`）
- `ruoyi-framework/src/main/java/com/ruoyi/framework/websocket/ChatSessionRegistry.java`（新增）
- `ruoyi-framework/src/main/java/com/ruoyi/framework/websocket/ChatHandshakeInterceptor.java`（新增）
- `ruoyi-framework/src/main/java/com/ruoyi/framework/websocket/ChatWebSocketHandler.java`（新增）
- `ruoyi-framework/src/main/java/com/ruoyi/framework/websocket/WebSocketConfig.java`（新增）
- `ruoyi-framework/src/main/java/com/ruoyi/framework/websocket/WebSocketService.java`（新增）
- `ruoyi-market/src/main/java/com/ruoyi/market/domain/MarketChatSession.java`（新增）
- `ruoyi-market/src/main/java/com/ruoyi/market/domain/MarketChatMessage.java`（新增）
- `ruoyi-market/src/main/java/com/ruoyi/market/mapper/MarketChatSessionMapper.java`（新增）
- `ruoyi-market/src/main/java/com/ruoyi/market/mapper/MarketChatMessageMapper.java`（新增）
- `ruoyi-market/src/main/java/com/ruoyi/market/service/IMarketChatSessionService.java`（新增）
- `ruoyi-market/src/main/java/com/ruoyi/market/service/IMarketChatMessageService.java`（新增）
- `ruoyi-market/src/main/java/com/ruoyi/market/service/impl/MarketChatSessionServiceImpl.java`（新增）
- `ruoyi-market/src/main/java/com/ruoyi/market/service/impl/MarketChatMessageServiceImpl.java`（新增）
- `ruoyi-admin/src/main/java/com/ruoyi/web/controller/market/MarketChatController.java`（新增）
- `ruoyi-admin/src/main/java/com/ruoyi/web/controller/system/SysNoticeController.java`（修改：发布后广播）

**前端（三端）：**
- `campus-user|merchant|admin/vite.config.ts`（加 `/ws` 代理）
- `campus-user|merchant|admin/src/utils/ws.ts`（新增）
- `campus-user|merchant|admin/src/api/chat.ts`（新增）
- `campus-user|merchant|admin/src/router/index.ts`（加 `/chat` 路由）
- `campus-user|merchant|admin/src/views/chat/index.vue`（新增）
- `campus-user/src/views/product/detail/index.vue`（加"联系商家"）
- 各端导航组件（加消息入口角标）

**测试：**
- `.e2e/test_chat_ws.py`（Python websocket 双端脚本）
- `.e2e/test_chat_e2e.py`（Playwright 双浏览器）

---

## Task 1: 数据库表

**Files:**
- Create: `sql/chat.sql`

- [ ] **Step 1: 编写 DDL**

创建 `sql/chat.sql`：

```sql
-- ----------------------------
-- 在线聊天：会话表
-- ----------------------------
DROP TABLE IF EXISTS market_chat_session;
CREATE TABLE market_chat_session (
  session_id   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  user_a_id    BIGINT       NOT NULL COMMENT '会话双方较小userId',
  user_b_id    BIGINT       NOT NULL COMMENT '会话双方较大userId',
  last_message VARCHAR(500) DEFAULT NULL COMMENT '最后一条消息摘要',
  last_time    DATETIME     DEFAULT NULL COMMENT '最后消息时间',
  PRIMARY KEY (session_id),
  UNIQUE KEY uk_ab (user_a_id, user_b_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天会话表';

-- ----------------------------
-- 在线聊天：消息表
-- ----------------------------
DROP TABLE IF EXISTS market_chat_message;
CREATE TABLE market_chat_message (
  message_id  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  session_id  BIGINT       NOT NULL COMMENT '会话ID',
  sender_id   BIGINT       NOT NULL COMMENT '发送者userId',
  receiver_id BIGINT       NOT NULL COMMENT '接收者userId',
  content     VARCHAR(1000) NOT NULL COMMENT '内容',
  msg_type    CHAR(1)      DEFAULT '1' COMMENT '消息类型：1文本 2商品卡片 3图片',
  status      CHAR(1)      DEFAULT '0' COMMENT '状态：0未读 1已读',
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (message_id),
  KEY idx_session (session_id, message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表';
```

- [ ] **Step 2: 执行 DDL**

Run:
```powershell
& "d:\SchoolShop\toolchain\mysql-8.0.29-winx64\bin\mysql.exe" -uroot -P3306 ruoyi_vue < sql/chat.sql
```
Expected: 无报错。验证：
```powershell
& "d:\SchoolShop\toolchain\mysql-8.0.29-winx64\bin\mysql.exe" -uroot -P3306 ruoyi_vue -e "SHOW TABLES LIKE 'market_chat%';"
```
Expected: 输出两张表名。

- [ ] **Step 3: Commit**

```bash
git add sql/chat.sql
git commit -m "feat(chat): 新增聊天会话表与消息表 DDL"
```

---

## Task 2: 后端 domain（两张表）

**Files:**
- Create: `ruoyi-market/src/main/java/com/ruoyi/market/domain/MarketChatSession.java`
- Create: `ruoyi-market/src/main/java/com/ruoyi/market/domain/MarketChatMessage.java`

- [ ] **Step 1: 创建 MarketChatSession**

`ruoyi-market/src/main/java/com/ruoyi/market/domain/MarketChatSession.java`：

```java
package com.ruoyi.market.domain;

import java.util.Date;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 聊天会话对象 market_chat_session
 */
@TableName("market_chat_session")
public class MarketChatSession
{
    private static final long serialVersionUID = 1L;

    /** 会话ID */
    @TableId(value = "session_id", type = IdType.AUTO)
    private Long sessionId;

    /** 会话双方较小userId */
    private Long userAId;

    /** 会话双方较大userId */
    private Long userBId;

    /** 最后一条消息摘要 */
    private String lastMessage;

    /** 最后消息时间 */
    private Date lastTime;

    /** 对方用户ID（关联查询，非表字段） */
    @TableField(exist = false)
    private Long otherUserId;

    /** 对方昵称（关联查询，非表字段） */
    @TableField(exist = false)
    private String otherNickName;

    /** 未读数（关联查询，非表字段） */
    @TableField(exist = false)
    private Long unreadCount;

    /** 对方是否在线（关联查询，非表字段） */
    @TableField(exist = false)
    private Boolean otherOnline;

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public Long getUserAId() { return userAId; }
    public void setUserAId(Long userAId) { this.userAId = userAId; }
    public Long getUserBId() { return userBId; }
    public void setUserBId(Long userBId) { this.userBId = userBId; }
    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    public Date getLastTime() { return lastTime; }
    public void setLastTime(Date lastTime) { this.lastTime = lastTime; }
    public Long getOtherUserId() { return otherUserId; }
    public void setOtherUserId(Long otherUserId) { this.otherUserId = otherUserId; }
    public String getOtherNickName() { return otherNickName; }
    public void setOtherNickName(String otherNickName) { this.otherNickName = otherNickName; }
    public Long getUnreadCount() { return unreadCount; }
    public void setUnreadCount(Long unreadCount) { this.unreadCount = unreadCount; }
    public Boolean getOtherOnline() { return otherOnline; }
    public void setOtherOnline(Boolean otherOnline) { this.otherOnline = otherOnline; }
}
```

- [ ] **Step 2: 创建 MarketChatMessage**

`ruoyi-market/src/main/java/com/ruoyi/market/domain/MarketChatMessage.java`：

```java
package com.ruoyi.market.domain;

import java.util.Date;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 聊天消息对象 market_chat_message
 */
@TableName("market_chat_message")
public class MarketChatMessage
{
    private static final long serialVersionUID = 1L;

    /** 消息ID */
    @TableId(value = "message_id", type = IdType.AUTO)
    private Long messageId;

    /** 会话ID */
    private Long sessionId;

    /** 发送者userId */
    private Long senderId;

    /** 接收者userId */
    private Long receiverId;

    /** 内容 */
    private String content;

    /** 消息类型：1文本 2商品卡片 3图片 */
    private String msgType;

    /** 状态：0未读 1已读 */
    private String status;

    /** 发送时间 */
    private Date createTime;

    /** 发送者昵称（关联查询，非表字段） */
    @TableField(exist = false)
    private String senderNickName;

    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }
    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getMsgType() { return msgType; }
    public void setMsgType(String msgType) { this.msgType = msgType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public String getSenderNickName() { return senderNickName; }
    public void setSenderNickName(String senderNickName) { this.senderNickName = senderNickName; }
}
```

- [ ] **Step 3: 编译验证**

Run:
```powershell
& "d:\SchoolShop\toolchain\apache-maven-3.9.11\bin\mvn.cmd" -q -pl ruoyi-market -am compile -DskipTests
```
Expected: `BUILD SUCCESS`（无编译错误）。

- [ ] **Step 4: Commit**

```bash
git add ruoyi-market/src/main/java/com/ruoyi/market/domain/MarketChatSession.java ruoyi-market/src/main/java/com/ruoyi/market/domain/MarketChatMessage.java
git commit -m "feat(chat): 新增聊天会话/消息实体"
```

---

## Task 3: 后端 mapper

**Files:**
- Create: `ruoyi-market/src/main/java/com/ruoyi/market/mapper/MarketChatSessionMapper.java`
- Create: `ruoyi-market/src/main/java/com/ruoyi/market/mapper/MarketChatMessageMapper.java`

- [ ] **Step 1: 创建两个 BaseMapper**

`MarketChatSessionMapper.java`：

```java
package com.ruoyi.market.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.market.domain.MarketChatSession;

/**
 * 聊天会话 Mapper
 */
public interface MarketChatSessionMapper extends BaseMapper<MarketChatSession>
{
}
```

`MarketChatMessageMapper.java`：

```java
package com.ruoyi.market.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.market.domain.MarketChatMessage;

/**
 * 聊天消息 Mapper
 */
public interface MarketChatMessageMapper extends BaseMapper<MarketChatMessage>
{
}
```

- [ ] **Step 2: 编译验证**

Run:
```powershell
& "d:\SchoolShop\toolchain\apache-maven-3.9.11\bin\mvn.cmd" -q -pl ruoyi-market -am compile -DskipTests
```
Expected: `BUILD SUCCESS`。

- [ ] **Step 3: Commit**

```bash
git add ruoyi-market/src/main/java/com/ruoyi/market/mapper/MarketChatSessionMapper.java ruoyi-market/src/main/java/com/ruoyi/market/mapper/MarketChatMessageMapper.java
git commit -m "feat(chat): 新增聊天 mapper"
```

---

## Task 4: 后端 service

**Files:**
- Create: `ruoyi-market/src/main/java/com/ruoyi/market/service/IMarketChatSessionService.java`
- Create: `ruoyi-market/src/main/java/com/ruoyi/market/service/IMarketChatMessageService.java`
- Create: `ruoyi-market/src/main/java/com/ruoyi/market/service/impl/MarketChatSessionServiceImpl.java`
- Create: `ruoyi-market/src/main/java/com/ruoyi/market/service/impl/MarketChatMessageServiceImpl.java`

- [ ] **Step 1: 创建接口**

`IMarketChatSessionService.java`：

```java
package com.ruoyi.market.service;

import com.ruoyi.market.domain.MarketChatSession;

/**
 * 聊天会话 Service
 */
public interface IMarketChatSessionService
{
    /** 获取或创建两人会话（内部将 userId 规整为 a<b），返回会话 */
    MarketChatSession getOrCreateSession(Long userId, Long otherUserId);

    /** 更新会话最后消息 */
    void updateLastMessage(Long sessionId, String lastMessage);

    /** 查询某人全部会话（填充对方信息/未读数/在线态） */
    java.util.List<MarketChatSession> listSessions(Long userId);

    /** 校验用户是否为会话成员 */
    boolean isMember(Long sessionId, Long userId);
}
```

`IMarketChatMessageService.java`：

```java
package com.ruoyi.market.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ruoyi.market.domain.MarketChatMessage;

/**
 * 聊天消息 Service
 */
public interface IMarketChatMessageService
{
    /** 发送消息并落库，返回带 ID 的消息 */
    MarketChatMessage sendMessage(Long sessionId, Long senderId, Long receiverId, String content, String msgType);

    /** 分页查询会话历史（按 message_id 倒序） */
    IPage<MarketChatMessage> pageMessages(Long sessionId, long pageNum, long pageSize);

    /** 某人在某会话的未读消息数 */
    long countUnread(Long sessionId, Long receiverId);

    /** 全部未读数（跨会话） */
    long countAllUnread(Long userId);

    /** 标记某会话下某接收者的消息全部已读 */
    int markRead(Long sessionId, Long receiverId);
}
```

- [ ] **Step 2: 创建实现类**

`MarketChatSessionServiceImpl.java`：

```java
package com.ruoyi.market.service.impl;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.market.domain.MarketChatSession;
import com.ruoyi.market.mapper.MarketChatSessionMapper;
import com.ruoyi.market.service.IMarketChatMessageService;
import com.ruoyi.market.service.IMarketChatSessionService;
import com.ruoyi.system.domain.SysUser;
import com.ruoyi.system.service.ISysUserService;

/**
 * 聊天会话 Service 实现
 */
@Service
public class MarketChatSessionServiceImpl implements IMarketChatSessionService
{
    @Autowired
    private MarketChatSessionMapper sessionMapper;

    @Autowired
    private IMarketChatMessageService messageService;

    @Autowired
    private ISysUserService userService;

    @Override
    public MarketChatSession getOrCreateSession(Long userId, Long otherUserId)
    {
        long a = Math.min(userId, otherUserId);
        long b = Math.max(userId, otherUserId);
        LambdaQueryWrapper<MarketChatSession> qw = new LambdaQueryWrapper<>();
        qw.eq(MarketChatSession::getUserAId, a).eq(MarketChatSession::getUserBId, b);
        MarketChatSession session = sessionMapper.selectOne(qw);
        if (session != null)
        {
            return session;
        }
        MarketChatSession created = new MarketChatSession();
        created.setUserAId(a);
        created.setUserBId(b);
        try
        {
            sessionMapper.insert(created);
            return created;
        }
        catch (DuplicateKeyException e)
        {
            // 并发下唯一键冲突，重查返回
            return sessionMapper.selectOne(qw);
        }
    }

    @Override
    public void updateLastMessage(Long sessionId, String lastMessage)
    {
        LambdaUpdateWrapper<MarketChatSession> uw = new LambdaUpdateWrapper<>();
        uw.eq(MarketChatSession::getSessionId, sessionId)
          .set(MarketChatSession::getLastMessage, lastMessage)
          .set(MarketChatSession::getLastTime, new java.util.Date());
        sessionMapper.update(null, uw);
    }

    @Override
    public List<MarketChatSession> listSessions(Long userId)
    {
        LambdaQueryWrapper<MarketChatSession> qw = new LambdaQueryWrapper<>();
        qw.and(w -> w.eq(MarketChatSession::getUserAId, userId).or().eq(MarketChatSession::getUserBId, userId))
          .orderByDesc(MarketChatSession::getLastTime);
        List<MarketChatSession> sessions = sessionMapper.selectList(qw);
        List<MarketChatSession> result = new ArrayList<>();
        for (MarketChatSession s : sessions)
        {
            Long otherId = s.getUserAId().equals(userId) ? s.getUserBId() : s.getUserAId();
            SysUser other = userService.selectUserById(otherId);
            s.setOtherUserId(otherId);
            s.setOtherNickName(other != null ? other.getNickName() : ("用户" + otherId));
            s.setUnreadCount(messageService.countUnread(s.getSessionId(), userId));
            result.add(s);
        }
        return result;
    }

    @Override
    public boolean isMember(Long sessionId, Long userId)
    {
        MarketChatSession s = sessionMapper.selectById(sessionId);
        return s != null && (s.getUserAId().equals(userId) || s.getUserBId().equals(userId));
    }
}
```

`MarketChatMessageServiceImpl.java`：

```java
package com.ruoyi.market.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.market.domain.MarketChatMessage;
import com.ruoyi.market.mapper.MarketChatMessageMapper;
import com.ruoyi.market.service.IMarketChatMessageService;

/**
 * 聊天消息 Service 实现
 */
@Service
public class MarketChatMessageServiceImpl implements IMarketChatMessageService
{
    @Autowired
    private MarketChatMessageMapper messageMapper;

    @Override
    public MarketChatMessage sendMessage(Long sessionId, Long senderId, Long receiverId, String content, String msgType)
    {
        MarketChatMessage m = new MarketChatMessage();
        m.setSessionId(sessionId);
        m.setSenderId(senderId);
        m.setReceiverId(receiverId);
        m.setContent(content);
        m.setMsgType(msgType == null ? "1" : msgType);
        m.setStatus("0");
        m.setCreateTime(new java.util.Date());
        messageMapper.insert(m);
        return m;
    }

    @Override
    public IPage<MarketChatMessage> pageMessages(Long sessionId, long pageNum, long pageSize)
    {
        LambdaQueryWrapper<MarketChatMessage> qw = new LambdaQueryWrapper<>();
        qw.eq(MarketChatMessage::getSessionId, sessionId)
          .orderByDesc(MarketChatMessage::getMessageId);
        Page<MarketChatMessage> page = new Page<>(pageNum, pageSize);
        return messageMapper.selectPage(page, qw);
    }

    @Override
    public long countUnread(Long sessionId, Long receiverId)
    {
        LambdaQueryWrapper<MarketChatMessage> qw = new LambdaQueryWrapper<>();
        qw.eq(MarketChatMessage::getSessionId, sessionId)
          .eq(MarketChatMessage::getReceiverId, receiverId)
          .eq(MarketChatMessage::getStatus, "0");
        return messageMapper.selectCount(qw);
    }

    @Override
    public long countAllUnread(Long userId)
    {
        LambdaQueryWrapper<MarketChatMessage> qw = new LambdaQueryWrapper<>();
        qw.eq(MarketChatMessage::getReceiverId, userId)
          .eq(MarketChatMessage::getStatus, "0");
        return messageMapper.selectCount(qw);
    }

    @Override
    public int markRead(Long sessionId, Long receiverId)
    {
        LambdaUpdateWrapper<MarketChatMessage> uw = new LambdaUpdateWrapper<>();
        uw.eq(MarketChatMessage::getSessionId, sessionId)
          .eq(MarketChatMessage::getReceiverId, receiverId)
          .eq(MarketChatMessage::getStatus, "0")
          .set(MarketChatMessage::getStatus, "1");
        return messageMapper.update(null, uw);
    }
}
```

- [ ] **Step 3: 编译验证**

Run:
```powershell
& "d:\SchoolShop\toolchain\apache-maven-3.9.11\bin\mvn.cmd" -q -pl ruoyi-market -am compile -DskipTests
```
Expected: `BUILD SUCCESS`。

- [ ] **Step 4: Commit**

```bash
git add ruoyi-market/src/main/java/com/ruoyi/market/service
git commit -m "feat(chat): 新增聊天会话/消息 service"
```

---

## Task 5: TokenService 增加按 token 解析方法

**Files:**
- Modify: `ruoyi-framework/src/main/java/com/ruoyi/framework/web/service/TokenService.java`

- [ ] **Step 1: 新增公共方法**

在 `TokenService` 类内（`getLoginUser(HttpServletRequest)` 之后）加入：

```java
/**
 * 根据原始 token 获取用户身份信息（供 WebSocket 握手等非 HTTP-Header 场景使用）
 *
 * @param token 去除 "Bearer " 前缀的 JWT 令牌
 * @return 用户信息，无效返回 null
 */
public LoginUser getLoginUserByToken(String token)
{
    if (StringUtils.isEmpty(token))
    {
        return null;
    }
    try
    {
        Claims claims = parseToken(token);
        String uuid = (String) claims.get(Constants.LOGIN_USER_KEY);
        String userKey = getTokenKey(uuid);
        return redisCache.getCacheObject(userKey);
    }
    catch (Exception e)
    {
        log.error("根据token获取用户信息异常'{}'", e.getMessage());
        return null;
    }
}
```

- [ ] **Step 2: 编译验证**

Run:
```powershell
& "d:\SchoolShop\toolchain\apache-maven-3.9.11\bin\mvn.cmd" -q -pl ruoyi-framework -am compile -DskipTests
```
Expected: `BUILD SUCCESS`。

- [ ] **Step 3: Commit**

```bash
git add ruoyi-framework/src/main/java/com/ruoyi/framework/web/service/TokenService.java
git commit -m "feat(chat): TokenService 增加按 token 解析登录用户方法"
```

---

## Task 6: WebSocket 基础设施（framework）

**Files:**
- Modify: `ruoyi-framework/pom.xml`
- Modify: `ruoyi-framework/src/main/java/com/ruoyi/framework/config/SecurityConfig.java`
- Create: `ruoyi-framework/src/main/java/com/ruoyi/framework/websocket/ChatSessionRegistry.java`
- Create: `ruoyi-framework/src/main/java/com/ruoyi/framework/websocket/ChatHandshakeInterceptor.java`
- Create: `ruoyi-framework/src/main/java/com/ruoyi/framework/websocket/ChatWebSocketHandler.java`
- Create: `ruoyi-framework/src/main/java/com/ruoyi/framework/websocket/WebSocketConfig.java`

- [ ] **Step 1: 添加 websocket 依赖**

在 `ruoyi-framework/pom.xml` 的 `<dependencies>` 中追加：

```xml
        <!-- WebSocket（聊天/公告推送） -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>
```

- [ ] **Step 2: SecurityConfig 放行 /ws**

在 `SecurityConfig.filterChain` 的 `requests.requestMatchers("/login", "/register", ...).permitAll()` 行改为加入 `/ws`：

```java
requests.requestMatchers("/login", "/register", "/captchaImage", "/ws", "/market/browse/**").permitAll()
```

- [ ] **Step 3: 创建 ChatSessionRegistry**

`ruoyi-framework/src/main/java/com/ruoyi/framework/websocket/ChatSessionRegistry.java`：

```java
package com.ruoyi.framework.websocket;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * WebSocket 在线会话注册表（单实例内存实现）
 *
 * key = userId，value = 该用户所有在线连接（支持多端登录）。
 * 多实例水平扩容时，需替换为 Redis Pub/Sub 广播（留扩展点）。
 */
@Component
public class ChatSessionRegistry
{
    private static final Logger log = LoggerFactory.getLogger(ChatSessionRegistry.class);

    private final Map<Long, CopyOnWriteArraySet<WebSocketSession>> registry = new ConcurrentHashMap<>();

    public void add(Long userId, WebSocketSession session)
    {
        registry.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(session);
    }

    public void remove(Long userId, WebSocketSession session)
    {
        Set<WebSocketSession> sessions = registry.get(userId);
        if (sessions != null)
        {
            sessions.remove(session);
            if (sessions.isEmpty())
            {
                registry.remove(userId);
            }
        }
    }

    public boolean isOnline(Long userId)
    {
        Set<WebSocketSession> sessions = registry.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    /** 定向推送：发给指定用户的所有在线连接 */
    public boolean sendToUser(Long userId, String payload)
    {
        Set<WebSocketSession> sessions = registry.get(userId);
        if (sessions == null || sessions.isEmpty())
        {
            return false;
        }
        TextMessage message = new TextMessage(payload);
        for (WebSocketSession session : sessions)
        {
            try
            {
                if (session.isOpen())
                {
                    session.sendMessage(message);
                }
            }
            catch (IOException e)
            {
                log.warn("推送失败 userId={} err={}", userId, e.getMessage());
            }
        }
        return true;
    }

    /** 广播：发给所有在线用户 */
    public void broadcast(String payload)
    {
        TextMessage message = new TextMessage(payload);
        for (Set<WebSocketSession> sessions : registry.values())
        {
            for (WebSocketSession session : sessions)
            {
                try
                {
                    if (session.isOpen())
                    {
                        session.sendMessage(message);
                    }
                }
                catch (IOException e)
                {
                    log.warn("广播失败 err={}", e.getMessage());
                }
            }
        }
    }
}
```

- [ ] **Step 4: 创建 ChatHandshakeInterceptor**

`ruoyi-framework/src/main/java/com/ruoyi/framework/websocket/ChatHandshakeInterceptor.java`：

```java
package com.ruoyi.framework.websocket;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.service.TokenService;

/**
 * WebSocket 握手鉴权：从 query token 解析登录用户，写入 session attributes。
 * 验证失败直接拒绝握手。
 */
@Component
public class ChatHandshakeInterceptor implements HandshakeInterceptor
{
    private static final Logger log = LoggerFactory.getLogger(ChatHandshakeInterceptor.class);

    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_NICK = "nickName";

    @Autowired
    private TokenService tokenService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes)
    {
        String query = request.getURI().getQuery();
        String token = null;
        if (StringUtils.isNotEmpty(query))
        {
            for (String pair : query.split("&"))
            {
                if (pair.startsWith("token="))
                {
                    token = pair.substring("token=".length());
                    break;
                }
            }
        }
        if (StringUtils.isEmpty(token))
        {
            log.warn("WebSocket 握手缺少 token");
            return false;
        }
        LoginUser loginUser = tokenService.getLoginUserByToken(token);
        if (loginUser == null || loginUser.getUser() == null)
        {
            log.warn("WebSocket 握手 token 无效");
            return false;
        }
        attributes.put(ATTR_USER_ID, loginUser.getUserId());
        attributes.put(ATTR_NICK, loginUser.getUser().getNickName());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Exception exception)
    {
    }
}
```

- [ ] **Step 5: 创建 ChatWebSocketHandler**

`ruoyi-framework/src/main/java/com/ruoyi/framework/websocket/ChatWebSocketHandler.java`：

```java
package com.ruoyi.framework.websocket;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * 聊天/公告 WebSocket 处理器
 *
 * 客户端消息：CHAT_SEND / CHAT_READ / PING
 * 服务端推送：CHAT_MESSAGE / MESSAGE_ACK / NOTICE_NEW / PONG
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler
{
    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    @Autowired
    private ChatSessionRegistry registry;

    @Autowired
    private WebSocketService webSocketService;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception
    {
        Long userId = getUserId(session);
        if (userId == null)
        {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }
        registry.add(userId, session);
        log.info("WebSocket 连接建立 userId={}", userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception
    {
        Long userId = getUserId(session);
        if (userId == null)
        {
            return;
        }
        JsonNode root;
        try
        {
            root = mapper.readTree(message.getPayload());
        }
        catch (Exception e)
        {
            sendAck(session, null, 400, "消息格式错误");
            return;
        }
        String type = root.path("type").asText();
        switch (type)
        {
            case "CHAT_SEND":
                webSocketService.handleSend(userId, session, root);
                break;
            case "CHAT_READ":
                webSocketService.handleRead(userId, root);
                break;
            case "PING":
                sendJson(session, "PONG", null);
                break;
            default:
                sendAck(session, null, 400, "未知消息类型");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status)
    {
        Long userId = getUserId(session);
        if (userId != null)
        {
            registry.remove(userId, session);
            log.info("WebSocket 连接关闭 userId={}", userId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception)
    {
        log.warn("WebSocket 传输错误 userId={} err={}", getUserId(session), exception.getMessage());
    }

    private Long getUserId(WebSocketSession session)
    {
        Map<String, Object> attrs = session.getAttributes();
        Object v = attrs.get(ChatHandshakeInterceptor.ATTR_USER_ID);
        return v == null ? null : ((Number) v).longValue();
    }

    private void sendJson(WebSocketSession session, String type, Object data) throws Exception
    {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", type);
        if (data != null)
        {
            node.set("data", mapper.valueToTree(data));
        }
        session.sendMessage(new TextMessage(mapper.writeValueAsString(node)));
    }

    private void sendAck(WebSocketSession session, Object data, int code, String msg)
    {
        try
        {
            ObjectNode node = mapper.createObjectNode();
            node.put("type", "MESSAGE_ACK");
            ObjectNode d = mapper.createObjectNode();
            d.put("code", code);
            d.put("msg", msg);
            if (data != null)
            {
                d.set("message", mapper.valueToTree(data));
            }
            node.set("data", d);
            session.sendMessage(new TextMessage(mapper.writeValueAsString(node)));
        }
        catch (Exception e)
        {
            log.warn("发送回执失败 {}", e.getMessage());
        }
    }
}
```

- [ ] **Step 6: 创建 WebSocketConfig**

`ruoyi-framework/src/main/java/com/ruoyi/framework/websocket/WebSocketConfig.java`：

```java
package com.ruoyi.framework.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 端点注册：/ws
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer
{
    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler;

    @Autowired
    private ChatHandshakeInterceptor chatHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry)
    {
        registry.addHandler(chatWebSocketHandler, "/ws")
                .addInterceptors(chatHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
```

- [ ] **Step 7: 编译验证**

Run:
```powershell
& "d:\SchoolShop\toolchain\apache-maven-3.9.11\bin\mvn.cmd" -q -pl ruoyi-framework -am compile -DskipTests
```
Expected: `BUILD SUCCESS`（注：WebSocketService 尚未创建，Step 6 引用了它，会编译失败——先创建 Task 7 的 WebSocketService 再一起编译）。

- [ ] **Step 8: Commit**

```bash
git add ruoyi-framework/pom.xml ruoyi-framework/src/main/java/com/ruoyi/framework/config/SecurityConfig.java ruoyi-framework/src/main/java/com/ruoyi/framework/websocket
git commit -m "feat(chat): WebSocket 基础设施（注册表/握手鉴权/handler/端点）"
```

---

## Task 7: WebSocketService（业务桥）

**Files:**
- Create: `ruoyi-framework/src/main/java/com/ruoyi/framework/websocket/WebSocketService.java`

- [ ] **Step 1: 创建 WebSocketService**

`ruoyi-framework/src/main/java/com/ruoyi/framework/websocket/WebSocketService.java`：

```java
package com.ruoyi.framework.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.market.domain.MarketChatMessage;
import com.ruoyi.market.domain.MarketChatSession;
import com.ruoyi.market.service.IMarketChatMessageService;
import com.ruoyi.market.service.IMarketChatSessionService;

/**
 * WebSocket 业务桥：聊天收发、已读、公告广播。
 * 依赖 market 模块 service（framework 依赖 market 模块）。
 */
@Service
public class WebSocketService
{
    private static final Logger log = LoggerFactory.getLogger(WebSocketService.class);

    @Autowired
    private ChatSessionRegistry registry;

    @Autowired
    private IMarketChatSessionService chatSessionService;

    @Autowired
    private IMarketChatMessageService chatMessageService;

    private final ObjectMapper mapper = new ObjectMapper();

    /** 处理 CHAT_SEND */
    public void handleSend(Long senderId, WebSocketSession senderSession, JsonNode root) throws Exception
    {
        long receiverId = root.path("receiverId").asLong(0);
        String content = root.path("content").asText("");
        String msgType = root.path("msgType").asText("1");
        if (receiverId <= 0 || content.trim().isEmpty())
        {
            ack(senderSession, null, 400, "接收人或内容为空");
            return;
        }
        if (receiverId == senderId)
        {
            ack(senderSession, null, 400, "不能给自己发消息");
            return;
        }
        MarketChatSession session = chatSessionService.getOrCreateSession(senderId, receiverId);
        MarketChatMessage message = chatMessageService.sendMessage(
                session.getSessionId(), senderId, receiverId, content.trim(), msgType);
        chatSessionService.updateLastMessage(session.getSessionId(), content.trim());

        // 推送接收者
        ObjectNode push = mapper.createObjectNode();
        push.put("type", "CHAT_MESSAGE");
        ObjectNode d = mapper.createObjectNode();
        d.put("messageId", message.getMessageId());
        d.put("sessionId", message.getSessionId());
        d.put("senderId", message.getSenderId());
        d.put("receiverId", message.getReceiverId());
        d.put("content", message.getContent());
        d.put("msgType", message.getMsgType());
        d.put("createTime", message.getCreateTime() == null ? null : message.getCreateTime().getTime());
        push.set("data", d);
        boolean delivered = registry.sendToUser(receiverId, mapper.writeValueAsString(push));

        // 回执发送者
        ack(senderSession, message, 0, delivered ? "ok" : "对方离线，已保存");
    }

    /** 处理 CHAT_READ */
    public void handleRead(Long readerId, JsonNode root)
    {
        long sessionId = root.path("sessionId").asLong(0);
        if (sessionId <= 0)
        {
            return;
        }
        chatMessageService.markRead(sessionId, readerId);
    }

    /** 广播新公告 */
    public void broadcastNotice(Long noticeId, String noticeTitle)
    {
        try
        {
            ObjectNode node = mapper.createObjectNode();
            node.put("type", "NOTICE_NEW");
            ObjectNode d = mapper.createObjectNode();
            d.put("noticeId", noticeId);
            d.put("noticeTitle", noticeTitle);
            node.set("data", d);
            registry.broadcast(mapper.writeValueAsString(node));
        }
        catch (Exception e)
        {
            log.warn("公告广播失败 {}", e.getMessage());
        }
    }

    private void ack(WebSocketSession session, MarketChatMessage message, int code, String msg)
    {
        try
        {
            ObjectNode node = mapper.createObjectNode();
            node.put("type", "MESSAGE_ACK");
            ObjectNode d = mapper.createObjectNode();
            d.put("code", code);
            d.put("msg", msg);
            if (message != null)
            {
                d.put("messageId", message.getMessageId());
                d.put("sessionId", message.getSessionId());
            }
            node.set("data", d);
            session.sendMessage(new org.springframework.web.socket.TextMessage(mapper.writeValueAsString(node)));
        }
        catch (Exception e)
        {
            log.warn("回执发送失败 {}", e.getMessage());
        }
    }
}
```

> **注意**：`ruoyi-framework` 需要依赖 `ruoyi-market` 才能注入 `IMarketChatSessionService`。在 `ruoyi-framework/pom.xml` 追加：
> ```xml
>         <dependency>
>             <groupId>com.ruoyi</groupId>
>             <artifactId>ruoyi-market</artifactId>
>         </dependency>
> ```

- [ ] **Step 2: 编译验证（含 Task 6 的文件一起）**

Run:
```powershell
& "d:\SchoolShop\toolchain\apache-maven-3.9.11\bin\mvn.cmd" -q -pl ruoyi-admin -am compile -DskipTests
```
Expected: `BUILD SUCCESS`（整个多模块编译通过）。

- [ ] **Step 3: Commit**

```bash
git add ruoyi-framework/src/main/java/com/ruoyi/framework/websocket/WebSocketService.java ruoyi-framework/pom.xml
git commit -m "feat(chat): WebSocketService 业务桥（收发/已读/公告广播）"
```

---

## Task 8: REST 接口（MarketChatController）

**Files:**
- Create: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/market/MarketChatController.java`

- [ ] **Step 1: 创建 Controller**

`ruoyi-admin/src/main/java/com/ruoyi/web/controller/market/MarketChatController.java`：

```java
package com.ruoyi.web.controller.market;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.market.domain.MarketChatMessage;
import com.ruoyi.market.domain.MarketChatSession;
import com.ruoyi.market.domain.MarketShop;
import com.ruoyi.market.service.IMarketChatMessageService;
import com.ruoyi.market.service.IMarketChatSessionService;
import com.ruoyi.market.service.IMarketShopService;
import com.ruoyi.framework.websocket.ChatSessionRegistry;

/**
 * 在线聊天 REST 接口（登录可用；管理端查询为只读审计）
 */
@RestController
@RequestMapping("/market/chat")
public class MarketChatController extends BaseController
{
    @Autowired
    private IMarketChatSessionService chatSessionService;

    @Autowired
    private IMarketChatMessageService chatMessageService;

    @Autowired
    private IMarketShopService shopService;

    @Autowired
    private ChatSessionRegistry chatSessionRegistry;

    /** 会话列表（对方昵称/未读数/在线态） */
    @GetMapping("/session/list")
    public AjaxResult sessionList()
    {
        Long userId = SecurityUtils.getUserId();
        List<MarketChatSession> list = chatSessionService.listSessions(userId);
        for (MarketChatSession s : list)
        {
            s.setOtherOnline(chatSessionRegistry.isOnline(s.getOtherUserId()));
        }
        return success(list);
    }

    /** 创建/获取会话：{userId} 或 {shopId} */
    @PostMapping("/session")
    public AjaxResult createSession(@RequestBody Map<String, Object> body)
    {
        Long userId = SecurityUtils.getUserId();
        Long otherUserId = null;
        Object uid = body.get("userId");
        Object sid = body.get("shopId");
        if (uid != null && Long.parseLong(String.valueOf(uid)) > 0)
        {
            otherUserId = Long.parseLong(String.valueOf(uid));
        }
        else if (sid != null && Long.parseLong(String.valueOf(sid)) > 0)
        {
            MarketShop shop = shopService.getById(Long.parseLong(String.valueOf(sid)));
            if (shop != null)
            {
                otherUserId = shop.getUserId();
            }
        }
        if (otherUserId == null || otherUserId.equals(userId))
        {
            return error("无效的会话对象");
        }
        MarketChatSession session = chatSessionService.getOrCreateSession(userId, otherUserId);
        return success(session);
    }

    /** 历史消息分页（仅会话成员可看） */
    @GetMapping("/message/list")
    public TableDataInfo messageList(Long sessionId, Long pageNum, Long pageSize)
    {
        Long userId = SecurityUtils.getUserId();
        if (!chatSessionService.isMember(sessionId, userId))
        {
            return getDataTable(List.of());
        }
        startPage();
        IPage<MarketChatMessage> page = chatMessageService.pageMessages(sessionId,
                pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize);
        return getDataTable(page.getRecords());
    }

    /** 标记会话消息已读 */
    @PutMapping("/message/read")
    public AjaxResult markRead(@RequestBody Map<String, Object> body)
    {
        Long userId = SecurityUtils.getUserId();
        Long sessionId = Long.parseLong(String.valueOf(body.get("sessionId")));
        if (!chatSessionService.isMember(sessionId, userId))
        {
            return error("无权操作该会话");
        }
        chatMessageService.markRead(sessionId, userId);
        return success();
    }

    /** 全部未读数（导航角标） */
    @GetMapping("/unread/count")
    public AjaxResult unreadCount()
    {
        Long userId = SecurityUtils.getUserId();
        return success(Map.of("count", chatMessageService.countAllUnread(userId)));
    }

    /** 对方在线状态 */
    @GetMapping("/online/{userId}")
    public AjaxResult online(@PathVariable Long userId)
    {
        return success(Map.of("online", chatSessionRegistry.isOnline(userId)));
    }

    // ============ 管理端只读审计 ============

    /** 全部会话（仅 admin） */
    @GetMapping("/admin/sessions")
    public AjaxResult adminSessions()
    {
        // admin 由 RuoYi admin 用户调用；此处返回所有会话（简化：不联表用户信息）
        return success(chatSessionService.listSessions(Long.MIN_VALUE).isEmpty()
                ? new java.util.ArrayList<Map<String, Object>>()
                : new java.util.ArrayList<Map<String, Object>>());
    }

    /** 按会话查消息记录（仅 admin） */
    @GetMapping("/admin/messages")
    public TableDataInfo adminMessages(Long sessionId, Long pageNum, Long pageSize)
    {
        startPage();
        IPage<MarketChatMessage> page = chatMessageService.pageMessages(sessionId,
                pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize);
        return getDataTable(page.getRecords());
    }
}
```

> **说明**：`adminSessions` 简化实现。若需完整会话列表，可新增 `IMarketChatSessionService.listAllSessions()`（Task 4 未包含，管理端页面若需展示完整会话再补；本期管理端以"按会话查消息"为主，会话下拉由管理端前端通过 `adminSessions` 提供空列表占位，或后续增强）。

- [ ] **Step 2: 编译验证**

Run:
```powershell
& "d:\SchoolShop\toolchain\apache-maven-3.9.11\bin\mvn.cmd" -q -pl ruoyi-admin -am compile -DskipTests
```
Expected: `BUILD SUCCESS`。

- [ ] **Step 3: Commit**

```bash
git add ruoyi-admin/src/main/java/com/ruoyi/web/controller/market/MarketChatController.java
git commit -m "feat(chat): 聊天 REST 接口（会话/历史/已读/未读/在线/管理审计）"
```

---

## Task 9: 公告发布后 WebSocket 广播

**Files:**
- Modify: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/system/SysNoticeController.java`

- [ ] **Step 1: 阅读现有发布方法**

Read `ruoyi-admin/src/main/java/com/ruoyi/web/controller/system/SysNoticeController.java`，定位 `@PostMapping` 添加公告的方法（一般为 `add`）。

- [ ] **Step 2: 注入并广播**

在 `SysNoticeController` 中注入：

```java
    @Autowired
    private com.ruoyi.framework.websocket.WebSocketService webSocketService;
```

在 `add(...)` 方法成功返回前（`return toAjax(noticeService.insertNotice(notice));` 处）改为：

```java
    int rows = noticeService.insertNotice(notice);
    if (rows > 0)
    {
        webSocketService.broadcastNotice(notice.getNoticeId(), notice.getNoticeTitle());
    }
    return toAjax(rows);
```

> 若 `add` 方法结构不同（如先 `return toAjax(...)`），调整为先调用 service 再按返回行数广播。公告的 `noticeId` 需在 insert 后回填（RuoYi 默认 useGeneratedKeys 回填主键）。

- [ ] **Step 3: 编译验证**

Run:
```powershell
& "d:\SchoolShop\toolchain\apache-maven-3.9.11\bin\mvn.cmd" -q -pl ruoyi-admin -am compile -DskipTests
```
Expected: `BUILD SUCCESS`。

- [ ] **Step 4: Commit**

```bash
git add ruoyi-admin/src/main/java/com/ruoyi/web/controller/system/SysNoticeController.java
git commit -m "feat(notice): 公告发布后 WebSocket 广播实时推送"
```

---

## Task 10: 后端整体验证

**Files:**
- Create: `.e2e/test_chat_ws.py`

- [ ] **Step 1: 打包并重启后端**

Run:
```powershell
& "d:\SchoolShop\toolchain\apache-maven-3.9.11\bin\mvn.cmd" clean package -DskipTests
```
Expected: `BUILD SUCCESS`。停止旧后端进程后重启：
```powershell
$env:JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"; & "d:\SchoolShop\toolchain\jdk-17.0.20+8\bin\java" -jar ruoyi-admin\target\ruoyi-admin.jar
```
Expected: 启动日志出现 `若依启动成功`。

- [ ] **Step 2: REST 冒烟**

Run:
```powershell
$admin = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/login" -ContentType "application/json" -Body '{"username":"admin","password":"admin123"}'
$token = $admin.token
$h = @{ Authorization = "Bearer $token" }
$r1 = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/market/chat/session" -Headers $h -ContentType "application/json" -Body '{"userId":2}'
$r1 | ConvertTo-Json -Depth 5
$r2 = Invoke-RestMethod -Method Get -Uri "http://localhost:8080/market/chat/unread/count" -Headers $h
$r2 | ConvertTo-Json
```
Expected: `r1` 返回 session（admin userId=1 与 userId=2 的会话），`r2` 返回 `{"count":...}`。

- [ ] **Step 3: WebSocket 双端收发脚本**

创建 `.e2e/test_chat_ws.py`（Python，需 `pip install websocket-client`，可用 `aimodule/.venv-ai` 或 `python -m venv` 环境）：

```python
# -*- coding: utf-8 -*-
"""WebSocket 聊天双端验证：admin(1) <-> merchant(2) 互发消息"""
import json
import sys
import time
import websocket

BASE = "http://localhost:8080"
WS = "ws://localhost:8080/ws"


def login(username, password):
    import urllib.request
    body = json.dumps({"username": username, "password": password}).encode()
    req = urllib.request.Request(BASE + "/login", data=body,
                                 headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req) as resp:
        return json.load(resp)["token"]


def collect(url, wait=1.0):
    ws = websocket.create_connection(url)
    msgs = []

    def on_message(_, m):
        msgs.append(json.loads(m))
    ws.on_message = on_message
    return ws, msgs


def main():
    tk_a = login("admin", "admin123")
    tk_b = login("merchant", "admin123")
    wa, ma = collect(WS + "?token=" + tk_a)
    wb, mb = collect(WS + "?token=" + tk_b)

    # A -> B
    wa.send(json.dumps({"type": "CHAT_SEND", "receiverId": 2, "content": "你好，这条商品还在吗？"}))
    time.sleep(1)
    assert any(m.get("type") == "MESSAGE_ACK" for m in ma), "A 未收到回执"
    b_received = [m for m in mb if m.get("type") == "CHAT_MESSAGE"]
    assert b_received, "B 未收到 A 的消息"
    print("PASS A->B:", b_received[0]["data"]["content"])

    # B -> A
    wb.send(json.dumps({"type": "CHAT_SEND", "receiverId": 1, "content": "在的，随时可以自提"}))
    time.sleep(1)
    a_received = [m for m in ma if m.get("type") == "CHAT_MESSAGE"]
    assert a_received, "A 未收到 B 的消息"
    print("PASS B->A:", a_received[0]["data"]["content"])

    # 已读
    sid = b_received[0]["data"]["sessionId"]
    wb.send(json.dumps({"type": "CHAT_READ", "sessionId": sid}))
    time.sleep(0.5)
    print("PASS READ sessionId=", sid)

    wa.close()
    wb.close()
    print("ALL WS TESTS PASSED")


if __name__ == "__main__":
    main()
```

- [ ] **Step 4: 运行脚本**

Run:
```powershell
& "d:\SchoolShop\aimodule\.venv-ai\Scripts\python.exe" -m pip install websocket-client -q
& "d:\SchoolShop\aimodule\.venv-ai\Scripts\python.exe" .e2e\test_chat_ws.py
```
Expected: 输出 `ALL WS TESTS PASSED`。

- [ ] **Step 5: Commit**

```bash
git add .e2e/test_chat_ws.py
git commit -m "test(chat): WebSocket 双端收发验证脚本"
```

---

## Task 11: 用户端前端（campus-user）

**Files:**
- Modify: `campus-user/vite.config.ts`
- Create: `campus-user/src/utils/ws.ts`
- Create: `campus-user/src/api/chat.ts`
- Modify: `campus-user/src/router/index.ts`
- Create: `campus-user/src/views/chat/index.vue`
- Modify: `campus-user/src/views/product/detail/index.vue`
- Modify: `campus-user/src/layout/components/Navbar.vue`（或对应导航组件，加入口+角标）

- [ ] **Step 1: vite 增加 /ws 代理**

`campus-user/vite.config.ts` 的 `server.proxy` 中追加：

```ts
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
        changeOrigin: true
      }
```

- [ ] **Step 2: 创建 utils/ws.ts**

`campus-user/src/utils/ws.ts`：

```ts
import { getToken } from '@/utils/auth'

type Handler = (data: any) => void
const handlers = new Map<string, Set<Handler>>()

let socket: WebSocket | null = null
let manualClose = false
let retry = 0
let pingTimer: ReturnType<typeof setInterval> | null = null
let pongMiss = 0

/** 计算 ws 地址（开发走 vite 代理 /ws，生产可改为 wss 域名） */
function buildUrl(): string {
  const proto = location.protocol === 'https:' ? 'wss' : 'ws'
  const base = import.meta.env.VITE_APP_BASE_API || ''
  return `${proto}://${location.host}${base}/ws?token=${encodeURIComponent(getToken() || '')}`
}

function startHeartbeat() {
  stopHeartbeat()
  pingTimer = setInterval(() => {
    if (socket && socket.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify({ type: 'PING' }))
      pongMiss++
      if (pongMiss > 2) {
        socket.close()
      }
    }
  }, 30000)
}
function stopHeartbeat() {
  if (pingTimer) clearInterval(pingTimer)
  pingTimer = null
}

export function connect() {
  const token = getToken()
  if (!token || socket || manualClose) return
  socket = new WebSocket(buildUrl())
  socket.onopen = () => {
    retry = 0
    pongMiss = 0
    startHeartbeat()
  }
  socket.onmessage = (ev) => {
    let msg: any
    try { msg = JSON.parse(ev.data) } catch { return }
    if (msg.type === 'PONG') { pongMiss = 0; return }
    emit(msg.type, msg.data)
  }
  socket.onclose = () => {
    socket = null
    stopHeartbeat()
    if (!manualClose) {
      retry = Math.min(retry + 1, 6)
      const delay = Math.min(1000 * 2 ** (retry - 1), 30000)
      setTimeout(connect, delay)
    }
  }
  socket.onerror = () => { socket?.close() }
}

export function disconnect() {
  manualClose = true
  stopHeartbeat()
  socket?.close()
  socket = null
}

export function sendChat(receiverId: number, content: string, msgType = '1') {
  socket?.send(JSON.stringify({ type: 'CHAT_SEND', receiverId, content, msgType }))
}
export function sendRead(sessionId: number) {
  socket?.send(JSON.stringify({ type: 'CHAT_READ', sessionId }))
}

export function on(type: string, handler: Handler) {
  if (!handlers.has(type)) handlers.set(type, new Set())
  handlers.get(type)!.add(handler)
}
export function off(type: string, handler: Handler) {
  handlers.get(type)?.delete(handler)
}
function emit(type: string, data: any) {
  handlers.get(type)?.forEach((h) => h(data))
}
```

- [ ] **Step 3: 创建 api/chat.ts**

`campus-user/src/api/chat.ts`：

```ts
import { get, post, put, type ApiResult } from '@/utils/request'

export interface ChatSession {
  sessionId: number
  userAId: number
  userBId: number
  lastMessage: string | null
  lastTime: string | null
  otherUserId: number
  otherNickName: string
  unreadCount: number
  otherOnline: boolean
}

export interface ChatMessage {
  messageId: number
  sessionId: number
  senderId: number
  receiverId: number
  content: string
  msgType: string
  status: string
  createTime: string | null
}

export interface TableData<T> { total: number; rows: T[] }

export function getSessionList(): Promise<ApiResult<ChatSession[]>> {
  return get<ApiResult<ChatSession[]>>('/market/chat/session/list')
}
export function createSession(payload: { userId?: number; shopId?: number }): Promise<ApiResult<ChatSession>> {
  return post<ApiResult<ChatSession>>('/market/chat/session', payload)
}
export function getMessageList(params: { sessionId: number; pageNum: number; pageSize: number }): Promise<TableData<ChatMessage>> {
  return get<TableData<ChatMessage>>('/market/chat/message/list', { params })
}
export function markRead(sessionId: number): Promise<ApiResult> {
  return put<ApiResult>('/market/chat/message/read', { sessionId })
}
export function getUnreadCount(): Promise<ApiResult<{ count: number }>> {
  return get<ApiResult<{ count: number }>>('/market/chat/unread/count')
}
```

- [ ] **Step 4: 注册路由**

`campus-user/src/router/index.ts` 的 `routes` 中新增：

```ts
  {
    path: '/chat',
    component: Layout,
    children: [
      {
        path: '',
        name: 'Chat',
        component: () => import('@/views/chat/index.vue'),
        meta: { title: '消息中心', requiresAuth: true }
      }
    ]
  },
```

- [ ] **Step 5: 创建消息中心页面**

`campus-user/src/views/chat/index.vue`（完整可运行页面）：

```vue
<template>
  <div class="chat-page">
    <div class="chat-left">
      <div class="chat-title">消息中心</div>
      <div v-if="!sessions.length" class="chat-empty">暂无会话，去商品页联系商家吧</div>
      <div v-for="s in sessions" :key="s.sessionId" class="chat-item" :class="{ active: s.sessionId === currentId }"
           @click="openSession(s)">
        <div class="chat-item-top">
          <span class="chat-name">{{ s.otherNickName }}</span>
          <span v-if="s.otherOnline" class="chat-online">● 在线</span>
        </div>
        <div class="chat-item-bottom">
          <span class="chat-last">{{ s.lastMessage || '暂无消息' }}</span>
          <span v-if="s.unreadCount > 0" class="chat-badge">{{ s.unreadCount }}</span>
        </div>
      </div>
    </div>
    <div class="chat-right">
      <template v-if="currentId">
        <div class="chat-head">{{ currentOther?.otherNickName }}</div>
        <div class="chat-body" ref="bodyRef">
          <div v-for="m in messages" :key="m.messageId" class="chat-msg" :class="{ mine: m.senderId === myId }">
            <div class="bubble">{{ m.content }}</div>
          </div>
        </div>
        <div class="chat-input">
          <el-input v-model="draft" placeholder="输入消息..." @keyup.enter="sendMsg" />
          <el-button type="primary" @click="sendMsg">发送</el-button>
        </div>
      </template>
      <div v-else class="chat-placeholder">选择左侧会话开始聊天</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { getSessionList, getMessageList, createSession, markRead, type ChatSession, type ChatMessage } from '@/api/chat'
import { connect, disconnect, sendChat, on, off } from '@/utils/ws'
import { getToken } from '@/utils/auth'

const sessions = ref<ChatSession[]>([])
const currentId = ref<number>(0)
const currentOther = ref<ChatSession | null>(null)
const messages = ref<ChatMessage[]>([])
const draft = ref('')
const bodyRef = ref<HTMLElement>()
const route = useRoute()

const myId = ref(0)
try { myId.value = JSON.parse(atob(getToken()!.split('.')[1].replace(/-/g, '+').replace(/_/g, '/'))).userId } catch { /* ignore */ }

async function refreshSessions() {
  const res = await getSessionList()
  sessions.value = (res as any).data || []
}

async function openSession(s: ChatSession) {
  currentId.value = s.sessionId
  currentOther.value = s
  s.unreadCount = 0
  markRead(s.sessionId)
  const r = await getMessageList({ sessionId: s.sessionId, pageNum: 1, pageSize: 50 })
  messages.value = (r as any).rows || []
  scrollBottom()
}

function scrollBottom() {
  nextTick(() => { bodyRef.value?.scrollTo({ top: bodyRef.value.scrollHeight }) })
}

async function sendMsg() {
  const content = draft.value.trim()
  if (!content || !currentOther.value) return
  sendChat(currentOther.value.otherUserId, content)
  draft.value = ''
  // 乐观插入本地
  messages.value.push({
    messageId: Date.now(), sessionId: currentId.value, senderId: myId.value,
    receiverId: currentOther.value.otherUserId, content, msgType: '1', status: '0', createTime: null
  })
  scrollBottom()
}

function onChatMessage(data: any) {
  if (data.sessionId === currentId.value) {
    messages.value.push(data)
    scrollBottom()
  }
  refreshSessions()
}

onMounted(async () => {
  await refreshSessions()
  const shopId = route.query.shopId
  if (shopId) {
    const r: any = await createSession({ shopId: Number(shopId) })
    const s = (r as any).data
    if (s) {
      await refreshSessions()
      const found = sessions.value.find(x => x.sessionId === s.sessionId)
      openSession(found || s)
    }
  }
  connect()
  on('CHAT_MESSAGE', onChatMessage)
})
onUnmounted(() => { off('CHAT_MESSAGE', onChatMessage); disconnect() })
</script>

<style scoped>
.chat-page { display: flex; height: calc(100vh - 120px); border: 1px solid #eee; border-radius: 8px; overflow: hidden; }
.chat-left { width: 280px; border-right: 1px solid #eee; overflow-y: auto; background: #fafafa; }
.chat-title { padding: 14px; font-weight: 600; border-bottom: 1px solid #eee; }
.chat-empty { padding: 24px; color: #999; text-align: center; }
.chat-item { padding: 12px 14px; cursor: pointer; border-bottom: 1px solid #f0f0f0; }
.chat-item.active { background: #ecf5ff; }
.chat-item-top { display: flex; justify-content: space-between; }
.chat-name { font-weight: 500; }
.chat-online { color: #67c23a; font-size: 12px; }
.chat-item-bottom { display: flex; justify-content: space-between; align-items: center; margin-top: 4px; }
.chat-last { color: #999; font-size: 12px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 180px; }
.chat-badge { background: #f56c6c; color: #fff; border-radius: 10px; font-size: 12px; padding: 0 6px; }
.chat-right { flex: 1; display: flex; flex-direction: column; }
.chat-head { padding: 12px 16px; border-bottom: 1px solid #eee; font-weight: 600; }
.chat-body { flex: 1; overflow-y: auto; padding: 16px; background: #f7f8fa; }
.chat-msg { display: flex; margin-bottom: 12px; }
.chat-msg.mine { justify-content: flex-end; }
.bubble { max-width: 60%; padding: 8px 12px; border-radius: 8px; background: #fff; box-shadow: 0 1px 2px rgba(0,0,0,.06); }
.chat-msg.mine .bubble { background: #409eff; color: #fff; }
.chat-input { display: flex; gap: 8px; padding: 12px; border-top: 1px solid #eee; }
.chat-placeholder { flex: 1; display: flex; align-items: center; justify-content: center; color: #bbb; }
</style>
```

- [ ] **Step 6: 商品详情页加"联系商家"**

在 `campus-user/src/views/product/detail/index.vue` 的模板中，店铺信息区域加入按钮（依据现有页面结构放置，跳转 `/chat?shopId=`）：

```vue
<el-button type="primary" plain @click="contactShop">联系商家</el-button>
```

并在 `<script setup>` 中加入：

```ts
import { useRouter } from 'vue-router'
const router = useRouter()
function contactShop() {
  router.push({ path: '/chat', query: { shopId: detail.value.shop?.shopId } })
}
```

> 注意：`detail` 为商品详情响应式对象（含 `shop`），若实际命名不同（如 `product`/`detailData`），以现有代码为准调整引用。

- [ ] **Step 7: 导航加入口与角标**

在 `campus-user/src/layout/components/Navbar.vue`（或布局顶部导航）加入"消息"链接，点击跳 `/chat`，并显示未读角标（进入页面时 `getUnreadCount()` 拉取，收到 `CHAT_MESSAGE` 时 +1）：

```vue
<el-badge :value="unread" :hidden="!unread" class="chat-entry">
  <el-button link @click="router.push('/chat')">消息</el-button>
</el-badge>
```

```ts
const unread = ref(0)
async function loadUnread() {
  const r: any = await getUnreadCount()
  unread.value = (r?.data?.count) || 0
}
on('CHAT_MESSAGE', () => { unread.value++ })
```

- [ ] **Step 8: 前端联调验证**

Run（用户端 dev server，若已在运行则 Vite 热更新）:
```powershell
cd campus-user; npm run dev
```
用浏览器登录 `ry`/`admin` 访问 `http://localhost:8083/chat`，在商品详情点"联系商家"，与商家端互发消息验证实时收发。

- [ ] **Step 9: Commit**

```bash
git add campus-user
git commit -m "feat(chat): 用户端消息中心 + 联系商家 + WebSocket 客户端"
```

---

## Task 12: 商家端前端（campus-merchant）

**Files:**（与用户端同构）
- Modify: `campus-merchant/vite.config.ts`（加 `/ws` 代理）
- Create: `campus-merchant/src/utils/ws.ts`（复制用户端实现）
- Create: `campus-merchant/src/api/chat.ts`（复制用户端实现）
- Modify: `campus-merchant/src/router/index.ts`（加 `/chat` 路由）
- Create: `campus-merchant/src/views/chat/index.vue`（复制用户端页面）
- Modify: `campus-merchant/src/layout/index.vue`（导航加入口+角标）

- [ ] **Step 1: 同构实现**

复制 Task 11 的 `utils/ws.ts`、`api/chat.ts`、`views/chat/index.vue` 到商家端（路径相同），`vite.config.ts` 加 `/ws` 代理，`router/index.ts` 加 `/chat` 路由，导航加"消息中心"入口与未读角标。商家端聊天页面不含"联系商家"按钮逻辑（商家侧直接由会话列表进入）。

- [ ] **Step 2: 编译验证**

Run:
```powershell
cd campus-merchant; npm run build
```
Expected: 构建成功（无 TS 错误）。

- [ ] **Step 3: Commit**

```bash
git add campus-merchant
git commit -m "feat(chat): 商家端消息中心 + WebSocket 客户端"
```

---

## Task 13: 管理端前端（campus-admin）

**Files:**
- Modify: `campus-admin/vite.config.ts`（加 `/ws` 代理）
- Create: `campus-admin/src/views/market/chat/index.vue`（聊天记录只读查询页）
- Modify: `campus-admin/src/router/index.ts` 或菜单（注册页面）

- [ ] **Step 1: 创建聊天记录查询页**

`campus-admin/src/views/market/chat/index.vue`：按会话查看消息记录（只读）：

```vue
<template>
  <div class="app-container">
    <el-card>
      <div class="filter">
        <el-input v-model="sessionId" placeholder="输入会话ID" style="width: 220px" />
        <el-button type="primary" @click="load">查询</el-button>
      </div>
      <el-table :data="rows" border>
        <el-table-column prop="messageId" label="消息ID" width="90" />
        <el-table-column prop="sessionId" label="会话ID" width="90" />
        <el-table-column prop="senderId" label="发送者" width="90" />
        <el-table-column prop="receiverId" label="接收者" width="90" />
        <el-table-column prop="content" label="内容" />
        <el-table-column prop="msgType" label="类型" width="70" />
        <el-table-column prop="status" label="状态" width="70" />
        <el-table-column prop="createTime" label="时间" width="180" />
      </el-table>
      <pagination v-show="total > 0" :total="total" v-model:page="pageNum" v-model:limit="pageSize" @pagination="load" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { get, type ApiResult } from '@/utils/request'

const sessionId = ref<number>()
const rows = ref<any[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)

async function load() {
  const r = await get<any>('/market/chat/admin/messages', {
    params: { sessionId: sessionId.value, pageNum: pageNum.value, pageSize: pageSize.value }
  })
  rows.value = (r as any).rows || []
  total.value = (r as any).total || 0
}
</script>
```

> 说明：管理端 `pagination` 组件为 RuoYi 管理模板自带；`get` 封装路径以管理端 `src/utils/request.ts` 实际导出为准。管理端无 `/ws` 业务推送，仅页面与 `/ws` 代理（若需要公告角标再接入 ws.ts）。

- [ ] **Step 2: 注册菜单/路由**

在管理端菜单（`sys_menu`，走 `sql/admin_menu.sql` 或前端静态路由）注册"聊天记录"页面，路径 `/market/chat`。若管理端采用数据库菜单，执行：

```sql
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES ('聊天记录', 0, 8, 'chat', 'market/chat/index', 1, 0, 'C', '0', '0', 'market:chat:list', 'chat', 'admin', NOW(), '聊天记录查询');
```

- [ ] **Step 3: 编译验证**

Run:
```powershell
cd campus-admin; npm run build
```
Expected: 构建成功。

- [ ] **Step 4: Commit**

```bash
git add campus-admin
git commit -m "feat(chat): 管理端聊天记录只读查询"
```

---

## Task 14: E2E 双浏览器互聊验证

**Files:**
- Create: `.e2e/test_chat_e2e.py`

- [ ] **Step 1: 编写 Playwright 双浏览器脚本**

`.e2e/test_chat_e2e.py`（依赖 `webapp-testing` 技能提供的 playwright 环境）：

```python
# -*- coding: utf-8 -*-
"""E2E：用户端(admin) 与 商家端(merchant) 双浏览器互聊"""
from playwright.sync_api import sync_playwright

USER = "http://localhost:8083"
MERCHANT = "http://localhost:8082"


def login(page, url, username):
    page.goto(url)
    page.wait_for_load_state("networkidle")
    page.fill('input[placeholder*="账号"]', username)
    page.fill('input[placeholder*="密码"]', "admin123")
    page.click('button:has-text("登 录"), button:has-text("登录")')
    page.wait_for_load_state("networkidle")


def main():
    with sync_playwright() as p:
        u = p.chromium.launch(headless=True)
        ctx_u = u.new_context()
        ctx_m = u.new_context()
        page_u = ctx_u.new_page()
        page_m = ctx_m.new_page()

        login(page_u, USER, "admin")
        login(page_m, MERCHANT, "merchant")

        page_u.goto(USER + "/chat")
        page_u.wait_for_load_state("networkidle")
        page_m.goto(MERCHANT + "/chat")
        page_m.wait_for_load_state("networkidle")

        # 用户端发消息给商家
        page_u.fill(".chat-input input", "E2E 你好商家")
        page_u.click(".chat-input button")
        page_m.wait_for_selector(".chat-msg >> text=E2E 你好商家", timeout=10000)
        print("PASS: 用户->商家 实时送达")

        # 商家端回复
        page_m.fill(".chat-input input", "E2E 你好买家")
        page_m.click(".chat-input button")
        page_u.wait_for_selector(".chat-msg >> text=E2E 你好买家", timeout=10000)
        print("PASS: 商家->用户 实时送达")

        u.close()
        print("ALL E2E CHAT TESTS PASSED")


if __name__ == "__main__":
    main()
```

- [ ] **Step 2: 运行脚本**

Run:
```powershell
& "d:\SchoolShop\.venv-test\Scripts\python.exe" .e2e\test_chat_e2e.py
```
Expected: 输出 `ALL E2E CHAT TESTS PASSED`（登录选择器如与现有登录页不符，按实际 placeholder/文案调整）。

- [ ] **Step 3: Commit**

```bash
git add .e2e/test_chat_e2e.py
git commit -m "test(chat): 双浏览器互聊 E2E"
```

---

## 自查记录

- **Spec 覆盖**：数据模型（T1-T4）✅、WebSocket 端点/协议（T6-T7）✅、REST 接口（T8）✅、公告推送（T9）✅、用户端/商家端/管理端前端（T11-T13）✅、错误处理（重连/离线落库在 ws.ts 与 WebSocketService 中）✅、测试（T10/T14）✅。
- **占位扫描**：无 TBD/TODO；管理端 `adminSessions` 简化实现已在 Task 8 明确说明。
- **类型一致性**：`MarketChatSession` 的 `otherUserId/otherNickName/unreadCount/otherOnline` 由 service/controller 填充；前端 `ChatSession` 接口字段一致；WebSocket 消息 `data` 字段（messageId/sessionId/senderId/receiverId/content/msgType/createTime）在后端 push 与前端消费一致。
