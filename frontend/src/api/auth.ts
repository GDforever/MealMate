import request from './request'
import type { LoginRequest, RegisterRequest, AuthResponse, User, UpdatePreferencesRequest } from '@/types'

export const authApi = {
  login(data: LoginRequest) {
    return request.post<AuthResponse>('/auth/login', data)
  },

  register(data: RegisterRequest) {
    return request.post<AuthResponse>('/auth/register', data)
  },

  getUserInfo() {
    return request.get<User>('/user/me')
  },

  updatePreferences(data: UpdatePreferencesRequest) {
    return request.put<User>('/user/preferences', data)
  }
}
