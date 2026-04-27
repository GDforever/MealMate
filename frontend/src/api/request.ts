import axios, { type AxiosError, type AxiosRequestConfig, type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from './types'

// Create axios instance with custom response type that unwraps ApiResponse
function createApiInstance(): AxiosInstance {
  const instance = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
    timeout: 30000,
    headers: {
      'Content-Type': 'application/json'
    }
  })

  // Request interceptor - Add token
  instance.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
      const token = localStorage.getItem('token')
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }
      return config
    },
    (error) => Promise.reject(error)
  )

  // Response interceptor - Unified error handling, unwraps ApiResponse
  instance.interceptors.response.use(
    (response) => {
      const apiResponse = response.data as ApiResponse
      const { code, message, data } = apiResponse

      if (code === 200) {
        return data as any
      } else {
        ElMessage.error(message || '请求失败')
        return Promise.reject(new Error(message))
      }
    },
    (error: AxiosError<any>) => {
      if (error.response) {
        switch (error.response.status) {
          case 401:
            ElMessage.error('登录已过期，请重新登录')
            localStorage.removeItem('token')
            window.location.href = '/login'
            break
          case 403:
            ElMessage.error('没有权限访问')
            break
          case 404:
            ElMessage.error('请求的资源不存在')
            break
          case 500:
            ElMessage.error('服务器错误，请稍后重试')
            break
          default:
            ElMessage.error(error.response.data?.message || '请求失败')
        }
      } else if (error.request) {
        ElMessage.error('网络连接失败，请检查网络')
      } else {
        ElMessage.error('请求配置错误')
      }
      return Promise.reject(error)
    }
  )

  return instance
}

const request = createApiInstance()

// Override the request methods to return unwrapped data type
type UnwrappedAxiosRequestConfig = AxiosRequestConfig & {
  returnType?: 'data'
}

export default request

export type { AxiosRequestConfig }
