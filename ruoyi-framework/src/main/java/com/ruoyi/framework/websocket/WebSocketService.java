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
