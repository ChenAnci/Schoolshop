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
