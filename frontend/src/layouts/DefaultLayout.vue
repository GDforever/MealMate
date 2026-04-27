<template>
  <el-container class="layout-container">
    <AppHeader />
    <el-main class="layout-main">
      <router-view />
    </el-main>
    <AppFooter />
  </el-container>
</template>

<script setup lang="ts">
import AppHeader from '@/components/common/AppHeader.vue'
import AppFooter from '@/components/common/AppFooter.vue'
</script>

<style scoped lang="scss">
.layout-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.layout-main {
  flex: 1;
  padding: 0;
  overflow-x: hidden;
}
</style>
