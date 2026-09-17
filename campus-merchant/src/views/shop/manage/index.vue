<template>
  <div class="app-container">
    <el-card shadow="never" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>店铺资料</span>
          <el-tag v-if="shop && shop.shopId" type="success">已开通店铺</el-tag>
          <el-tag v-else type="info">尚未开通</el-tag>
        </div>
      </template>

      <el-empty
        v-if="!shop || !shop.shopId"
        description="当前账号尚未开通店铺，请联系管理员开通后再编辑资料"
      />

      <el-form
        v-else
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="90px"
        style="max-width: 640px"
      >
        <el-form-item label="店铺名称" prop="shopName">
          <el-input v-model="form.shopName" placeholder="请输入店铺名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.contactPhone" placeholder="请输入联系电话" maxlength="20" />
        </el-form-item>
        <el-form-item label="店铺Logo">
          <el-input v-model="form.shopLogo" placeholder="Logo 图片地址（可选）" />
        </el-form-item>
        <el-form-item label="店铺简介">
          <el-input
            v-model="form.shopDesc"
            type="textarea"
            :rows="4"
            placeholder="请输入店铺简介"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="营业状态">
          <el-radio-group v-model="form.status">
            <el-radio value="0">正常营业</el-radio>
            <el-radio value="1">暂停营业</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注" maxlength="500" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSave">保存资料</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getMyShop, updateMyShop, type ShopData } from '@/api/shop'

const loading = ref(false)
const submitting = ref(false)
const shop = ref<ShopData | null>(null)
const formRef = ref<FormInstance>()

const form = reactive<ShopData>({
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

async function loadShop() {
  loading.value = true
  try {
    const res = await getMyShop()
    const data = res.data
    if (data && data.shopId) {
      shop.value = data
      Object.assign(form, {
        shopName: data.shopName ?? '',
        shopLogo: data.shopLogo ?? '',
        shopDesc: data.shopDesc ?? '',
        contactPhone: data.contactPhone ?? '',
        status: data.status ?? '0',
        remark: data.remark ?? ''
      })
    }
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  if (!formRef.value) return
  await formRef.value.validate()
  submitting.value = true
  try {
    await updateMyShop(form)
    ElMessage.success('保存成功')
    loadShop()
  } finally {
    submitting.value = false
  }
}

onMounted(loadShop)
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>