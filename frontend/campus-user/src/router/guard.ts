import router from './index'
import { getToken, setToken } from '@/utils/auth'
import { useUserStore } from '@/stores/user'

const WHITE_LIST = ['/login']

router.beforeEach(async (to, _from, next) => {
  document.title = to.meta.title ? `${to.meta.title as string} · 校园市场用户端` : '校园市场用户端'

  // 统一登录入口：接收其他端通过 URL 传递的 token 存入本端，并清除 URL 参数防止泄露
  const urlToken = to.query.token
  if (urlToken && typeof urlToken === 'string') {
    setToken(urlToken)
    const query = { ...to.query }
    delete query.token
    next({ path: to.path, query, replace: true })
    return
  }

  const userStore = useUserStore()
  const hasToken = getToken()

  // 已登录
  if (hasToken) {
    if (to.path === '/login') {
      next({ path: '/' })
      return
    }
    // 未拉取过用户信息则拉取（骨架阶段仅内存，刷新页面后重新拉取）
    if (userStore.roles.length === 0) {
      try {
        await userStore.fetchUserInfo()
      } catch {
        // 用户信息拉取失败：清 token 回到游客态，继续访问公开页
        userStore.resetToken()
        if (to.meta.requiresAuth) {
          next({ path: '/login', query: { redirect: to.fullPath } })
          return
        }
      }
    }
    next()
    return
  }

  // 游客：公开页可直接访问
  if (WHITE_LIST.includes(to.path) || !to.meta.requiresAuth) {
    next()
    return
  }

  // 需要登录的页面
  next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
})
