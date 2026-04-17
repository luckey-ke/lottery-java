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
const systemApi = axios.create({ baseURL: '/api/system', timeout: 15000 })
const authApi = axios.create({ baseURL: '/api/auth', timeout: 15000 })

// 懒加载全局状态，避免循环依赖
function getGlobal() {
  return import('../composables/useGlobal').then(m => m.useGlobal())
}

// 请求拦截器 — 附带 JWT Token + Loading
const requestInterceptor = (config: any) => {
  const token = localStorage.getItem('lottery_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  if (config.timeout !== 0) {
    getGlobal().then(g => g.startLoading())
  }
  return config
}
api.interceptors.request.use(requestInterceptor)
systemApi.interceptors.request.use(requestInterceptor)
authApi.interceptors.request.use(requestInterceptor)

// ===== Refresh Token 自动续期 =====
let refreshPromise: Promise<string | null> | null = null

async function tryRefreshToken(): Promise<string | null> {
  const rt = localStorage.getItem('lottery_refresh_token')
  if (!rt) return null
  try {
    const { data } = await authApi.post('/refresh', { refreshToken: rt })
    localStorage.setItem('lottery_token', data.token)
    localStorage.setItem('lottery_refresh_token', data.refreshToken)
    return data.token
  } catch {
    return null
  }
}

function getRefreshPromise(): Promise<string | null> {
  if (!refreshPromise) {
    refreshPromise = tryRefreshToken().finally(() => { refreshPromise = null })
  }
  return refreshPromise
}

function clearAndNotify() {
  localStorage.removeItem('lottery_token')
  localStorage.removeItem('lottery_refresh_token')
  localStorage.removeItem('lottery_user')
  getGlobal().then(g => g.showToast('登录已过期，请重新登录', 'error'))
}

// 响应拦截器 — 401 自动续期 + 错误 toast
const responseInterceptorSuccess = (response: any) => {
    if (response.config.timeout !== 0) {
      getGlobal().then(g => g.stopLoading())
    }
    return response
  }
const responseInterceptorError = async (error: any) => {
    if (error.config?.timeout !== 0) {
      getGlobal().then(g => g.stopLoading())
    }

    const status = error.response?.status
    const originalConfig = error.config

    // 401：尝试自动刷新 token 后重试
    if (status === 401 && !originalConfig._retry) {
      // 跳过登录/注册/刷新接口
      const url: string = originalConfig.url || ''
      if (url.includes('/login') || url.includes('/register') || url.includes('/refresh')) {
        clearAndNotify()
        return Promise.reject(error)
      }

      originalConfig._retry = true
      const newToken = await getRefreshPromise()
      if (newToken) {
        originalConfig.headers.Authorization = `Bearer ${newToken}`
        // 重新发请求（api 或 systemApi 都行，axios 实例会走各自的 baseURL）
        return axios(originalConfig)
      }
      clearAndNotify()
      return Promise.reject(error)
    }

    const data = error.response?.data
    const message = data?.error || data?.message || error.message

    if (status === 403) {
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
api.interceptors.response.use(responseInterceptorSuccess, responseInterceptorError)
systemApi.interceptors.response.use(responseInterceptorSuccess, responseInterceptorError)
authApi.interceptors.response.use(responseInterceptorSuccess, responseInterceptorError)

function withParams(params: Record<string, unknown>): Record<string, unknown> {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== '')
  )
}

// ========== 认证 API ==========

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
  updateProfile: (body: Record<string, unknown>) =>
    authApi.put('/profile', body, {
      headers: { Authorization: `Bearer ${localStorage.getItem('lottery_token')}` }
    }),
  changePassword: (body: Record<string, unknown>) =>
    authApi.put('/password', body, {
      headers: { Authorization: `Bearer ${localStorage.getItem('lottery_token')}` }
    }),
  getMenusByLocation: (location: string) =>
    authApi.get(`/menus/${location}`),

  // ===== 管理员 - 用户 =====
  listUsers: (limit = 20, offset = 0): Promise<AxiosResponse<{ data: any[]; total: number }>> =>
    systemApi.get('/users', { params: { limit, offset } }),
  addUser: (body: Record<string, unknown>) =>
    systemApi.post('/users', body),
  updateUser: (id: number, body: Record<string, unknown>) =>
    systemApi.put(`/users/${id}`, body),
  deleteUser: (id: number) =>
    systemApi.delete(`/users/${id}`),

  // ===== 管理员 - 角色 =====
  listRoles: (): Promise<AxiosResponse<{ data: Array<{ roleId: number; roleName: string; roleKey: string; menuIds: number[] }> }>> =>
    systemApi.get('/roles'),
  addRole: (body: Record<string, unknown>) =>
    systemApi.post('/roles', body),
  updateRole: (id: number, body: Record<string, unknown>) =>
    systemApi.put(`/roles/${id}`, body),
  deleteRole: (id: number) =>
    systemApi.delete(`/roles/${id}`),
  getRoleMenus: (roleId: number) =>
    systemApi.get(`/menus/role/${roleId}`),

  // ===== 管理员 - 菜单 =====
  listMenus: (params: Record<string, unknown> = {}): Promise<AxiosResponse<{ data: any[] }>> =>
    systemApi.get('/menus', { params: withParams(params) }),
  addMenu: (body: Record<string, unknown>) =>
    systemApi.post('/menus', body),
  updateMenu: (id: number, body: Record<string, unknown>) =>
    systemApi.put(`/menus/${id}`, body),
  deleteMenu: (id: number) =>
    systemApi.delete(`/menus/${id}`),

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
