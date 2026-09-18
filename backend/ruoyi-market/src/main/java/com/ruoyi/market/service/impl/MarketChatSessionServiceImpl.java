package com.ruoyi.market.service.impl;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.market.domain.MarketChatSession;
import com.ruoyi.market.mapper.MarketChatSessionMapper;
import com.ruoyi.market.service.IMarketChatMessageService;
import com.ruoyi.market.service.IMarketChatSessionService;
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
