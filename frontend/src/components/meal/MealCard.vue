<!-- src/components/meal/MealCard.vue -->
<template>
  <el-card class="meal-card" shadow="hover">
    <div class="meal-header">
      <el-tag :type="mealTypeColor" size="small">{{ mealTypeText }}</el-tag>
      <span class="meal-time">{{ formattedTime }}</span>
    </div>
    <h3 class="meal-name">{{ record.foodName }}</h3>
    <p v-if="record.restaurantName" class="meal-restaurant">
      <el-icon><Shop /></el-icon>
      {{ record.restaurantName }}
    </p>
    <div v-if="tagList.length" class="meal-tags">
      <el-tag
        v-for="tag in tagList"
        :key="tag"
        size="small"
        class="tag-item"
      >
        {{ tag }}
      </el-tag>
    </div>
    <div v-if="record.userRating" class="meal-rating">
      <el-rate
        v-model="record.userRating"
        disabled
        show-score
        text-color="#ff9900"
      />
    </div>
    <div class="meal-actions">
      <el-button size="small" text @click="handleEdit">编辑</el-button>
      <el-popconfirm
        title="确定删除此记录吗?"
        @confirm="handleDelete"
      >
        <template #reference>
          <el-button size="small" text type="danger">删除</el-button>
        </template>
      </el-popconfirm>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Shop } from '@element-plus/icons-vue'
import { formatDate } from '@/utils/date'
import type { MealRecord } from '@/types'

const props = defineProps<{
  record: MealRecord
}>()

const emit = defineEmits<{
  edit: [record: MealRecord]
  delete: [id: number]
}>()

const mealTypeText = computed(() => {
  const typeMap: Record<string, string> = {
    BREAKFAST: '早餐',
    LUNCH: '午餐',
    DINNER: '晚餐',
    SNACK: '加餐'
  }
  return typeMap[props.record.mealType] || props.record.mealType
})

const mealTypeColor = computed(() => {
  const colorMap: Record<string, string> = {
    BREAKFAST: 'success',
    LUNCH: 'primary',
    DINNER: 'warning',
    SNACK: 'info'
  }
  return colorMap[props.record.mealType] || 'info'
})

const formattedTime = computed(() => {
  return formatDate(props.record.recordedAt, 'MM-DD HH:mm')
})

const tagList = computed(() => {
  if (!props.record.tags) return []
  return props.record.tags.split(',').map(t => t.trim()).filter(Boolean)
})

const handleEdit = () => {
  emit('edit', props.record)
}

const handleDelete = () => {
  emit('delete', props.record.id)
}
</script>

<style scoped lang="scss">
.meal-card {
  margin-bottom: 16px;

  :deep(.el-card__body) {
    padding: 16px;
  }
}

.meal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.meal-time {
  font-size: 12px;
  color: #909399;
}

.meal-name {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
}

.meal-restaurant {
  display: flex;
  align-items: center;
  gap: 4px;
  margin: 8px 0;
  color: #606266;
  font-size: 14px;
}

.meal-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 12px 0;
}

.tag-item {
  cursor: default;
}

.meal-rating {
  margin: 12px 0;
}

.meal-actions {
  display: flex;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}
</style>
