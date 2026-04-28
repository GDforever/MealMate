<!-- src/components/restaurant/RestaurantList.vue -->
<template>
  <div class="restaurant-list">
    <RestaurantCard
      v-for="restaurant in restaurants"
      :key="restaurant.id"
      :restaurant="restaurant"
      @click="handleClick"
    />
  </div>
</template>

<script setup lang="ts">
import RestaurantCard from './RestaurantCard.vue'
import type { Restaurant } from '@/types'

defineProps<{
  restaurants: Restaurant[]
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
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}
</style>
