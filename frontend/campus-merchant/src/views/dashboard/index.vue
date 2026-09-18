<template>
  <div class="placeholder-page">
    <el-card shadow="never">
      <el-result icon="success" title="欢迎使用校园市场商家端" sub-title="骨架已就绪，店铺/商品/订单功能将逐步实现（见开发文档 §8.2）">
      </el-result>
      <div class="stat-row">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="在售商品" :value="0" />
        </el-card>
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="待接单" :value="0" />
        </el-card>
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="今日销售额(元)" :value="0" />
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
