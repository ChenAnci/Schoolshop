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
