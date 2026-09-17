<template>
  <div class="shop-list-page">
    <h2 class="page-title">校内店铺</h2>
    <el-empty v-if="shops.length === 0" description="暂无店铺" />
    <div v-else class="shop-grid">
      <div
        v-for="s in shops"
        :key="s.shopId"
        class="shop-card"
        @click="router.push(`/shop/detail/${s.shopId}`)"
      >
        <img :src="productImageOf(s)" :alt="s.shopName" class="shop-logo" />
        <div class="shop-info">
          <div class="shop-name">{{ s.shopName }}</div>
          <div class="shop-desc">{{ s.shopDesc || '暂无简介' }}</div>
          <div class="shop-count">在售 {{ s.productCount }} 件</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getShopList, productImageOf, type Shop } from '@/api/market'

const router = useRouter()
const shops = ref<Shop[]>([])

async function load() {
  try {
    const res = await getShopList()
    shops.value = res.data ?? []
  } catch {
    // 加载失败保持空态
  }
}

onMounted(load)
</script>

<style scoped>
.shop-list-page {
  width: 1200px;
  margin: 0 auto;
  padding: 16px 0 32px;
}
.page-title {
  font-size: 20px;
  color: #333;
  margin: 0 0 14px;
}
.shop-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.shop-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  gap: 16px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}
.shop-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
}
.shop-logo {
  width: 72px;
  height: 72px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;
  background: #f5f6f8;
}
.shop-info {
  min-width: 0;
}
.shop-name {
  font-size: 16px;
  color: #333;
  font-weight: 600;
}
.shop-desc {
  color: #999;
  font-size: 13px;
  margin-top: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.shop-count {
  color: #ff6a00;
  font-size: 13px;
  margin-top: 8px;
}
</style>