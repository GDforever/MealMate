<!-- src/components/restaurant/RestaurantCard.vue -->
<template>
  <el-card class="restaurant-card" shadow="hover" @click="handleClick">
    <div v-if="restaurant.photoUrl" class="card-image">
      <img :src="restaurant.photoUrl" :alt="restaurant.name" />
    </div>
    <div class="card-body">
      <div class="card-header">
        <h3 class="name">{{ restaurant.name }}</h3>
        <span v-if="restaurant.distance != null" class="distance">
          {{ formatDistance(restaurant.distance) }}
        </span>
      </div>

      <div class="card-tags">
        <el-tag v-if="restaurant.cuisineType" size="small" type="primary" effect="plain">
          {{ restaurant.cuisineType }}
        </el-tag>
      </div>

      <div class="card-info">
        <div v-if="restaurant.rating != null" class="rating">
          <el-rate :model-value="restaurant.rating / 2" disabled :max="5" size="small" />
          <span class="rating-text">{{ restaurant.rating.toFixed(1) }}</span>
        </div>
        <span v-if="restaurant.avgPrice != null" class="price">
          ¥{{ restaurant.avgPrice }}/人
        </span>
      </div>

      <p v-if="restaurant.address" class="address">
        <el-icon><Location /></el-icon>
        {{ restaurant.address }}
      </p>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { Location } from '@element-plus/icons-vue'
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

const formatDistance = (meters: number) => {
  if (meters < 1000) return `${Math.round(meters)}m`
  return `${(meters / 1000).toFixed(1)}km`
}
</script>

<style scoped lang="scss">
.restaurant-card {
  cursor: pointer;
  transition: transform 0.2s;
  overflow: hidden;

  &:hover {
    transform: translateY(-2px);
  }

  :deep(.el-card__body) {
    padding: 0;
  }
}

.card-image {
  width: 100%;
  height: 160px;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.card-body {
  padding: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 10px;
}

.name {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.distance {
  flex-shrink: 0;
  margin-left: 8px;
  font-size: 13px;
  color: #409eff;
  font-weight: 500;
}

.card-tags {
  display: flex;
  gap: 6px;
  margin-bottom: 10px;
}

.card-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.rating {
  display: flex;
  align-items: center;
  gap: 4px;
}

.rating-text {
  font-size: 13px;
  font-weight: 600;
  color: #ff9900;
}

.price {
  font-size: 13px;
  color: #909399;
}

.address {
  display: flex;
  align-items: center;
  gap: 4px;
  margin: 0;
  color: #606266;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
