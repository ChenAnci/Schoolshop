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
