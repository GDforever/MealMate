export interface ChatSession {
  id: number
  title: string
  createdAt: string
  updatedAt: string
}

export interface ChatMessage {
  id: number
  sessionId: number
  role: 'USER' | 'ASSISTANT' | 'SYSTEM'
  content: string
  createdAt: string
}

export interface ChatRequest {
  message: string
  sessionId: number
}

export interface ChatSessionDto {
  id: number
  title: string
  createdAt: string
  updatedAt: string
  messageCount?: number
}

export interface ChatMessageDto {
  id: number
  sessionId: number
  role: 'USER' | 'ASSISTANT' | 'SYSTEM'
  content: string
  createdAt: string
}

export interface SSEMessage {
  type: 'message' | 'done' | 'error'
  content?: string
  error?: string
}
