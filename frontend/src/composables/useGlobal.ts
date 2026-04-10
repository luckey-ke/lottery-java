import { ref, readonly } from 'vue'

// ===== Toast =====
const message = ref('')
const messageType = ref<'success' | 'error' | 'warning'>('success')
let toastTimer: ReturnType<typeof setTimeout> | null = null

function showToast(text: string, type: 'success' | 'error' | 'warning' = 'success', durationMs = 3000) {
  if (toastTimer) clearTimeout(toastTimer)
  message.value = text
  messageType.value = type
  toastTimer = setTimeout(() => {
    message.value = ''
    toastTimer = null
  }, durationMs)
}

function dismissToast() {
  if (toastTimer) clearTimeout(toastTimer)
  message.value = ''
}

// ===== Loading =====
const isLoading = ref(false)
let loadingDepth = 0

function startLoading() {
  loadingDepth++
  isLoading.value = true
}

function stopLoading() {
  loadingDepth = Math.max(0, loadingDepth - 1)
  if (loadingDepth === 0) isLoading.value = false
}

// ===== Composable =====
export function useGlobal() {
  return {
    // Toast
    message: readonly(message),
    messageType: readonly(messageType),
    showToast,
    dismissToast,

    // Loading
    isLoading: readonly(isLoading),
    startLoading,
    stopLoading,
  }
}
