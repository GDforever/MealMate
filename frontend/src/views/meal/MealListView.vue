<!-- src/views/meal/MealListView.vue -->
<template>
  <div class="meal-list-view">
    <div class="page-header">
      <h2>用餐记录</h2>
      <div class="header-actions">
        <el-button :icon="Camera" @click="router.push('/meals/photo')">拍照记录</el-button>
        <el-button type="primary" :icon="Plus" @click="showCreateDialog">添加记录</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        @change="handleDateRangeChange"
      />
      <el-select v-model="mealTypeFilter" placeholder="用餐类型" clearable @change="handleFilterChange">
        <el-option label="早餐" value="BREAKFAST" />
        <el-option label="午餐" value="LUNCH" />
        <el-option label="晚餐" value="DINNER" />
        <el-option label="加餐" value="SNACK" />
      </el-select>
      <el-button :icon="Refresh" @click="handleRefresh">刷新</el-button>
    </div>

    <div v-loading="loading" class="meal-list">
      <MealCard
        v-for="record in records"
        :key="record.id"
        :record="record"
        @edit="handleEdit"
        @delete="handleDelete"
      />
      <el-empty v-if="!loading && records.length === 0" description="暂无用餐记录" />
    </div>

    <div v-if="total > pageSize" class="pagination">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editingRecord ? '编辑记录' : '添加记录'"
      width="500px"
    >
      <MealForm
        :record="editingRecord"
        :loading="submitting"
        :submit-text="editingRecord ? '保存' : '创建'"
        @submit="handleSubmit"
        @cancel="dialogVisible = false"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Camera, Plus, Refresh } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import MealCard from '@/components/meal/MealCard.vue'
import MealForm from '@/components/meal/MealForm.vue'
import { useMealStore } from '@/stores/meal'
import type { MealRecord, MealRecordRequest } from '@/types'

const mealStore = useMealStore()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const editingRecord = ref<MealRecord>()
const dateRange = ref<[Date, Date]>()
const mealTypeFilter = ref<string>()

const records = computed(() => mealStore.records)
const total = computed(() => mealStore.total)
const currentPage = computed({
  get: () => mealStore.currentPage + 1,
  set: (v) => { mealStore.currentPage = v - 1 }
})
const pageSize = computed(() => mealStore.pageSize)

onMounted(() => {
  loadRecords()
})

const loadRecords = async () => {
  loading.value = true
  try {
    await mealStore.fetchRecords(currentPage.value - 1)
  } finally {
    loading.value = false
  }
}

const showCreateDialog = () => {
  editingRecord.value = undefined
  dialogVisible.value = true
}

const handleEdit = (record: MealRecord) => {
  editingRecord.value = record
  dialogVisible.value = true
}

const handleDelete = async (id: number) => {
  await mealStore.deleteRecord(id)
  ElMessage.success('删除成功')
  await loadRecords()
}

const handleDateRangeChange = () => {
  mealStore.setFilters({
    startDate: dateRange.value?.[0].toISOString().split('T')[0],
    endDate: dateRange.value?.[1].toISOString().split('T')[0]
  })
  loadRecords()
}

const handleFilterChange = () => {
  mealStore.setFilters({ mealType: mealTypeFilter.value as any })
  loadRecords()
}

const handleRefresh = () => {
  loadRecords()
}

const handlePageChange = () => {
  loadRecords()
}

const handleSubmit = async (data: MealRecordRequest) => {
  submitting.value = true
  try {
    if (editingRecord.value) {
      await mealStore.updateRecord(editingRecord.value.id, data)
      ElMessage.success('更新成功')
    } else {
      await mealStore.createRecord(data)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await loadRecords()
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.meal-list-view {
  padding: 24px;
  max-width: 1200px;
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

.header-actions {
  display: flex;
  gap: 8px;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.meal-list {
  min-height: 200px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
