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
