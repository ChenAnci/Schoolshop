import { get, put, del } from '@/utils/request'
import type { ApiResult } from '@/utils/request'

/** 商品信息（对应后端 MarketProduct） */
export interface ProductData {
  productId?: number
  shopId?: number
  categoryId?: number
  productName?: string
  productDesc?: string
  productImage?: string | null
  price?: number
  stock?: number
  sales?: number
  status?: string
  sort?: number
  /** selectProductList 联查字段 */
  categoryName?: string
  shopName?: string
  createTime?: string
  updateBy?: string
  remark?: string
}

/** 分页列表返回（后端 TableDataInfo：rows/total） */
export interface TableData<T = unknown> {
  rows: T[]
  total: number
}

/** 查询全平台商品列表 */
export function listProduct(params: Record<string, unknown>): Promise<TableData<ProductData>> {
  return get<TableData<ProductData>>('/market/product/list', { params })
}

/** 商品详情 */
export function getProduct(productId: number): Promise<ApiResult<ProductData>> {
  return get<ApiResult<ProductData>>(`/market/product/${productId}`)
}

/** 上下架（管理端事后管控）：status 0上架 1下架 */
export function setProductStatus(productId: number, status: string): Promise<ApiResult> {
  return put<ApiResult>(`/market/product/${productId}/status`, null, { params: { status } })
}

/** 删除商品 */
export function delProduct(productIds: string): Promise<ApiResult> {
  return del<ApiResult>(`/market/product/${productIds}`)
}