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
