import request from './request'
import type { ChatRequest, ChatSession, ChatMessage, ChatSessionDto, ChatMessageDto, SSEMessage } from '@/types'

export const chatApi = {
  sendMessage(message: string, sessionId?: number | null, location?: { latitude: number; longitude: number }): Response {
    const token = localStorage.getItem('token')
    const fullUrl = `${request.defaults.baseURL}/chat`

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
