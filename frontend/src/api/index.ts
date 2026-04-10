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

// 请求拦截器 — 附带 JWT Token + Loading
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('lottery_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  if (config.timeout !== 0) {
    getGlobal().then(g => g.startLoading())
  }
  return config
})

// 响应拦截器 — 错误处理 + 401 自动清登录状态
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
      // Token 过期或无效，清除登录状态
      localStorage.removeItem('lottery_token')
      localStorage.removeItem('lottery_refresh_token')
      localStorage.removeItem('lottery_user')
      getGlobal().then(g => g.showToast('登录已过期，请重新登录', 'error'))
    } else if (status === 403) {
      getGlobal().then(g => g.showToast('权限不足，需要管理员权限', 'error'))
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

// ========== 认证 API ==========
const authApi = axios.create({ baseURL: '/api/auth', timeout: 15000 })

export default {
  // ===== 认证 =====
  login: (username: string, password: string) =>
    authApi.post('/login', { username, password }),
  register: (username: string, password: string, nickname?: string, inviteCode?: string) =>
    authApi.post('/register', withParams({ username, password, nickname, inviteCode })),
  refreshToken: (refreshToken: string) =>
    authApi.post('/refresh', { refreshToken }),
  me: () => authApi.get('/me', {
    headers: { Authorization: `Bearer ${localStorage.getItem('lottery_token')}` }
  }),
  authConfig: () => authApi.get('/config'),

  // ===== 管理员 =====
  listUsers: (limit = 20, offset = 0): Promise<AxiosResponse<{ data: any[]; total: number }>> =>
    api.get('/auth/users', { params: { limit, offset } }),
  updateUserRole: (id: number, role: string) =>
    api.put(`/auth/users/${id}/role`, { role }),

  // ===== 状态 =====
  status: (): Promise<AxiosResponse<LotteryStatus>> => api.get('/status'),

  // ===== 查询 =====
  results: (type: string, limit = 20, offset = 0): Promise<AxiosResponse<{ data: LotteryResult[]; total: number }>> =>
    api.get('/results', { params: { type, limit, offset } }),
  latest: (type: string): Promise<AxiosResponse<{ type: string; latestDrawNum: string }>> =>
    api.get('/latest', { params: { type } }),

  // ===== 拉取（需要 ADMIN） =====
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

  // ===== 分析 =====
  analyze: (type: string): Promise<AxiosResponse<AnalysisData>> =>
    api.get('/analyze', { params: { type } }),
  analyzeAll: (): Promise<AxiosResponse<Record<string, AnalysisData>>> =>
    api.get('/analyze'),
  trend: (type: string, n = 30): Promise<AxiosResponse<TrendData>> =>
    api.get('/trend', { params: { type, n } }),

  // ===== 推荐 =====
  recommend: (type: string): Promise<AxiosResponse<RecommendData>> =>
    api.get('/recommend', { params: { type } }),
  recommendHistory: (type: string, limit = 20, offset = 0): Promise<AxiosResponse<RecommendHistoryResponse>> =>
    api.get('/recommend/history', { params: { type, limit, offset } }),
  recommendStats: (type: string): Promise<AxiosResponse<RecommendStatsResponse>> =>
    api.get('/recommend/stats', { params: { type } }),
}
