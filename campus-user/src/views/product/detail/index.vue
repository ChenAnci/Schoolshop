<template>
  <div class="detail-page">
    <el-card v-if="product" shadow="never" class="detail-card">
      <div class="detail-body">
        <div class="detail-img">
          <img :src="productImageOf(product)" :alt="product.productName" />
        </div>
        <div class="detail-info">
          <h2 class="detail-name">{{ product.productName }}</h2>
          <div class="detail-price">
            <span class="now">￥{{ product.price }}</span>
            <span class="tag">{{ product.categoryName || '闲置转让' }}</span>
          </div>
          <div class="detail-meta">
            <span>已售 {{ product.sales }}</span>
            <span>库存 {{ product.stock }}</span>
          </div>
          <div class="detail-shop" @click="router.push(`/shop/detail/${product.shopId}`)">
            <el-icon><Shop /></el-icon>
            <span>{{ product.shopName }}</span>
            <el-link type="primary" :underline="false">进入店铺</el-link>
          </div>
          <div class="detail-desc">{{ product.productDesc || '暂无商品描述' }}</div>
          <div class="detail-actions">
            <span class="qty-label">数量</span>
            <el-input-number v-model="quantity" :min="1" :max="Math.max(product.stock, 1)" size="large" />
            <el-button
              type="primary"
              size="large"
              class="add-btn"
              :icon="ShoppingCart"
              :disabled="product.stock <= 0"
              @click="handleAdd"
            >
              加入购物车
            </el-button>
            <el-button size="large" @click="router.push('/cart')">去购物车结算</el-button>
          </div>
          <p class="pickup-tip">线下自提 / 当面交易，平台仅管理订单状态，不支持在线支付</p>
        </div>
      </div>
    </el-card>

    <!-- 商品评价 -->
    <el-card v-if="product" shadow="never" class="review-card">
      <template #header>
        <div class="review-head">
          <span class="review-title">商品评价</span>
          <div v-if="reviews.count > 0" class="review-summary">
            <span class="review-avg">{{ reviews.avgRating }}</span>
            <el-rate :model-value="reviews.avgRating" disabled allow-half class="review-stars" />
            <span class="review-count">共 {{ reviews.count }} 条评价</span>
          </div>
        </div>
      </template>

      <el-empty v-if="reviews.loading" description="评价加载中..." :image-size="60" />
      <el-empty v-else-if="reviews.list.length === 0" description="暂无评价，下单并确认收货后可发表评价" :image-size="80" />
      <div v-else class="review-list">
        <div v-for="r in reviews.list" :key="r.reviewId" class="review-row-item">
          <div class="review-user">
            <el-avatar :size="36">{{ (r.userName || '匿名').slice(0, 1) }}</el-avatar>
            <div class="review-user-meta">
              <span class="review-uname">{{ r.userName || '匿名用户' }}</span>
              <div class="review-meta-lower">
                <el-rate :model-value="r.rating" disabled allow-half class="review-stars-sm" />
                <span class="review-time">{{ formatTime(r.createTime) }}</span>
              </div>
            </div>
          </div>
          <p v-if="r.content" class="review-content">{{ r.content }}</p>
        </div>
      </div>
    </el-card>

    <el-empty v-else-if="!loading" description="商品不存在或已下架">
      <el-button type="primary" @click="router.push('/product/list')">返回商品列表</el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ShoppingCart } from '@element-plus/icons-vue'
import { getProductDetail, getProductReviews, productImageOf, type Product, type Review } from '@/api/market'
import { useCartStore } from '@/stores/cart'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()
const userStore = useUserStore()

const product = ref<Product | null>(null)
const loading = ref(true)
const quantity = ref(1)

/** 商品评价汇总与列表（游客可看） */
const reviews = ref<{ loading: boolean; avgRating: number; count: number; list: Review[] }>({
  loading: true,
  avgRating: 0,
  count: 0,
  list: []
})

function formatTime(t?: string) {
  if (!t) return '-'
  return t.replace('T', ' ').slice(0, 16)
}

async function loadReviews() {
  reviews.value.loading = true
  try {
    const res = await getProductReviews(Number(route.params.id))
    const data = res.data
    reviews.value.avgRating = data?.avgRating ?? 0
    reviews.value.count = data?.count ?? 0
    reviews.value.list = data?.list ?? []
  } catch {
    reviews.value.list = []
  } finally {
    reviews.value.loading = false
  }
}

async function load() {
  loading.value = true
  try {
    const res = await getProductDetail(Number(route.params.id))
    product.value = res.data?.product ?? null
  } catch {
    product.value = null
  } finally {
    loading.value = false
  }
  if (product.value) {
    await loadReviews()
  }
}

async function handleAdd() {
  if (!product.value) return
  // 加入购物车需登录
  if (!userStore.token) {
    ElMessage.warning('请先登录')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  try {
    await cartStore.add(product.value.productId, quantity.value)
    ElMessage.success(`已加入购物车：${product.value.productName} × ${quantity.value}`)
  } catch {
    // 请求层已提示（如库存不足）
  }
}

onMounted(load)
</script>

<style scoped>
.detail-page {
  width: 1200px;
  margin: 0 auto;
  padding: 16px 0 32px;
}
.detail-card {
  border-radius: 8px;
}
.detail-body {
  display: flex;
  gap: 40px;
  padding: 12px;
}
.detail-img {
  width: 420px;
  height: 420px;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f6f8;
  flex-shrink: 0;
}
.detail-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.detail-info {
  flex: 1;
}
.detail-name {
  font-size: 22px;
  color: #333;
  margin: 4px 0 12px;
}
.detail-price {
  background: #fff3ee;
  border-radius: 6px;
  padding: 14px 16px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 10px;
}
.detail-price .now {
  color: #ff5000;
  font-size: 26px;
  font-weight: 700;
}
.detail-price .tag {
  color: #ff8a4c;
  font-size: 13px;
  background: #fff0e6;
  padding: 2px 8px;
  border-radius: 4px;
}
.detail-meta {
  display: flex;
  gap: 24px;
  color: #999;
  font-size: 13px;
  margin-bottom: 12px;
}
.detail-shop {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #555;
  font-size: 14px;
  margin-bottom: 12px;
  cursor: pointer;
}
.detail-desc {
  color: #666;
  font-size: 14px;
  line-height: 1.7;
  margin-bottom: 20px;
}
.detail-actions {
  display: flex;
  align-items: center;
  gap: 14px;
}
.qty-label {
  color: #555;
  font-size: 14px;
}
.add-btn {
  background: #ff5000;
  border-color: #ff5000;
}
.pickup-tip {
  margin-top: 16px;
  color: #b0b4bb;
  font-size: 12px;
}
.review-card {
  margin-top: 16px;
  border-radius: 8px;
}
.review-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.review-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}
.review-summary {
  display: flex;
  align-items: center;
  gap: 8px;
}
.review-avg {
  color: #ff5000;
  font-size: 20px;
  font-weight: 700;
}
.review-stars {
  --el-rate-icon-margin: 2px;
  scale: 0.9;
}
.review-count {
  color: #999;
  font-size: 13px;
}
.review-list {
  display: flex;
  flex-direction: column;
}
.review-row-item {
  display: flex;
  gap: 14px;
  padding: 16px 0;
  border-top: 1px solid #f5f6f8;
}
.review-user {
  display: flex;
  gap: 10px;
  min-width: 180px;
}
.review-user-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.review-uname {
  color: #333;
  font-size: 14px;
}
.review-meta-lower {
  display: flex;
  align-items: center;
  gap: 8px;
}
.review-stars-sm {
  --el-rate-icon-margin: 2px;
  scale: 0.8;
  transform-origin: left center;
}
.review-time {
  color: #999;
  font-size: 12px;
}
.review-content {
  flex: 1;
  color: #555;
  font-size: 14px;
  line-height: 1.7;
  margin: 0;
  word-break: break-word;
}
</style>