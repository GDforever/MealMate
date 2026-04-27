export interface User {
  id: number
  username: string
  email: string
  nickname?: string
  avatarUrl?: string
  createdAt: string
  updatedAt: string
}

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  email: string
  nickname?: string
}

export interface AuthResponse {
  token: string
  user: User
}

export interface UpdatePreferencesRequest {
  nickname?: string
  avatarUrl?: string
}
