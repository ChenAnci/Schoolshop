<template>
  <div class="app-container">
    <el-card shadow="never">
      <div class="toolbar">
        <el-input
          v-model="query.menuName"
          placeholder="按菜单名称搜索"
          clearable
          style="width: 200px"
          @keyup.enter="loadList()"
          @clear="loadList()"
        />
        <el-select
          v-model="query.status"
          placeholder="全部状态"
          clearable
          style="width: 140px"
          @change="loadList()"
          @clear="loadList()"
        >
          <el-option v-for="(v, k) in MENU_STATUS" :key="k" :label="v.text" :value="k" />
        </el-select>
        <el-button type="primary" icon="Search" @click="loadList()">搜索</el-button>
        <el-button type="primary" icon="Plus" @click="openDialog()">新增菜单</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="tree"
        row-key="menuId"
        style="width: 100%"
        :tree-props="{ children: 'children' }"
        default-expand-all
      >
        <el-table-column prop="menuName" label="菜单名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="类型" width="70">
          <template #default="{ row }">
            <el-tag :type="MENU_TYPE_TAG[row.menuType] ?? 'info'">{{ MENU_TYPE[row.menuType] ?? row.menuType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="orderNum" label="排序" width="70" />
        <el-table-column prop="perms" label="权限标识" min-width="160" show-overflow-tooltip />
        <el-table-column prop="path" label="路由地址" min-width="130" show-overflow-tooltip />
        <el-table-column prop="component" label="组件路径" min-width="150" show-overflow-tooltip />
        <el-table-column label="显示" width="80">
          <template #default="{ row }">
            <el-tag :type="row.visible === '0' ? 'success' : 'info'">{{ MENU_VISIBLE[row.visible] ?? '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="MENU_STATUS[row.status]?.type ?? 'success'">
              {{ MENU_STATUS[row.status]?.text ?? '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button
              v-if="row.status === '0' && row.menuId !== 1"
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
    </el-card>

    <!-- 新增/编辑菜单弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="form.menuId ? '编辑菜单' : '新增菜单'"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="上级菜单" prop="parentId">
          <el-tree-select
            v-model="form.parentId"
            placeholder="选择上级菜单（默认顶级）"
            :data="parentTree"
            :props="{ label: 'label', children: 'children' }"
            check-strictly
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="菜单类型" prop="menuType">
          <el-radio-group v-model="form.menuType">
            <el-radio value="M">目录</el-radio>
            <el-radio value="C">菜单</el-radio>
            <el-radio value="F">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName">
          <el-input v-model="form.menuName" placeholder="请输入菜单名称" />
        </el-form-item>
        <el-form-item label="显示顺序" prop="orderNum">
          <el-input-number v-model="form.orderNum" :min="0" :max="999" />
        </el-form-item>
        <el-form-item v-if="form.menuType !== 'F'" label="路由地址" prop="path">
          <el-input v-model="form.path" placeholder="如 system/user" />
        </el-form-item>
        <el-form-item v-if="form.menuType === 'C'" label="组件路径" prop="component">
          <el-input v-model="form.component" placeholder="如 system/user/index" />
        </el-form-item>
        <el-form-item label="权限标识" prop="perms">
          <el-input v-model="form.perms" placeholder="如 system:user:list" />
        </el-form-item>
        <el-form-item v-if="form.menuType !== 'F'" label="显示状态">
          <el-radio-group v-model="form.visible">
            <el-radio value="0">显示</el-radio>
            <el-radio value="1">隐藏</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单状态">
          <el-radio-group v-model="form.status">
            <el-radio value="0">正常</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.menuType !== 'F'" label="菜单图标">
          <el-input v-model="form.icon" placeholder="Element Plus 图标名称">
            <template #append>
              <el-icon style="display: inline-flex; vertical-align: middle">
                <component :is="form.icon || 'Menu'" />
              </el-icon>
            </template>
          </el-input>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  listMenu,
  getMenu,
  addMenu,
  updateMenu,
  delMenu,
  menuTreeselect,
  MENU_STATUS,
  MENU_TYPE,
  MENU_VISIBLE,
  type MenuData,
  type MenuTree
} from '@/api/system/menu'

const MENU_TYPE_TAG: Record<string, 'primary' | 'success' | 'warning' | 'info'> = {
  M: 'warning',
  C: 'success',
  F: 'info'
}

const loading = ref(false)
const submitting = ref(false)
const rows = ref<MenuData[]>([])
const query = reactive<{ menuName?: string; status?: string }>({})

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<MenuData>({})
const rules: FormRules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }]
}

const allTree = ref<MenuTree[]>([])

/** 树形表格数据 */
const tree = computed(() => buildTree(rows.value))

/** 上级菜单下拉：去掉按钮节点，去掉自身/子孙节点避免循环 */
const parentTree = computed(() => {
  const exclude = new Set<number>()
  if (form.menuId) {
    collectChildren(rows.value, form.menuId, exclude)
  }
  return filterTree(allTree.value, exclude)
})

/** 扁平数组转树形（用于表格展示） */
function buildTree(list: MenuData[], parentId = 0): MenuData[] {
  return list
    .filter((item) => item.parentId === parentId)
    .map((item) => ({
      ...item,
      children: buildTree(list, item.menuId)
    }))
    .sort((a, b) => (a.orderNum ?? 0) - (b.orderNum ?? 0))
}

/** 收集某节点及其全部子孙的 id */
function collectChildren(list: MenuData[], parentId: number, out: Set<number>) {
  list
    .filter((item) => item.parentId === parentId)
    .forEach((item) => {
      out.add(item.menuId!)
      collectChildren(list, item.menuId!, out)
    })
}

/** 过滤树：排除按钮节点 + 需要排除的 id，仅保留目录/菜单用于上级选择 */
function filterTree(nodes: MenuTree[], exclude: Set<number>): MenuTree[] {
  return nodes
    .filter((n) => n.disabled !== true && !exclude.has(n.id))
    .map((n) => ({
      ...n,
      children: n.children ? filterTree(n.children, exclude) : []
    }))
}

async function loadTree() {
  try {
    const res = await menuTreeselect()
    allTree.value = res.data ?? []
  } catch {
    /* 请求层已提示 */
  }
}

async function loadList() {
  loading.value = true
  try {
    const res = await listMenu({ ...query })
    rows.value = res.data ?? []
  } finally {
    loading.value = false
  }
}

async function openDialog(row?: MenuData) {
  Object.assign(form, {
    menuId: undefined,
    parentId: 0,
    menuName: undefined,
    menuType: 'C',
    orderNum: 0,
    path: undefined,
    component: undefined,
    perms: undefined,
    visible: '0',
    status: '0',
    icon: undefined
  })
  if (row?.menuId) {
    try {
      Object.assign(form, {
        menuId: row.menuId,
        parentId: row.parentId ?? 0,
        menuName: row.menuName,
        menuType: row.menuType,
        orderNum: row.orderNum ?? 0,
        path: row.path,
        component: row.component,
        perms: row.perms,
        visible: row.visible ?? '0',
        status: row.status ?? '0',
        icon: row.icon
      })
    } catch {
      return
    }
  } else {
    // 新增时默认上级为当前选中的节点
    if (row) form.parentId = row.menuId
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (form.menuId) {
      await updateMenu({ ...form })
      ElMessage.success('修改成功')
    } else {
      await addMenu({ ...form })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadList()
    loadTree()
  } finally {
    submitting.value = false
  }
}

async function handleChangeStatus(row: MenuData, status: string) {
  const op = status === '1' ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(`确定${op}菜单「${row.menuName}」吗？`, '提示', { type: 'warning' })
    await updateMenu({ ...row, status })
    ElMessage.success(`已${op}`)
    loadList()
  } catch (e) {
    /* 取消 */
  }
}

async function handleDelete(row: MenuData) {
  try {
    await ElMessageBox.confirm(
      `确定删除菜单「${row.menuName}」吗？若有子菜单/按钮将一并删除。`,
      '提示',
      { type: 'warning' }
    )
    await delMenu(row.menuId!)
    ElMessage.success('已删除')
    loadList()
    loadTree()
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
</style>