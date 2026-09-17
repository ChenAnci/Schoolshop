<template>
  <div class="app-container">
    <el-card shadow="never">
      <div class="toolbar">
        <el-input
          v-model="query.productName"
          placeholder="按商品名称搜索"
          clearable
          style="width: 220px"
          @keyup.enter="loadList(1)"
          @clear="loadList(1)"
        />
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px" @change="loadList(1)">
          <el-option label="上架" value="0" />
          <el-option label="下架" value="1" />
        </el-select>
        <el-button type="primary" icon="Search" @click="loadList(1)">搜索</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" style="width: 100%">
        <el-table-column label="图片" width="80">
          <template #default="{ row }">
            <el-image :src="imageOf(row)" fit="cover" class="prod-img" :preview-src-list="[imageOf(row)]" preview-teleported />
          </template>
        </el-table-column>
        <el-table-column prop="productName" label="商品名称" min-width="170" show-overflow-tooltip />
        <el-table-column prop="shopName" label="所属店铺" width="140" show-overflow-tooltip />
        <el-table-column prop="categoryName" label="分类" width="110" />
        <el-table-column label="价格" width="100">
          <template #default="{ row }">¥{{ Number(row.price).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="70" />
        <el-table-column prop="sales" label="销量" width="70" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="PRODUCT_STATUS[row.status]?.type ?? 'info'">
              {{ PRODUCT_STATUS[row.status]?.text ?? row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="165" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link :type="row.status === '0' ? 'warning' : 'success'" @click="handleToggle(row)">
              {{ row.status === '0' ? '下架' : '上架' }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadList"
        @size-change="loadList(1)"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listProduct, setProductStatus, delProduct, type ProductData } from '@/api/market/product'

const loading = ref(false)
const rows = ref<ProductData[]>([])
const total = ref(0)
const query = reactive<{ productName?: string; status?: string; pageNum: number; pageSize: number }>({
  pageNum: 1,
  pageSize: 10
})

/** 商品状态字典（0上架 1下架） */
const PRODUCT_STATUS: Record<string, { text: string; type: 'primary' | 'success' | 'info' }> = {
  '0': { text: '上架', type: 'success' },
  '1': { text: '下架', type: 'info' }
}

/** 商品主图：无图时用平台兜底图 */
const IMG = 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image'
function imageOf(p: ProductData): string {
  if (p.productImage && p.productImage.trim()) return p.productImage
  return `${IMG}?prompt=${encodeURIComponent(
    `a clean product photo of ${p.productName || 'item'} for a campus second-hand marketplace card, soft daylight, neat background`
  )}&image_size=square_hd`
}

async function loadList(page = query.pageNum) {
  loading.value = true
  try {
    query.pageNum = page
    const data = await listProduct({ ...query })
    rows.value = data.rows ?? []
    total.value = data.total ?? 0
  } finally {
    loading.value = false
  }
}

function handleToggle(row: ProductData) {
  const next = row.status === '0' ? '1' : '0'
  const text = next === '0' ? '上架' : '下架'
  ElMessageBox.confirm(`确定将「${row.productName}」${text}吗？`, '提示')
    .then(async () => {
      await setProductStatus(row.productId!, next)
      ElMessage.success(`${text}成功`)
      loadList()
    })
    .catch(() => {})
}

function handleDelete(row: ProductData) {
  ElMessageBox.confirm(`确定删除商品「${row.productName}」吗？删除后不可恢复。`, '警告', { type: 'warning' })
    .then(async () => {
      await delProduct(String(row.productId))
      ElMessage.success('删除成功')
      loadList()
    })
    .catch(() => {})
}

onMounted(() => loadList())
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.prod-img {
  width: 56px;
  height: 56px;
  border-radius: 6px;
  display: block;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>