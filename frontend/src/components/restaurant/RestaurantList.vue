<!-- src/components/restaurant/RestaurantList.vue -->
<template>
  <div class="restaurant-list">
    <div v-if="loading" class="loading-state">
      <LoadingSpinner text="搜索附近餐厅..." />
    </div>
    <div v-else-if="restaurants.length === 0" class="empty-state">
      <el-empty description="暂无餐厅数据" />
    </div>
    <div v-else class="list-content">
      <RestaurantCard
        v-for="restaurant in restaurants"
        :key="restaurant.id"
        :restaurant="restaurant"
        @click="handleClick"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import RestaurantCard from './RestaurantCard.vue'
import type { Restaurant } from '@/types'

defineProps<{
  restaurants: Restaurant[]
  loading?: boolean
}>()

const emit = defineEmits<{
  click: [restaurant: Restaurant]
}>()

const handleClick = (restaurant: Restaurant) => {
  emit('click', restaurant)
}
</script>

<style scoped lang="scss">
.restaurant-list {
  height: 100%;
  overflow-y: auto;
}

.loading-state,
.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 300px;
}

.list-content {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  padding: 16px;
}
</style>
