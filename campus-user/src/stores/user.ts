import { defineStore } from 'pinia'
import { getToken, setToken, setRefreshToken, removeToken } from '@/utils/auth'
import { login as loginApi, getInfo, logout as logoutApi, type LoginData } from '@/api/login'
import { useCartStore } from '@/stores/cart'

interface UserState {
  token: string
  refreshToken: string
  name: string
  avatar: string
  roles: string[]
  permissions: string[]
  userInfo: Record<string, unknown> | null
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    token: getToken() || '',
    refreshToken: '',
    name: '',
    avatar: '',
    roles: [],
    permissions: [],
    userInfo: null
  }),

  actions: {
    /** 登录：成功后保存 accessToken（refreshToken 待后端双 Token 落地后由 /auth/refresh 返回） */
    async login(loginData: LoginData) {
      const res = await loginApi(loginData)
      this.token = res.token || ''
      setToken(this.token)
      // TODO(M1): res.refreshToken 存在时写入
      // this.refreshToken = res.refreshToken || ''
      // setRefreshToken(this.refreshToken)
      return res
    },

    /** 拉取用户信息与角色权限 */
    async fetchUserInfo() {
      const info = await getInfo()
      this.roles = info?.roles ?? []
      this.permissions = info?.permissions ?? []
      this.name = (info?.user?.nickName as string) || (info?.user?.userName as string) || ''
      this.avatar = (info?.user?.avatar as string) || ''
      this.userInfo = (info?.user as Record<string, unknown>) || null
      return info
    },

    /** 退出登录 */
    async logout() {
      try {
        await logoutApi()
      } finally {
        removeToken()
        useCartStore().reset()
        this.$reset()
      }
    },

    resetToken() {
      removeToken()
      this.token = ''
      this.roles = []
      this.permissions = []
    }
  }
})
