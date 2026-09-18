<template>
  <div class="app-container">
    <el-card shadow="never">
      <div class="toolbar">
        <el-input
          v-model="query.orderNo"
          placeholder="按订单号搜索"
          clearable
          style="width: 220px"
          @keyup.enter="loadList(1)"
          @clear="loadList(1)"
        />
        <el-select
          v-model="query.status"
          placeholder="全部状态"
          clearable
          style="width: 140px"
          @change="loadList(1)"
          @clear="loadList(1)"
        >
          <el-option v-for="(v, k) in ORDER_STATUS" :key="k" :label="v.text" :value="k" />
        </el-select>
        <el-select
          v-model="query.auditFlag"
          placeholder="全部审查状态"
          clearable
          style="width: 140px"
          @change="loadList(1)"
          @clear="loadList(1)"
        >
          <el-option v-for="(v, k) in AUDIT_FLAG" :key="k" :label="v.text" :value="k" />
        </el-select>
        <el-button type="primary" icon="Search" @click="loadList(1)">搜索</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" style="width: 100%">
        <el-table-column prop="orderNo" label="订单号" min-width="150" show-overflow-tooltip />
        <el-table-column prop="userName" label="买家" width="100" />
        <el-table-column prop="shopName" label="店铺" width="130" show-overflow-tooltip />
        <el-table-column label="实付" width="90">
          <template #default="{ row }">¥{{ Number(row.payAmount ?? row.totalAmount).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="ORDER_STATUS[row.status]?.type ?? 'info'">
              {{ ORDER_STATUS[row.status]?.text ?? row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审查" width="80">
          <template #default="{ row }">
            <el-tag :type="AUDIT_FLAG[row.auditFlag]?.type ?? 'info'">
              {{ AUDIT_FLAG[row.auditFlag ?? '0']?.text ?? '未审查' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="auditRemark" label="审查/仲裁意见" min-width="140" show-overflow-tooltip />
        <el-table-column prop="createTime" label="下单时间" width="160" />
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="canReview(row)"
              link
              type="warning"
              @click="handleStartAudit(row)"
            >
              进入审查
            </el-button>
            <el-button
              v-if="row.status === '4'"
              link
              type="danger"
              @click="openArbitrate(row)"
            >
              仲裁
            </el-button>
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

    <!-- 仲裁弹窗 -->
    <el-dialog v-model="arbitrateVisible" title="订单仲裁" width="480px" :close-on-click-modal="false">
      <el-form label-width="90px">
        <el-form-item label="处理结论" required>
          <el-radio-group v-model="arbitrateForm.action">
            <el-radio value="complete">强制已完成</el-radio>
            <el-radio value="cancel">强制已取消</el-radio>
            <el-radio value="restore">恢复流转</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="仲裁意见">
          <el-input
            v-model="arbitrateForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请填写处理结论与理由（写入审计）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="arbitrateVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleArbitrate">提交仲裁</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listOrder, startAudit, arbitrate, ORDER_STATUS, AUDIT_FLAG, type OrderData } from '@/api/market/order'

const loading = ref(false)
const submitting = ref(false)
const rows = ref<OrderData[]>([])
const total = ref(0)
const query = reactive<{ orderNo?: string; status?: string; auditFlag?: string; pageNum: number; pageSize: number }>({
  pageNum: 1,
  pageSize: 10
})

const arbitrateVisible = ref(false)
const arbitrateForm = reactive<{ action: string; remark: string }>({ action: 'complete', remark: '' })
let current: OrderData | null = null

/** 可进入审查：未审查且当前非 4/5 */
function canReview(row: OrderData) {
  return (row.auditFlag ?? '0') === '0' && row.status !== '4' && row.status !== '5'
}

async function loadList(page = query.pageNum) {
  loading.value = true
  try {
    query.pageNum = page
    const data = await listOrder({ ...query })
    rows.value = data.rows ?? []
    total.value = data.total ?? 0
  } finally {
    loading.value = false
  }
}

async function handleStartAudit(row: OrderData) {
  try {
    const { value } = await ElMessageBox.prompt('进入审查中，可填写异常原因（可选）', '进入审查', {
      inputPlaceholder: '如：用户投诉未收到货',
      confirmButtonText: '确认审查',
      cancelButtonText: '取消'
    })
    await startAudit(row.orderId!, value?.trim() || '')
    ElMessage.success('已进入审查中')
    loadList()
  } catch (e) {
    /* 取消 */
  }
}

function openArbitrate(row: OrderData) {
  current = row
  arbitrateForm.action = 'complete'
  arbitrateForm.remark = ''
  arbitrateVisible.value = true
}

async function handleArbitrate() {
  if (!current) return
  submitting.value = true
  try {
    await arbitrate(current.orderId!, arbitrateForm.action, arbitrateForm.remark.trim())
    ElMessage.success('仲裁完成')
    arbitrateVisible.value = false
    loadList()
  } finally {
    submitting.value = false
  }
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
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>