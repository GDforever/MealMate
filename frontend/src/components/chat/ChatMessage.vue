<!-- src/components/chat/ChatMessage.vue -->
<template>
  <div :class="['chat-message', role.toLowerCase()]">
    <div class="message-avatar">
      <el-avatar v-if="role === 'USER'" :icon="UserFilled" />
      <el-avatar v-else :icon="ChatDotRound" />
    </div>
    <div class="message-content">
      <div class="message-header">
        <span class="message-role">{{ role === 'USER' ? '你' : 'AI 助手' }}</span>
        <span class="message-time">{{ formattedTime }}</span>
      </div>
      <div class="message-text" v-html="renderedContent"></div>
      <div v-if="options && options.length > 0" class="message-options">
        <div
          v-for="(option, index) in options"
          :key="index"
          class="option-btn"
          @click="$emit('selectOption', option)"
        >
          {{ option }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { UserFilled, ChatDotRound } from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'
import { formatDate } from '@/utils/date'

const props = defineProps<{
  role: 'USER' | 'ASSISTANT' | 'SYSTEM'
  content: string
  createdAt?: string
  options?: string[]
}>()

defineEmits<{
  selectOption: [option: string]
}>()

const md = new MarkdownIt()

const formattedTime = computed(() => {
  return props.createdAt ? formatDate(props.createdAt, 'HH:mm') : ''
})

const renderedContent = computed(() => {
  return md.render(props.content)
})
</script>

<style scoped lang="scss">
.chat-message {
  display: flex;
  gap: 12px;
  padding: 16px;
  animation: fadeIn 0.3s ease;

  &.user {
    flex-direction: row-reverse;

    .message-content {
      background: #e3f2fd;
      align-items: flex-end;
    }
  }

  &.assistant {
    .message-content {
      background: #f5f7fa;
      align-items: flex-start;
    }
  }
}

.message-avatar {
  flex-shrink: 0;
}

.message-content {
  display: flex;
  flex-direction: column;
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 12px;
}

.message-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 12px;
  color: #909399;
}

.message-role {
  font-weight: 600;
}

.message-text {
  line-height: 1.6;
  word-wrap: break-word;

  :deep(p) {
    margin: 0 0 8px 0;

    &:last-child {
      margin-bottom: 0;
    }
  }

  :deep(code) {
    background: rgba(0, 0, 0, 0.1);
    padding: 2px 4px;
    border-radius: 4px;
    font-family: 'Monaco', 'Consolas', monospace;
  }

  :deep(pre) {
    background: rgba(0, 0, 0, 0.05);
    padding: 12px;
    border-radius: 8px;
    overflow-x: auto;
    margin: 8px 0;
  }
}

.message-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.option-btn {
  padding: 6px 16px;
  background: #fff;
  border: 1px solid #409eff;
  border-radius: 18px;
  color: #409eff;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;

  &:hover {
    background: #409eff;
    color: #fff;
  }

  &:active {
    transform: scale(0.96);
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
