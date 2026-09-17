import { defineStore } from 'pinia'
import {
  getCartList,
  addToCart,
  updateCartQty,
  removeCart,
  clearCart,
  type CartItem
} from '@/api/market'

/**
 * 购物车状态（对接后端 /market/user/cart，需登录）
 * 数据以服务端为准，前端仅做登录态下缓存与操作代理。
 */
export const useCartStore = defineStore('cart', {
  state: () => ({
    items: [] as CartItem[],
    loaded: false
  }),

  getters: {
    count: (state) => state.items.reduce((sum, item) => sum + item.quantity, 0),
    totalPrice: (state) =>
      state.items.reduce((sum, item) => sum + item.price * item.quantity, 0)
  },

  actions: {
    /** 拉取购物车（刷新后自动清空缓存并重新加载） */
    async fetch() {
      const res = await getCartList()
      this.items = res.rows ?? []
      this.loaded = true
      return this.items
    },

    /** 加入购物车（已存在由后端累加数量） */
    async add(productId: number, quantity = 1) {
      await addToCart({ productId, quantity })
      await this.fetch()
    },

    /** 修改数量 */
    async updateQuantity(cartId: number, quantity: number) {
      await updateCartQty(cartId, quantity)
      const item = this.items.find((i) => i.cartId === cartId)
      if (item) item.quantity = Math.max(1, quantity)
    },

    /** 删除条目 */
    async remove(cartIds: number[]) {
      await removeCart(cartIds)
      this.items = this.items.filter((i) => !cartIds.includes(i.cartId))
    },

    /** 清空 */
    async clear() {
      await clearCart()
      this.items = []
    },

    /** 退出登录时清空本地购物车缓存 */
    reset() {
      this.items = []
      this.loaded = false
    }
  }
})