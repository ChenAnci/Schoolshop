<template>
  <div class="app-container">
    <el-card shadow="never" class="search-card">
      <el-form :model="queryParams" inline>
        <el-form-item label="店铺名称">
          <el-input
            v-model="queryParams.shopName"
            placeholder="请输入店铺名称"
            clearable
            style="width: 200px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="营业状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 200px">
            <el-option label="正常营业" value="0" />
            <el-option label="已停用" value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增店铺</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="shopName" label="店铺名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="contactPhone" label="联系电话" width="140" align="center" />
        <el-table-column prop="productCount" label="在售商品" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.productCount" type="warning">{{ row.productCount }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="营业状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'danger'">
              {{ row.status === '0' ? '正常营业' : '已停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" align="center" />
        <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @current-change="getList"
        @size-change="getList"
      />
    </el-card>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog
      v-model="dialog.visible"
      :title="dialog.isEdit ? '编辑店铺' : '新增店铺'"
      width="520px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="店铺名称" prop="shopName">
          <el-input v-model="form.shopName" placeholder="请输入店铺名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" placeholder="请输入联系电话" maxlength="20" />
        </el-form-item>
        <el-form-item label="店铺Logo">
          <el-input v-model="form.shopLogo" placeholder="Logo 图片地址（可选）" />
        </el-form-item>
        <el-form-item label="店铺简介">
          <el-input v-model="form.shopDesc" type="textarea" :rows="3" placeholder="请输入店铺简介" maxlength="500" />
        </el-form-item>
        <el-form-item label="营业状态">
          <el-radio-group v-model="form.status">
            <el-radio value="0">正常营业</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取 消</el-button>
        <el-button type="primary" :loading="dialog.submitting" @click="handleSubmit">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Search, Refresh, Edit, Delete } from '@element-plus/icons-vue'
import { listShop, addShop, updateShop, delShop, type ShopData } from '@/api/market/shop'

const loading = ref(false)
const list = ref<ShopData[]>([])
const total = ref(0)

const queryParams = reactive({
  shopName: '',
  status: '',
  pageNum: 1,
  pageSize: 10
})

const formRef = ref<FormInstance>()
const dialog = reactive({ visible: false, isEdit: false, submitting: false })
const form = reactive<ShopData>({
  shopId: undefined,
  shopName: '',
  shopLogo: '',
  shopDesc: '',
  contactPhone: '',
  status: '0',
  remark: ''
})

const rules: FormRules = {
  shopName: [{ required: true, message: '请输入店铺名称', trigger: 'blur' }]
}

async function getList() {
  loading.value = true
  try {
    const res = await listShop({ ...queryParams })
    list.value = res.rows ?? []
    total.value = res.total ?? 0
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  queryParams.shopName = ''
  queryParams.status = ''
  queryParams.pageNum = 1
  getList()
}

function resetForm() {
  formRef.value?.clearValidate()
  Object.assign(form, {
    shopId: undefined,
    shopName: '',
    shopLogo: '',
    shopDesc: '',
    contactPhone: '',
    status: '0',
    remark: ''
  })
}

function handleAdd() {
  dialog.isEdit = false
  dialog.visible = true
}

function handleEdit(row: ShopData) {
  dialog.isEdit = true
  Object.assign(form, row)
  dialog.visible = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  dialog.submitting = true
  try {
    if (dialog.isEdit) {
      await updateShop(form)
      ElMessage.success('修改成功')
    } else {
      await addShop(form)
      ElMessage.success('新增成功')
    }
    dialog.visible = false
    getList()
  } finally {
    dialog.submitting = false
  }
}

function handleDelete(row: ShopData) {
  ElMessageBox.confirm(`确认删除店铺「${row.shopName}」吗？`, '提示', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  })
    .then(async () => {
      await delShop(String(row.shopId))
      ElMessage.success('删除成功')
      getList()
    })
    .catch(() => {})
}

onMounted(getList)
</script>

<style scoped>
.search-card {
  margin-bottom: 16px;
}
.toolbar {
  margin-bottom: 16px;
}
</style>