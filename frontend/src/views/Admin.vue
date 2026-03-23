<template>
  <div class="admin-page">
    <div class="page-header">
      <div>
        <h2>管理后台</h2>
        <p class="sub-text">在这里拉取真实数据并查看本次进度与汇总。</p>
      </div>
      <div class="actions">
        <select v-model="selectedFetchType" :disabled="loading">
          <option v-for="option in fetchTypeOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>
        <select v-model="selectedScope" :disabled="loading">
          <option v-for="option in fetchScopeOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>
        <button class="btn btn-primary" @click="startFetch" :disabled="loading">
          {{ loading ? '拉取中...' : '开始拉取' }}
        </button>
        <button class="btn btn-secondary" @click="refreshAll" :disabled="loading">刷新统计</button>
      </div>
    </div>

    <div class="summary-grid">
      <div class="summary-card highlight">
        <div class="summary-label">全库总量</div>
        <div class="summary-value">{{ totalCount }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">本次抓取</div>
        <div class="summary-value">{{ taskInfo.totalFetched || 0 }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">本次新增</div>
        <div class="summary-value">{{ taskInfo.inserted || 0 }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">本次更新</div>
        <div class="summary-value">{{ taskInfo.updated || 0 }}</div>
      </div>
    </div>

    <div class="panel progress-panel">
      <div class="panel-header">
        <h3>抓取进度</h3>
        <span class="status-tag" :class="taskInfo.status || 'idle'">{{ statusLabel }}</span>
      </div>
      <div class="progress-grid">
        <div class="progress-item">
          <span class="label">任务 ID</span>
          <span class="value mono">{{ taskInfo.taskId || '-' }}</span>
        </div>
        <div class="progress-item">
          <span class="label">当前彩种</span>
          <span class="value">{{ currentTypeLabel }}</span>
        </div>
        <div class="progress-item">
          <span class="label">当前页</span>
          <span class="value">{{ taskInfo.currentPage || 0 }}</span>
        </div>
        <div class="progress-item">
          <span class="label">完成进度</span>
          <span class="value">{{ completedTypesText }}</span>
        </div>
      </div>
      <div class="msg success" v-if="message">{{ message }}</div>
      <div class="msg error" v-if="errorMessage">{{ errorMessage }}</div>
    </div>

    <div class="panel" v-if="recentHistory.length">
      <div class="panel-header">
        <h3>最近执行记录</h3>
        <router-link class="link-btn" to="/history">查看全部</router-link>
      </div>
      <table class="table">
        <thead>
          <tr>
            <th>任务 ID</th>
            <th>来源</th>
            <th>彩种</th>
            <th>范围</th>
            <th>状态</th>
            <th>开始时间</th>
            <th>结束时间</th>
            <th>抓取</th>
            <th>新增</th>
            <th>更新</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in recentHistory" :key="item.taskId">
            <td class="mono">{{ item.taskId }}</td>
            <td>{{ triggerSourceLabel(item.triggerSource) }}</td>
            <td>{{ typeLabel(item.type) }}</td>
            <td>{{ item.scope }}</td>
            <td>{{ historyStatusLabel(item.status) }}</td>
            <td>{{ item.startedAt || '-' }}</td>
            <td>{{ item.finishedAt || '-' }}</td>
            <td>{{ item.totalFetched || 0 }}</td>
            <td>{{ item.inserted || 0 }}</td>
            <td>{{ item.updated || 0 }}</td>
            <td>
              <router-link class="link-btn" :to="`/history?taskId=${item.taskId}`">查看详情</router-link>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="panel" v-if="typeStats.length">
      <div class="panel-header">
        <h3>分类统计</h3>
      </div>
      <div class="type-grid">
        <div class="type-card" v-for="item in typeStats" :key="item.code">
          <div class="type-name">{{ item.name }}</div>
          <div class="type-count">{{ item.count }}</div>
          <div class="type-latest">最新期号：{{ item.latestDraw || '-' }}</div>
        </div>
      </div>
    </div>

    <div class="panel" v-if="resultRows.length">
      <div class="panel-header">
        <h3>本次结果摘要</h3>
      </div>
      <table class="table">
        <thead>
          <tr>
            <th>彩种</th>
            <th>状态</th>
            <th>页码</th>
            <th>抓取</th>
            <th>新增</th>
            <th>更新</th>
            <th>错误</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in resultRows" :key="item.type">
            <td>{{ item.name }}</td>
            <td>{{ item.status }}</td>
            <td>{{ item.currentPage || 0 }}</td>
            <td>{{ item.totalFetched || 0 }}</td>
            <td>{{ item.inserted || 0 }}</td>
            <td>{{ item.updated || 0 }}</td>
            <td class="error-cell">{{ item.error || '-' }}</td>
          </tr>
        </tbody>
      </table>
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

const fetchTypeOptions = [
  { value: 'all', label: '所有' },
  { value: 'ssq', label: '双色球' },
  { value: 'dlt', label: '大乐透' },
  { value: 'fc3d', label: '福彩3D' },
  { value: 'pl3', label: '排列三' },
  { value: 'pl5', label: '排列五' },
  { value: 'qlc', label: '七乐彩' },
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

const totalCount = computed(() => Object.values(status.value).reduce((sum, item) => sum + (item.count || 0), 0))
const typeStats = computed(() => Object.entries(status.value).map(([code, info]) => ({ code, ...info })))
const statusLabel = computed(() => {
  const value = taskInfo.value.status
  if (!value) return '未开始'
  if (value === 'pending') return '等待中'
  if (value === 'running') return '进行中'
  if (value === 'success') return '已完成'
  if (value === 'partial_failed') return '部分失败'
  if (value === 'failed') return '失败'
  return value
})
const currentTypeLabel = computed(() => {
  const type = taskInfo.value.currentType
  if (!type || type === 'all') return '-'
  return status.value[type]?.name || fetchTypeOptions.find(item => item.value === type)?.label || type
})
const completedTypesText = computed(() => {
  const done = taskInfo.value.completedTypes || 0
  const total = taskInfo.value.totalTypes || 0
  return total ? `${done} / ${total}` : '-'
})
const resultRows = computed(() => {
  const source = taskInfo.value.results || taskInfo.value.summary || {}
  return Object.entries(source)
    .filter(([, value]) => value && typeof value === 'object' && value.type)
    .map(([, value]) => value)
})

function historyStatusLabel(value) {
  if (!value) return '-'
  if (value === 'pending') return '等待中'
  if (value === 'running') return '进行中'
  if (value === 'success') return '已完成'
  if (value === 'partial_failed') return '部分失败'
  if (value === 'failed') return '失败'
  return value
}

function triggerSourceLabel(value) {
  if (value === 'manual') return '手动'
  if (value === 'scheduled') return '定时'
  return value || '-'
}

function typeLabel(value) {
  if (!value) return '-'
  return status.value[value]?.name || fetchTypeOptions.find(item => item.value === value)?.label || value
}

async function loadRecentHistory() {
  const { data } = await api.fetchHistory({ limit: 8, offset: 0 })
  recentHistory.value = data.data || []
}

async function loadStatus() {
  const { data } = await api.status()
  status.value = data || {}
}

async function afterTaskFinished() {
  await Promise.all([loadStatus(), loadRecentHistory()])
}

function stopPolling() {
  if (pollTimer.value) {
    clearInterval(pollTimer.value)
    pollTimer.value = null
  }
}

async function pollTask(taskId) {
  const { data } = await api.fetchTask(taskId)
  taskInfo.value = data || {}
  if (['success', 'partial_failed', 'failed', 'not_found'].includes(taskInfo.value.status)) {
    stopPolling()
    loading.value = false
    await afterTaskFinished()
    if (taskInfo.value.status === 'success') {
      message.value = '拉取完成。'
    } else if (taskInfo.value.status === 'partial_failed') {
      errorMessage.value = '拉取完成，但部分彩种失败。'
    } else if (taskInfo.value.status === 'failed') {
      errorMessage.value = taskInfo.value.error || '拉取失败。'
    }
  }
}

function startPolling(taskId) {
  stopPolling()
  pollTask(taskId)
  pollTimer.value = setInterval(() => {
    pollTask(taskId)
  }, 1500)
}

async function startFetch() {
  loading.value = true
  message.value = ''
  errorMessage.value = ''
  taskInfo.value = {}
  try {
    const response = selectedFetchType.value === 'all'
      ? await api.fetchAll(selectedScope.value)
      : await api.fetchOne(selectedFetchType.value, selectedScope.value)
    taskInfo.value = response.data || {}
    if (taskInfo.value.taskId) {
      startPolling(taskInfo.value.taskId)
    } else {
      loading.value = false
      errorMessage.value = '未获取到任务 ID。'
    }
  } catch (e) {
    loading.value = false
    errorMessage.value = '拉取失败: ' + (e.response?.data?.message || e.message)
  }
}

async function refreshAll() {
  await Promise.all([loadStatus(), loadRecentHistory()])
  if (taskInfo.value.taskId) {
    await pollTask(taskInfo.value.taskId)
  }
}

onMounted(async () => {
  await Promise.all([loadStatus(), loadRecentHistory()])
})

onBeforeUnmount(() => {
  stopPolling()
})
</script>

<style scoped>
.admin-page { display: flex; flex-direction: column; gap: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.page-header h2 { font-size: 24px; margin-bottom: 6px; }
.sub-text { color: #7f93a8; font-size: 14px; }
.actions { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.summary-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 16px; }
.summary-card, .panel { background: linear-gradient(145deg, #1a2a3a, #0f1f2f); border: 1px solid #2a4a6a; border-radius: 12px; padding: 18px; }
.summary-card.highlight { border-color: #4a9eff; }
.summary-label { color: #7f93a8; font-size: 13px; margin-bottom: 10px; }
.summary-value { font-size: 30px; font-weight: 700; color: #4a9eff; }
.panel-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.panel-header h3 { font-size: 18px; }
.progress-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; }
.progress-item { display: flex; flex-direction: column; gap: 6px; padding: 12px; border-radius: 10px; background: rgba(255,255,255,.02); border: 1px solid rgba(74,158,255,.08); }
.label { color: #7f93a8; font-size: 12px; }
.value { color: #e0e6ed; font-size: 15px; font-weight: 600; }
.mono { font-family: Consolas, Monaco, monospace; font-size: 12px; word-break: break-all; }
.status-tag { padding: 4px 10px; border-radius: 999px; font-size: 12px; }
.status-tag.pending { background: rgba(255,193,7,.14); color: #f1c40f; }
.status-tag.running { background: rgba(74,158,255,.14); color: #4a9eff; }
.status-tag.success { background: rgba(46,213,115,.14); color: #2ed573; }
.status-tag.partial_failed, .status-tag.failed { background: rgba(255,71,87,.14); color: #ff6b81; }
.type-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; }
.type-card { padding: 14px; border-radius: 10px; background: rgba(255,255,255,.02); border: 1px solid rgba(74,158,255,.08); }
.type-name { font-size: 15px; font-weight: 600; margin-bottom: 10px; }
.type-count { font-size: 28px; color: #4a9eff; font-weight: 700; }
.type-latest { color: #7f93a8; margin-top: 8px; font-size: 12px; }
.table { width: 100%; border-collapse: collapse; background: #1a2a3a; border-radius: 12px; overflow: hidden; }
.table th { background: #0f1923; padding: 12px 16px; text-align: left; font-size: 13px; color: #667788; text-transform: uppercase; }
.table td { padding: 12px 16px; border-top: 1px solid #1f2f3f; }
.table tr:hover td { background: rgba(74,158,255,.05); }
.error-cell { max-width: 260px; word-break: break-word; color: #ff9aa7; }
select { background: #1a2a3a; color: #e0e6ed; border: 1px solid #2a4a6a; padding: 8px 12px; border-radius: 8px; font-size: 14px; }
.btn { padding: 10px 20px; border: none; border-radius: 8px; cursor: pointer; font-size: 14px; font-weight: 600; transition: all .2s; }
.btn:disabled { opacity: .5; cursor: not-allowed; }
.btn-primary { background: linear-gradient(135deg, #4a9eff, #0077cc); color: #fff; }
.btn-primary:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(74,158,255,.4); }
.btn-secondary { background: #2a3a4a; color: #c7d2dd; border: 1px solid #3a4a5a; }
.btn-secondary:hover:not(:disabled) { background: #33485d; color: #fff; }
.msg { margin-top: 16px; padding: 12px 16px; border-radius: 8px; font-size: 14px; }
.msg.success { background: rgba(46,213,115,.1); color: #2ed573; border: 1px solid rgba(46,213,115,.3); }
.msg.error { background: rgba(255,71,87,.1); color: #ff4757; border: 1px solid rgba(255,71,87,.3); }
</style>
