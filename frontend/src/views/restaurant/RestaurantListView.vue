<!-- src/views/restaurant/RestaurantListView.vue -->
<template>
  <div class="restaurant-list-view">
    <div class="page-header">
      <h2>餐厅搜索</h2>
      <el-button :icon="Location" @click="handleGetCurrentLocation">获取当前位置</el-button>
    </div>

    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm" @submit.prevent="handleSearch">
        <el-form-item label="距离">
          <el-slider
            v-model="searchForm.radius"
            :min="500"
            :max="10000"
            :step="500"
            :marks="{ 1000: '1km', 3000: '3km', 5000: '5km', 10000: '10km' }"
            show-input
            :input-size="'small'"
          />
        </el-form-item>
        <el-form-item label="菜系">
          <el-select v-model="searchForm.cuisineType" placeholder="选择菜系" clearable>
            <el-option label="中餐" value="中餐" />
            <el-option label="西餐" value="西餐" />
            <el-option label="日料" value="日料" />
            <el-option label="韩料" value="韩料" />
            <el-option label="快餐" value="快餐" />
            <el-option label="火锅" value="火锅" />
            <el-option label="烧烤" value="烧烤" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" native-type="submit">搜索</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div v-loading="loading" class="results-container">
      <RestaurantList
        :restaurants="restaurants"
        :loading="loading"
        @click="handleRestaurantClick"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Location, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import RestaurantList from '@/components/restaurant/RestaurantList.vue'
import { useRestaurantStore } from '@/stores/restaurant'

const router = useRouter()
const restaurantStore = useRestaurantStore()

const loading = ref(false)
const searchForm = ref({
  radius: 3000,
  cuisineType: undefined as string | undefined
})

const restaurants = computed(() => restaurantStore.list)

onMounted(async () => {
  await loadRestaurants()
})

const loadRestaurants = async () => {
  loading.value = true
  try {
    await restaurantStore.searchNearby(searchForm.value)
  } finally {
    loading.value = false
  }
}

const handleGetCurrentLocation = async () => {
  try {
    const location = await restaurantStore.getCurrentLocation()
    ElMessage.success(`已定位: ${location.latitude}, ${location.longitude}`)
    await loadRestaurants()
  } catch (error) {
    ElMessage.error('获取位置失败')
  }
}

const handleSearch = () => {
  loadRestaurants()
}

const handleRestaurantClick = (restaurant: any) => {
  router.push(`/restaurants/${restaurant.id}`)
}
</script>

<style scoped lang="scss">
.restaurant-list-view {
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

.search-card {
  margin-bottom: 24px;
}

.results-container {
  min-height: 400px;
}
</style>
