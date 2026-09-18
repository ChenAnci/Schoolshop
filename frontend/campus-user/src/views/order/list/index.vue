<template>
  <div class="order-page">
    <h2 class="page-title">我的订单</h2>

    <el-card shadow="never" class="order-card">
      <el-tabs v-model="activeTab" @tab-change="load">
        <el-tab-pane v-for="tab in tabs" :key="tab.name" :label="tab.label" :name="tab.name" />
      </el-tabs>

      <el-empty v-if="loading" description="加载中..." />
      <el-empty v-else-if="orders.length === 0" description="暂无订单，快去挑选心仪的宝贝吧">
        <el-button type="primary" @click="router.push('/product/list')">去逛逛</el-button>
      </el-empty>

      <div v-else class="order-list">
        <el-card v-for="order in orders" :key="order.orderId" shadow="never" class="order-item">
          <div class="order-head">
            <div class="order-no">订单号：{{ order.orderNo }}</div>
            <div class="order-shop">
              <el-icon><Shop /></el-icon>{{ order.shopName }}
            </div>
            <el-tag :type="statusMeta(order.status).type" size="small">
              {{ statusMeta(order.status).text }}
            </el-tag>
          </div>

          <div class="order-body">
            <div class="order-meta">
              <span>下单时间：{{ formatTime(order.createTime) }}</span>
              <span v-if="order.receiverName">取货人：{{ order.receiverName }} {{ order.receiverPhone }}</span>
            </div>
            <div class="order-amount">
              实付：<span class="total">￥{{ Number(order.payAmount ?? order.totalAmount).toFixed(2) }}</span>
            </div>
            <div class="order-actions">
              <el-button v-if="order.status === '0'" size="small" type="danger" plain @click="handleCancel(order)">
                取消订单
              </el-button>
              <el-button v-if="order.status === '1'" size="small" type="primary" @click="handleConfirm(order)">
                确认收货
              </el-button>
              <el-button v-if="order.status === '2'" size="small" type="warning" plain @click="openReview(order)">
                去评价
              </el-button>
              <el-button size="small" type="primary" plain @click="showDetail(order)">查看详情</el-button>
            </div>
          </div>
        </el-card>
      </div>
    </el-card>

    <!-- 订单详情 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="720px">
      <template v-if="detail">
        <div class="detail-head">
          <div>订单号：{{ detail.order.orderNo }}</div>
          <el-tag :type="statusMeta(detail.order.status).type" size="small">
            {{ statusMeta(detail.order.status).text }}
          </el-tag>
        </div>
        <el-table :data="detail.items" class="detail-items">
          <el-table-column label="商品" min-width="300">
            <template #default="{ row }">
              <div class="detail-item">
                <img
                  :src="productImageOf({ productName: row.productName, productImage: row.productImage })"
                  :alt="row.productName"
                  class="detail-item-img"
                />
                <span>{{ row.productName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="单价" width="100">
            <template #default="{ row }">￥{{ row.price }}</template>
          </el-table-column>
          <el-table-column label="数量" width="80" prop="quantity" />
          <el-table-column label="小计" width="100">
            <template #default="{ row }">￥{{ Number(row.subtotal).toFixed(2) }}</template>
          </el-table-column>
        </el-table>
        <div class="detail-footer">
          <div class="detail-meta" v-if="detail.order.receiverName">
            {{ detail.order.receiverName }}&nbsp;{{ detail.order.receiverPhone }}&nbsp;{{ detail.order.receiverAddress }}
          </div>
          <div class="detail-amount">
            订单金额：<span class="total">￥{{ Number(detail.order.payAmount ?? detail.order.totalAmount).toFixed(2) }}</span>
          </div>
        </div>
      </template>
    </el-dialog>

    <!-- 评价弹窗 -->
    <el-dialog v-model="reviewVisible" title="评价商品" width="640px">
      <el-empty v-if="reviewItems.length === 0" description="本订单商品都已评价过了" :image-size="80" />
      <div v-else class="review-list">
        <div v-for="item in reviewItems" :key="item.itemId" class="review-item">
          <div class="review-row">
            <img :src="productImageOf({ productName: item.productName, productImage: item.productImage })"
              :alt="item.productName" class="review-img" />
            <span class="review-name">{{ item.productName }}</span>
          </div>
          <div class="review-rate">
            <span class="review-label">商品评分</span>
            <el-rate v-model="item.rating" allow-half :max="5" />
          </div>
          <el-input v-model="item.content" type="textarea" :rows="3" maxlength="300" show-word-limit
            placeholder="说说商品的质量、新旧程度、是否与描述一致（选填）" />
        </div>
      </div>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :disabled="reviewItems.length === 0" :loading="reviewSubmitting" @click="handleSubmitReview">
          提交评价
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Shop } from '@element-plus/icons-vue'
import {
  getOrderList,
  getOrderDetail,
  cancelOrder,
  confirmOrder,
  submitReview,
  productImageOf,
  ORDER_STATUS,
  type Order,
  type OrderItem
} from '@/api/market'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

/** 待评价条目的可编辑结构 */
interface ReviewableItem extends OrderItem {
  rating: number
  content: string
}

const tabs = [
  { label: '全部', name: 'all' },
  { label: '待商家接单', name: 'pending' },
  { label: '待自提', name: 'ongoing' },
  { label: '已完成', name: 'done' },
  { label: '已取消', name: 'cancelled' }
]
const activeTab = ref('all')
const orders = ref<Order[]>([])
const loading = ref(true)

const detailVisible = ref(false)
const detail = ref<{ order: Order; items: OrderItem[] } | null>(null)

const TAB_STATUS: Record<string, string> = {
  all: '',
  pending: '0',
  ongoing: '1',
  done: '2',
  cancelled: '3'
}

function statusMeta(status: string) {
  return ORDER_STATUS[status] ?? { text: '未知', type: 'info' as const }
}

function formatTime(t?: string) {
  if (!t) return '-'
  return t.replace('T', ' ').slice(0, 19)
}

async function load() {
  if (!userStore.token) {
    router.replace('/login')
    return
  }
  loading.value = true
  try {
    const status = TAB_STATUS[activeTab.value]
    const res = await getOrderList(status)
    orders.value = res.rows ?? []
  } catch {
    // 请求层已提示
  } finally {
    loading.value = false
  }
}

async function showDetail(order: Order) {
  try {
    const res = await getOrderDetail(order.orderId)
    detail.value = res.data ?? null
    detailVisible.value = true
  } catch {
    // 请求层已提示
  }
}

function handleCancel(order: Order) {
  ElMessageBox.confirm(`确定取消订单【${order.orderNo}】吗？`, '提示', { type: 'warning' })
    .then(async () => {
      try {
        await cancelOrder(order.orderId)
        ElMessage.success('订单已取消')
        await load()
      } catch {
        // 请求层已提示
      }
    })
    .catch(() => undefined)
}

async function handleConfirm(order: Order) {
  ElMessageBox.confirm(`确认已收到【${order.orderNo}】的货品吗？确认后订单将完成。`, '确认收货', {
    type: 'warning'
  })
    .then(async () => {
      try {
        await confirmOrder(order.orderId)
        ElMessage.success('已确认收货')
        await load()
      } catch {
        // 请求层已提示
      }
    })
    .catch(() => undefined)
}

const reviewVisible = ref(false)
const reviewSubmitting = ref(false)
const reviewItems = ref<ReviewableItem[]>([])

/** 打开评价弹窗：加载订单明细，仅展示未评价条目 */
async function openReview(order: Order) {
  try {
    const res = await getOrderDetail(order.orderId)
    const items = res.data?.items ?? []
    reviewItems.value = items
      .filter((it) => !it.reviewed)
      .map((it) => ({ ...it, rating: 5, content: '' }))
    reviewVisible.value = true
  } catch {
    // 请求层已提示
  }
}

/** 提交评价 */
async function handleSubmitReview() {
  const reviews = reviewItems.value.map((it) => ({
    orderItemId: it.itemId,
    rating: Math.round(it.rating),
    content: (it.content ?? '').trim()
  }))
  reviewSubmitting.value = true
  try {
    const res = await submitReview(reviews)
    ElMessage.success(`评价成功${res.data?.count ? `（${res.data.count} 条）` : ''}`)
    reviewVisible.value = false
    await load()
  } catch {
    // 请求层已提示
  } finally {
    reviewSubmitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.order-page {
  width: 1200px;
  margin: 0 auto;
  padding: 16px 0 32px;
}
.page-title {
  font-size: 20px;
  color: #333;
  margin: 0 0 14px;
}
.order-card {
  border-radius: 8px;
  min-height: 300px;
}
.order-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.order-item {
  border-radius: 8px;
}
.order-head {
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px dashed #f0f0f0;
  padding-bottom: 10px;
  font-size: 13px;
  color: #666;
}
.order-no {
  color: #999;
}
.order-shop {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #333;
}
.order-body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}
.order-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #999;
  font-size: 12px;
}
.total {
  color: #ff5000;
  font-size: 18px;
  font-weight: 700;
}
.order-actions {
  display: flex;
  gap: 8px;
}
.detail-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  color: #555;
}
.detail-item {
  display: flex;
  align-items: center;
  gap: 10px;
}
.detail-item-img {
  width: 48px;
  height: 48px;
  border-radius: 6px;
  object-fit: cover;
  background: #f5f6f8;
}
.detail-footer {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.detail-meta {
  color: #666;
  font-size: 13px;
}
.detail-amount {
  color: #555;
  font-size: 14px;
}
.review-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: 60vh;
  overflow-y: auto;
}
.review-item {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 12px;
}
.review-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.review-img {
  width: 44px;
  height: 44px;
  border-radius: 6px;
  object-fit: cover;
  background: #f5f6f8;
}
.review-name {
  color: #333;
  font-size: 14px;
}
.review-rate {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.review-label {
  color: #666;
  font-size: 13px;
}
</style>