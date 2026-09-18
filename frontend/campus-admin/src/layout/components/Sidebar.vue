<template>
  <el-scrollbar class="sidebar-scroll">
    <el-menu
      :default-active="activeMenu"
      router
      background-color="#001529"
      text-color="rgba(255,255,255,0.68)"
      active-text-color="#ffffff"
      class="sidebar-menu"
    >
      <template v-for="menu in menus" :key="menu.path">
        <el-sub-menu v-if="menu.children && menu.children.length" :index="menu.path">
          <template #title>
            <el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon>
            <span>{{ menu.title }}</span>
          </template>
          <el-menu-item v-for="child in menu.children" :key="child.path" :index="child.path">
            <el-icon v-if="child.icon"><component :is="child.icon" /></el-icon>
            <span>{{ child.title }}</span>
            <el-badge v-if="child.badge && child.badge > 0" :value="child.badge" class="menu-badge" />
          </el-menu-item>
        </el-sub-menu>
        <el-menu-item v-else :index="menu.path">
          <el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon>
          <span>{{ menu.title }}</span>
        </el-menu-item>
      </template>
    </el-menu>
  </el-scrollbar>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { usePermissionStore } from '@/stores/permission'
import { storeToRefs } from 'pinia'

interface MenuItem {
  path: string
  title: string
  icon?: string
  badge?: number
  children?: MenuItem[]
}

const permissionStore = usePermissionStore()
const { dynamicRoutes } = storeToRefs(permissionStore)

/** 待接入：订单待办数角标（可后续从接口填充） */
const orderBadge = () => 0

/** 侧边菜单：首页固定 + 后端动态路由生成 */
const menus = computed<MenuItem[]>(() => {
  const list: MenuItem[] = [
    {
      path: '/dashboard',
      title: '首页',
      icon: 'HomeFilled'
    }
  ]
  for (const r of dynamicRoutes.value) {
    const meta = r.meta as { title?: string; icon?: string; hidden?: boolean } | undefined
    if (meta?.hidden) continue
    const children = (r.children || [])
      .map((c) => {
        const cm = c.meta as { title?: string; icon?: string; hidden?: boolean; fullPath?: string } | undefined
        if (cm?.hidden) return null
        return {
          path: cm?.fullPath || `${meta?.title ? '' : ''}/${c.path}`.replace(/\/+/, '/'),
          title: cm?.title || '',
          icon: cm?.icon,
          badge: c.name === 'MarketOrder' ? orderBadge() : undefined
        } as MenuItem
      })
      .filter((x): x is MenuItem => !!x)
    const item: MenuItem = {
      path: `/${(r.path as string).replace(/^\//, '')}`,
      title: meta?.title || '',
      icon: meta?.icon
    }
    if (children.length && r.path) {
      item.children = children
    }
    list.push(item)
  }
  return list
})

const route = useRoute()
const activeMenu = computed(() => route.path)
</script>

<style scoped>
.sidebar-scroll {
  flex: 1;
  height: calc(100vh - 50px);
}
.sidebar-menu {
  border-right: none;
}
.menu-badge {
  margin-left: 8px;
}
</style>