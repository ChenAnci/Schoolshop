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
