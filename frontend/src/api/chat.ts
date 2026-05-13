import request from './request'
import type { ChatSessionDto } from '@/types'

export const chatApi = {
  sendMessage(
    message: string,
    sessionId?: number | null,
    location?: { latitude: number; longitude: number },
    image?: File | null
  ): Response {
    const token = localStorage.getItem('token')
    const fullUrl = `${request.defaults.baseURL}/chat`

    const formData = new FormData()
    formData.append('message', message)
    if (sessionId) formData.append('sessionId', String(sessionId))
    if (location) {
      formData.append('latitude', String(location.latitude))
      formData.append('longitude', String(location.longitude))
    }
    if (image) {
      formData.append('image', image)
    }

    return fetch(fullUrl, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`
      },
      body: formData
    })
  },

  getSessions() {
    return request.get<ChatSessionDto[]>('/chat/sessions')
  },

  getSessionDetail(id: number) {
    return request.get<ChatSessionDto>(`/chat/sessions/${id}`)
  },

  deleteSession(id: number) {
    return request.delete(`/chat/sessions/${id}`)
  }
}
