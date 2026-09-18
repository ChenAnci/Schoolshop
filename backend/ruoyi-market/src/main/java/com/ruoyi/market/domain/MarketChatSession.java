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
