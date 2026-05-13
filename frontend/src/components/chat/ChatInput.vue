<!-- src/components/chat/ChatInput.vue -->
<template>
  <div class="chat-input">
    <div v-if="imagePreview" class="image-preview">
      <img :src="imagePreview" alt="preview" />
      <el-button
        class="remove-btn"
        :icon="Close"
        circle
        size="small"
        @click="removeImage"
      />
    </div>
    <div class="input-row">
      <el-button
        :icon="PictureFilled"
        circle
        :disabled="disabled"
        @click="triggerFileInput"
      />
      <input
        ref="fileInput"
        type="file"
        accept="image/jpeg,image/png,image/webp"
        style="display: none"
        @change="handleFileSelect"
      />
      <el-input
        v-model="message"
        type="textarea"
        :rows="rows"
        placeholder="输入消息... (Enter 发送, Shift+Enter 换行)"
        :disabled="disabled"
        @keydown="handleKeydown"
        class="message-input"
      />
      <el-button
        type="primary"
        :icon="Promotion"
        :disabled="!canSend"
        @click="handleSend"
      >
        发送
      </el-button>
    </div>
    <div class="input-hint">{{ message.length }} / 2000</div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Promotion, PictureFilled, Close } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  disabled?: boolean
}>()

const emit = defineEmits<{
  send: [message: string, image?: File]
}>()

const message = ref('')
const rows = ref(1)
const selectedImage = ref<File | null>(null)
const imagePreview = ref<string | null>(null)
const fileInput = ref<HTMLInputElement>()

const canSend = computed(() => {
  const hasContent = message.value.trim().length > 0 || selectedImage.value
  return !props.disabled && hasContent && message.value.length <= 2000
})

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

const triggerFileInput = () => {
  fileInput.value?.click()
}

const handleFileSelect = (e: Event) => {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('图片不能超过10MB')
    return
  }

  selectedImage.value = file
  const reader = new FileReader()
  reader.onload = (ev) => {
    imagePreview.value = ev.target?.result as string
  }
  reader.readAsDataURL(file)
}

const removeImage = () => {
  selectedImage.value = null
  imagePreview.value = null
  if (fileInput.value) fileInput.value.value = ''
}

const handleSend = () => {
  if (!canSend.value) return
  emit('send', message.value.trim(), selectedImage.value || undefined)
  message.value = ''
  rows.value = 1
  removeImage()
}
</script>

<style scoped lang="scss">
.chat-input {
  padding: 16px;
  background: #fff;
  border-top: 1px solid #e4e7ed;
}

.image-preview {
  position: relative;
  display: inline-block;
  margin-bottom: 12px;

  img {
    max-width: 200px;
    max-height: 150px;
    border-radius: 8px;
    object-fit: cover;
  }

  .remove-btn {
    position: absolute;
    top: -8px;
    right: -8px;
  }
}

.input-row {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

.message-input {
  flex: 1;
}

.input-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  text-align: right;
}
</style>
