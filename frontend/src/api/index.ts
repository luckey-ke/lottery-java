import axios, { type AxiosResponse } from 'axios'
export * from './types'
import type {
  LotteryStatus,
  LotteryResult,
  FetchTaskInfo,
  FetchHistoryResponse,
  FetchHistoryDetail,
  AnalysisData,
  TrendData,
  RecommendData,
  RecommendHistoryResponse,
  RecommendStatsResponse,
} from './types'

const api = axios.create({ baseURL: '/api/lottery', timeout: 30000 })

// 懒加载全局状态，避免循环依赖
function getGlobal() {
  return import('../composables/useGlobal').then(m => m.useGlobal())
}

// 响应拦截器 - 统一错误处理 + 全局 loading + toast
api.interceptors.request.use((config) => {
  if (config.timeout !== 0) {
    getGlobal().then(g => g.startLoading())
  }
  return config
})

api.interceptors.response.use(
  (response) => {
    if (response.config.timeout !== 0) {
      getGlobal().then(g => g.stopLoading())
    }
    return response
  },
  (error) => {
    if (error.config?.timeout !== 0) {
      getGlobal().then(g => g.stopLoading())
    }

    const status = error.response?.status
    const data = error.response?.data
    const message = data?.error || data?.message || error.message

    if (status === 401) {
      getGlobal().then(g => g.showToast('未授权，请检查 Token 配置', 'error'))
    } else if (status === 400) {
      getGlobal().then(g => g.showToast(message || '请求参数错误', 'warning'))
    } else if (status === 404) {
      getGlobal().then(g => g.showToast('资源不存在', 'error'))
    } else if (status === 405) {
      getGlobal().then(g => g.showToast('请求方法不允许', 'error'))
    } else if (status >= 500) {
      getGlobal().then(g => g.showToast(message || '服务器错误', 'error'))
    } else if (error.code === 'ECONNABORTED') {
      getGlobal().then(g => g.showToast('请求超时，请稍后重试', 'error'))
    } else if (!error.response) {
      getGlobal().then(g => g.showToast('网络异常，无法连接服务器', 'error'))
    }

    return Promise.reject(error)
  }
)

function withParams(params: Record<string, unknown>): Record<string, unknown> {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== '')
  )
}

// ========== API 方法 ==========

export default {
  // 状态
  status: (): Promise<AxiosResponse<LotteryStatus>> => api.get('/status'),

  // 查询
  results: (type: string, limit = 20, offset = 0): Promise<AxiosResponse<{ data: LotteryResult[]; total: number }>> =>
    api.get('/results', { params: { type, limit, offset } }),
  latest: (type: string): Promise<AxiosResponse<{ type: string; latestDrawNum: string }>> =>
    api.get('/latest', { params: { type } }),

  // 拉取
  fetchAll: (scope = 'latest-1', count?: number): Promise<AxiosResponse<FetchTaskInfo>> =>
    api.post('/fetch', null, { params: withParams({ scope, count }), timeout: 0 }),
  fetchOne: (type: string, scope = 'latest-1', count?: number): Promise<AxiosResponse<FetchTaskInfo>> =>
    api.post(`/fetch/${type}`, null, { params: withParams({ scope, count }), timeout: 0 }),
  fetchTask: (taskId: string): Promise<AxiosResponse<FetchTaskInfo>> =>
    api.get(`/fetch/tasks/${taskId}`),
  fetchHistory: (params: Record<string, unknown> = {}): Promise<AxiosResponse<FetchHistoryResponse>> =>
    api.get('/fetch/history', { params: withParams(params) }),
  fetchHistoryDetail: (taskId: string): Promise<AxiosResponse<FetchHistoryDetail>> =>
    api.get(`/fetch/history/${taskId}`),

  // 分析
  analyze: (type: string): Promise<AxiosResponse<AnalysisData>> =>
    api.get('/analyze', { params: { type } }),
  analyzeAll: (): Promise<AxiosResponse<Record<string, AnalysisData>>> =>
    api.get('/analyze'),
  trend: (type: string, n = 30): Promise<AxiosResponse<TrendData>> =>
    api.get('/trend', { params: { type, n } }),

  // 推荐
  recommend: (type: string): Promise<AxiosResponse<RecommendData>> =>
    api.get('/recommend', { params: { type } }),
  recommendHistory: (type: string, limit = 20, offset = 0): Promise<AxiosResponse<RecommendHistoryResponse>> =>
    api.get('/recommend/history', { params: { type, limit, offset } }),
  recommendStats: (type: string): Promise<AxiosResponse<RecommendStatsResponse>> =>
    api.get('/recommend/stats', { params: { type } }),
}
