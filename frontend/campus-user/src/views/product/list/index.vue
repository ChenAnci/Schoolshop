<template>
  <div class="list-page">
    <div class="filter-bar">
      <span class="filter-label">分类：</span>
      <el-radio-group v-model="activeCategory" @change="load(1)">
        <el-radio-button :value="undefined">全部</el-radio-button>
        <el-radio-button v-for="c in categories" :key="c.categoryId" :value="c.categoryId">
          {{ c.categoryName }}
        </el-radio-button>
      </el-radio-group>
    </div>

    <div class="list-meta">
      <span>共 {{ total }} 件商品</span>
      <span v-if="keyword">关键词：{{ keyword }}</span>
    </div>

    <el-empty v-if="!loading && products.length === 0" description="没有找到相关商品" />
    <div v-else class="product-grid">
      <ProductCard v-for="p in products" :key="p.productId" :product="p" />
    </div>

    <div v-if="total > pageSize" class="pager">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next, total"
        @current-change="load()"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import ProductCard from '@/components/ProductCard.vue'
import { getCategoryList, getProductList, type Category, type Product } from '@/api/market'

const route = useRoute()

const categories = ref<Category[]>([])
const products = ref<Product[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(12)
const loading = ref(false)
const keyword = ref((route.query.keyword as string) || '')
const activeCategory = ref<number | undefined>(route.query.categoryId ? Number(route.query.categoryId) : undefined)

async function load(page = currentPage.value) {
  loading.value = true
  try {
    const res = await getProductList({
      pageNum: page,
      pageSize: pageSize.value,
      categoryId: activeCategory.value,
      productName: keyword.value || undefined
    })
    total.value = res.total ?? 0
    products.value = res.rows ?? []
    currentPage.value = page
  } catch {
    // 查询失败保持当前数据
  } finally {
    loading.value = false
  }
}

watch(
  () => route.query,
  (q) => {
    keyword.value = (q.keyword as string) || ''
    activeCategory.value = q.categoryId ? Number(q.categoryId) : undefined
    load(1)
  }
)

onMounted(async () => {
  try {
    const catRes = await getCategoryList()
    categories.value = catRes.data ?? []
  } catch {
    // 分类加载失败不阻断商品加载
  }
  load(1)
})
</script>

<style scoped>
.list-page {
  width: 1200px;
  margin: 0 auto;
  padding: 16px 0 32px;
}
.filter-bar {
  background: #fff;
  border-radius: 8px;
  padding: 14px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}
.filter-label {
  color: #555;
  font-size: 14px;
  flex-shrink: 0;
}
.list-meta {
  color: #999;
  font-size: 13px;
  margin: 14px 2px 12px;
}
.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.pager {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>