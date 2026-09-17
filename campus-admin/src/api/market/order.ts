import { get, put } from '@/utils/request'
import type { ApiResult } from '@/utils/request'

/** 订单信息（对应后端 MarketOrder） */
export interface OrderData {
  orderId?: number
  orderNo?: string
  userId?: number
  shopId?: number
  totalAmount?: number | string
  payAmount?: number | string
  /** 0待商家接单 1待自提 2已完成 3已取消 4审查中 5审查完成 */
  status?: string
  /** 审查位 0未审查 1审查中 2已仲裁 */
  auditFlag?: string
  auditRemark?: string
  receiverName?: string
  receiverPhone?: string
  receiverAddress?: string
  userName?: string
  shopName?: string
  createTime?: string
  cancelTime?: string
  finishTime?: string
  updateBy?: string
  remark?: string
}

/** 分页列表返回（后端 TableDataInfo：rows/total） */
export interface TableData<T = unknown> {
  rows: T[]
  total: number
}

/** 订单状态字典（含 4审查中 5审查完成） */
export const ORDER_STATUS: Record<string, { text: string; type: 'primary' | 'success' | 'info' | 'danger' | 'warning' }> = {
  '0': { text: '待商家接单', type: 'warning' },
  '1': { text: '待自提', type: 'primary' },
  '2': { text: '已完成', type: 'success' },
  '3': { text: '已取消', type: 'info' },
  '4': { text: '审查中', type: 'danger' },
  '5': { text: '审查完成', type: 'info' }
}

/** 审查位字典 */
export const AUDIT_FLAG: Record<string, { text: string; type: 'primary' | 'success' | 'danger' | 'info' }> = {
  '0': { text: '未审查', type: 'info' },
  '1': { text: '审查中', type: 'danger' },
  '2': { text: '已仲裁', type: 'success' }
}

/** 查询订单列表（支持状态/审查位筛选） */
export function listOrder(params: Record<string, unknown>): Promise<TableData<OrderData>> {
  return get<TableData<OrderData>>('/market/order/list', { params })
}

/** 进入审查中（status=4） */
export function startAudit(orderId: number, reason?: string): Promise<ApiResult> {
  return put<ApiResult>(`/market/admin/order/${orderId}/start-audit`, null, { params: reason ? { reason } : {} })
}

/** 仲裁：action complete(强制完成)/cancel(强制取消)/restore(恢复流转) */
export function arbitrate(orderId: number, action: string, remark?: string): Promise<ApiResult> {
  return put<ApiResult>(`/market/admin/order/${orderId}/arbitrate`, { action, remark })
}