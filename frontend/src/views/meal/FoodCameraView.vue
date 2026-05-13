<!-- src/views/meal/FoodCameraView.vue -->
<template>
  <div class="food-camera-view">
    <div class="page-header">
      <el-button :icon="ArrowLeft" @click="router.back()">返回</el-button>
      <h2>拍照记录</h2>
      <div style="width: 60px"></div>
    </div>

    <!-- Step 1: Select/Take photo -->
    <div v-if="step === 1" class="step-content">
      <div class="upload-area" @click="triggerFileInput">
        <el-icon :size="64" color="#909399"><UploadFilled /></el-icon>
        <p>点击拍照或选择图片</p>
        <p class="hint">支持 JPG、PNG、WEBP，最大 10MB</p>
      </div>
      <input
        ref="fileInput"
        type="file"
        accept="image/jpeg,image/png,image/webp"
        capture="environment"
        style="display: none"
        @change="handleFileSelect"
      />
    </div>

    <!-- Step 2: Preview and select meal type -->
    <div v-if="step === 2" class="step-content">
      <div class="preview-section">
        <img :src="imagePreview!" alt="preview" class="preview-image" />
      </div>

      <div class="meal-type-section">
        <h3>选择餐次</h3>
        <div class="meal-type-options">
          <div
            v-for="option in mealTypeOptions"
            :key="option.value"
            :class="['meal-type-card', { active: selectedMealType === option.value }]"
            @click="selectedMealType = option.value"
          >
            <span class="emoji">{{ option.emoji }}</span>
            <span class="label">{{ option.label }}</span>
          </div>
        </div>
      </div>

      <div class="actions">
        <el-button @click="resetPhoto">重新选择</el-button>
        <el-button type="primary" :loading="recognizing" @click="handleRecognize">
          识别并记录
        </el-button>
      </div>
    </div>

    <!-- Step 3: Result -->
    <div v-if="step === 3" class="step-content">
      <el-result
        :icon="resultSuccess ? 'success' : 'warning'"
        :title="resultSuccess ? '识别成功' : '识别未确定'"
        :sub-title="resultMessage"
      >
        <template #extra>
          <el-button type="primary" @click="resetAll">继续拍照</el-button>
          <el-button @click="router.push('/meals')">查看记录</el-button>
        </template>
      </el-result>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, UploadFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { mealApi } from '@/api/meal'
import type { FoodRecognitionResponse, MealType } from '@/types'

const router = useRouter()

const step = ref(1)
const fileInput = ref<HTMLInputElement>()
const selectedImage = ref<File | null>(null)
const imagePreview = ref<string | null>(null)
const selectedMealType = ref<MealType>('LUNCH')
const recognizing = ref(false)
const resultSuccess = ref(false)
const resultMessage = ref('')

const mealTypeOptions = [
  { value: 'BREAKFAST' as MealType, label: '早餐', emoji: '\u{1F305}' },
  { value: 'LUNCH' as MealType, label: '午餐', emoji: '\u2600\uFE0F' },
  { value: 'DINNER' as MealType, label: '晚餐', emoji: '\u{1F319}' },
  { value: 'SNACK' as MealType, label: '加餐', emoji: '\u{1F36A}' }
]

onMounted(() => {
  const hour = new Date().getHours()
  if (hour >= 6 && hour < 9) selectedMealType.value = 'BREAKFAST'
  else if (hour >= 11 && hour < 14) selectedMealType.value = 'LUNCH'
  else if (hour >= 17 && hour < 20) selectedMealType.value = 'DINNER'
  else selectedMealType.value = 'SNACK'
})

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
    step.value = 2
  }
  reader.readAsDataURL(file)
}

const resetPhoto = () => {
  selectedImage.value = null
  imagePreview.value = null
  step.value = 1
  if (fileInput.value) fileInput.value.value = ''
}

const handleRecognize = async () => {
  if (!selectedImage.value) return
  recognizing.value = true

  try {
    const result: FoodRecognitionResponse = await mealApi.recognizeFood(
      selectedImage.value,
      selectedMealType.value
    )
    resultSuccess.value = result.confidence >= 0.5
    resultMessage.value = result.message || `识别为「${result.foodName}」，约${result.calories}kcal`
    step.value = 3
    if (resultSuccess.value) {
      ElMessage.success('已自动记录')
    }
  } catch {
    resultSuccess.value = false
    resultMessage.value = '识别失败，请重试或手动记录'
    step.value = 3
  } finally {
    recognizing.value = false
  }
}

const resetAll = () => {
  step.value = 1
  resetPhoto()
  resultSuccess.value = false
  resultMessage.value = ''
}
</script>

<style scoped lang="scss">
.food-camera-view {
  padding: 24px;
  max-width: 600px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;

  h2 {
    margin: 0;
  }
}

.step-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.upload-area {
  width: 100%;
  padding: 60px 20px;
  border: 2px dashed #dcdfe6;
  border-radius: 12px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.2s;

  &:hover {
    border-color: #409eff;
  }

  p {
    margin: 12px 0 0;
    color: #606266;
  }

  .hint {
    font-size: 12px;
    color: #909399;
  }
}

.preview-section {
  width: 100%;
  text-align: center;
  margin-bottom: 24px;

  .preview-image {
    max-width: 100%;
    max-height: 300px;
    border-radius: 12px;
    object-fit: cover;
  }
}

.meal-type-section {
  width: 100%;

  h3 {
    margin: 0 0 16px;
    text-align: center;
  }
}

.meal-type-options {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.meal-type-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 8px;
  border: 2px solid #e4e7ed;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;

  .emoji {
    font-size: 28px;
    margin-bottom: 8px;
  }

  .label {
    font-size: 14px;
    color: #606266;
  }

  &.active {
    border-color: #409eff;
    background: #ecf5ff;

    .label {
      color: #409eff;
      font-weight: 600;
    }
  }
}

.actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
  width: 100%;

  .el-button {
    flex: 1;
  }
}
</style>
