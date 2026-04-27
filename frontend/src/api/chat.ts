import request from './request'
import type { ChatRequest, ChatSession, ChatMessage, ChatSessionDto, ChatMessageDto, SSEMessage } from '@/types'

export const chatApi = {
  sendMessage(message: string, sessionId?: number): Response {
    const token = localStorage.getItem('token')
    const url = sessionId ? `/chat?sessionId=${sessionId}` : '/chat'
    const fullUrl = `${request.defaults.baseURL}${url}`

    return fetch(fullUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({ message })
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
