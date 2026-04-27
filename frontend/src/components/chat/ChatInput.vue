<!-- src/components/chat/ChatInput.vue -->
<template>
  <div class="chat-input">
    <el-input
      v-model="message"
      type="textarea"
      :rows="rows"
      placeholder="输入消息... (Enter 发送, Shift+Enter 换行)"
      :disabled="disabled"
      @keydown="handleKeydown"
    />
    <div class="input-actions">
      <span class="input-hint">{{ message.length }} / 2000</span>
      <el-button
        type="primary"
        :icon="Promotion"
        :disabled="!canSend"
        @click="handleSend"
      >
        发送
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Promotion } from '@element-plus/icons-vue'

const props = defineProps<{
  disabled?: boolean
}>()

const emit = defineEmits<{
  send: [message: string]
}>()

const message = ref('')
const rows = ref(1)

const canSend = computed(() => {
  return !props.disabled && message.value.trim().length > 0 && message.value.length <= 2000
})

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

const handleSend = () => {
  if (!canSend.value) return
  emit('send', message.value.trim())
  message.value = ''
  rows.value = 1
}

defineExpose({
  focus: () => {
    // Focus implementation if needed
  }
})
</script>

<style scoped lang="scss">
.chat-input {
  padding: 16px;
  background: #fff;
  border-top: 1px solid #e4e7ed;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}

.input-hint {
  font-size: 12px;
  color: #909399;
}
</style>
