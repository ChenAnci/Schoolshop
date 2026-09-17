import { get, put } from '@/utils/request'
import type { ApiResult } from '@/utils/request'

/** 订单（对应后端 MarketOrder） */
export interface ShopOrder {
  orderId: number
  orderNo: string
  userId: number
  shopId: number
  shopName: string
  userName: string
  totalAmount: number
  payAmount: number
  status: string
  receiverName?: string
  receiverPhone?: string
  receiverAddress?: string
  takeTime?: string
  finishTime?: string
  cancelTime?: string
  createTime?: string
  remark?: string
}

/** 订单明细（对应后端 MarketOrderItem） */
export interface ShopOrderItem {
  itemId: number
  orderId: number
  orderNo: string
  productId: number
  productName: string
  productImage: string | null
  price: number
  quantity: number
  subtotal: number
}

/** 订单状态字典（开发文档 §7：0待商家接单 1待自提 2已完成 3已取消 4审查中 5审查完成） */
export const ORDER_STATUS: Record<string, { text: string; type: 'primary' | 'warning' | 'success' | 'info' | 'danger' }> = {
  '0': { text: '待商家接单', type: 'warning' },
  '1': { text: '待自提', type: 'primary' },
  '2': { text: '已完成', type: 'success' },
  '3': { text: '已取消', type: 'info' },
  '4': { text: '审查中', type: 'danger' },
  '5': { text: '审查完成', type: 'info' }
}

/** RuoYi TableDataInfo 分页结构（list 接口顶层返回 total/rows） */
export interface TableData<T> {
  total: number
  rows: T[]
}

/** 商家订单列表（可按状态筛选） */
export function getShopOrderList(status?: string): Promise<TableData<ShopOrder>> {
  return get<TableData<ShopOrder>>('/market/shop/order/list', {
    params: status ? { status } : {}
  })
}

/** 商家订单详情（含明细） */
export function getShopOrderDetail(orderId: number): Promise<ApiResult<{ order: ShopOrder; items: ShopOrderItem[] }>> {
  return get<ApiResult<{ order: ShopOrder; items: ShopOrderItem[] }>>(`/market/shop/order/${orderId}`)
}

/** 接单：0 → 1 */
export function acceptOrder(orderId: number): Promise<ApiResult> {
  return put<ApiResult>(`/market/shop/order/${orderId}/accept`)
}

/** 拒单/取消：0 → 3 */
export function cancelShopOrder(orderId: number): Promise<ApiResult> {
  return put<ApiResult>(`/market/shop/order/${orderId}/cancel`)
}
