<!-- src/views/chat/ChatView.vue -->
<template>
  <div class="chat-view">
    <div class="chat-sidebar">
      <SessionList
        :sessions="sessions"
        :current-session-id="currentSessionId"
        @new="handleNewSession"
        @select="handleSelectSession"
        @delete="handleDeleteSession"
      />
    </div>
    <div class="chat-main">
      <div class="chat-messages" ref="messagesContainer">
        <div v-if="currentMessages.length === 0" class="empty-state">
          <el-empty description="开始新对话..." />
        </div>
        <ChatMessage
          v-for="message in currentMessages"
          :key="message.id"
          :role="message.role"
          :content="message.content"
          :created-at="message.createdAt"
        />
        <div v-if="isLoading && currentMessages.length === 0" class="streaming-message">
          <ChatMessage
            role="ASSISTANT"
            content="正在思考..."
          />
        </div>
      </div>
      <div class="chat-input-container">
        <ChatInput
          :disabled="isLoading"
          @send="handleSendMessage"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, watch } from 'vue'
import ChatMessage from '@/components/chat/ChatMessage.vue'
import ChatInput from '@/components/chat/ChatInput.vue'
import SessionList from '@/components/chat/SessionList.vue'
import { useChat } from '@/composables/useChat'
import { ElMessage } from 'element-plus'

const {
  sessions,
  currentSessionId,
  currentMessages,
  isLoading,
  streamingContent,
  loadSessions,
  createNewSession,
  selectSession,
  sendMessage,
  deleteSession
} = useChat()

const messagesContainer = ref<HTMLElement>()

onMounted(async () => {
  await loadSessions()
})

watch(currentSessionId, async (newId) => {
  if (newId) {
    await nextTick()
    scrollToBottom()
  }
})

watch(currentMessages, () => {
  nextTick(() => scrollToBottom())
})

const handleNewSession = async () => {
  await createNewSession()
}

const handleSelectSession = async (sessionId: number) => {
  await selectSession(sessionId)
}

const handleDeleteSession = async (sessionId: number) => {
  await deleteSession(sessionId)
  ElMessage.success('对话已删除')
}

const handleSendMessage = async (message: string) => {
  await sendMessage(message)
}

const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}
</script>

<style scoped lang="scss">
.chat-view {
  display: flex;
  height: calc(100vh - 60px);
}

.chat-sidebar {
  width: 280px;
  flex-shrink: 0;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.streaming-message {
  opacity: 0.8;
}

.chat-input-container {
  flex-shrink: 0;
}
</style>
