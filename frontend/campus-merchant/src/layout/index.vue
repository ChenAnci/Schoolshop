<template>
  <div class="app-wrapper">
    <el-container class="app-container">
      <el-aside width="210px" class="app-aside">
        <div class="logo">
          <el-icon><ShoppingBag /></el-icon>
          <span>校园市场商家端</span>
        </div>
        <Sidebar />
      </el-aside>
      <el-container>
        <el-header class="app-header" height="50px">
          <Navbar />
        </el-header>
        <el-main class="app-main">
          <AppMain />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import Sidebar from './components/Sidebar.vue'
import Navbar from './components/Navbar.vue'
import AppMain from './components/AppMain.vue'
</script>

<style scoped>
.app-wrapper {
  height: 100%;
}
.app-container {
  height: 100%;
}
.app-aside {
  background: #001529;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.logo {
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  flex-shrink: 0;
}
.app-header {
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  padding: 0 16px;
  display: flex;
  align-items: center;
}
.app-main {
  background: #f0f2f5;
  padding: 0;
  overflow: auto;
}
</style>
