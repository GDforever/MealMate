<!-- src/components/restaurant/RestaurantCard.vue -->
<template>
  <el-card class="restaurant-card" shadow="hover" @click="handleClick">
    <div class="restaurant-header">
      <h3 class="restaurant-name">{{ restaurant.name }}</h3>
      <div v-if="restaurant.rating" class="restaurant-rating">
        <el-icon color="#ff9900"><StarFilled /></el-icon>
        <span>{{ restaurant.rating.toFixed(1) }}</span>
      </div>
    </div>
    <p v-if="restaurant.cuisineType" class="restaurant-cuisine">
      <el-tag size="small">{{ restaurant.cuisineType }}</el-tag>
    </p>
    <p v-if="restaurant.address" class="restaurant-address">
      <el-icon><Location /></el-icon>
      {{ restaurant.address }}
    </p>
    <div class="restaurant-footer">
      <span v-if="restaurant.distance" class="restaurant-distance">
        {{ (restaurant.distance / 1000).toFixed(1) }} km
      </span>
      <span v-if="restaurant.avgPrice" class="restaurant-price">
        ¥{{ restaurant.avgPrice }}/人
      </span>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { StarFilled, Location } from '@element-plus/icons-vue'
import type { Restaurant } from '@/types'

const props = defineProps<{
  restaurant: Restaurant
}>()

const emit = defineEmits<{
  click: [restaurant: Restaurant]
}>()

const handleClick = () => {
  emit('click', props.restaurant)
}
</script>

<style scoped lang="scss">
.restaurant-card {
  cursor: pointer;
  transition: transform 0.2s;

  &:hover {
    transform: translateY(-2px);
  }

  :deep(.el-card__body) {
    padding: 16px;
  }
}

.restaurant-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.restaurant-name {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  flex: 1;
}

.restaurant-rating {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  font-weight: 600;
}

.restaurant-cuisine {
  margin: 8px 0;
}

.restaurant-address {
  display: flex;
  align-items: center;
  gap: 4px;
  margin: 8px 0;
  color: #606266;
  font-size: 14px;
}

.restaurant-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
  font-size: 13px;
  color: #909399;
}
</style>
