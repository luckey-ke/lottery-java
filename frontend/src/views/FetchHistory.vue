<template>
  <div class="history-page">
    <div class="page-header">
      <div>
        <h2>抓取历史</h2>
        <p class="sub-text">查看手动与定时抓取的执行记录和明细。</p>
      </div>
      <div class="filters">
        <select v-model="filters.triggerSource" @change="loadHistory()">
          <option value="">全部来源</option>
          <option value="manual">手动</option>
          <option value="scheduled">定时</option>
        </select>
        <select v-model="filters.status" @change="loadHistory()">
          <option value="">全部状态</option>
          <option value="pending">等待中</option>
          <option value="running">进行中</option>
          <option value="success">已完成</option>
          <option value="partial_failed">部分失败</option>
          <option value="failed">失败</option>
        </select>
        <select v-model="filters.type" @change="loadHistory()">
          <option value="">全部彩种</option>
          <option v-for="item in typeOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
        <button class="btn btn-secondary" @click="loadHistory">刷新</button>
      </div>
    </div>

    <div class="panel">
      <div class="panel-header">
        <h3>执行记录</h3>
        <span class="sub-text">共 {{ total }} 条</span>
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
          <tr v-for="item in historyRows" :key="item.taskId">
            <td class="mono">{{ item.taskId }}</td>
            <td>{{ triggerSourceLabel(item.triggerSource) }}</td>
            <td>{{ typeLabel(item.type) }}</td>
            <td>{{ item.scope }}</td>
            <td>{{ statusLabel(item.status) }}</td>
            <td>{{ item.startedAt || '-' }}</td>
            <td>{{ item.finishedAt || '-' }}</td>
            <td>{{ item.totalFetched || 0 }}</td>
            <td>{{ item.inserted || 0 }}</td>
            <td>{{ item.updated || 0 }}</td>
            <td><button class="link-btn" @click="loadDetail(item.taskId)">查看详情</button></td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="panel" v-if="detail.taskId">
      <div class="panel-header">
        <h3>任务详情</h3>
        <span class="status-tag" :class="detail.status">{{ statusLabel(detail.status) }}</span>
      </div>
      <div class="detail-grid">
        <div class="detail-item"><span class="label">任务 ID</span><span class="value mono">{{ detail.taskId }}</span></div>
        <div class="detail-item"><span class="label">来源</span><span class="value">{{ triggerSourceLabel(detail.triggerSource) }}</span></div>
        <div class="detail-item"><span class="label">彩种</span><span class="value">{{ typeLabel(detail.type) }}</span></div>
        <div class="detail-item"><span class="label">范围</span><span class="value">{{ detail.scope || '-' }}</span></div>
        <div class="detail-item"><span class="label">开始时间</span><span class="value">{{ detail.startedAt || '-' }}</span></div>
        <div class="detail-item"><span class="label">结束时间</span><span class="value">{{ detail.finishedAt || '-' }}</span></div>
      </div>
      <table class="table detail-table" v-if="detailRows.length">
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
          <tr v-for="item in detailRows" :key="item.type">
            <td>{{ item.name || typeLabel(item.type) }}</td>
            <td>{{ statusLabel(item.status) }}</td>
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
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api'

const route = useRoute()

const typeOptions = [
  { value: 'all', label: '所有' },
  { value: 'ssq', label: '双色球' },
  { value: 'dlt', label: '大乐透' },
  { value: 'fc3d', label: '福彩3D' },
  { value: 'pl3', label: '排列三' },
  { value: 'pl5', label: '排列五' },
  { value: 'qlc', label: '七乐彩' },
]

const filters = ref({ status: '', triggerSource: '', type: '' })
const history = ref([])
const total = ref(0)
const detail = ref({})

const historyRows = computed(() => history.value || [])
const detailRows = computed(() => Object.values(detail.value.results || {}))

function statusLabel(value) {
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
  return typeOptions.find(item => item.value === value)?.label || value
}

async function loadHistory() {
  const { data } = await api.fetchHistory({ ...filters.value, limit: 50, offset: 0 })
  history.value = data.data || []
  total.value = data.total || 0
  const targetTaskId = route.query.taskId
  if (targetTaskId && history.value.some(item => item.taskId === targetTaskId)) {
    await loadDetail(targetTaskId)
    return
  }
  if ((!detail.value.taskId || targetTaskId) && history.value.length) {
    await loadDetail(history.value[0].taskId)
  }
}

async function loadDetail(taskId) {
  const { data } = await api.fetchHistoryDetail(taskId)
  detail.value = data || {}
}

onMounted(async () => {
  await loadHistory()
})
</script>

<style scoped>
.history-page { display: flex; flex-direction: column; gap: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.page-header h2 { font-size: 24px; margin-bottom: 6px; }
.sub-text { color: #7f93a8; font-size: 14px; }
.filters { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; }
.panel { background: linear-gradient(145deg, #1a2a3a, #0f1f2f); border: 1px solid #2a4a6a; border-radius: 12px; padding: 18px; }
.panel-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.table { width: 100%; border-collapse: collapse; background: #1a2a3a; border-radius: 12px; overflow: hidden; }
.table th { background: #0f1923; padding: 12px 16px; text-align: left; font-size: 13px; color: #667788; text-transform: uppercase; }
.table td { padding: 12px 16px; border-top: 1px solid #1f2f3f; }
.table tr:hover td { background: rgba(74,158,255,.05); }
.detail-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 12px; margin-bottom: 16px; }
.detail-item { display: flex; flex-direction: column; gap: 6px; padding: 12px; border-radius: 10px; background: rgba(255,255,255,.02); border: 1px solid rgba(74,158,255,.08); }
.label { color: #7f93a8; font-size: 12px; }
.value { color: #e0e6ed; font-size: 15px; font-weight: 600; }
.mono { font-family: Consolas, Monaco, monospace; font-size: 12px; word-break: break-all; }
.status-tag { padding: 4px 10px; border-radius: 999px; font-size: 12px; }
.status-tag.pending { background: rgba(255,193,7,.14); color: #f1c40f; }
.status-tag.running { background: rgba(74,158,255,.14); color: #4a9eff; }
.status-tag.success { background: rgba(46,213,115,.14); color: #2ed573; }
.status-tag.partial_failed, .status-tag.failed { background: rgba(255,71,87,.14); color: #ff6b81; }
.error-cell { max-width: 260px; word-break: break-word; color: #ff9aa7; }
select { background: #1a2a3a; color: #e0e6ed; border: 1px solid #2a4a6a; padding: 8px 12px; border-radius: 8px; font-size: 14px; }
.btn { padding: 10px 20px; border: none; border-radius: 8px; cursor: pointer; font-size: 14px; font-weight: 600; transition: all .2s; }
.btn-secondary { background: #2a3a4a; color: #c7d2dd; border: 1px solid #3a4a5a; }
.btn-secondary:hover { background: #33485d; color: #fff; }
.link-btn { background: none; border: none; color: #4a9eff; cursor: pointer; }
.link-btn:hover { text-decoration: underline; }
</style>
