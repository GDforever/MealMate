<!-- src/views/restaurant/RestaurantListView.vue -->
<template>
  <div class="restaurant-list-view">
    <div class="page-header">
      <h2>餐厅搜索</h2>
      <div class="location-info">
        <el-tag v-if="locationStatus === 'located'" type="success" effect="plain">
          <el-icon><Location /></el-icon>
          已定位
        </el-tag>
        <el-tag v-else-if="locationStatus === 'default'" type="warning" effect="plain">
          使用默认位置
        </el-tag>
        <el-button size="small" :loading="locating" @click="handleGetCurrentLocation">
          {{ locationStatus === 'located' ? '重新定位' : '获取当前位置' }}
        </el-button>
      </div>
    </div>

    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="searchForm" @submit.prevent="handleSearch">
        <el-form-item>
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索餐厅名称"
            clearable
            style="width: 200px"
            @clear="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-select v-model="searchForm.cuisineType" placeholder="菜系" clearable style="width: 120px" @change="handleSearch">
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
          <el-select v-model="searchForm.radius" style="width: 120px" @change="handleSearch">
            <el-option :value="1000" label="1 km" />
            <el-option :value="3000" label="3 km" />
            <el-option :value="5000" label="5 km" />
            <el-option :value="10000" label="10 km" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" native-type="submit">搜索</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div v-loading="loading" class="results-container">
      <div v-if="!loading && restaurants.length === 0" class="empty-state">
        <el-empty description="附近没有找到餐厅，试试调整搜索条件" />
      </div>
      <div v-else class="list-content">
        <RestaurantCard
          v-for="restaurant in restaurants"
          :key="restaurant.id"
          :restaurant="restaurant"
          @click="handleRestaurantClick"
        />
      </div>

      <div v-if="restaurantStore.totalElements > 0" class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="restaurantStore.pageSize"
          :total="restaurantStore.totalElements"
          layout="prev, pager, next, total"
          background
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Location, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import RestaurantCard from '@/components/restaurant/RestaurantCard.vue'
import { useRestaurantStore } from '@/stores/restaurant'

const router = useRouter()
const restaurantStore = useRestaurantStore()

const loading = ref(false)
const locating = ref(false)
const locationStatus = ref<'default' | 'located'>('default')
const searchForm = ref({
  keyword: undefined as string | undefined,
  radius: 3000,
  cuisineType: undefined as string | undefined
})

const restaurants = computed(() => restaurantStore.list)
const currentPage = computed({
  get: () => restaurantStore.currentPage + 1,
  set: (val: number) => { restaurantStore.currentPage = val - 1 }
})

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
  locating.value = true
  try {
    await restaurantStore.getCurrentLocation()
    locationStatus.value = 'located'
    ElMessage.success('定位成功')
    await loadRestaurants()
  } catch {
    ElMessage.error('获取位置失败，请检查浏览器定位权限')
  } finally {
    locating.value = false
  }
}

const handleSearch = () => {
  restaurantStore.currentPage = 0
  loadRestaurants()
}

const handlePageChange = (page: number) => {
  restaurantStore.currentPage = page - 1
  loadRestaurants()
}

const handleRestaurantClick = (restaurant: any) => {
  router.push(`/restaurants/${restaurant.id}`)
}
</script>

<style scoped lang="scss">
.restaurant-list-view {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  h2 {
    margin: 0;
  }
}

.location-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-card {
  margin-bottom: 20px;

  :deep(.el-card__body) {
    padding: 16px 20px;
  }
}

.results-container {
  min-height: 400px;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 400px;
}

.list-content {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
