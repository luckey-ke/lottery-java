<template>
  <div class="history-page">
    <div class="page-hero">
      <div>
        <h1 class="page-title">抓取历史</h1>
        <p class="page-subtitle">查看手动与定时抓取的执行记录和明细</p>
      </div>
      <span class="hero-badge">共 {{ total }} 条记录</span>
    </div>

    <!-- 筛选器 -->
    <div class="filter-bar">
      <div class="filter-group">
        <select v-model="filters.triggerSource" @change="loadHistory()" class="select">
          <option value="">全部来源</option>
          <option value="manual">手动</option>
          <option value="scheduled">定时</option>
        </select>
        <select v-model="filters.status" @change="loadHistory()" class="select">
          <option value="">全部状态</option>
          <option value="pending">等待中</option>
          <option value="running">进行中</option>
          <option value="success">已完成</option>
          <option value="partial_failed">部分失败</option>
          <option value="failed">失败</option>
        </select>
        <select v-model="filters.type" @change="loadHistory()" class="select">
          <option value="">全部彩种</option>
          <option v-for="item in typeOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
      </div>
      <button class="btn-ghost" @click="loadHistory">🔄 刷新</button>
    </div>

    <!-- 记录表格 -->
    <div class="panel">
      <div class="table-wrap">
        <table class="data-table">
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
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="item in historyRows"
              :key="item.taskId"
              :class="{ 'is-active': detail.taskId === item.taskId }"
              @click="loadDetail(item.taskId)"
            >
              <td class="mono">{{ item.taskId?.slice(0, 12) }}…</td>
              <td>
                <span :class="['source-tag', item.triggerSource]">{{ triggerSourceLabel(item.triggerSource) }}</span>
              </td>
              <td>{{ typeLabel(item.type) }}</td>
              <td class="text-muted">{{ item.scope }}</td>
              <td><span :class="['status-pill', 'sm', item.status]">{{ statusLabel(item.status) }}</span></td>
              <td class="text-muted">{{ item.startedAt || '—' }}</td>
              <td class="text-muted">{{ item.finishedAt || '—' }}</td>
              <td>{{ item.totalFetched || 0 }}</td>
              <td class="green">{{ item.inserted || 0 }}</td>
              <td><span class="link">详情 →</span></td>
            </tr>
            <tr v-if="!historyRows.length">
              <td colspan="10">
                <div class="empty-state">
                  <span class="empty-icon">📋</span>
                  <span>暂无抓取记录</span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 任务详情 -->
    <Transition name="slide">
      <div class="panel detail-panel" v-if="detail.taskId">
        <div class="panel-header">
          <h3>任务详情</h3>
          <span :class="['status-pill', detail.status]">{{ statusLabel(detail.status) }}</span>
        </div>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="detail-label">任务 ID</span>
            <span class="detail-value mono">{{ detail.taskId }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">来源</span>
            <span class="detail-value">{{ triggerSourceLabel(detail.triggerSource) }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">彩种</span>
            <span class="detail-value">{{ typeLabel(detail.type) }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">范围</span>
            <span class="detail-value">{{ detail.scope || '—' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">开始时间</span>
            <span class="detail-value">{{ detail.startedAt || '—' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">结束时间</span>
            <span class="detail-value">{{ detail.finishedAt || '—' }}</span>
          </div>
        </div>
        <div class="detail-results" v-if="detailRows.length">
          <h4>各彩种结果</h4>
          <div class="result-chips">
            <div class="result-chip" v-for="item in detailRows" :key="item.type">
              <div class="chip-header">
                <span class="chip-name">{{ item.name || typeLabel(item.type) }}</span>
                <span :class="['status-pill', 'sm', item.status]">{{ statusLabel(item.status) }}</span>
              </div>
              <div class="chip-stats">
                <span><b>{{ item.totalFetched || 0 }}</b> 抓取</span>
                <span class="green"><b>{{ item.inserted || 0 }}</b> 新增</span>
                <span class="blue"><b>{{ item.updated || 0 }}</b> 更新</span>
              </div>
              <div class="chip-error" v-if="item.error">{{ item.error }}</div>
            </div>
          </div>
        </div>
      </div>
    </Transition>
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

function statusLabel(v) {
  const m = { pending: '等待中', running: '进行中', success: '已完成', partial_failed: '部分失败', failed: '失败' }
  return m[v] || v || '—'
}
function triggerSourceLabel(v) { return v === 'manual' ? '手动' : v === 'scheduled' ? '定时' : v || '—' }
function typeLabel(v) { return v ? typeOptions.find(i => i.value === v)?.label || v : '—' }

async function loadHistory() {
  const { data } = await api.fetchHistory({ ...filters.value, limit: 50, offset: 0 })
  history.value = data.data || []
  total.value = data.total || 0
  const tid = route.query.taskId
  if (tid && history.value.some(i => i.taskId === tid)) { await loadDetail(tid); return }
  if ((!detail.value.taskId || tid) && history.value.length) { await loadDetail(history.value[0].taskId) }
}
async function loadDetail(taskId) {
  const { data } = await api.fetchHistoryDetail(taskId)
  detail.value = data || {}
}
onMounted(async () => { await loadHistory() })
</script>

<style scoped>
.page-hero {
  display: flex; justify-content: space-between; align-items: flex-end;
  margin-bottom: 24px; gap: 16px;
}
.page-title { font-size: 28px; font-weight: 800; letter-spacing: -0.5px; margin-bottom: 4px; }
.page-subtitle { color: var(--text-secondary); font-size: 14px; }
.hero-badge {
  font-size: 13px; font-weight: 600; color: var(--accent);
  background: var(--accent-bg); padding: 6px 14px; border-radius: 999px;
}

/* Filters */
.filter-bar {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 20px; gap: 12px; flex-wrap: wrap;
}
.filter-group { display: flex; gap: 8px; flex-wrap: wrap; }
.select {
  background: var(--bg-card); color: var(--text-primary); border: 1px solid var(--border);
  padding: 8px 12px; border-radius: var(--radius-sm); font-size: 13px;
  font-weight: 500; font-family: var(--font); cursor: pointer;
}
.select:focus { border-color: var(--accent); outline: none; }
.btn-ghost {
  padding: 8px 16px; border: 1px solid var(--border); border-radius: var(--radius-sm);
  background: var(--bg-card); color: var(--text-secondary); font-size: 13px;
  font-weight: 500; cursor: pointer; font-family: var(--font); transition: all 0.2s;
}
.btn-ghost:hover { border-color: var(--accent); color: var(--accent); }

/* Panel */
.panel {
  background: var(--bg-card); border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm); border: 1px solid var(--border-light);
  overflow: hidden; margin-bottom: 24px;
}
.panel-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 20px 24px; border-bottom: 1px solid var(--border-light);
}
.panel-header h3 { font-size: 17px; font-weight: 700; }

/* Status */
.status-pill {
  display: inline-flex; align-items: center; padding: 4px 12px;
  border-radius: 999px; font-size: 12px; font-weight: 600;
}
.status-pill.sm { padding: 3px 10px; font-size: 11px; }
.status-pill.pending { background: var(--orange-bg); color: var(--orange); }
.status-pill.running { background: var(--blue-bg); color: var(--blue); }
.status-pill.success { background: var(--green-bg); color: var(--green); }
.status-pill.partial_failed, .status-pill.failed { background: var(--red-bg); color: var(--red); }

.source-tag {
  display: inline-block; padding: 2px 8px; border-radius: 999px;
  font-size: 11px; font-weight: 600;
}
.source-tag.manual { background: var(--blue-bg); color: var(--blue); }
.source-tag.scheduled { background: var(--purple-bg); color: var(--purple); }

/* Table */
.table-wrap { overflow-x: auto; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th {
  padding: 12px 20px; text-align: left; font-size: 11px; font-weight: 600;
  color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.5px;
  border-bottom: 1px solid var(--border-light); white-space: nowrap;
}
.data-table td {
  padding: 14px 20px; border-bottom: 1px solid var(--border-light);
  font-size: 13px; cursor: pointer;
}
.data-table tr:last-child td { border-bottom: none; }
.data-table tr:hover td { background: var(--bg-card-hover); }
.data-table tr.is-active td { background: var(--accent-bg); }
.text-muted { color: var(--text-muted); }
.green { color: var(--green); font-weight: 600; }
.blue { color: var(--blue); font-weight: 600; }
.mono { font-family: 'SF Mono', Consolas, monospace; font-size: 12px; }
.link { color: var(--accent); font-weight: 600; font-size: 13px; }

.empty-state {
  display: flex; flex-direction: column; align-items: center; gap: 8px;
  padding: 48px 20px; color: var(--text-muted); font-size: 14px;
}
.empty-icon { font-size: 32px; }

/* Detail Panel */
.detail-panel { border-color: var(--accent); }
.detail-grid {
  display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px; padding: 0 24px 20px;
}
.detail-item {
  display: flex; flex-direction: column; gap: 4px;
  padding: 12px; border-radius: var(--radius-sm);
  background: var(--bg); border: 1px solid var(--border-light);
}
.detail-label { font-size: 11px; color: var(--text-muted); font-weight: 500; }
.detail-value { font-size: 14px; font-weight: 600; }

.detail-results { padding: 0 24px 24px; }
.detail-results h4 { font-size: 14px; font-weight: 600; margin-bottom: 12px; color: var(--text-secondary); }
.result-chips { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 12px; }
.result-chip {
  padding: 14px; border-radius: var(--radius);
  background: var(--bg); border: 1px solid var(--border-light);
}
.chip-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.chip-name { font-weight: 600; font-size: 14px; }
.chip-stats { display: flex; gap: 12px; font-size: 12px; color: var(--text-secondary); }
.chip-stats b { font-size: 16px; }
.chip-error { margin-top: 8px; font-size: 12px; color: var(--red); }

.slide-enter-active { animation: slideDown 0.3s ease; }
.slide-leave-active { animation: slideDown 0.3s ease reverse; }
@keyframes slideDown { from { opacity: 0; transform: translateY(-12px); } to { opacity: 1; transform: translateY(0); } }

@media (max-width: 768px) {
  .page-hero { flex-direction: column; align-items: flex-start; }
  .filter-bar { flex-direction: column; align-items: flex-start; }
}
</style>
