<template>
  <div class="app-container">
    <el-card shadow="never">
      <div class="toolbar">
        <el-input
          v-model="query.userName"
          placeholder="按账号/昵称搜索"
          clearable
          style="width: 200px"
          @keyup.enter="loadList(1)"
          @clear="loadList(1)"
        />
        <el-select
          v-model="query.status"
          placeholder="全部账号状态"
          clearable
          style="width: 140px"
          @change="loadList(1)"
          @clear="loadList(1)"
        >
          <el-option v-for="(v, k) in USER_STATUS" :key="k" :label="v.text" :value="k" />
        </el-select>
        <el-select
          v-model="query.merchantApplyStatus"
          placeholder="全部入驻状态"
          clearable
          style="width: 140px"
          @change="loadList(1)"
          @clear="loadList(1)"
        >
          <el-option v-for="(v, k) in MERCHANT_APPLY_STATUS" :key="k" :label="v.text" :value="k" />
        </el-select>
        <el-button type="primary" icon="Search" @click="loadList(1)">搜索</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" style="width: 100%">
        <el-table-column prop="userId" label="ID" width="70" />
        <el-table-column prop="userName" label="账号" width="120" show-overflow-tooltip />
        <el-table-column prop="nickName" label="昵称" width="120" show-overflow-tooltip />
        <el-table-column prop="phonenumber" label="手机号" width="120" />
        <el-table-column label="账号状态" width="90">
          <template #default="{ row }">
            <el-tag :type="USER_STATUS[row.status]?.type ?? 'success'">
              {{ USER_STATUS[row.status]?.text ?? '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="入驻状态" width="90">
          <template #default="{ row }">
            <el-tag :type="MERCHANT_APPLY_STATUS[row.merchantApplyStatus ?? '0']?.type ?? 'info'">
              {{ MERCHANT_APPLY_STATUS[row.merchantApplyStatus ?? '0']?.text ?? '未申请' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="merchantApplyRemark" label="入驻申请/审核意见" min-width="150" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === '0'"
              link
              type="danger"
              @click="handleChangeStatus(row, '1')"
            >
              停用
            </el-button>
            <el-button
              v-if="row.status === '1'"
              link
              type="success"
              @click="handleChangeStatus(row, '0')"
            >
              启用
            </el-button>
            <el-button link type="warning" @click="handleResetPwd(row)">重置密码</el-button>
            <template v-if="row.merchantApplyStatus === '1'">
              <el-button link type="success" @click="handleAudit(row, 'approve')">通过</el-button>
              <el-button link type="danger" @click="openReject(row)">驳回</el-button>
            </template>
            <el-button link type="info" @click="handleDelete(row)">删除</el-button>
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

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="resetVisible" title="重置密码" width="420px" :close-on-click-modal="false">
      <el-form label-width="80px">
        <el-form-item label="登录账号">
          <el-input :model-value="current?.userName" disabled />
        </el-form-item>
        <el-form-item label="新密码" required>
          <el-input
            v-model="newPassword"
            type="password"
            show-password
            placeholder="请输入新密码"
            maxlength="50"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleResetConfirm">确认重置</el-button>
      </template>
    </el-dialog>

    <!-- 驳回审核弹窗 -->
    <el-dialog v-model="rejectVisible" title="驳回入驻申请" width="480px" :close-on-click-modal="false">
      <el-form label-width="90px">
        <el-form-item label="登录账号">
          <el-input :model-value="current?.userName" disabled />
        </el-form-item>
        <el-form-item label="驳回意见" required>
          <el-input
            v-model="rejectRemark"
            type="textarea"
            :rows="3"
            placeholder="请填写驳回原因（对商家可见）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleRejectConfirm">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listUser,
  changeStatus,
  resetPwd,
  delUser,
  merchantAudit,
  MERCHANT_APPLY_STATUS,
  USER_STATUS,
  type UserData
} from '@/api/system/user'

const loading = ref(false)
const submitting = ref(false)
const rows = ref<UserData[]>([])
const total = ref(0)
const query = reactive<{ userName?: string; status?: string; merchantApplyStatus?: string; pageNum: number; pageSize: number }>({
  pageNum: 1,
  pageSize: 10
})

const resetVisible = ref(false)
const newPassword = ref('')
const rejectVisible = ref(false)
const rejectRemark = ref('')
let current: UserData | null = null

async function loadList(page = query.pageNum) {
  loading.value = true
  try {
    query.pageNum = page
    const data = await listUser({ ...query })
    rows.value = data.rows ?? []
    total.value = data.total ?? 0
  } finally {
    loading.value = false
  }
}

async function handleChangeStatus(row: UserData, status: string) {
  const op = status === '1' ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(`确定${op}账号「${row.userName}」吗？`, '提示', { type: 'warning' })
    await changeStatus(row.userId!, status)
    ElMessage.success(`已${op}`)
    loadList()
  } catch (e) {
    /* 取消 */
  }
}

function handleResetPwd(row: UserData) {
  current = row
  newPassword.value = ''
  resetVisible.value = true
}

async function handleResetConfirm() {
  if (!current) return
  if (!newPassword.value) {
    ElMessage.warning('请输入新密码')
    return
  }
  submitting.value = true
  try {
    await resetPwd(current.userId!, newPassword.value)
    ElMessage.success('密码已重置')
    resetVisible.value = false
  } finally {
    submitting.value = false
  }
}

async function handleAudit(row: UserData, action: string) {
  try {
    await ElMessageBox.confirm(`确定通过「${row.userName}」的商家入驻申请吗？通过后将自动授权商家角色。`, '审核通过', {
      confirmButtonText: '确认通过',
      cancelButtonText: '取消',
      type: 'success'
    })
    await merchantAudit(row.userId!, 'approve')
    ElMessage.success('已审核通过')
    loadList()
  } catch (e) {
    /* 取消 */
  }
}

function openReject(row: UserData) {
  current = row
  rejectRemark.value = ''
  rejectVisible.value = true
}

async function handleRejectConfirm() {
  if (!current) return
  if (!rejectRemark.value.trim()) {
    ElMessage.warning('请填写驳回意见')
    return
  }
  submitting.value = true
  try {
    await merchantAudit(current.userId!, 'reject', rejectRemark.value.trim())
    ElMessage.success('已驳回')
    rejectVisible.value = false
    loadList()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: UserData) {
  try {
    await ElMessageBox.confirm(`确定删除账号「${row.userName}」吗？`, '提示', { type: 'warning' })
    await delUser(row.userId!)
    ElMessage.success('已删除')
    loadList()
  } catch (e) {
    /* 取消 */
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