<!-- src/components/chat/SessionList.vue -->
<template>
  <div class="session-list">
    <div class="session-header">
      <h3>对话历史</h3>
      <el-button :icon="Plus" circle size="small" @click="handleNew" />
    </div>
    <div class="session-items">
      <div
        v-for="session in sessions"
        :key="session.id"
        :class="['session-item', { active: session.id === currentSessionId }]"
        @click="handleSelect(session.id)"
      >
        <div class="session-title">{{ session.title }}</div>
        <div class="session-meta">
          <span class="session-time">{{ formatDate(session.updatedAt) }}</span>
          <el-popconfirm
            title="确定删除此对话吗?"
            @confirm="handleDelete(session.id)"
          >
            <template #reference>
              <el-button
                :icon="Delete"
                size="small"
                text
                class="delete-btn"
              />
            </template>
          </el-popconfirm>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Plus, Delete } from '@element-plus/icons-vue'
import { formatDate } from '@/utils/date'

const props = defineProps<{
  sessions: Array<{
    id: number
    title: string
    updatedAt: string
  }>
  currentSessionId: number | null
}>()

const emit = defineEmits<{
  new: []
  select: [id: number]
  delete: [id: number]
}>()

const handleNew = () => {
  emit('new')
}

const handleSelect = (id: number) => {
  emit('select', id)
}

const handleDelete = (id: number) => {
  emit('delete', id)
}
</script>

<style scoped lang="scss">
.session-list {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f5f7fa;
  border-right: 1px solid #e4e7ed;
}

.session-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid #e4e7ed;

  h3 {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
  }
}

.session-items {
  flex: 1;
  overflow-y: auto;
}

.session-item {
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid #ebeef5;
  transition: background 0.2s;

  &:hover {
    background: #e9ecf0;
  }

  &.active {
    background: #e3f2fd;
  }
}

.session-title {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #909399;
}

.delete-btn {
  &:hover {
    color: #f56c6c;
  }
}
</style>
