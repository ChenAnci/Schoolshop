<template>
  <div class="navbar">
    <div class="navbar-left">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>
    <div class="navbar-right">
      <router-link class="chat-link" to="/chat">
        <el-badge :value="unread" :hidden="unread === 0" :max="99">
          <el-icon :size="18"><ChatDotRound /></el-icon>
        </el-badge>
        <span>消息中心</span>
      </router-link>
      <el-dropdown trigger="click" @command="handleCommand">
        <span class="user-info">
          <el-avatar :size="28" :src="userStore.avatar">{{ avatarText }}</el-avatar>
          <span class="user-name">{{ userStore.name || '管理员' }}</span>
          <el-icon><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="logout">
              <el-icon><SwitchButton /></el-icon>退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { ChatDotRound } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getUnreadCount } from '@/api/chat'
import { connect, on, off } from '@/utils/ws'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const unread = ref(0)

async function loadUnread() {
  if (!userStore.token) return
  try {
    const r = await getUnreadCount()
    unread.value = r.data?.count || 0
  } catch {
    // 未登录或接口异常时静默忽略
  }
}

function onChatMessage() {
  unread.value++
}

onMounted(() => {
  if (userStore.token) {
    loadUnread()
    connect()
    on('CHAT_MESSAGE', onChatMessage)
  }
})

onUnmounted(() => {
  off('CHAT_MESSAGE', onChatMessage)
})

const avatarText = computed(() => (userStore.name ? userStore.name.charAt(0) : '管'))

function handleCommand(command: string) {
  if (command === 'logout') {
    ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
      .then(async () => {
        await userStore.logout()
        ElMessage.success('已退出登录')
        router.replace('/login')
      })
      .catch(() => undefined)
  }
}
</script>

<style scoped>
.navbar {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.navbar-left {
  display: flex;
  align-items: center;
}
.navbar-right {
  display: flex;
  align-items: center;
}
.chat-link {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #333;
  font-size: 14px;
  margin-right: 16px;
}
.chat-link:hover {
  color: #409eff;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #333;
  outline: none;
}
.user-name {
  font-size: 14px;
}
</style>
