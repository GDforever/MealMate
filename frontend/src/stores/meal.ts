import { defineStore } from 'pinia'
import { ref } from 'vue'
import { mealApi } from '@/api/meal'
import type { MealRecord, MealRecordRequest, MealFilters } from '@/types'

export const useMealStore = defineStore('meal', () => {
  const records = ref<MealRecord[]>([])
  const total = ref(0)
  const currentPage = ref(0)
  const pageSize = ref(20)
  const filters = ref<MealFilters>({})

  const fetchRecords = async (page = 0) => {
    const response = await mealApi.getRecords({
      page,
      size: pageSize.value,
      ...filters.value
    })

    records.value = response.content
    total.value = response.totalElements
    currentPage.value = response.number
  }

  const createRecord = async (data: MealRecordRequest) => {
    const record = await mealApi.createRecord(data)
    records.value.unshift(record)
    total.value++
    return record
  }

  const updateRecord = async (id: number, data: MealRecordRequest) => {
    const record = await mealApi.updateRecord(id, data)
    const index = records.value.findIndex((r) => r.id === id)
    if (index !== -1) {
      records.value[index] = record
    }
    return record
  }

  const deleteRecord = async (id: number) => {
    await mealApi.deleteRecord(id)
    records.value = records.value.filter((r) => r.id !== id)
    total.value--
  }

  const setFilters = (newFilters: Partial<MealFilters>) => {
    filters.value = { ...filters.value, ...newFilters }
  }

  const clearFilters = () => {
    filters.value = {}
  }

  return {
    records,
    total,
    currentPage,
    pageSize,
    filters,
    fetchRecords,
    createRecord,
    updateRecord,
    deleteRecord,
    setFilters,
    clearFilters
  }
})
