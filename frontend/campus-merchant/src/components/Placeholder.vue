<template>
  <el-card shadow="never">
    <template #header>
      <span>{{ title }}</span>
    </template>
    <el-empty :description="desc" />
    <div class="placeholder-tip">该模块为骨架占位页，功能待后续里程碑实现。</div>
  </el-card>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  desc?: string
}>()
</script>

<style scoped>
.placeholder-tip {
  text-align: center;
  color: #909399;
  font-size: 13px;
}
</style>
