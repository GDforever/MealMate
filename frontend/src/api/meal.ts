import request from './request'
import type { MealRecord, MealRecordRequest, MealFilters, PageResponse } from '@/types'

export const mealApi = {
  getRecords(params: { page: number; size: number; startDate?: string; endDate?: string; mealType?: string }) {
    return request.get<PageResponse<MealRecord>>('/meal-records', { params })
  },

  getRecord(id: number) {
    return request.get<MealRecord>(`/meal-records/${id}`)
  },

  createRecord(data: MealRecordRequest) {
    return request.post<MealRecord>('/meal-records', data)
  },

  updateRecord(id: number, data: MealRecordRequest) {
    return request.put<MealRecord>(`/meal-records/${id}`, data)
  },

  deleteRecord(id: number) {
    return request.delete(`/meal-records/${id}`)
  }
}
