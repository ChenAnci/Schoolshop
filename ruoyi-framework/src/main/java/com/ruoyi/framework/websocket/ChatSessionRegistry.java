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
