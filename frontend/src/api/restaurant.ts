import request from './request'
import type { Restaurant, RestaurantSearchParams, PageResponse } from '@/types'

export const restaurantApi = {
  searchNearby(params: RestaurantSearchParams) {
    return request.get<PageResponse<Restaurant>>('/restaurants/nearby', { params })
  },

  getDetail(id: number) {
    return request.get<Restaurant>(`/restaurants/${id}`)
  }
}
