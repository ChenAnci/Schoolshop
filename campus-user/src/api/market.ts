import { get, post, put, del, type ApiResult } from '@/utils/request'

/** 商品（C 端浏览，对应后端 MarketProduct 上架查询） */
export interface Product {
  productId: number
  shopId: number
  categoryId: number
  productName: string
  productDesc: string
  productImage: string | null
  price: number
  stock: number
  sales: number
  sort: number
  categoryName: string
  shopName: string
}

/** 商品分类（C 端） */
export interface Category {
  categoryId: number
  parentId: number
  categoryName: string
  sort: number
}

/** 店铺（C 端） */
export interface Shop {
  shopId: number
  userId: number
  shopName: string
  shopLogo: string | null
  shopDesc: string
  contactPhone: string
  status: string
  productCount: number
}

/** RuoYi TableDataInfo 分页结构 */
export interface TableData<T> {
  total: number
  rows: T[]
}

/** 商品主图：无图时用平台 text_to_image 生成兜底图 */
const IMG = 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image'

/** 适用于含图片/名称来源的对象（商品、店铺或快照），无图时用平台占位图 */
type HasImageName = { productName?: string; productImage?: string | null; shopName?: string }

export function productImageOf(p: Product | Shop | HasImageName): string {
  const raw =
    'productImage' in p ? (p.productImage ?? '') : (p as Shop).shopLogo ?? ''
  const name =
    'productName' in p ? (p.productName ?? '') : (p as Shop).shopName ?? '校园好物'
  if (raw.trim()) return raw
  return `${IMG}?prompt=${encodeURIComponent(
    `a clean product photo of ${name || 'item'} for a campus second-hand marketplace card, soft daylight, neat background`
  )}&image_size=square_hd`
}

/** 正常分类列表（C 端公开接口） */
export function getCategoryList(): Promise<ApiResult<Category[]>> {
  return get<ApiResult<Category[]>>('/market/browse/category/list')
}

/** 正常店铺列表（C 端公开接口，含在售商品数） */
export function getShopList(): Promise<ApiResult<Shop[]>> {
  return get<ApiResult<Shop[]>>('/market/browse/shop/list')
}

/** 上架商品分页列表（C 端公开接口） */
export function getProductList(params: {
  pageNum: number
  pageSize: number
  categoryId?: number
  shopId?: number
  productName?: string
}): Promise<TableData<Product>> {
  return get<TableData<Product>>('/market/browse/product/list', { params })
}

/** 上架商品详情（C 端公开接口，附带店铺信息） */
export function getProductDetail(productId: number): Promise<ApiResult<{ product: Product; shop: Shop }>> {
  return get<ApiResult<{ product: Product; shop: Shop }>>(`/market/browse/product/${productId}`)
}

// ===================== 购物车（C 端，需登录 /market/user/cart） =====================

/** 购物车条目（对应后端 MarketCart，含商品快照与店铺信息） */
export interface CartItem {
  cartId: number
  productId: number
  productName: string
  productImage: string | null
  price: number
  quantity: number
  checked: string
  productStatus: string
  shopId: number
  shopName: string
  createTime?: string
}

/** 我的购物车列表 */
export function getCartList(): Promise<TableData<CartItem>> {
  return get<TableData<CartItem>>('/market/user/cart/list')
}

/** 加入购物车（productId + quantity；已存在则累加） */
export function addToCart(data: { productId: number; quantity: number }): Promise<ApiResult> {
  return post<ApiResult>('/market/user/cart', data)
}

/** 修改购物车条目数量 */
export function updateCartQty(cartId: number, quantity: number): Promise<ApiResult> {
  return put<ApiResult>('/market/user/cart', { cartId, quantity })
}

/** 删除购物车条目（传入 cartId 集合） */
export function removeCart(cartIds: number[]): Promise<ApiResult> {
  return del<ApiResult>(`/market/user/cart/${cartIds.join(',')}`)
}

/** 清空当前用户购物车 */
export function clearCart(): Promise<ApiResult> {
  return del<ApiResult>('/market/user/cart/clear')
}

// ===================== 订单（C 端，需登录 /market/user/order） =====================

/** 订单（对应后端 MarketOrder） */
export interface Order {
  orderId: number
  orderNo: string
  shopId: number
  shopName: string
  totalAmount: number
  payAmount: number
  status: string
  receiverName?: string
  receiverPhone?: string
  receiverAddress?: string
  createTime?: string
  cancelTime?: string
  finishTime?: string
}

/** 订单明细（对应后端 MarketOrderItem） */
export interface OrderItem {
  itemId: number
  orderId: number
  orderNo: string
  productId: number
  productName: string
  productImage: string | null
  price: number
  quantity: number
  subtotal: number
  /** 是否已评价（已完成订单明细标记） */
  reviewed?: boolean
}

/** 订单状态字典（0待商家接单 1待自提 2已完成 3已取消 4审查中 5审查完成） */
export const ORDER_STATUS: Record<string, { text: string; type: 'primary' | 'success' | 'info' | 'danger' | 'warning' }> = {
  '0': { text: '待商家接单', type: 'warning' },
  '1': { text: '待自提', type: 'primary' },
  '2': { text: '已完成', type: 'success' },
  '3': { text: '已取消', type: 'info' },
  '4': { text: '审查中', type: 'danger' },
  '5': { text: '审查完成', type: 'info' }
}

/** 我的订单列表（可选 status 筛选） */
export function getOrderList(status?: string): Promise<TableData<Order>> {
  return get<TableData<Order>>('/market/user/order/list', { params: status ? { status } : {} })
}

/** 我的订单详情（含明细 items） */
export function getOrderDetail(orderId: number): Promise<ApiResult<{ order: Order; items: OrderItem[] }>> {
  return get<ApiResult<{ order: Order; items: OrderItem[] }>>(`/market/user/order/${orderId}`)
}

/** 结算下单：从购物车选中条目生成订单（按店铺拆分），返回订单编号数组 */
export function createOrder(data: {
  cartIds: number[]
  receiverName?: string
  receiverPhone?: string
  receiverAddress?: string
  remark?: string
}): Promise<ApiResult<string[]>> {
  return post<ApiResult<string[]>>('/market/user/order', data)
}

/** 取消订单（仅待接单可取消） */
export function cancelOrder(orderId: number): Promise<ApiResult> {
  return put<ApiResult>(`/market/user/order/${orderId}/cancel`)
}

/** 确认收货（提货）：1待自提 → 2已完成 */
export function confirmOrder(orderId: number): Promise<ApiResult> {
  return put<ApiResult>(`/market/user/order/${orderId}/confirm`)
}

// ===================== 商品评价（C 端） =====================

/** 商品评价（对应后端 MarketReview，游客可看商品评价列表） */
export interface Review {
  reviewId: number
  orderId: number
  productId: number
  rating: number
  content: string
  userName?: string
  createTime?: string
}

/** 提交评价条目 */
export interface ReviewCreateItem {
  orderItemId: number
  rating: number
  content: string
}

/** 商品评价汇总（列表 + 均分 + 数量） */
export interface ProductReviewResult {
  list: Review[]
  avgRating: number
  count: number
}

/** 提交评价（已完成订单的若干明细，每条明细仅可评价一次） */
export function submitReview(reviews: ReviewCreateItem[]): Promise<ApiResult<{ count: number }>> {
  return post<ApiResult<{ count: number }>>('/market/user/review', { reviews })
}

/** 商品评价列表（公开接口，游客可看） */
export function getProductReviews(productId: number): Promise<ApiResult<ProductReviewResult>> {
  return get<ApiResult<ProductReviewResult>>(`/market/browse/product/${productId}/reviews`)
}