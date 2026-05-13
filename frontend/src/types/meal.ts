export type MealType = 'BREAKFAST' | 'LUNCH' | 'DINNER' | 'SNACK'

export interface MealRecord {
  id: number
  userId: number
  mealType: MealType
  foodName: string
  restaurantName?: string
  location?: string
  latitude?: number
  longitude?: number
  recordedAt: string
  userRating?: number
  tags?: string
  createdAt: string
  imageUrl?: string
  source?: string
}

export interface MealRecordRequest {
  mealType: MealType
  foodName: string
  restaurantName?: string
  location?: string
  latitude?: number
  longitude?: number
  recordedAt: string
  userRating?: number
  tags?: string
  imageUrl?: string
  source?: string
}

export interface MealFilters {
  startDate?: string
  endDate?: string
  mealType?: MealType
}

export interface MealStats {
  totalRecords: number
  totalCalories: number
  avgCalories: number
  byType: Record<MealType, number>
}

export interface FoodRecognitionResponse {
  foodName: string
  calories: number
  confidence: number
  mealRecordId?: number
  imageUrl?: string
  message: string
}
