<!-- src/views/meal/MealCalendarView.vue -->
<template>
  <div class="meal-calendar-view">
    <div class="page-header">
      <h2>用餐日历</h2>
      <el-date-picker
        v-model="currentMonth"
        type="month"
        placeholder="选择月份"
        @change="handleMonthChange"
      />
    </div>

    <el-row :gutter="24">
      <el-col :span="16">
        <el-card>
          <div class="calendar-header">
            <el-button :icon="ArrowLeft" @click="prevMonth" />
            <h3>{{ monthTitle }}</h3>
            <el-button :icon="ArrowRight" @click="nextMonth" />
          </div>
          <div class="calendar-grid">
            <div class="calendar-weekdays">
              <div v-for="day in weekdays" :key="day" class="weekday">{{ day }}</div>
            </div>
            <div class="calendar-days">
              <div
                v-for="(day, index) in calendarDays"
                :key="index"
                :class="['calendar-day', { 'other-month': day.otherMonth, today: day.isToday }]"
                @click="handleDayClick(day)"
              >
                <span class="day-number">{{ day.day }}</span>
                <div v-if="day.meals.length > 0" class="day-meals">
                  <div
                    v-for="meal in day.meals.slice(0, 3)"
                    :key="meal.id"
                    class="meal-dot"
                    :title="meal.foodName"
                  />
                  <span v-if="day.meals.length > 3" class="more-meals">+{{ day.meals.length - 3 }}</span>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>
            <h3>{{ selectedDate || '选择日期查看详情' }}</h3>
          </template>
          <div v-loading="loading" class="day-details">
            <div v-if="selectedDateMeals.length === 0" class="empty-state">
              <el-empty description="当日无用餐记录" />
            </div>
            <MealCard
              v-for="meal in selectedDateMeals"
              :key="meal.id"
              :record="meal"
              @edit="handleEdit"
              @delete="handleDelete"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="dialogVisible" title="编辑记录" width="500px">
      <MealForm
        :record="editingRecord"
        :loading="submitting"
        submit-text="保存"
        @submit="handleSubmit"
        @cancel="dialogVisible = false"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import MealCard from '@/components/meal/MealCard.vue'
import MealForm from '@/components/meal/MealForm.vue'
import { mealApi } from '@/api/meal'
import type { MealRecord, MealRecordRequest } from '@/types'

const weekdays = ['日', '一', '二', '三', '四', '五', '六']

const currentMonth = ref(new Date())
const selectedDate = ref<string>()
const selectedDateMeals = ref<MealRecord[]>([])
const editingRecord = ref<MealRecord>()
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const allMeals = ref<MealRecord[]>([])

const monthTitle = computed(() => {
  return currentMonth.value.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long' })
})

const calendarDays = computed(() => {
  const year = currentMonth.value.getFullYear()
  const month = currentMonth.value.getMonth()
  const firstDay = new Date(year, month, 1)
  const lastDay = new Date(year, month + 1, 0)
  const startDayOfWeek = firstDay.getDay()
  const daysInMonth = lastDay.getDate()

  const days: Array<{
    day: number
    date: string
    otherMonth: boolean
    isToday: boolean
    meals: MealRecord[]
  }> = []

  // Previous month days
  const prevMonthLastDay = new Date(year, month, 0).getDate()
  for (let i = startDayOfWeek - 1; i >= 0; i--) {
    days.push({
      day: prevMonthLastDay - i,
      date: '',
      otherMonth: true,
      isToday: false,
      meals: []
    })
  }

  // Current month days
  const today = new Date()
  for (let i = 1; i <= daysInMonth; i++) {
    const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(i).padStart(2, '0')}`
    const isToday = today.getFullYear() === year && today.getMonth() === month && today.getDate() === i
    const dayMeals = allMeals.value.filter(m => m.recordedAt.startsWith(dateStr))
    days.push({
      day: i,
      date: dateStr,
      otherMonth: false,
      isToday,
      meals: dayMeals
    })
  }

  // Next month days
  const remainingCells = 42 - days.length
  for (let i = 1; i <= remainingCells; i++) {
    days.push({
      day: i,
      date: '',
      otherMonth: true,
      isToday: false,
      meals: []
    })
  }

  return days
})

onMounted(() => {
  loadMeals()
})

const loadMeals = async () => {
  loading.value = true
  try {
    const year = currentMonth.value.getFullYear()
    const month = currentMonth.value.getMonth() + 1
    const startDate = `${year}-${String(month).padStart(2, '0')}-01`
    const lastDay = new Date(year, currentMonth.value.getMonth() + 1, 0).getDate()
    const endDate = `${year}-${String(month).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`

    const response = await mealApi.getRecords({
      page: 0,
      size: 100,
      startDate,
      endDate
    })
    allMeals.value = response.content || []
  } finally {
    loading.value = false
  }
}

const prevMonth = () => {
  currentMonth.value = new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth() - 1)
  loadMeals()
}

const nextMonth = () => {
  currentMonth.value = new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth() + 1)
  loadMeals()
}

const handleMonthChange = () => {
  loadMeals()
}

const handleDayClick = (day: any) => {
  if (day.otherMonth) return
  selectedDate.value = day.date
  selectedDateMeals.value = day.meals
}

const handleEdit = (record: MealRecord) => {
  editingRecord.value = record
  dialogVisible.value = true
}

const handleDelete = async (id: number) => {
  await mealApi.deleteRecord(id)
  ElMessage.success('删除成功')
  loadMeals()
}

const handleSubmit = async (data: MealRecordRequest) => {
  submitting.value = true
  try {
    await mealApi.updateRecord(editingRecord.value!.id, data)
    ElMessage.success('更新成功')
    dialogVisible.value = false
    loadMeals()
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.meal-calendar-view {
  padding: 24px;
  max-width: 1400px;
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

.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;

  h3 {
    margin: 0;
  }
}

.calendar-grid {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
}

.calendar-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  background: #f5f7fa;

  .weekday {
    padding: 12px;
    text-align: center;
    font-weight: 600;
    border-right: 1px solid #ebeef5;

    &:last-child {
      border-right: none;
    }
  }
}

.calendar-days {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
}

.calendar-day {
  min-height: 80px;
  padding: 8px;
  border-top: 1px solid #ebeef5;
  border-right: 1px solid #ebeef5;
  cursor: pointer;
  transition: background 0.2s;

  &:nth-child(7n) {
    border-right: none;
  }

  &:hover:not(.other-month) {
    background: #f5f7fa;
  }

  &.other-month {
    color: #c0c4cc;
    background: #fafafa;
  }

  &.today {
    background: #e3f2fd;
  }
}

.day-number {
  font-size: 14px;
  font-weight: 600;
  display: block;
  margin-bottom: 4px;
}

.day-meals {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.meal-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #409eff;
}

.more-meals {
  font-size: 10px;
  color: #909399;
}

.day-details {
  min-height: 200px;
}

.empty-state {
  padding: 20px 0;
}
</style>
