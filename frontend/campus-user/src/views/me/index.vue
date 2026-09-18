<template>
  <div class="me-page">
    <h2 class="page-title">个人中心</h2>

    <el-card shadow="never" class="profile-card">
      <div class="profile-head">
        <el-avatar :size="72" :src="userStore.avatar">{{ avatarText }}</el-avatar>
        <div class="profile-info">
          <div class="nickname">{{ userStore.name || '校园用户' }}</div>
          <div class="roles">
            <el-tag v-for="r in userStore.roles" :key="r" size="small">{{ r }}</el-tag>
          </div>
        </div>
      </div>
    </el-card>

    <div class="entry-grid">
      <div class="entry-card" @click="router.push('/order/list')">
        <el-icon :size="30" color="#ff6a00"><Tickets /></el-icon>
        <span>我的订单</span>
      </div>
      <div class="entry-card" @click="router.push('/cart')">
        <el-icon :size="30" color="#ff6a00"><ShoppingCart /></el-icon>
        <span>购物车</span>
      </div>
      <div class="entry-card" @click="router.push('/home')">
        <el-icon :size="30" color="#ff6a00"><House /></el-icon>
        <span>回到首页</span>
      </div>
    </div>

    <el-card shadow="never" class="placeholder-card">
      <el-empty description="收货地址 / 收藏 / 浏览历史等功能待后续里程碑实现" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const avatarText = computed(() => (userStore.name ? userStore.name.charAt(0) : '用'))
</script>

<style scoped>
.me-page {
  width: 1200px;
  margin: 0 auto;
  padding: 16px 0 32px;
}
.page-title {
  font-size: 20px;
  color: #333;
  margin: 0 0 14px;
}
.profile-card {
  border-radius: 8px;
}
.profile-head {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 8px;
}
.nickname {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}
.entry-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin: 16px 0;
}
.entry-card {
  background: #fff;
  border-radius: 8px;
  padding: 26px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  color: #555;
  cursor: pointer;
  transition: box-shadow 0.2s;
}
.entry-card:hover {
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);
}
.placeholder-card {
  border-radius: 8px;
}
</style>
