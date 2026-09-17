<template>
  <div class="chat-page">
    <div class="chat-left">
      <div class="chat-title">消息中心</div>
      <div v-if="!sessions.length" class="chat-empty">暂无会话，去商品页联系商家吧</div>
      <div v-for="s in sessions" :key="s.sessionId" class="chat-item" :class="{ active: s.sessionId === currentId }"
           @click="openSession(s)">
        <div class="chat-item-top">
          <span class="chat-name">{{ s.otherNickName }}</span>
          <span v-if="s.otherOnline" class="chat-online">● 在线</span>
        </div>
        <div class="chat-item-bottom">
          <span class="chat-last">{{ s.lastMessage || '暂无消息' }}</span>
          <span v-if="s.unreadCount > 0" class="chat-badge">{{ s.unreadCount }}</span>
        </div>
      </div>
    </div>
    <div class="chat-right">
      <template v-if="currentId">
        <div class="chat-head">{{ currentOther?.otherNickName }}</div>
        <div class="chat-body" ref="bodyRef">
          <div v-for="m in messages" :key="m.messageId" class="chat-msg" :class="{ mine: m.senderId === myId }">
            <div class="bubble">{{ m.content }}</div>
          </div>
        </div>
        <div class="chat-input">
          <el-input v-model="draft" placeholder="输入消息..." @keyup.enter="sendMsg" />
          <el-button type="primary" @click="sendMsg">发送</el-button>
        </div>
      </template>
      <div v-else class="chat-placeholder">选择左侧会话开始聊天</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { getSessionList, getMessageList, createSession, markRead, type ChatSession, type ChatMessage } from '@/api/chat'
import { connect, disconnect, sendChat, on, off } from '@/utils/ws'
import { getToken } from '@/utils/auth'

const sessions = ref<ChatSession[]>([])
const currentId = ref<number>(0)
const currentOther = ref<ChatSession | null>(null)
const messages = ref<ChatMessage[]>([])
const draft = ref('')
const bodyRef = ref<HTMLElement>()
const route = useRoute()

// 解析 token 中的 userId（RuoYi JWT 的 payload 含 userId）
const myId = ref(0)
try {
  const token = getToken()
  if (token) {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
    myId.value = payload?.userId || 0
  }
} catch {
  // token 解析失败时 myId 保持 0，仅影响气泡左右
}

async function refreshSessions() {
  const res = await getSessionList()
  sessions.value = res.data || []
}

async function openSession(s: ChatSession) {
  currentId.value = s.sessionId
  currentOther.value = s
  s.unreadCount = 0
  markRead(s.sessionId)
  const r = await getMessageList({ sessionId: s.sessionId, pageNum: 1, pageSize: 50 })
  messages.value = r.rows || []
  scrollBottom()
}

function scrollBottom() {
  nextTick(() => { bodyRef.value?.scrollTo({ top: bodyRef.value.scrollHeight }) })
}

async function sendMsg() {
  const content = draft.value.trim()
  if (!content || !currentOther.value) return
  sendChat(currentOther.value.otherUserId, content)
  draft.value = ''
  // 乐观插入本地
  messages.value.push({
    messageId: Date.now(), sessionId: currentId.value, senderId: myId.value,
    receiverId: currentOther.value.otherUserId, content, msgType: '1', status: '0', createTime: null
  })
  scrollBottom()
}

function onChatMessage(data: any) {
  if (data.sessionId === currentId.value) {
    messages.value.push(data)
    scrollBottom()
  }
  refreshSessions()
}

onMounted(async () => {
  await refreshSessions()
  const shopId = route.query.shopId
  if (shopId) {
    const r = await createSession({ shopId: Number(shopId) })
    const s = r.data
    if (s) {
      await refreshSessions()
      const found = sessions.value.find(x => x.sessionId === s.sessionId)
      openSession(found || s)
    }
  }
  connect()
  on('CHAT_MESSAGE', onChatMessage)
})
onUnmounted(() => { off('CHAT_MESSAGE', onChatMessage); disconnect() })
</script>

<style scoped>
.chat-page { display: flex; height: calc(100vh - 120px); border: 1px solid #eee; border-radius: 8px; overflow: hidden; }
.chat-left { width: 280px; border-right: 1px solid #eee; overflow-y: auto; background: #fafafa; }
.chat-title { padding: 14px; font-weight: 600; border-bottom: 1px solid #eee; }
.chat-empty { padding: 24px; color: #999; text-align: center; }
.chat-item { padding: 12px 14px; cursor: pointer; border-bottom: 1px solid #f0f0f0; }
.chat-item.active { background: #ecf5ff; }
.chat-item-top { display: flex; justify-content: space-between; }
.chat-name { font-weight: 500; }
.chat-online { color: #67c23a; font-size: 12px; }
.chat-item-bottom { display: flex; justify-content: space-between; align-items: center; margin-top: 4px; }
.chat-last { color: #999; font-size: 12px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 180px; }
.chat-badge { background: #f56c6c; color: #fff; border-radius: 10px; font-size: 12px; padding: 0 6px; }
.chat-right { flex: 1; display: flex; flex-direction: column; }
.chat-head { padding: 12px 16px; border-bottom: 1px solid #eee; font-weight: 600; }
.chat-body { flex: 1; overflow-y: auto; padding: 16px; background: #f7f8fa; }
.chat-msg { display: flex; margin-bottom: 12px; }
.chat-msg.mine { justify-content: flex-end; }
.bubble { max-width: 60%; padding: 8px 12px; border-radius: 8px; background: #fff; box-shadow: 0 1px 2px rgba(0,0,0,.06); }
.chat-msg.mine .bubble { background: #409eff; color: #fff; }
.chat-input { display: flex; gap: 8px; padding: 12px; border-top: 1px solid #eee; }
.chat-placeholder { flex: 1; display: flex; align-items: center; justify-content: center; color: #bbb; }
</style>
