import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { chatApi } from '@/api/chat'
import type { ChatSession, ChatMessage } from '@/types'

export const useChatStore = defineStore('chat', () => {
  const sessions = ref<ChatSession[]>([])
  const currentSessionId = ref<number | null>(null)
  const messages = ref<Map<number, ChatMessage[]>>(new Map())
  const isLoading = ref(false)
  const streamingContent = ref('')

  const currentSession = computed(() =>
    sessions.value.find((s) => s.id === currentSessionId.value) || null
  )

  const currentMessages = computed(() =>
    messages.value.get(currentSessionId.value || -1) || []
  )

  const fetchSessions = async () => {
    const data = await chatApi.getSessions()
    sessions.value = data

    if (data.length > 0 && !currentSessionId.value) {
      setCurrentSession(data[0].id)
    }
  }

  const createSession = async () => {
    // New session will be created when sending first message
    currentSessionId.value = null
    messages.value.set(-1, [])
  }

  const setCurrentSession = (sessionId: number) => {
    currentSessionId.value = sessionId
  }

  const fetchSessionMessages = async (sessionId: number) => {
    const data = await chatApi.getSessionDetail(sessionId)
    messages.value.set(sessionId, data.messages || [])
  }

  const addUserMessage = (content: string) => {
    const msg: ChatMessage = {
      id: Date.now(),
      sessionId: currentSessionId.value || -1,
      role: 'USER',
      content,
      createdAt: new Date().toISOString()
    }

    const sessionMessages = messages.value.get(currentSessionId.value || -1) || []
    messages.value.set(currentSessionId.value || -1, [...sessionMessages, msg])
  }

  const appendAssistantMessage = (delta: string) => {
    const sessionMessages = messages.value.get(currentSessionId.value || -1) || []
    const lastMessage = sessionMessages[sessionMessages.length - 1]

    if (lastMessage && lastMessage.role === 'ASSISTANT' && !lastMessage.createdAt) {
      // Update streaming message
      lastMessage.content += delta
    } else {
      // Create new streaming message
      const newMessage: ChatMessage = {
        id: Date.now(),
        sessionId: currentSessionId.value || -1,
        role: 'ASSISTANT',
        content: delta,
        createdAt: ''
      }
      messages.value.set(currentSessionId.value || -1, [...sessionMessages, newMessage])
    }

    streamingContent.value = (streamingContent.value || '') + delta
  }

  const finalizeMessage = (sessionId: number) => {
    const sessionMessages = messages.value.get(sessionId) || []
    const lastMessage = sessionMessages[sessionMessages.length - 1]

    if (lastMessage) {
      lastMessage.createdAt = new Date().toISOString()
    }

    currentSessionId.value = sessionId
    streamingContent.value = ''
  }

  const setLoading = (loading: boolean) => {
    isLoading.value = loading
  }

  const setStreamingContent = (delta: string) => {
    appendAssistantMessage(delta)
  }

  const setMessageOptions = (items: string[]) => {
    const sessionMessages = messages.value.get(currentSessionId.value || -1) || []
    const lastMessage = sessionMessages[sessionMessages.length - 1]
    if (lastMessage && lastMessage.role === 'ASSISTANT') {
      lastMessage.options = items
      messages.value.set(currentSessionId.value || -1, [...sessionMessages])
    }
  }

  const deleteSession = async (sessionId: number) => {
    await chatApi.deleteSession(sessionId)
    sessions.value = sessions.value.filter((s) => s.id !== sessionId)
    messages.value.delete(sessionId)

    if (currentSessionId.value === sessionId) {
      currentSessionId.value = sessions.value[0]?.id || null
    }
  }

  const migrateMessages = (newSessionId: number) => {
    const tempMessages = messages.value.get(-1)
    if (tempMessages && tempMessages.length > 0) {
      messages.value.delete(-1)
      messages.value.set(newSessionId, tempMessages)
    }
    currentSessionId.value = newSessionId
  }

  return {
    sessions,
    currentSessionId,
    currentSession,
    currentMessages,
    messages,
    isLoading,
    streamingContent,
    fetchSessions,
    createSession,
    setCurrentSession,
    fetchSessionMessages,
    addUserMessage,
    appendAssistantMessage,
    finalizeMessage,
    setLoading,
    setStreamingContent,
    setMessageOptions,
    deleteSession,
    migrateMessages
  }
})
