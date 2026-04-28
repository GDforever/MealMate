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

      let buffer = ''
      let idleTimer: ReturnType<typeof setTimeout> | null = null
      let completed = false

      const finish = () => {
        if (completed) return
        completed = true
        if (buffer.trim()) {
          processSSEBuffer(buffer + '\n\n', handlers)
        }
        isConnected.value = false
        handlers.onDone?.({})
      }

      const resetIdleTimer = () => {
        if (idleTimer) clearTimeout(idleTimer)
        // If no data received for 15s, assume the stream has ended
        idleTimer = setTimeout(() => {
          reader.cancel().catch(() => {})
          finish()
        }, 15000)
      }

      resetIdleTimer()

      try {
        while (true) {
          const { done, value } = await reader.read()

          if (done) {
            finish()
            break
          }

          resetIdleTimer()
          // stream: true preserves incomplete UTF-8 sequences across chunks
          buffer += decoder.decode(value, { stream: true })
          buffer = processSSEBuffer(buffer, handlers)
        }
      } finally {
        if (idleTimer) clearTimeout(idleTimer)
      }
    } catch (error) {
      if (!completed) {
        isConnected.value = false
        handlers.onError?.(error)
      }
    }
  }

  /**
   * Process SSE buffer, handling events separated by blank lines (\n\n).
   * Returns the unprocessed remainder of the buffer.
   */
  function processSSEBuffer(buffer: string, handlers: SSEHandlers): string {
    // Normalize \r\n to \n for consistent parsing
    const normalized = buffer.replace(/\r\n/g, '\n')
    // SSE events are separated by \n\n
    const events = normalized.split('\n\n')

    // Last element is potentially incomplete — keep it in buffer
    const remainder = events.pop() || ''

    for (const event of events) {
      const lines = event.split('\n')
      for (const line of lines) {
        if (line.startsWith('data:')) {
          const dataStr = line.slice(5).trim()
          if (!dataStr) continue

          try {
            const data: SSEMessage = JSON.parse(dataStr)

            switch (data.type) {
              case 'message':
                handlers.onMessage?.(data.delta || '')
                break
              case 'done':
                handlers.onDone?.(data)
                isConnected.value = false
                return ''
              case 'error':
                handlers.onError?.(data.error)
                isConnected.value = false
                return ''
            }
          } catch {
            // Plain text SSE from backend — treat as content delta
            handlers.onMessage?.(dataStr)
          }
        }
      }
    }

    return remainder
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
