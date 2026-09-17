import router from './index'
import { getToken, setToken } from '@/utils/auth'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'

/**
 * 统一登录入口：全系统仅在用户端 8083 提供登录，登录后按账号角色跳转到对应端。
 * 管理端不再提供独立登录页，未登录或非管理员一律回到统一入口。
 */
const UNIFIED_LOGIN_URL = '//localhost:8083/login'

router.beforeEach(async (to, _from, next) => {
  document.title = to.meta.title ? `${to.meta.title as string} · 校园市场管理端` : '校园市场管理端'

  // 统一入口跳转：接收 URL 中传递的 token 存入本端，并清除 URL 参数防止泄露
  const urlToken = to.query.token
  if (urlToken && typeof urlToken === 'string') {
    setToken(urlToken)
    const query = { ...to.query }
    delete query.token
    next({ path: to.path, query, replace: true })
    return
  }

  const userStore = useUserStore()
  const permissionStore = usePermissionStore()
  const hasToken = getToken()

  if (!hasToken) {
    // 未登录（管理端无独立登录页）：回到统一登录入口
    window.location.href = UNIFIED_LOGIN_URL
    return
  }

  // 已登录：拉取用户信息（骨架阶段仅在内存，刷新后重新拉取）
  if (userStore.roles.length === 0) {
    try {
      await userStore.fetchUserInfo()
    } catch {
      userStore.resetToken()
      window.location.href = UNIFIED_LOGIN_URL
      return
    }
  }

  // 权限隔离：仅管理员角色可访问管理端，否则清 token 回统一入口
  if (!userStore.roles.includes('admin')) {
    userStore.resetToken()
    window.location.href = UNIFIED_LOGIN_URL
    return
  }

  // 动态路由：首登/刷新后从后端菜单生成并注册（仅执行一次）
  if (!permissionStore.addedRoutes) {
    try {
      const dynamicRoutes = await permissionStore.generateRoutes()
      dynamicRoutes.forEach((route) => router.addRoute(route))
      permissionStore.addedRoutes = true
    } catch {
      // 菜单加载失败回统一入口重登
      userStore.resetToken()
      window.location.href = UNIFIED_LOGIN_URL
      return
    }
    // 路由此时已注册，重放当前导航以命中动态路由
    next({ ...to, replace: true })
    return
  }

  if (to.path === '/login') {
    next({ path: '/' })
    return
  }
  next()
})