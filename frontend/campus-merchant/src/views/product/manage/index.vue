<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>我的商品</span>
          <el-button type="primary" icon="Plus" @click="$router.push('/product/publish')">发布商品</el-button>
        </div>
      </template>

      <div class="toolbar">
        <el-input
          v-model="query.productName"
          placeholder="按商品名称搜索"
          clearable
          class="search-input"
          @keyup.enter="loadList(1)"
          @clear="loadList(1)"
        />
        <el-select v-model="query.status" placeholder="全部状态" clearable class="status-select" @change="loadList(1)">
          <el-option label="上架" value="0" />
          <el-option label="下架" value="1" />
        </el-select>
        <el-button type="primary" icon="Search" @click="loadList(1)">搜索</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" style="width: 100%">
        <el-table-column label="图片" width="90">
          <template #default="{ row }">
            <el-image
              :src="imageOf(row)"
              fit="cover"
              class="prod-img"
              :preview-src-list="[imageOf(row)]"
              preview-teleported
            />
          </template>
        </el-table-column>
        <el-table-column prop="productName" label="商品名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="categoryName" label="分类" width="120" />
        <el-table-column label="价格" width="110">
          <template #default="{ row }">¥{{ Number(row.price).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="80" />
        <el-table-column prop="sales" label="销量" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="PRODUCT_STATUS[row.status]?.type ?? 'info'">
              {{ PRODUCT_STATUS[row.status]?.text ?? row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/product/publish?productId=${row.productId}`)">编辑</el-button>
            <el-button link :type="row.status === '0' ? 'warning' : 'success'" @click="handleToggle(row)">
              {{ row.status === '0' ? '下架' : '上架' }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadList"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getShopProductList,
  setShopProductStatus,
  delShopProduct,
  PRODUCT_STATUS,
  type ShopProduct
} from '@/api/product'

const loading = ref(false)
const rows = ref<ShopProduct[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const query = reactive<{ productName?: string; status?: string }>({})

/** 商品主图：无图时用平台兜底图 */
const IMG = 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image'
function imageOf(p: ShopProduct): string {
  if (p.productImage && p.productImage.trim()) return p.productImage
  return `${IMG}?prompt=${encodeURIComponent(
    `a clean product photo of ${p.productName || 'item'} for a campus second-hand marketplace card, soft daylight, neat background`
  )}&image_size=square_hd`
}

async function loadList(page = pageNum.value) {
  loading.value = true
  try {
    const data = await getShopProductList({ ...query } as any)
    rows.value = data.rows ?? []
    total.value = data.total ?? 0
    pageNum.value = page
  } finally {
    loading.value = false
  }
}

function handleToggle(row: ShopProduct) {
  const next = row.status === '0' ? '1' : '0'
  const text = next === '0' ? '上架' : '下架'
  ElMessageBox.confirm(`确定将「${row.productName}」${text}吗？`, '提示')
    .then(async () => {
      await setShopProductStatus(row.productId, next)
      ElMessage.success(`${text}成功`)
      loadList()
    })
    .catch(() => {})
}

function handleDelete(row: ShopProduct) {
  ElMessageBox.confirm(`确定删除商品「${row.productName}」吗？`, '警告', { type: 'warning' })
    .then(async () => {
      await delShopProduct([row.productId])
      ElMessage.success('删除成功')
      loadList()
    })
    .catch(() => {})
}

onMounted(() => loadList())
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}
.search-input {
  width: 240px;
}
.status-select {
  width: 140px;
}
.prod-img {
  width: 60px;
  height: 60px;
  border-radius: 6px;
  display: block;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>