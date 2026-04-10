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

// 响应拦截器 - 统一错误处理
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    const message = error.response?.data?.error || error.response?.data?.message || error.message

    if (status === 401) {
      console.error('未授权，请检查 Token 配置')
    } else if (status === 400) {
      console.warn('请求参数错误:', message)
    } else if (status >= 500) {
      console.error('服务器错误:', message)
    } else if (error.code === 'ECONNABORTED') {
      console.error('请求超时')
    } else if (!error.response) {
      console.error('网络异常，无法连接服务器')
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
