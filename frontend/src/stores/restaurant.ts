import { defineStore } from 'pinia'
import { ref } from 'vue'
import { restaurantApi } from '@/api/restaurant'
import type { Restaurant, RestaurantSearchParams, Location } from '@/types'

export const useRestaurantStore = defineStore('restaurant', () => {
  const list = ref<Restaurant[]>([])
  const totalElements = ref(0)
  const totalPages = ref(0)
  const currentPage = ref(0)
  const pageSize = ref(12)
  const searchParams = ref<RestaurantSearchParams>({
    latitude: 39.9042,
    longitude: 116.4074,
    radius: 3000
  })
  const selectedRestaurant = ref<Restaurant | null>(null)
  const userLocation = ref<Location | null>(null)

  const searchNearby = async (params?: Partial<RestaurantSearchParams>) => {
    const finalParams = {
      ...searchParams.value,
      ...params,
      page: currentPage.value,
      size: pageSize.value
    }
    searchParams.value = finalParams
    const page = await restaurantApi.searchNearby(finalParams)
    list.value = page.content || []
    totalElements.value = page.totalElements
    totalPages.value = page.totalPages
  }

  const fetchDetail = async (id: number) => {
    selectedRestaurant.value = await restaurantApi.getDetail(id)
  }

  const getCurrentLocation = (): Promise<Location> => {
    return new Promise((resolve, reject) => {
      if (!navigator.geolocation) {
        reject(new Error('Geolocation not supported'))
        return
      }

      navigator.geolocation.getCurrentPosition(
        (position) => {
          const location: Location = {
            latitude: position.coords.latitude,
            longitude: position.coords.longitude
          }
          setUserLocation(location)
          resolve(location)
        },
        (error) => {
          reject(error)
        }
      )
    })
  }

  const setUserLocation = (location: Location) => {
    userLocation.value = location
    searchParams.value.latitude = location.latitude
    searchParams.value.longitude = location.longitude
  }

  return {
    list,
    totalElements,
    totalPages,
    currentPage,
    pageSize,
    searchParams,
    selectedRestaurant,
    userLocation,
    searchNearby,
    fetchDetail,
    getCurrentLocation,
    setUserLocation
  }
})
