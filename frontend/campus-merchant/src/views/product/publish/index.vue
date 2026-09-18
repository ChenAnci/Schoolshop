<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <span>{{ isEdit ? '编辑商品' : '发布商品' }}</span>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="max-width: 680px" v-loading="loading">
        <el-form-item label="商品名称" prop="productName">
          <el-input v-model="form.productName" placeholder="请输入商品名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="商品分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="c in categories" :key="c.categoryId" :label="c.categoryName" :value="c.categoryId" />
          </el-select>
        </el-form-item>
        <el-form-item label="商品主图">
          <el-upload
            class="avatar-uploader"
            :show-file-list="false"
            :action="uploadAction"
            :headers="uploadHeaders"
            name="file"
            :before-upload="beforeUpload"
            :on-success="onUploadSuccess"
            :on-error="onUploadError"
          >
            <img v-if="form.productImage" :src="form.productImage" class="avatar" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
          <div class="tip">支持 jpg/png/gif/webp，大小不超过 5MB</div>
        </el-form-item>
        <el-form-item label="价格（元）" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" style="width: 200px" />
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="form.stock" :min="0" :step="1" style="width: 200px" />
        </el-form-item>
        <el-form-item label="显示顺序">
          <el-input-number v-model="form.sort" :min="0" :step="1" style="width: 200px" />
        </el-form-item>
        <el-form-item label="商品描述">
          <el-input v-model="form.productDesc" type="textarea" :rows="4" placeholder="请输入商品描述" maxlength="1000" show-word-limit />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可选" maxlength="500" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ isEdit ? '保存' : '发布' }}</el-button>
          <el-button @click="$router.back()">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules, type UploadRequestOptions } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getToken } from '@/utils/auth'
import {
  getCategoryList,
  getShopProductDetail,
  addShopProduct,
  updateShopProduct,
  type Category,
  type ShopProduct
} from '@/api/product'

const route = useRoute()
const isEdit = !!route.query.productId
const loading = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const categories = ref<Category[]>([])

const form = reactive<Partial<ShopProduct>>({
  productName: '',
  categoryId: undefined,
  productImage: '',
  price: 0,
  stock: 0,
  sort: 0,
  productDesc: '',
  remark: ''
})

const rules: FormRules = {
  productName: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择商品分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }]
}

/** 上传：走 dev 代理直连后端 /common/upload（非 axios，需带 token） */
const uploadAction = `${import.meta.env.VITE_APP_BASE_API}/common/upload`
const uploadHeaders = { Authorization: `Bearer ${getToken()}` }

function beforeUpload(req: UploadRequestOptions) {
  const size = req.file.size / 1024 / 1024
  if (size > 5) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

function onUploadSuccess(res: Record<string, any>) {
  if (res.code === 200 && res.url) {
    form.productImage = res.url
    ElMessage.success('上传成功')
  } else {
    ElMessage.error(res.msg || '上传失败')
  }
}

function onUploadError() {
  ElMessage.error('上传失败，请稍后重试')
}

async function loadCategories() {
  const res = await getCategoryList()
  categories.value = res.data ?? []
}

async function loadDetail() {
  const id = Number(route.query.productId)
  if (!id) return
  loading.value = true
  try {
    const res = await getShopProductDetail(id)
    const d = res.data
    if (d) {
      Object.assign(form, {
        productId: d.productId,
        productName: d.productName ?? '',
        categoryId: d.categoryId,
        productImage: d.productImage ?? '',
        price: Number(d.price),
        stock: d.stock ?? 0,
        sort: d.sort ?? 0,
        productDesc: d.productDesc ?? '',
        remark: d.remark ?? ''
      })
    }
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  submitting.value = true
  try {
    if (isEdit) {
      await updateShopProduct(form)
      ElMessage.success('保存成功')
    } else {
      await addShopProduct(form)
      ElMessage.success('发布成功')
    }
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await loadCategories()
  if (isEdit) await loadDetail()
})
</script>

<style scoped>
.avatar-uploader :deep(.el-upload) {
  border: 1px dashed var(--el-border-color);
  border-radius: 8px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
}
.avatar-uploader :deep(.el-upload:hover) {
  border-color: var(--el-color-primary);
}
.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 120px;
  height: 120px;
  text-align: center;
  line-height: 120px;
}
.avatar {
  width: 120px;
  height: 120px;
  display: block;
  object-fit: cover;
}
.tip {
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
}
</style>