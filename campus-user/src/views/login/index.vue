<template>
  <div class="login-page">
    <div class="login-box">
      <div class="login-title">
        <el-icon :size="30" color="#ff6a00"><ShoppingBag /></el-icon>
        <h2>校园市场 · 统一登录入口</h2>
        <p>登录后按账号角色自动进入对应端</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="账号" :prefix-icon="User" clearable />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item prop="code">
          <div class="captcha-row">
            <el-input v-model="form.code" placeholder="验证码" :prefix-icon="Key" />
            <img
              v-if="captchaEnabled"
              :src="captchaImg"
              class="captcha-img"
              alt="验证码"
              title="点击刷新"
              @click="loadCaptcha"
            />
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-foot">
        <el-link type="primary" @click="router.replace('/home')">游客浏览</el-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock, Key } from '@element-plus/icons-vue'
import { getCaptcha } from '@/api/login'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const captchaImg = ref('')
const captchaEnabled = ref(true)
const uuid = ref('')

const form = reactive({
  username: '',
  password: '',
  code: ''
})

/** 验证码校验规则：仅在后端开启验证码时必填 */
const rules = computed<FormRules>(() => ({
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  code: captchaEnabled.value ? [{ required: true, message: '请输入验证码', trigger: 'blur' }] : []
}))

async function loadCaptcha() {
  try {
    const res = await getCaptcha()
    if (res) {
      captchaEnabled.value = !!res.captchaEnabled
      if (res.captchaEnabled && res.img && res.uuid) {
        captchaImg.value = `data:image/jpeg;base64,${res.img}`
        uuid.value = res.uuid
      }
    }
  } catch {
    // 验证码加载失败不阻塞登录
  }
}

/** 统一入口：各端端口（admin/merchant/user 运行在不同源，localStorage 不互通） */
const ROLE_PORTS: Record<string, number> = {
  admin: 8081,
  merchant: 8082
}

function originOf(port: number) {
  return `${window.location.protocol}//${window.location.hostname}:${port}`
}

async function handleLogin() {
  if (!formRef.value) return
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.login({
      username: form.username,
      password: form.password,
      code: form.code,
      uuid: uuid.value
    })
    // 拉取角色，按账号权限跳转到对应端
    await userStore.fetchUserInfo()
    ElMessage.success('登录成功')
    const token = userStore.token
    for (const [role, port] of Object.entries(ROLE_PORTS)) {
      if (userStore.roles.includes(role)) {
        // 跨端 token 通过 URL 传递，目标端守卫消费后清除
        window.location.href = `${originOf(port)}?token=${encodeURIComponent(token)}`
        return
      }
    }
    // 普通用户留在用户端
    const redirect = (route.query.redirect as string) || '/home'
    router.replace(redirect)
  } catch {
    form.code = ''
    loadCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(loadCaptcha)
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ff9a3c 0%, #ff6a00 100%);
}
.login-box {
  width: 400px;
  padding: 40px 36px 24px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.18);
}
.login-title {
  text-align: center;
  margin-bottom: 28px;
  color: #1f2d3d;
}
.login-title h2 {
  margin: 10px 0 4px;
  font-size: 22px;
}
.login-title p {
  margin: 0;
  color: #909399;
  font-size: 13px;
}
.captcha-row {
  width: 100%;
  display: flex;
  gap: 10px;
}
.captcha-img {
  width: 110px;
  height: 40px;
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid #dcdfe6;
}
.login-btn {
  width: 100%;
  background: #ff6a00;
  border-color: #ff6a00;
}
.login-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
