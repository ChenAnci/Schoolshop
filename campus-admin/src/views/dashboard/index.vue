<template>
  <div class="placeholder-page">
    <el-card shadow="never">
      <el-result icon="success" title="欢迎使用校园市场管理端" sub-title="骨架已就绪，各功能模块将在此基础上逐步实现（见开发文档 §8.1）">
      </el-result>
      <div class="stat-row">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="平台商品数" :value="3" />
        </el-card>
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="商家数量" :value="1" />
        </el-card>
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="订单总数" :value="0" />
        </el-card>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.stat-row {
  display: flex;
  gap: 16px;
  margin-top: 8px;
}
.stat-card {
  flex: 1;
  text-align: center;
}
</style>
