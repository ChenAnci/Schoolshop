<template>
  <div class="navbar">
    <div class="nav-inner">
      <router-link class="logo" to="/">
        <el-icon :size="26" color="#ff6a00"><ShoppingBag /></el-icon>
        <span class="logo-text">校园市场</span>
      </router-link>

      <div class="nav-search">
        <el-input
          v-model="keyword"
          placeholder="搜索二手好物、教材、数码..."
          clearable
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button :icon="Search" @click="handleSearch">搜索</el-button>
          </template>
        </el-input>
      </div>

      <nav class="nav-links">
        <router-link to="/home" :class="{ active: isActive('/home') }">首页</router-link>
        <router-link to="/product/list" :class="{ active: isActive('/product/list') }">全部商品</router-link>
        <router-link to="/order/list" :class="{ active: isActive('/order/list') }">我的订单</router-link>
      </nav>

      <div class="nav-actions">
        <router-link class="cart-link" to="/cart">
          <el-badge :value="cartStore.count" :hidden="cartStore.count === 0" :max="99">
            <el-icon :size="22"><ShoppingCart /></el-icon>
          </el-badge>
          <span>购物车</span>
        </router-link>

        <template v-if="userStore.token">
          <el-dropdown trigger="click" @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="30" :src="userStore.avatar">{{ avatarText }}</el-avatar>
              <span class="user-name">{{ userStore.name || '用户' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="me">
                  <el-icon><User /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item command="order">
                  <el-icon><List /></el-icon>我的订单
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <router-link v-else class="login-btn" to="/login">登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()

const keyword = ref('')

async function loadCartCount() {
  if (!userStore.token || cartStore.loaded) return
  try {
    await cartStore.fetch()
  } catch {
    // 未登录或接口异常时静默忽略，购物车数量保持本地缓存
  }
}

onMounted(loadCartCount)

const avatarText = computed(() => (userStore.name ? userStore.name.charAt(0) : '用'))

function isActive(path: string) {
  return route.path === path || (path === '/home' && route.path === '/')
}

function handleSearch() {
  const kw = keyword.value.trim()
  router.push({ path: '/product/list', query: kw ? { keyword: kw } : {} })
}

function handleCommand(command: string) {
  if (command === 'logout') {
    ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
      .then(async () => {
        await userStore.logout()
        ElMessage.success('已退出登录')
        router.replace('/home')
      })
      .catch(() => undefined)
  } else {
    router.push(`/${command}`)
  }
}
</script>

<style scoped>
.navbar {
  background: #fff;
  border-bottom: 2px solid #ff6a00;
  position: sticky;
  top: 0;
  z-index: 100;
}
.nav-inner {
  width: 1200px;
  margin: 0 auto;
  height: 60px;
  display: flex;
  align-items: center;
  gap: 24px;
}
.logo {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 20px;
  font-weight: 700;
  color: #ff6a00;
  flex-shrink: 0;
}
.nav-search {
  flex: 1;
  max-width: 480px;
}
.nav-links {
  display: flex;
  gap: 20px;
  font-size: 14px;
}
.nav-links a {
  color: #333;
  padding: 4px 2px;
}
.nav-links a.active {
  color: #ff6a00;
  font-weight: 600;
}
.nav-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 18px;
}
.cart-link {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #333;
  font-size: 14px;
}
.cart-link:hover {
  color: #ff6a00;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #333;
  font-size: 14px;
  outline: none;
}
.login-btn {
  color: #fff;
  background: #ff6a00;
  padding: 6px 18px;
  border-radius: 4px;
  font-size: 14px;
}
.login-btn:hover {
  opacity: 0.9;
}
</style>
