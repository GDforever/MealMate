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
  photoUrl?: string
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
  page?: number
  size?: number
}

export interface Location {
  latitude: number
  longitude: number
  address?: string
}
