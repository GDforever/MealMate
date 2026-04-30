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
  options?: string[]
}

export interface ChatRequest {
  message: string
  sessionId: number
  latitude?: number
  longitude?: number
}

export interface ChatSessionDto {
  id: number
  title: string
  createdAt: string
  updatedAt: string
  messageCount?: number
  messages?: ChatMessage[]
}

export interface ChatMessageDto {
  id: number
  sessionId: number
  role: 'USER' | 'ASSISTANT' | 'SYSTEM'
  content: string
  createdAt: string
}

export interface SSEMessage {
  type: 'text' | 'options' | 'message' | 'done' | 'error'
  content?: string
  delta?: string
  items?: string[]
  error?: string
  sessionId?: number
}
