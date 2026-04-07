import axios from 'axios'

const api = axios.create({ baseURL: '/api/lottery', timeout: 30000 })

function withParams(params) {
  return Object.fromEntries(Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== ''))
}

export default {
  // 状态
  status: () => api.get('/status'),
  // 查询
  results: (type, limit = 20, offset = 0) => api.get('/results', { params: { type, limit, offset } }),
  latest: (type) => api.get('/latest', { params: { type } }),
  // 拉取真实数据
  fetchAll: (scope = 'latest-1', count) => api.post('/fetch', null, { params: withParams({ scope, count }), timeout: 0 }),
  fetchOne: (type, scope = 'latest-1', count) => api.post(`/fetch/${type}`, null, { params: withParams({ scope, count }), timeout: 0 }),
  fetchTask: (taskId) => api.get(`/fetch/tasks/${taskId}`),
  fetchHistory: (params = {}) => api.get('/fetch/history', { params: withParams(params) }),
  fetchHistoryDetail: (taskId) => api.get(`/fetch/history/${taskId}`),
  // 分析
  analyze: (type) => api.get('/analyze', { params: { type } }),
  analyzeAll: () => api.get('/analyze'),
  trend: (type, n = 30) => api.get('/trend', { params: { type, n } }),
  recommend: (type) => api.get('/recommend', { params: { type } }),
}
