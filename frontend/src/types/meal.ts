export type MealType = 'BREAKFAST' | 'LUNCH' | 'DINNER' | 'SNACK'

export interface MealRecord {
  id: number
  userId: number
  date: string
  mealType: MealType
  restaurantId?: number
  restaurantName?: string
  dishes: string
  calories?: number
  notes?: string
  createdAt: string
  updatedAt: string
}

export interface MealRecordRequest {
  date: string
  mealType: MealType
  restaurantId?: number
  dishes: string
  calories?: number
  notes?: string
}

export interface MealFilters {
  startDate?: string
  endDate?: string
  mealType?: MealType
  restaurantId?: number
}

export interface MealStats {
  totalRecords: number
  totalCalories: number
  avgCalories: number
  byType: Record<MealType, number>
}
