<!-- src/views/meal/MealStatsView.vue -->
<template>
  <div class="meal-stats-view">
    <div class="page-header">
      <h2>数据统计</h2>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        @change="loadStats"
      />
    </div>

    <el-row :gutter="24" v-loading="loading">
      <el-col :span="6">
        <el-card class="stat-card">
          <el-statistic title="总用餐次数" :value="stats.totalMeals" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <el-statistic title="平均评分" :value="stats.averageRating" :precision="1" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <el-statistic title="最喜欢的食物" :value="stats.topFoods[0]?.name || '-'" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <el-statistic title="最常去的餐厅" :value="stats.topRestaurants[0]?.name || '-'" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="24" style="margin-top: 24px;">
      <el-col :span="12">
        <el-card>
          <template #header>
            <h3>用餐趋势（近30天）</h3>
          </template>
          <v-chart class="chart" :option="trendChartOption" autoresize />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <h3>用餐类型分布</h3>
          </template>
          <v-chart class="chart" :option="typeChartOption" autoresize />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="24" style="margin-top: 24px;">
      <el-col :span="12">
        <el-card>
          <template #header>
            <h3>热门食物 TOP10</h3>
          </template>
          <v-chart class="chart" :option="foodChartOption" autoresize />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <h3>热门餐厅 TOP10</h3>
          </template>
          <v-chart class="chart" :option="restaurantChartOption" autoresize />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart, BarChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'
import type { MealStats } from '@/types'

use([
  CanvasRenderer,
  LineChart,
  PieChart,
  BarChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
])

const loading = ref(false)
const dateRange = ref<[Date, Date]>()
const stats = ref<MealStats>({
  totalMeals: 0,
  byType: { BREAKFAST: 0, LUNCH: 0, DINNER: 0, SNACK: 0 },
  topFoods: [],
  topRestaurants: [],
  averageRating: 0
})

const trendChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: {
    type: 'category',
    data: generateLast30Days()
  },
  yAxis: { type: 'value' },
  series: [{
    data: generateRandomData(30),
    type: 'line',
    smooth: true,
    areaStyle: { opacity: 0.3 }
  }]
}))

const typeChartOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { orient: 'vertical', left: 'left' },
  series: [{
    type: 'pie',
    radius: '60%',
    data: [
      { value: stats.value.byType.BREAKFAST, name: '早餐' },
      { value: stats.value.byType.LUNCH, name: '午餐' },
      { value: stats.value.byType.DINNER, name: '晚餐' },
      { value: stats.value.byType.SNACK, name: '加餐' }
    ]
  }]
}))

const foodChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: {
    type: 'category',
    data: stats.value.topFoods.map(f => f.name)
  },
  yAxis: { type: 'value' },
  series: [{
    data: stats.value.topFoods.map(f => f.count),
    type: 'bar',
    itemStyle: { color: '#409eff' }
  }]
}))

const restaurantChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: {
    type: 'category',
    data: stats.value.topRestaurants.map(r => r.name)
  },
  yAxis: { type: 'value' },
  series: [{
    data: stats.value.topRestaurants.map(r => r.count),
    type: 'bar',
    itemStyle: { color: '#67c23a' }
  }]
}))

function generateLast30Days(): string[] {
  const days: string[] = []
  for (let i = 29; i >= 0; i--) {
    const date = new Date()
    date.setDate(date.getDate() - i)
    days.push(date.toISOString().split('T')[0].substring(5))
  }
  return days
}

function generateRandomData(count: number): number[] {
  return Array.from({ length: count }, () => Math.floor(Math.random() * 5) + 1)
}

onMounted(() => {
  loadStats()
})

const loadStats = async () => {
  loading.value = true
  try {
    // For MVP, use mock data
    stats.value = {
      totalMeals: 156,
      byType: { BREAKFAST: 45, LUNCH: 52, DINNER: 48, SNACK: 11 },
      topFoods: [
        { name: '宫保鸡丁', count: 12 },
        { name: '红烧肉', count: 10 },
        { name: '鱼香肉丝', count: 8 }
      ],
      topRestaurants: [
        { name: '川味人家', count: 15 },
        { name: '兰州拉面', count: 12 },
        { name: '汉堡王', count: 10 }
      ],
      averageRating: 4.2
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.meal-stats-view {
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

.stat-card {
  text-align: center;

  :deep(.el-statistic__head) {
    font-size: 14px;
    color: #909399;
  }

  :deep(.el-statistic__number) {
    font-size: 28px;
    font-weight: 600;
    color: #409eff;
  }
}

.chart {
  height: 300px;
}
</style>
