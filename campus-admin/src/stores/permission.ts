import { defineStore } from 'pinia'
import type { RouteRecordRaw } from 'vue-router'
import Layout from '@/layout/index.vue'
import { getRouters, type RouterVo } from '@/api/login'

/** 组件目录（相对本文件），value 为 async 组件加载器 */
const viewModules = import.meta.glob('../views/**/*.vue')

/** 映射后端 component 字符串（如 system/user/index）到前端视图组件 */
function loadView(component?: string) {
  if (!component) return undefined
  const key = `../views/${component}.vue`
  const loader = viewModules[key]
  return loader
}

/** 拼接路径：保证以 / 开头并去除重复分隔符 */
function resolveFullPath(parent: string, child: string): string {
  const p = parent || ''
  const c = child || ''
  const joined = `${p.replace(/\/$/, '')}/${c.replace(/^\//, '')}`
  return `/${joined.replace(/^\/+/, '')}`
}

/** 将后端 RouterVo 树转换为可 addRoute 的 route（顶层目录以 Layout 为容器） */
function transformRoutes(routers: RouterVo[]): RouteRecordRaw[] {
  const result: RouteRecordRaw[] = []
  for (const r of routers) {
    const topPath = resolveFullPath('', r.path || '')
    const children = (r.children || [])
      .filter((c) => c.component && c.component !== 'ParentView')
      .map((c) => {
        const fullPath = resolveFullPath(topPath, c.path || '')
        return {
          path: c.path || '',
          name: c.name,
          component: loadView(c.component),
          meta: {
            title: c.meta?.title,
            icon: c.meta?.icon,
            hidden: c.hidden,
            fullPath
          }
        } as RouteRecordRaw
      })
    const redirect = r.redirect || (children[0]?.meta as { fullPath?: string } | undefined)?.fullPath
    result.push({
      path: topPath,
      component: Layout,
      redirect,
      meta: { title: r.meta?.title, icon: r.meta?.icon },
      children
    } as RouteRecordRaw)
  }
  return result
}

export const usePermissionStore = defineStore('permission', {
  state: () => ({
    /** 是否已把动态路由注册进 router */
    addedRoutes: false,
    /** 规整后的动态路由（供侧边栏与 addRoute 使用） */
    dynamicRoutes: [] as RouteRecordRaw[]
  }),

  actions: {
    /** 拉取后端菜单并生成动态路由 */
    async generateRoutes(): Promise<RouteRecordRaw[]> {
      const res = await getRouters()
      this.dynamicRoutes = transformRoutes(res.data ?? [])
      return this.dynamicRoutes
    }
  }
})