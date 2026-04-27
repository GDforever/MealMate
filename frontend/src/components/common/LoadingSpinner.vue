<template>
  <div class="loading-spinner">
    <el-icon class="is-loading" :size="40">
      <Loading />
    </el-icon>
    <p v-if="text">{{ text }}</p>
  </div>
</template>

<script setup lang="ts">
import { Loading } from '@element-plus/icons-vue'

defineProps<{
  text?: string
}>()
</script>

<style scoped lang="scss">
.loading-spinner {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #909399;

  p {
    margin-top: 16px;
  }
}
</style>
