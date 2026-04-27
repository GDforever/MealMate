import request from './request'
import type { Restaurant, RestaurantSearchParams } from '@/types'

export const restaurantApi = {
  searchNearby(params: RestaurantSearchParams) {
    return request.get<Restaurant[]>('/restaurants/nearby', { params })
  },

  getDetail(id: number) {
    return request.get<Restaurant>(`/restaurants/${id}`)
  }
}
