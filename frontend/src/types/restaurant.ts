export interface Restaurant {
  id: number
  name: string
  address: string
  latitude: number
  longitude: number
  cuisineType?: string
  avgPrice?: number
  rating?: number
  source?: string
  distance?: number
  createdAt: string
  updatedAt: string
}

export interface RestaurantSearchParams {
  keyword?: string
  latitude?: number
  longitude?: number
  radius?: number
  cuisineType?: string
}

export interface Location {
  latitude: number
  longitude: number
  address?: string
}
