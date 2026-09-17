import { get } from '@/utils/request'

/** 聊天消息（对应后端 MarketChatMessage） */
export interface ChatMessage {
  messageId?: number
  sessionId?: number
  senderId?: number
  receiverId?: number
  content?: string
  /** 消息类型：1文本 2商品卡片 3图片 */
  msgType?: string
  /** 状态：0未读 1已读 */
  status?: string
  createTime?: string
}

/** 分页列表返回（后端 TableDataInfo：rows/total） */
export interface TableData<T = unknown> {
  rows: T[]
  total: number
}

/** 按会话查询消息记录（管理端只读审计） */
export function listAdminMessages(params: { sessionId?: number; pageNum: number; pageSize: number }): Promise<TableData<ChatMessage>> {
  return get<TableData<ChatMessage>>('/market/chat/admin/messages', { params })
}
