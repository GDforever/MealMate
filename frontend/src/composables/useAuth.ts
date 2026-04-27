import { computed } from 'vue'
import { useUserStore } from '@/stores/user'

export function useAuth() {
  const userStore = useUserStore()

  const isAuthenticated = computed(() => userStore.isAuthenticated)
  const userInfo = computed(() => userStore.userInfo)
  const token = computed(() => userStore.token)

  const login = async (credentials: { username: string; password: string }) => {
    return await userStore.login(credentials)
  }

  const register = async (data: { username: string; email: string; password: string }) => {
    return await userStore.register(data)
  }

  const logout = () => {
    userStore.logout()
  }

  return {
    isAuthenticated,
    userInfo,
    token,
    login,
    register,
    logout
  }
}
