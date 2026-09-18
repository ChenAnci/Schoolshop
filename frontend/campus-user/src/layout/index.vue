<template>
  <div class="app-wrapper">
    <Navbar />
    <div class="app-body">
      <AppMain />
    </div>
    <footer class="app-footer">
      校园市场 · 学生闲置好物交易平台 · 骨架预览版
    </footer>
  </div>
</template>

<script setup lang="ts">
import Navbar from './components/Navbar.vue'
import AppMain from './components/AppMain.vue'
</script>

<style scoped>
.app-wrapper {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  background: #f5f6f8;
}
.app-body {
  flex: 1;
  width: 100%;
}
.app-footer {
  padding: 18px 0;
  text-align: center;
  color: #a0a4ab;
  font-size: 12px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
}
</style>
