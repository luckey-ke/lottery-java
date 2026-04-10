import { ref } from 'vue'
import { useGlobal } from './useGlobal'

/**
 * 视图层通用请求封装：自动 loading + 错误 toast
 */
export function useApi() {
  const { showToast } = useGlobal()

  /** 执行异步请求，失败时显示 toast */
  async function request<T>(fn: () => Promise<T>, errorMsg = '操作失败'): Promise<T | null> {
    try {
      return await fn()
    } catch (e: any) {
      const msg = e?.response?.data?.error || e?.message || errorMsg
      showToast(msg, 'error')
      return null
    }
  }

  /** 带 loading 状态的请求 */
  function useAsyncRequest<T>(defaultValue: T) {
    const data = ref<T>(defaultValue) as any
    const loading = ref(false)
    const error = ref<string | null>(null)

    async function execute(fn: () => Promise<{ data: T }>) {
      loading.value = true
      error.value = null
      try {
        const res = await fn()
        data.value = res.data
      } catch (e: any) {
        error.value = e?.response?.data?.error || e?.message || '加载失败'
        showToast(error.value!, 'error')
      } finally {
        loading.value = false
      }
    }

    return { data, loading, error, execute }
  }

  return { request, useAsyncRequest, showToast }
}
