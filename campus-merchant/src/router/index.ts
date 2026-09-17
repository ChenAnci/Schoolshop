import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import Layout from '@/layout/index.vue'

/**
 * 商家端路由
 * 骨架阶段菜单为静态注册；后续可改为后端菜单动态加载。
 */
export const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', hidden: true }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '工作台', icon: 'HomeFilled' }
      }
    ]
  },
  {
    path: '/shop',
    component: Layout,
    redirect: '/shop/manage',
    meta: { title: '店铺管理', icon: 'OfficeBuilding' },
    children: [
      {
        path: 'manage',
        name: 'ShopManage',
        component: () => import('@/views/shop/manage/index.vue'),
        meta: { title: '店铺资料', icon: 'OfficeBuilding' }
      }
    ]
  },
  {
    path: '/product',
    component: Layout,
    redirect: '/product/manage',
    meta: { title: '商品管理', icon: 'Goods' },
    children: [
      {
        path: 'manage',
        name: 'ProductManage',
        component: () => import('@/views/product/manage/index.vue'),
        meta: { title: '我的商品', icon: 'Goods' }
      },
      {
        path: 'publish',
        name: 'ProductPublish',
        component: () => import('@/views/product/publish/index.vue'),
        meta: { title: '发布商品', icon: 'Plus' }
      }
    ]
  },
  {
    path: '/order',
    component: Layout,
    redirect: '/order/manage',
    meta: { title: '订单管理', icon: 'Tickets' },
    children: [
      {
        path: 'manage',
        name: 'OrderManage',
        component: () => import('@/views/order/manage/index.vue'),
        meta: { title: '订单列表', icon: 'Tickets' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '404', hidden: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
