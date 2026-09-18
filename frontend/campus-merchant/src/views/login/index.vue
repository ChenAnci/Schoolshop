<template>
  <div class="redirect-page">
    <el-icon :size="40" color="#2f54eb"><Promotion /></el-icon>
    <h2>校园市场 · 商家端</h2>
    <p>登录已统一，正在跳转统一登录入口...</p>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { Promotion } from '@element-plus/icons-vue'

// 全系统唯一登录入口：用户端 8083
const UNIFIED_LOGIN_URL = '//localhost:8083/login'

onMounted(() => {
  window.location.replace(UNIFIED_LOGIN_URL)
})
</script>

<style scoped>
.redirect-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #606266;
}
.redirect-page h2 {
  margin: 0;
  color: #1f2d3d;
}
.redirect-page p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}
</style>