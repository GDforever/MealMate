<template>
  <router-view />
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

onMounted(async () => {
  if (userStore.isAuthenticated) {
    await userStore.fetchUserInfo()
  }
})
</script>

<style lang="scss">
@import '@/assets/styles/index.scss';
</style>
