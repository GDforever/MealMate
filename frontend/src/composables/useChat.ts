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
    chatStore.setLoading(false)
    chatStore.streamingContent = ''
    await chatStore.fetchSessions()
  }

  const createNewSession = async () => {
    chatStore.setLoading(false)
    await chatStore.createSession()
  }

  const selectSession = async (sessionId: number) => {
    chatStore.setLoading(false)
    chatStore.setCurrentSession(sessionId)
    await chatStore.fetchSessionMessages(sessionId)
  }

  const sendMessage = async (message: string) => {
    chatStore.addUserMessage(message)
    chatStore.setLoading(true)

    const responsePromise = Promise.resolve(chatApi.sendMessage(message, chatStore.currentSessionId))

    // Fire-and-forget: connect manages its own lifecycle via callbacks.
    // Do NOT await — if the SSE stream never closes, await would block forever.
    connect(responsePromise, {
      onMessage: (delta) => {
        chatStore.setStreamingContent(delta)
      },
      onDone: async (data) => {
        chatStore.finalizeMessage(data?.sessionId || chatStore.currentSessionId || -1)
        chatStore.setLoading(false)
        try {
          await chatStore.fetchSessions()
        } catch (e) {
          console.error('Failed to refresh sessions:', e)
        }
      },
      onError: (error) => {
        console.error('SSE error:', error)
        chatStore.setLoading(false)
      }
    }).catch(() => {
      chatStore.setLoading(false)
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
