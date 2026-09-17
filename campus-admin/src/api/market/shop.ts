import { get, post, put, del } from '@/utils/request'
import type { ApiResult } from '@/utils/request'

/** 店铺信息 */
export interface ShopData {
  shopId?: number
  shopName: string
  shopLogo?: string
  shopDesc?: string
  contactPhone?: string
  status?: string
  createTime?: string
  productCount?: number
  remark?: string
}

/** 分页列表返回（后端 TableDataInfo：rows/total） */
export interface TableData<T = unknown> {
  rows: T[]
  total: number
}

/** 查询店铺列表 */
export function listShop(params: Record<string, unknown>): Promise<TableData<ShopData>> {
  return get<TableData<ShopData>>('/market/shop/list', { params })
}

/** 获取店铺详情 */
export function getShop(shopId: number): Promise<ApiResult<ShopData>> {
  return get<ApiResult<ShopData>>(`/market/shop/${shopId}`)
}

/** 新增店铺 */
export function addShop(data: Partial<ShopData>): Promise<ApiResult> {
  return post<ApiResult>('/market/shop', data)
}

/** 修改店铺 */
export function updateShop(data: Partial<ShopData>): Promise<ApiResult> {
  return put<ApiResult>('/market/shop', data)
}

/** 删除店铺 */
export function delShop(shopIds: string): Promise<ApiResult> {
  return del<ApiResult>(`/market/shop/${shopIds}`)
}