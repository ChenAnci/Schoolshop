import { get, post, put, del, type ApiResult } from '@/utils/request'

/** 商品（对应后端 MarketProduct） */
export interface ShopProduct {
  productId: number
  shopId: number
  categoryId: number
  productName: string
  productDesc?: string
  productImage?: string | null
  price: number
  stock: number
  sales?: number
  status: string
  sort?: number
  categoryName?: string
  shopName?: string
  createTime?: string
  remark?: string
}

/** 商品分类（发布页下拉用，C 端公开 browse 接口） */
export interface Category {
  categoryId: number
  parentId: number
  categoryName: string
  sort: number
}

/** RuoYi TableDataInfo 分页结构（list 接口顶层返回 total/rows） */
export interface TableData<T> {
  total: number
  rows: T[]
}

/** 商品上下架状态字典（0上架 1下架） */
export const PRODUCT_STATUS: Record<string, { text: string; type: 'primary' | 'success' | 'info' }> = {
  '0': { text: '上架', type: 'success' },
  '1': { text: '下架', type: 'info' }
}

/** 商家商品列表（可按名称/状态筛选） */
export function getShopProductList(params?: { productName?: string; status?: string }): Promise<TableData<ShopProduct>> {
  return get<TableData<ShopProduct>>('/market/shop/product/list', { params })
}

/** 商家商品详情 */
export function getShopProductDetail(productId: number): Promise<ApiResult<ShopProduct>> {
  return get<ApiResult<ShopProduct>>(`/market/shop/product/${productId}`)
}

/** 发布商品 */
export function addShopProduct(data: Partial<ShopProduct>): Promise<ApiResult> {
  return post<ApiResult>('/market/shop/product', data)
}

/** 编辑商品 */
export function updateShopProduct(data: Partial<ShopProduct>): Promise<ApiResult> {
  return put<ApiResult>('/market/shop/product', data)
}

/** 上下架：status 0上架 1下架 */
export function setShopProductStatus(productId: number, status: string): Promise<ApiResult> {
  return put<ApiResult>(`/market/shop/product/${productId}/status`, null, { params: { status } })
}

/** 删除商品（传 productId 集合） */
export function delShopProduct(productIds: number[]): Promise<ApiResult> {
  return del<ApiResult>(`/market/shop/product/${productIds.join(',')}`)
}

/** 商品分类下拉（C 端公开接口，商家登录态调用） */
export function getCategoryList(): Promise<ApiResult<Category[]>> {
  return get<ApiResult<Category[]>>('/market/browse/category/list')
}