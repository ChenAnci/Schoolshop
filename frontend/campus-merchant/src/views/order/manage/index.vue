<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>订单列表</span>
        </div>
      </template>

      <!-- 状态筛选 Tabs -->
      <el-tabs v-model="activeStatus" @tab-change="loadList(1)">
        <el-tab-pane label="全部" name="" />
        <el-tab-pane label="待商家接单" name="0" />
        <el-tab-pane label="待自提" name="1" />
        <el-tab-pane label="已完成" name="2" />
        <el-tab-pane label="已取消" name="3" />
      </el-tabs>

      <!-- 订单列表 -->
      <el-table v-loading="loading" :data="rows" style="width: 100%">
        <el-table-column label="订单号" prop="orderNo" min-width="180" show-overflow-tooltip />
        <el-table-column label="买家" prop="userName" min-width="100" />
        <el-table-column label="实付金额" width="110">
          <template #default="{ row }">
            <span class="amount">¥{{ Number(row.payAmount ?? row.totalAmount ?? 0).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="ORDER_STATUS[row.status]?.type || 'info'">
              {{ ORDER_STATUS[row.status]?.text || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" prop="createTime" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === '0'" link type="success" @click="handleAccept(row)">接单</el-button>
            <el-button v-if="row.status === '0'" link type="danger" @click="handleCancel(row)">拒单</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="loadList()"
        @size-change="loadList(1)"
      />
    </el-card>

    <!-- 订单详情弹窗 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="640px">
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号" :span="2">{{ detail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="ORDER_STATUS[detail.status]?.type || 'info'">
              {{ ORDER_STATUS[detail.status]?.text || detail.status }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="买家">{{ detail.userName }}</el-descriptions-item>
          <el-descriptions-item label="实付金额">
            <span class="amount">¥{{ Number(detail.payAmount ?? detail.totalAmount ?? 0).toFixed(2) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ detail.createTime }}</el-descriptions-item>
          <el-descriptions-item label="收货人" :span="2">{{ detail.receiverName }} {{ detail.receiverPhone }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.receiverAddress" label="收货地址" :span="2">
            {{ detail.receiverAddress }}
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.takeTime" label="取货时间">{{ detail.takeTime }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.finishTime" label="完成时间">{{ detail.finishTime }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.cancelTime" label="取消时间">{{ detail.cancelTime }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.remark" label="备注" :span="2">{{ detail.remark }}</el-descriptions-item>
        </el-descriptions>

        <el-table :data="detailItems" style="width: 100%; margin-top: 16px" size="small">
          <el-table-column label="商品" min-width="180">
            <template #default="{ row }">
              <div class="item-cell">
                <el-image v-if="row.productImage" :src="row.productImage" fit="cover" class="item-img" />
                <div class="item-info">
                  <span class="item-name">{{ row.productName }}</span>
                  <span class="item-price">¥{{ Number(row.price).toFixed(2) }} × {{ row.quantity }}</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="小计" width="120">
            <template #default="{ row }">
              <span class="amount">¥{{ Number(row.subtotal).toFixed(2) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getShopOrderList,
  getShopOrderDetail,
  acceptOrder,
  cancelShopOrder,
  ORDER_STATUS,
  type ShopOrder,
  type ShopOrderItem
} from '@/api/order'

const loading = ref(false)
const activeStatus = ref('')
const rows = ref<ShopOrder[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const detailVisible = ref(false)
const detail = ref<ShopOrder | null>(null)
const detailItems = ref<ShopOrderItem[]>([])

async function loadList(page = pageNum.value) {
  loading.value = true
  try {
    const data = await getShopOrderList(activeStatus.value)
    rows.value = data.rows ?? []
    total.value = data.total ?? 0
    pageNum.value = page
  } finally {
    loading.value = false
  }
}

async function openDetail(row: ShopOrder) {
  const res = await getShopOrderDetail(row.orderId)
  if (res.data) {
    detail.value = res.data.order
    detailItems.value = res.data.items ?? []
    detailVisible.value = true
  }
}

async function handleAccept(row: ShopOrder) {
  await ElMessageBox.confirm(`确认接单订单「${row.orderNo}」？`, '接单', { type: 'warning' })
  await acceptOrder(row.orderId)
  ElMessage.success('已接单')
  loadList()
}

async function handleCancel(row: ShopOrder) {
  await ElMessageBox.confirm(`确认拒单并取消订单「${row.orderNo}」？`, '拒单', { type: 'warning' })
  await cancelShopOrder(row.orderId)
  ElMessage.success('已取消订单')
  loadList()
}

onMounted(() => loadList(1))
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.amount {
  color: #f56c6c;
  font-weight: 600;
}

.item-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.item-img {
  width: 48px;
  height: 48px;
  border-radius: 6px;
  flex-shrink: 0;
}

.item-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.item-name {
  font-size: 13px;
}

.item-price {
  font-size: 12px;
  color: #909399;
}
</style>
