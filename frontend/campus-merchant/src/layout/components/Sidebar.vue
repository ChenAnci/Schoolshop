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
import { routes } from '@/router'

interface MenuItem {
  path: string
  title: string
  icon?: string
  children?: MenuItem[]
}

/** 由路由表生成侧边菜单（剔除 hidden） */
const menus = computed<MenuItem[]>(() => {
  const result: MenuItem[] = []
  for (const r of routes) {
    if (r.meta?.hidden) continue
    const item: MenuItem = {
      path: r.path,
      title: (r.meta?.title as string) || '',
      icon: r.meta?.icon as string | undefined
    }
    const children = (r.children || [])
      .filter((c) => !c.meta?.hidden)
      .map((c) => ({
        path: `${r.path}/${c.path}`,
        title: (c.meta?.title as string) || '',
        icon: c.meta?.icon as string | undefined
      }))
    if (children.length) {
      item.children = children
    }
    result.push(item)
  }
  return result
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
</style>
