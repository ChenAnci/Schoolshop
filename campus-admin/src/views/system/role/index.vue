<template>
  <div class="app-container">
    <el-card shadow="never">
      <div class="toolbar">
        <el-input
          v-model="query.roleName"
          placeholder="按角色名称搜索"
          clearable
          style="width: 200px"
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
          <el-option v-for="(v, k) in ROLE_STATUS" :key="k" :label="v.text" :value="k" />
        </el-select>
        <el-button type="primary" icon="Search" @click="loadList(1)">搜索</el-button>
        <el-button type="primary" icon="Plus" @click="openDialog()">新增角色</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" style="width: 100%">
        <el-table-column prop="roleId" label="ID" width="70" />
        <el-table-column prop="roleName" label="角色名称" width="160" show-overflow-tooltip />
        <el-table-column prop="roleKey" label="权限字符" width="160" show-overflow-tooltip />
        <el-table-column prop="roleSort" label="排序" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="ROLE_STATUS[row.status]?.type ?? 'success'">
              {{ ROLE_STATUS[row.status]?.text ?? '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button
              v-if="row.status === '0' && row.roleId !== 1"
              link
              type="danger"
              @click="handleChangeStatus(row, '1')"
            >
              停用
            </el-button>
            <el-button v-if="row.status === '1'" link type="success" @click="handleChangeStatus(row, '0')">
              启用
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

    <!-- 新增/编辑角色弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.roleId ? '编辑角色' : '新增角色'" width="600px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="权限字符" prop="roleKey">
          <el-input v-model="form.roleKey" placeholder="如 admin / merchant / user" />
        </el-form-item>
        <el-form-item label="显示顺序" prop="roleSort">
          <el-input-number v-model="form.roleSort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="0">正常</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单权限">
          <el-tree
            ref="menuTreeRef"
            :data="menuTree"
            show-checkbox
            node-key="id"
            :props="{ label: 'label', children: 'children' }"
            :default-checked-keys="checkedMenuIds"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import type { TreeInstance } from 'element-plus'
import {
  listRole,
  getRole,
  addRole,
  updateRole,
  changeRoleStatus,
  delRole,
  ROLE_STATUS,
  type RoleData
} from '@/api/system/role'
import { menuTreeselect, type MenuTree } from '@/api/system/menu'

const loading = ref(false)
const submitting = ref(false)
const rows = ref<RoleData[]>([])
const total = ref(0)
const query = reactive<{ roleName?: string; status?: string; pageNum: number; pageSize: number }>({
  pageNum: 1,
  pageSize: 10
})

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<RoleData>({})
const rules: FormRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleKey: [{ required: true, message: '请输入权限字符', trigger: 'blur' }]
}

const menuTree = ref<MenuTree[]>([])
const checkedMenuIds = ref<number[]>([])
const menuTreeRef = ref<TreeInstance>()

async function loadTree() {
  try {
    const res = await menuTreeselect()
    menuTree.value = res.data ?? []
  } catch {
    /* 请求层已提示 */
  }
}

async function loadList(page = query.pageNum) {
  loading.value = true
  try {
    query.pageNum = page
    const data = await listRole({ ...query })
    rows.value = data.rows ?? []
    total.value = data.total ?? 0
  } finally {
    loading.value = false
  }
}

async function openDialog(row?: RoleData) {
  Object.assign(form, {
    roleId: undefined,
    roleName: undefined,
    roleKey: undefined,
    roleSort: 0,
    status: '0',
    remark: undefined,
    menuIds: []
  })
  checkedMenuIds.value = []
  if (row?.roleId) {
    form.roleId = row.roleId
    try {
      const res = await getRole(row.roleId)
      Object.assign(form, {
        roleName: res.data?.role?.roleName,
        roleKey: res.data?.role?.roleKey,
        roleSort: res.data?.role?.roleSort ?? 0,
        status: res.data?.role?.status ?? '0',
        remark: res.data?.role?.remark
      })
      checkedMenuIds.value = res.data?.menuIds ?? []
    } catch {
      return
    }
  }
  dialogVisible.value = true
  // 树数据渲染后回填勾选
  setTimeout(() => {
    if (row?.roleId) setCheckedKeys(checkedMenuIds.value)
  }, 50)
}

function setCheckedKeys(keys: number[]) {
  menuTreeRef.value?.setCheckedKeys(keys)
}

function collectMenuIds(): number[] {
  const checked = menuTreeRef.value?.getCheckedKeys(false) ?? []
  const half = menuTreeRef.value?.getHalfCheckedKeys() ?? []
  return [...(checked as number[]), ...(half as number[])]
}

async function handleSubmit() {
  await formRef.value?.validate()
  const payload: RoleData = {
    ...form,
    menuIds: collectMenuIds()
  }
  submitting.value = true
  try {
    if (form.roleId) {
      await updateRole(payload)
      ElMessage.success('修改成功')
    } else {
      await addRole(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadList()
  } finally {
    submitting.value = false
  }
}

async function handleChangeStatus(row: RoleData, status: string) {
  const op = status === '1' ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(`确定${op}角色「${row.roleName}」吗？`, '提示', { type: 'warning' })
    await changeRoleStatus(row.roleId!, status)
    ElMessage.success(`已${op}`)
    loadList()
  } catch (e) {
    /* 取消 */
  }
}

async function handleDelete(row: RoleData) {
  try {
    await ElMessageBox.confirm(`确定删除角色「${row.roleName}」吗？`, '提示', { type: 'warning' })
    await delRole([row.roleId!])
    ElMessage.success('已删除')
    loadList()
  } catch (e) {
    /* 取消 */
  }
}

onMounted(() => {
  loadList()
  loadTree()
})
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