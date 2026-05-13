import { computed } from 'vue'
import { useChatStore } from '@/stores/chat'
import { useSSE } from './useSSE'
import { chatApi } from '@/api/chat'
import { useRestaurantStore } from '@/stores/restaurant'

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

  const sendMessage = async (message: string, image?: File) => {
    chatStore.addUserMessage(message, image ? URL.createObjectURL(image) : undefined)
    chatStore.setLoading(true)

    // Try to get user location
    let location: { latitude: number; longitude: number } | undefined
    try {
      const restaurantStore = useRestaurantStore()
      if (restaurantStore.userLocation) {
        location = restaurantStore.userLocation
      } else {
        location = await restaurantStore.getCurrentLocation()
      }
    } catch {
      // Location not available, continue without it
    }

    const responsePromise = Promise.resolve(
      chatApi.sendMessage(message, chatStore.currentSessionId, location, image || null)
    )

    // Fire-and-forget: connect manages its own lifecycle via callbacks.
    // Do NOT await — if the SSE stream never closes, await would block forever.
    connect(responsePromise, {
      onSession: (sessionId) => {
        chatStore.migrateMessages(sessionId)
      },
      onMessage: (delta) => {
        chatStore.setStreamingContent(delta)
      },
      onOptions: (items) => {
        chatStore.setMessageOptions(items)
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
