<template>
  <div class="product-card" @click="$router.push(`/product/detail/${product.productId}`)">
    <div class="card-img">
      <img :src="productImageOf(product)" :alt="product.productName" loading="lazy" />
    </div>
    <div class="card-body">
      <div class="card-name" :title="product.productName">{{ product.productName }}</div>
      <div class="card-shop">
        <el-icon><Shop /></el-icon>
        <span>{{ product.shopName }}</span>
      </div>
      <div class="card-foot">
        <span class="price">
          <b>￥{{ product.price }}</b>
        </span>
        <span class="sales">已售 {{ product.sales }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Product } from '@/api/market'
import { productImageOf } from '@/api/market'

defineProps<{
  product: Product
}>()
</script>

<style scoped>
.product-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}
.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
}
.card-img {
  width: 100%;
  aspect-ratio: 1;
  overflow: hidden;
  background: #f5f6f8;
}
.card-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.card-body {
  padding: 10px 12px 12px;
}
.card-name {
  font-size: 14px;
  color: #333;
  line-height: 1.4;
  height: 40px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.card-shop {
  display: flex;
  align-items: center;
  gap: 2px;
  color: #999;
  font-size: 12px;
  margin: 6px 0 8px;
}
.card-foot {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}
.price b {
  color: #ff5000;
  font-size: 18px;
}
.sales {
  color: #999;
  font-size: 12px;
}
</style>