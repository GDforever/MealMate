<!-- src/views/restaurant/RestaurantDetailView.vue -->
<template>
  <div class="restaurant-detail-view" v-loading="loading">
    <el-page-header @back="goBack" class="page-header">
      <template #content>
        <h2>{{ restaurant?.name }}</h2>
      </template>
    </el-page-header>

    <el-row :gutter="24" v-if="restaurant">
      <el-col :span="16">
        <el-card class="detail-card">
          <div class="detail-header">
            <div class="rating-info">
              <el-rate
                v-model="displayRating"
                disabled
                show-score
                text-color="#ff9900"
              />
            </div>
            <div class="meta-info">
              <el-tag v-if="restaurant.cuisineType" type="primary">{{ restaurant.cuisineType }}</el-tag>
              <span v-if="restaurant.avgPrice" class="avg-price">¥{{ restaurant.avgPrice }}/人</span>
            </div>
          </div>

          <el-divider />

          <div class="detail-section">
            <h3>地址</h3>
            <p v-if="restaurant.address">
              <el-icon><Location /></el-icon>
              {{ restaurant.address }}
            </p>
            <el-button
              type="primary"
              size="small"
              :icon="Position"
              @click="openMap"
            >
              在地图中查看
            </el-button>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card>
          <template #header>
            <h3>快捷操作</h3>
          </template>
          <el-space direction="vertical" fill style="width: 100%;">
            <el-button type="primary" :icon="Plus" @click="handleAddMeal">
              添加用餐记录
            </el-button>
            <el-button :icon="Star" @click="handleFavorite">
              收藏餐厅
            </el-button>
          </el-space>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="dialogVisible" title="添加用餐记录" width="500px">
      <MealForm
        :record="mealRecord"
        :loading="submitting"
        submit-text="创建"
        @submit="handleSubmit"
        @cancel="dialogVisible = false"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Location, Position, Plus, Star } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import MealForm from '@/components/meal/MealForm.vue'
import { useRestaurantStore } from '@/stores/restaurant'
import { useMealStore } from '@/stores/meal'
import type { MealRecordRequest } from '@/types'

const router = useRouter()
const route = useRoute()
const restaurantStore = useRestaurantStore()
const mealStore = useMealStore()

const loading = ref(false)
const dialogVisible = ref(false)
const submitting = ref(false)

const restaurant = computed(() => restaurantStore.selectedRestaurant)
const displayRating = computed(() => restaurant.value?.rating || 0)

const mealRecord = ref<MealRecordRequest>({
  mealType: 'LUNCH',
  foodName: '',
  restaurantName: restaurant.value?.name || '',
  location: restaurant.value?.address || '',
  latitude: restaurant.value?.latitude,
  longitude: restaurant.value?.longitude,
  recordedAt: new Date().toISOString().slice(0, 19).replace('T', ' '),
  tags: []
})

onMounted(async () => {
  const id = Number(route.params.id)
  if (id) {
    await loadRestaurant(id)
  }
})

const loadRestaurant = async (id: number) => {
  loading.value = true
  try {
    await restaurantStore.fetchDetail(id)
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.back()
}

const openMap = () => {
  if (restaurant.value?.latitude && restaurant.value?.longitude) {
    const url = `https://uri.amap.com/marker?position=${restaurant.value.longitude},${restaurant.value.latitude}&name=${encodeURIComponent(restaurant.value.name)}`
    window.open(url, '_blank')
  }
}

const handleAddMeal = () => {
  dialogVisible.value = true
}

const handleFavorite = () => {
  ElMessage.success('已收藏')
}

const handleSubmit = async (data: MealRecordRequest) => {
  submitting.value = true
  try {
    await mealStore.createRecord(data)
    ElMessage.success('记录已添加')
    dialogVisible.value = false
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.restaurant-detail-view {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;

  h2 {
    margin: 0;
  }
}

.detail-card {
  min-height: 300px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.rating-info {
  font-size: 24px;
  font-weight: 600;
}

.meta-info {
  display: flex;
  gap: 16px;
  align-items: center;
  color: #606266;
}

.avg-price {
  font-size: 14px;
}

.detail-section {
  h3 {
    margin-bottom: 12px;
    font-size: 16px;
    font-weight: 600;
  }

  p {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 12px 0;
    color: #606266;
  }
}
</style>
