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

    if (image) {
      const formData = new FormData()
      formData.append('message', message)
      if (sessionId) formData.append('sessionId', String(sessionId))
      if (location) {
        formData.append('latitude', String(location.latitude))
        formData.append('longitude', String(location.longitude))
      }
      formData.append('image', image)

      return fetch(fullUrl, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`
        },
        body: formData
      })
    }

    return fetch(fullUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        message,
        sessionId: sessionId || undefined,
        ...(location ? { latitude: location.latitude, longitude: location.longitude } : {})
      })
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
