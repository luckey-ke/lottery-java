<template>
  <div class="admin-page">
    <!-- 页面标题 -->
    <div class="page-hero">
      <div>
        <h1 class="page-title">管理后台</h1>
        <p class="page-subtitle">拉取真实数据、查看执行进度与汇总统计</p>
      </div>
    </div>

    <!-- 操作栏 -->
    <div class="action-bar">
      <div class="action-group">
        <select v-model="selectedFetchType" :disabled="loading" class="select">
          <option v-for="o in fetchTypeOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
        </select>
        <select v-model="selectedScope" :disabled="loading" class="select">
          <option v-for="o in fetchScopeOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
        </select>
        <button class="btn-primary" @click="startFetch" :disabled="loading">
          <span v-if="loading" class="spinner"></span>
          {{ loading ? '拉取中...' : '🚀 开始拉取' }}
        </button>
        <button class="btn-ghost" @click="refreshAll" :disabled="loading">🔄 刷新</button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card accent">
        <div class="stat-icon">📦</div>
        <div class="stat-info">
          <span class="stat-value">{{ totalCount }}</span>
          <span class="stat-label">全库总量</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">📡</div>
        <div class="stat-info">
          <span class="stat-value">{{ taskInfo.totalFetched || 0 }}</span>
          <span class="stat-label">本次抓取</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">✨</div>
        <div class="stat-info">
          <span class="stat-value">{{ taskInfo.inserted || 0 }}</span>
          <span class="stat-label">本次新增</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">🔄</div>
        <div class="stat-info">
          <span class="stat-value">{{ taskInfo.updated || 0 }}</span>
          <span class="stat-label">本次更新</span>
        </div>
      </div>
    </div>

    <!-- 抓取进度 -->
    <div class="panel">
      <div class="panel-header">
        <h3>抓取进度</h3>
        <span :class="['status-pill', taskInfo.status || 'idle']">{{ statusLabel }}</span>
      </div>
      <div class="progress-grid">
        <div class="progress-item">
          <span class="progress-label">任务 ID</span>
          <span class="progress-value mono">{{ taskInfo.taskId || '—' }}</span>
        </div>
        <div class="progress-item">
          <span class="progress-label">当前彩种</span>
          <span class="progress-value">{{ currentTypeLabel }}</span>
        </div>
        <div class="progress-item">
          <span class="progress-label">当前页</span>
          <span class="progress-value">{{ taskInfo.currentPage || 0 }}</span>
        </div>
        <div class="progress-item">
          <span class="progress-label">完成进度</span>
          <span class="progress-value">{{ completedTypesText }}</span>
        </div>
      </div>
      <Transition name="fade">
        <div class="alert success" v-if="message">
          <span>✅</span> {{ message }}
          <button class="alert-close" @click="message = ''">×</button>
        </div>
      </Transition>
      <Transition name="fade">
        <div class="alert error" v-if="errorMessage">
          <span>❌</span> {{ errorMessage }}
          <button class="alert-close" @click="errorMessage = ''">×</button>
        </div>
      </Transition>
    </div>

    <!-- 分类统计 -->
    <div class="panel" v-if="typeStats.length">
      <div class="panel-header">
        <h3>分类统计</h3>
      </div>
      <div class="type-grid">
        <div class="type-card" v-for="item in typeStats" :key="item.code">
          <span class="type-icon">{{ typeIcons[item.code] || '🎱' }}</span>
          <div class="type-info">
            <span class="type-name">{{ item.name }}</span>
            <span class="type-count">{{ item.count }}</span>
          </div>
          <span class="type-latest">{{ item.latestDraw || '—' }}</span>
        </div>
      </div>
    </div>

    <!-- 最近记录 -->
    <div class="panel" v-if="recentHistory.length">
      <div class="panel-header">
        <h3>最近执行记录</h3>
        <router-link to="/admin/history" class="link">查看全部 →</router-link>
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>任务 ID</th>
              <th>来源</th>
              <th>彩种</th>
              <th>状态</th>
              <th>开始时间</th>
              <th>抓取</th>
              <th>新增</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in recentHistory" :key="item.taskId">
              <td class="mono">{{ item.taskId?.slice(0, 12) }}…</td>
              <td>
                <span :class="['source-tag', item.triggerSource]">{{ triggerSourceLabel(item.triggerSource) }}</span>
              </td>
              <td>{{ typeLabel(item.type) }}</td>
              <td><span :class="['status-pill', 'sm', item.status]">{{ historyStatusLabel(item.status) }}</span></td>
              <td class="text-muted">{{ item.startedAt || '—' }}</td>
              <td>{{ item.totalFetched || 0 }}</td>
              <td>{{ item.inserted || 0 }}</td>
              <td>
                <router-link :to="`/admin/history?taskId=${item.taskId}`" class="link">详情 →</router-link>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 本次结果 -->
    <div class="panel" v-if="resultRows.length">
      <div class="panel-header">
        <h3>本次结果摘要</h3>
      </div>
      <div class="result-grid">
        <div class="result-card" v-for="item in resultRows" :key="item.type">
          <div class="result-header">
            <span class="result-name">{{ item.name }}</span>
            <span :class="['status-pill', 'sm', item.status]">{{ item.status }}</span>
          </div>
          <div class="result-stats">
            <div class="result-stat">
              <span class="result-stat-value">{{ item.totalFetched || 0 }}</span>
              <span class="result-stat-label">抓取</span>
            </div>
            <div class="result-stat">
              <span class="result-stat-value green">{{ item.inserted || 0 }}</span>
              <span class="result-stat-label">新增</span>
            </div>
            <div class="result-stat">
              <span class="result-stat-value blue">{{ item.updated || 0 }}</span>
              <span class="result-stat-label">更新</span>
            </div>
          </div>
          <div class="result-error" v-if="item.error">{{ item.error }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import api from '../api'

const selectedFetchType = ref('all')
const selectedScope = ref('latest-1')
const loading = ref(false)
const message = ref('')
const errorMessage = ref('')
const status = ref({})
const taskInfo = ref({})
const recentHistory = ref([])
const pollTimer = ref(null)

const typeIcons = { ssq: '🔴🔵', dlt: '🟡🔵', fc3d: '🎲', pl3: '🎯', pl5: '🎯🎯', qlc: '🎱', all: '📦' }

const fetchTypeOptions = [
  { value: 'all', label: '📦 所有彩种' },
  { value: 'ssq', label: '🔴🔵 双色球' },
  { value: 'dlt', label: '🟡🔵 大乐透' },
  { value: 'fc3d', label: '🎲 福彩3D' },
  { value: 'pl3', label: '🎯 排列三' },
  { value: 'pl5', label: '🎯🎯 排列五' },
  { value: 'qlc', label: '🎱 七乐彩' },
]
const fetchScopeOptions = [
  { value: 'latest-1', label: '最新 1 期' },
  { value: 'latest-10', label: '最新 10 期' },
  { value: 'latest-50', label: '最新 50 期' },
  { value: 'latest-100', label: '最新 100 期' },
  { value: 'year-1', label: '最近 1 年' },
  { value: 'year-3', label: '最近 3 年' },
  { value: 'all', label: '过往所有' },
]

const totalCount = computed(() => Object.values(status.value).reduce((s, i) => s + (i.count || 0), 0))
const typeStats = computed(() => Object.entries(status.value).map(([code, info]) => ({ code, ...info })))
const statusLabel = computed(() => {
  const m = { pending: '等待中', running: '进行中', success: '已完成', partial_failed: '部分失败', failed: '失败' }
  return m[taskInfo.value.status] || '未开始'
})
const currentTypeLabel = computed(() => {
  const t = taskInfo.value.currentType
  if (!t || t === 'all') return '—'
  return status.value[t]?.name || fetchTypeOptions.find(o => o.value === t)?.label || t
})
const completedTypesText = computed(() => {
  const d = taskInfo.value.completedTypes || 0, t = taskInfo.value.totalTypes || 0
  return t ? `${d} / ${t}` : '—'
})
const resultRows = computed(() => {
  const src = taskInfo.value.results || taskInfo.value.summary || {}
  return Object.entries(src).filter(([,v]) => v && typeof v === 'object' && v.type).map(([,v]) => v)
})

function historyStatusLabel(v) {
  const m = { pending: '等待中', running: '进行中', success: '已完成', partial_failed: '部分失败', failed: '失败' }
  return m[v] || v || '—'
}
function triggerSourceLabel(v) { return v === 'manual' ? '手动' : v === 'scheduled' ? '定时' : v || '—' }
function typeLabel(v) { return v ? status.value[v]?.name || fetchTypeOptions.find(o => o.value === v)?.label || v : '—' }

async function loadRecentHistory() { const { data } = await api.fetchHistory({ limit: 8, offset: 0 }); recentHistory.value = data.data || [] }
async function loadStatus() { const { data } = await api.status(); status.value = data || {} }
async function afterTaskFinished() { await Promise.all([loadStatus(), loadRecentHistory()]) }
function stopPolling() { if (pollTimer.value) { clearInterval(pollTimer.value); pollTimer.value = null } }

async function pollTask(taskId) {
  const { data } = await api.fetchTask(taskId)
  taskInfo.value = data || {}
  if (['success', 'partial_failed', 'failed', 'not_found'].includes(taskInfo.value.status)) {
    stopPolling(); loading.value = false; await afterTaskFinished()
    if (taskInfo.value.status === 'success') message.value = '拉取完成！'
    else if (taskInfo.value.status === 'partial_failed') errorMessage.value = '部分彩种拉取失败。'
    else if (taskInfo.value.status === 'failed') errorMessage.value = taskInfo.value.error || '拉取失败。'
  }
}
function startPolling(taskId) { stopPolling(); pollTask(taskId); pollTimer.value = setInterval(() => pollTask(taskId), 1500) }

async function startFetch() {
  loading.value = true; message.value = ''; errorMessage.value = ''; taskInfo.value = {}
  try {
    const resp = selectedFetchType.value === 'all' ? await api.fetchAll(selectedScope.value) : await api.fetchOne(selectedFetchType.value, selectedScope.value)
    taskInfo.value = resp.data || {}
    if (taskInfo.value.taskId) startPolling(taskInfo.value.taskId)
    else { loading.value = false; errorMessage.value = '未获取到任务 ID。' }
  } catch (e) { loading.value = false; errorMessage.value = '拉取失败: ' + (e.response?.data?.message || e.message) }
}
async function refreshAll() { await Promise.all([loadStatus(), loadRecentHistory()]); if (taskInfo.value.taskId) await pollTask(taskInfo.value.taskId) }
onMounted(async () => { await Promise.all([loadStatus(), loadRecentHistory()]) })
onBeforeUnmount(() => { stopPolling() })
</script>

<style scoped>
.page-hero { margin-bottom: 24px; }
.page-title { font-size: 28px; font-weight: 800; letter-spacing: -0.5px; margin-bottom: 4px; }
.page-subtitle { color: var(--text-secondary); font-size: 14px; }

/* Action Bar */
.action-bar { margin-bottom: 24px; }
.action-group { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.select {
  background: var(--bg-card); color: var(--text-primary); border: 1px solid var(--border);
  padding: 10px 14px; border-radius: var(--radius-sm); font-size: 13px; font-weight: 500;
  font-family: var(--font); cursor: pointer;
}
.select:focus { border-color: var(--accent); outline: none; }
.btn-primary {
  display: flex; align-items: center; gap: 6px;
  padding: 10px 20px; border: none; border-radius: var(--radius-sm);
  background: linear-gradient(135deg, var(--accent), var(--purple));
  color: #fff; font-size: 13px; font-weight: 600; cursor: pointer;
  font-family: var(--font); transition: all 0.2s;
}
.btn-primary:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(99,102,241,0.4); }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-ghost {
  padding: 10px 16px; border: 1px solid var(--border); border-radius: var(--radius-sm);
  background: var(--bg-card); color: var(--text-secondary); font-size: 13px; font-weight: 500;
  cursor: pointer; font-family: var(--font); transition: all 0.2s;
}
.btn-ghost:hover:not(:disabled) { border-color: var(--accent); color: var(--accent); }

.spinner {
  display: inline-block; width: 14px; height: 14px;
  border: 2px solid rgba(255,255,255,0.3); border-top-color: #fff;
  border-radius: 50%; animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* Stat Cards */
.stat-cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; margin-bottom: 24px; }
.stat-card {
  background: var(--bg-card); border-radius: var(--radius-lg); padding: 20px;
  display: flex; align-items: center; gap: 16px; box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-light);
}
.stat-card.accent { border-color: var(--accent); background: linear-gradient(135deg, var(--accent-bg), #faf5ff); }
.stat-icon { font-size: 28px; }
.stat-info { display: flex; flex-direction: column; }
.stat-value { font-size: 28px; font-weight: 800; color: var(--text-primary); line-height: 1.2; }
.stat-card.accent .stat-value { color: var(--accent); }
.stat-label { font-size: 12px; color: var(--text-muted); font-weight: 500; margin-top: 2px; }

/* Panels */
.panel {
  background: var(--bg-card); border-radius: var(--radius-lg);
  padding: 24px; box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-light); margin-bottom: 24px;
}
.panel-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.panel-header h3 { font-size: 17px; font-weight: 700; }

/* Status Pills */
.status-pill {
  display: inline-flex; align-items: center; padding: 4px 12px;
  border-radius: 999px; font-size: 12px; font-weight: 600;
}
.status-pill.sm { padding: 3px 10px; font-size: 11px; }
.status-pill.idle, .status-pill.pending { background: var(--orange-bg); color: var(--orange); }
.status-pill.running { background: var(--blue-bg); color: var(--blue); }
.status-pill.success { background: var(--green-bg); color: var(--green); }
.status-pill.partial_failed, .status-pill.failed { background: var(--red-bg); color: var(--red); }

/* Progress */
.progress-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; }
.progress-item {
  display: flex; flex-direction: column; gap: 4px;
  padding: 14px; border-radius: var(--radius);
  background: var(--bg); border: 1px solid var(--border-light);
}
.progress-label { font-size: 12px; color: var(--text-muted); font-weight: 500; }
.progress-value { font-size: 15px; font-weight: 600; }
.mono { font-family: 'SF Mono', Consolas, monospace; font-size: 12px; word-break: break-all; }

/* Alerts */
.alert {
  display: flex; align-items: center; gap: 8px;
  margin-top: 16px; padding: 12px 16px; border-radius: var(--radius-sm);
  font-size: 13px; font-weight: 500;
}
.alert.success { background: var(--green-bg); color: #065f46; border: 1px solid #a7f3d0; }
.alert.error { background: var(--red-bg); color: #991b1b; border: 1px solid #fecaca; }
.alert-close { background: none; border: none; font-size: 16px; cursor: pointer; margin-left: auto; opacity: 0.5; }
.alert-close:hover { opacity: 1; }
.fade-enter-active, .fade-leave-active { transition: all 0.3s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateY(-8px); }

/* Type Grid */
.type-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px; }
.type-card {
  display: flex; align-items: center; gap: 12px;
  padding: 14px; border-radius: var(--radius);
  background: var(--bg); border: 1px solid var(--border-light);
  transition: all 0.2s;
}
.type-card:hover { border-color: var(--border); box-shadow: var(--shadow-sm); }
.type-icon { font-size: 22px; }
.type-info { flex: 1; }
.type-name { display: block; font-size: 13px; font-weight: 600; }
.type-count { display: block; font-size: 20px; font-weight: 800; color: var(--accent); }
.type-latest { font-size: 11px; color: var(--text-muted); }

/* Table */
.table-wrap { overflow-x: auto; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th {
  padding: 10px 16px; text-align: left; font-size: 11px; font-weight: 600;
  color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.5px;
  border-bottom: 1px solid var(--border-light); white-space: nowrap;
}
.data-table td { padding: 12px 16px; border-bottom: 1px solid var(--border-light); font-size: 13px; }
.data-table tr:last-child td { border-bottom: none; }
.data-table tr:hover td { background: var(--bg-card-hover); }
.text-muted { color: var(--text-muted); }
.source-tag {
  display: inline-block; padding: 2px 8px; border-radius: 999px;
  font-size: 11px; font-weight: 600;
}
.source-tag.manual { background: var(--blue-bg); color: var(--blue); }
.source-tag.scheduled { background: var(--purple-bg); color: var(--purple); }
.link { color: var(--accent); text-decoration: none; font-size: 13px; font-weight: 600; }
.link:hover { text-decoration: underline; }

/* Result Grid */
.result-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 12px; }
.result-card {
  padding: 16px; border-radius: var(--radius);
  background: var(--bg); border: 1px solid var(--border-light);
}
.result-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.result-name { font-weight: 600; }
.result-stats { display: flex; gap: 16px; }
.result-stat { display: flex; flex-direction: column; }
.result-stat-value { font-size: 20px; font-weight: 800; }
.result-stat-value.green { color: var(--green); }
.result-stat-value.blue { color: var(--blue); }
.result-stat-label { font-size: 11px; color: var(--text-muted); }
.result-error { margin-top: 8px; font-size: 12px; color: var(--red); }

@media (max-width: 768px) {
  .stat-cards { grid-template-columns: repeat(2, 1fr); }
  .action-group { width: 100%; }
  .select { flex: 1; min-width: 0; }
}
</style>
