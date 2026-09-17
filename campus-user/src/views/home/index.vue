<template>
  <div class="home-page">
    <!-- 轮播 Banner -->
    <el-carousel height="320px" class="banner">
      <el-carousel-item v-for="(b, i) in banners" :key="i">
        <img :src="b.image" :alt="b.text" class="banner-img" />
        <div class="banner-text">{{ b.text }}</div>
      </el-carousel-item>
    </el-carousel>

    <!-- 分类快捷入口 -->
    <div class="section">
      <h3 class="section-title">商品分类</h3>
      <div class="category-row">
        <div
          v-for="c in categories"
          :key="c.categoryId"
          class="category-item"
          @click="router.push({ path: '/product/list', query: { categoryId: c.categoryId } })"
        >
          <el-icon :size="28"><Grid /></el-icon>
          <span>{{ c.categoryName }}</span>
        </div>
      </div>
    </div>

    <!-- 推荐商品 -->
    <div class="section">
      <h3 class="section-title">好物推荐</h3>
      <el-empty v-if="loading && recommend.length === 0" :image-size="60" description="加载中..." />
      <div v-else-if="recommend.length === 0" class="empty-tip">暂无在售商品</div>
      <div v-else class="product-grid">
        <ProductCard v-for="p in recommend" :key="p.productId" :product="p" />
      </div>
    </div>

    <!-- 店铺 -->
    <div class="section">
      <h3 class="section-title">校内店铺</h3>
      <div v-if="shops.length === 0" class="empty-tip">暂无店铺</div>
      <div v-else class="shop-grid">
        <div
          v-for="s in shops"
          :key="s.shopId"
          class="shop-card"
          @click="router.push(`/shop/detail/${s.shopId}`)"
        >
          <img :src="shopImageOf(s)" :alt="s.shopName" class="shop-logo" />
          <div class="shop-info">
            <div class="shop-name">{{ s.shopName }}</div>
            <div class="shop-desc">{{ s.shopDesc || '暂无简介' }}</div>
            <div class="shop-count">在售 {{ s.productCount }} 件</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import ProductCard from '@/components/ProductCard.vue'
import { getCategoryList, getProductList, getShopList, productImageOf, type Category, type Product, type Shop } from '@/api/market'

const router = useRouter()

const IMG = 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image'
const banners = [
  {
    text: '毕业季 · 闲置好物大促',
    image: `${IMG}?prompt=${encodeURIComponent('campus flea market with students browsing second-hand goods booth, warm sunlight, cheerful atmosphere, wide banner')}&image_size=landscape_16_9`
  },
  {
    text: '数码好物 低至五折',
    image: `${IMG}?prompt=${encodeURIComponent('second-hand electronics table with phone, keyboard and headphones on campus, product display banner, bright clean')}&image_size=landscape_16_9`
  }
]

const categories = ref<Category[]>([])
const recommend = ref<Product[]>([])
const shops = ref<Shop[]>([])
const loading = ref(false)

const shopImageOf = productImageOf

async function load() {
  try {
    loading.value = true
    const [catRes, prodRes, shopRes] = await Promise.all([
      getCategoryList(),
      getProductList({ pageNum: 1, pageSize: 8 }),
      getShopList()
    ])
    categories.value = catRes.data ?? []
    recommend.value = prodRes.rows ?? []
    shops.value = shopRes.data ?? []
  } catch {
    // 接口失败时保持空态，不阻塞页面
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.home-page {
  width: 1200px;
  margin: 0 auto;
  padding: 16px 0 32px;
}
.banner {
  border-radius: 8px;
  overflow: hidden;
}
.banner-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.banner-text {
  position: absolute;
  left: 48px;
  bottom: 40px;
  color: #fff;
  font-size: 28px;
  font-weight: 700;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.4);
}
.section {
  margin-top: 24px;
}
.section-title {
  font-size: 18px;
  color: #333;
  margin: 0 0 14px;
  padding-left: 10px;
  border-left: 4px solid #ff6a00;
}
.category-row {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}
.category-item {
  background: #fff;
  border-radius: 8px;
  padding: 22px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #555;
  cursor: pointer;
  transition: box-shadow 0.2s;
}
.category-item:hover {
  color: #ff6a00;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);
}
.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.empty-tip {
  color: #999;
  font-size: 14px;
  padding: 24px 0;
  text-align: center;
  background: #fff;
  border-radius: 8px;
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