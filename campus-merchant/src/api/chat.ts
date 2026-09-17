import { get, post, put, type ApiResult } from '@/utils/request'

export interface ChatSession {
  sessionId: number
  userAId: number
  userBId: number
  lastMessage: string | null
  lastTime: string | null
  otherUserId: number
  otherNickName: string
  unreadCount: number
  otherOnline: boolean
}

export interface ChatMessage {
  messageId: number
  sessionId: number
  senderId: number
  receiverId: number
  content: string
  msgType: string
  status: string
  createTime: string | null
}

/** RuoYi TableDataInfo 分页结构 */
export interface TableData<T> { total: number; rows: T[] }

/** 会话列表（对方昵称/未读数/在线态） */
export function getSessionList(): Promise<ApiResult<ChatSession[]>> {
  return get<ApiResult<ChatSession[]>>('/market/chat/session/list')
}

/** 创建/获取会话：传 userId 或 shopId */
export function createSession(payload: { userId?: number; shopId?: number }): Promise<ApiResult<ChatSession>> {
  return post<ApiResult<ChatSession>>('/market/chat/session', payload)
}

/** 历史消息分页（message_id 倒序） */
export function getMessageList(params: { sessionId: number; pageNum: number; pageSize: number }): Promise<TableData<ChatMessage>> {
  return get<TableData<ChatMessage>>('/market/chat/message/list', { params })
}

/** 标记会话消息已读 */
export function markRead(sessionId: number): Promise<ApiResult> {
  return put<ApiResult>('/market/chat/message/read', { sessionId })
}

/** 全部未读数（导航角标） */
export function getUnreadCount(): Promise<ApiResult<{ count: number }>> {
  return get<ApiResult<{ count: number }>>('/market/chat/unread/count')
}
