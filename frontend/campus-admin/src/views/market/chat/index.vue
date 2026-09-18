<template>
  <div class="app-container">
    <el-card shadow="never">
      <div class="toolbar">
        <el-input
          v-model="sessionId"
          placeholder="输入会话ID"
          clearable
          style="width: 220px"
          @keyup.enter="loadList(1)"
          @clear="loadList(1)"
        />
        <el-button type="primary" icon="Search" @click="loadList(1)">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" style="width: 100%">
        <el-table-column prop="messageId" label="消息ID" width="90" />
        <el-table-column prop="sessionId" label="会话ID" width="90" />
        <el-table-column prop="senderId" label="发送者" width="90" />
        <el-table-column prop="receiverId" label="接收者" width="90" />
        <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="MSG_TYPE[row.msgType]?.type ?? 'info'">
              {{ MSG_TYPE[row.msgType]?.text ?? row.msgType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="MSG_STATUS[row.status]?.type ?? 'info'">
              {{ MSG_STATUS[row.status]?.text ?? row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="165" />
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
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listAdminMessages, type ChatMessage } from '@/api/market/chat'

const loading = ref(false)
const rows = ref<ChatMessage[]>([])
const total = ref(0)
const sessionId = ref<number>()
const query = reactive<{ pageNum: number; pageSize: number }>({
  pageNum: 1,
  pageSize: 10
})

/** 消息类型字典（1文本 2商品卡片 3图片） */
const MSG_TYPE: Record<string, { text: string; type: 'primary' | 'success' | 'info' | 'warning' }> = {
  '1': { text: '文本', type: 'primary' },
  '2': { text: '商品卡片', type: 'success' },
  '3': { text: '图片', type: 'warning' }
}

/** 消息状态字典（0未读 1已读） */
const MSG_STATUS: Record<string, { text: string; type: 'danger' | 'success' }> = {
  '0': { text: '未读', type: 'danger' },
  '1': { text: '已读', type: 'success' }
}

async function loadList(page = query.pageNum) {
  if (!sessionId.value) {
    ElMessage.warning('请输入会话ID')
    return
  }
  loading.value = true
  try {
    query.pageNum = page
    const data = await listAdminMessages({ sessionId: sessionId.value, ...query })
    rows.value = data.rows ?? []
    total.value = data.total ?? 0
  } finally {
    loading.value = false
  }
}
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
