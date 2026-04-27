import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'
import type { User, LoginRequest, RegisterRequest } from '@/types'

export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const userInfo = ref<User | null>(null)
  const tastePreferences = ref<string[]>([])

  const isAuthenticated = computed(() => !!token.value)
  const userId = computed(() => userInfo.value?.id)

  const login = async (credentials: LoginRequest) => {
    const response = await authApi.login(credentials)
    token.value = response.token
    userInfo.value = response.user
    tastePreferences.value = response.user.tastePreferences
    localStorage.setItem('token', response.token)
  }

  const register = async (data: RegisterRequest) => {
    const response = await authApi.register(data)
    token.value = response.token
    userInfo.value = response.user
    tastePreferences.value = response.user.tastePreferences
    localStorage.setItem('token', response.token)
  }

  const fetchUserInfo = async () => {
    if (!token.value) return
    const user = await authApi.getUserInfo()
    userInfo.value = user
    tastePreferences.value = user.tastePreferences
  }

  const updatePreferences = async (preferences: string[]) => {
    const user = await authApi.updatePreferences({ tastePreferences: preferences })
    userInfo.value = user
    tastePreferences.value = user.tastePreferences
  }

  const logout = () => {
    token.value = null
    userInfo.value = null
    tastePreferences.value = []
    localStorage.removeItem('token')
  }

  return {
    token,
    userInfo,
    tastePreferences,
    isAuthenticated,
    userId,
    login,
    register,
    fetchUserInfo,
    updatePreferences,
    logout
  }
})
