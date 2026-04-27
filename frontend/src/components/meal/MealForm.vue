<!-- src/components/meal/MealForm.vue -->
<template>
  <el-form
    ref="formRef"
    :model="formData"
    :rules="rules"
    label-width="100px"
    @submit.prevent="handleSubmit"
  >
    <el-form-item label="用餐类型" prop="mealType">
      <el-select v-model="formData.mealType" placeholder="请选择">
        <el-option label="早餐" value="BREAKFAST" />
        <el-option label="午餐" value="LUNCH" />
        <el-option label="晚餐" value="DINNER" />
        <el-option label="加餐" value="SNACK" />
      </el-select>
    </el-form-item>

    <el-form-item label="食物名称" prop="foodName">
      <el-input
        v-model="formData.foodName"
        placeholder="如：宫保鸡丁"
        maxlength="100"
        show-word-limit
      />
    </el-form-item>

    <el-form-item label="餐厅名称">
      <el-input
        v-model="formData.restaurantName"
        placeholder="选填"
        maxlength="100"
      />
    </el-form-item>

    <el-form-item label="用餐地点">
      <el-input
        v-model="formData.location"
        placeholder="选填"
        maxlength="200"
      />
    </el-form-item>

    <el-form-item label="用餐时间" prop="recordedAt">
      <el-date-picker
        v-model="formData.recordedAt"
        type="datetime"
        placeholder="选择日期时间"
        format="YYYY-MM-DD HH:mm"
        value-format="YYYY-MM-DD HH:mm:ss"
      />
    </el-form-item>

    <el-form-item label="评分">
      <el-rate v-model="formData.userRating" :max="5" allow-half />
    </el-form-item>

    <el-form-item label="标签">
      <el-select
        v-model="formData.tags"
        multiple
        filterable
        allow-create
        placeholder="选择或创建标签"
      >
        <el-option
          v-for="tag in commonTags"
          :key="tag"
          :label="tag"
          :value="tag"
        />
      </el-select>
    </el-form-item>

    <el-form-item>
      <el-button type="primary" native-type="submit" :loading="loading">
        {{ submitText }}
      </el-button>
      <el-button @click="handleCancel">取消</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { MealRecord, MealRecordRequest } from '@/types'

const props = withDefaults(defineProps<{
  record?: MealRecord
  loading?: boolean
  submitText?: string
}>(), {
  submitText: '提交'
})

const emit = defineEmits<{
  submit: [data: MealRecordRequest]
  cancel: []
}>()

const formRef = ref<FormInstance>()
const formData = reactive<MealRecordRequest>({
  mealType: props.record?.mealType || 'LUNCH',
  foodName: props.record?.foodName || '',
  restaurantName: props.record?.restaurantName || '',
  location: props.record?.location || '',
  latitude: props.record?.latitude,
  longitude: props.record?.longitude,
  recordedAt: props.record?.recordedAt || new Date().toISOString().slice(0, 19).replace('T', ' '),
  userRating: props.record?.userRating,
  tags: props.record?.tags || []
})

const commonTags = ['辣', '清淡', '素食', '高蛋白', '低碳水', '快餐', '火锅', '烧烤', '日料', '西餐']

const rules: FormRules = {
  mealType: [{ required: true, message: '请选择用餐类型', trigger: 'change' }],
  foodName: [{ required: true, message: '请输入食物名称', trigger: 'blur' }],
  recordedAt: [{ required: true, message: '请选择用餐时间', trigger: 'change' }]
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate((valid) => {
    if (valid) {
      emit('submit', formData)
    }
  })
}

const handleCancel = () => {
  emit('cancel')
}

defineExpose({
  reset: () => formRef.value?.resetFields()
})
</script>
