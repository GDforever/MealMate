export interface Restaurant {
  id: number
  name: string
  address: string
  phone?: string
  latitude: number
  longitude: number
  category?: string
  rating?: number
  distance?: number
  createdAt: string
  updatedAt: string
}

export interface RestaurantSearchParams {
  keyword?: string
  latitude?: number
  longitude?: number
  radius?: number
  category?: string
}

export interface Location {
  latitude: number
  longitude: number
  address?: string
}
