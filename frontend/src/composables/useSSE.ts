import { ref, onUnmounted, readonly } from 'vue'
import type { Ref } from 'vue'
import type { SSEMessage } from '@/types'

export interface SSEHandlers {
  onOpen?: () => void
  onMessage?: (delta: string) => void
  onDone?: (data: any) => void
  onError?: (error: any) => void
}

export function useSSE() {
  const isConnected = ref(false)
  const controller = ref<AbortController | null>(null)

  const connect = async (responsePromise: Promise<Response>, handlers: SSEHandlers) => {
    try {
      const response = await responsePromise

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      isConnected.value = true
      handlers.onOpen?.()

      const reader = response.body?.getReader()
      const decoder = new TextDecoder()

      if (!reader) {
        throw new Error('Response body is null')
      }

      controller.value = new AbortController()

      while (true) {
        const { done, value } = await reader.read()

        if (done) {
          isConnected.value = false
          break
        }

        const chunk = decoder.decode(value)
        const lines = chunk.split('\n')

        for (const line of lines) {
          if (line.startsWith('data: ')) {
            try {
              const data: SSEMessage = JSON.parse(line.slice(6))

              switch (data.type) {
                case 'message':
                  handlers.onMessage?.(data.delta || '')
                  break
                case 'done':
                  handlers.onDone?.(data)
                  isConnected.value = false
                  break
                case 'error':
                  handlers.onError?.(data.error)
                  isConnected.value = false
                  break
              }
            } catch (e) {
              console.error('SSE parse error:', e)
            }
          }
        }
      }
    } catch (error) {
      isConnected.value = false
      handlers.onError?.(error)
    }
  }

  const disconnect = () => {
    controller.value?.abort()
    controller.value = null
    isConnected.value = false
  }

  onUnmounted(() => {
    disconnect()
  })

  return {
    isConnected: readonly(isConnected) as Ref<boolean>,
    connect,
    disconnect
  }
}
