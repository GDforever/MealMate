import { computed } from 'vue'
import { useChatStore } from '@/stores/chat'
import { useSSE } from './useSSE'
import { chatApi } from '@/api/chat'

export function useChat() {
  const chatStore = useChatStore()

  const sessions = computed(() => chatStore.sessions)
  const currentSession = computed(() => chatStore.currentSession)
  const currentMessages = computed(() => chatStore.currentMessages)
  const isLoading = computed(() => chatStore.isLoading)
  const streamingContent = computed(() => chatStore.streamingContent)

  const { isConnected, connect, disconnect } = useSSE()

  const loadSessions = async () => {
    await chatStore.fetchSessions()
  }

  const createNewSession = async () => {
    await chatStore.createSession()
  }

  const selectSession = async (sessionId: number) => {
    chatStore.setCurrentSession(sessionId)
  }

  const sendMessage = async (message: string) => {
    const responsePromise = chatApi.sendMessage(message, chatStore.currentSessionId)

    await connect(responsePromise, {
      onOpen: () => {
        chatStore.setLoading(true)
      },
      onMessage: (delta) => {
        chatStore.setStreamingContent(delta)
      },
      onDone: async (data) => {
        chatStore.finalizeMessage(data.sessionId)
        chatStore.setLoading(false)
        await chatStore.fetchSessions()
      },
      onError: (error) => {
        console.error('SSE error:', error)
        chatStore.setLoading(false)
      }
    })
  }

  const deleteSession = async (sessionId: number) => {
    await chatStore.deleteSession(sessionId)
  }

  return {
    sessions,
    currentSession,
    currentMessages,
    isLoading,
    streamingContent,
    isConnected,
    loadSessions,
    createNewSession,
    selectSession,
    sendMessage,
    deleteSession
  }
}
