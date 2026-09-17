import { get, put } from '@/utils/request'
import type { ApiResult } from '@/utils/request'

/** 店铺信息 */
export interface ShopData {
  shopId?: number
  userId?: number
  shopName?: string
  shopLogo?: string
  shopDesc?: string
  contactPhone?: string
  status?: string
  productCount?: number
  remark?: string
}

/** 获取当前商家的店铺信息 */
export function getMyShop(): Promise<ApiResult<ShopData>> {
  return get<ApiResult<ShopData>>('/market/shop/my')
}

/** 修改当前商家的店铺信息 */
export function updateMyShop(data: Partial<ShopData>): Promise<ApiResult> {
  return put<ApiResult>('/market/shop/my', data)
}