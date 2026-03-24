<template>
  <div class="dashboard">
    <!-- 页面标题区 -->
    <div class="page-hero">
      <div>
        <h1 class="page-title">数据总览</h1>
        <p class="page-subtitle">实时查看各彩种最新开奖与数据状态</p>
      </div>
      <div class="hero-stats" v-if="Object.keys(status).length">
        <div class="hero-stat">
          <span class="hero-stat-value">{{ totalCount }}</span>
          <span class="hero-stat-label">总期数</span>
        </div>
        <div class="hero-stat">
          <span class="hero-stat-value">{{ Object.keys(status).length }}</span>
          <span class="hero-stat-label">彩种</span>
        </div>
      </div>
    </div>

    <!-- 彩种卡片网格 -->
    <div class="lottery-grid" v-if="Object.keys(status).length">
      <div
        v-for="(info, key) in status"
        :key="key"
        :class="['lottery-card', { 'is-selected': selectedType === key }]"
        @click="selectType(key)"
      >
        <div class="lottery-card-header">
          <span class="lottery-icon">{{ icons[key] || '🎱' }}</span>
          <div class="lottery-meta">
            <span class="lottery-name">{{ info.name }}</span>
            <span class="lottery-code">{{ key.toUpperCase() }}</span>
          </div>
          <span class="lottery-count">{{ info.count }}<small>期</small></span>
        </div>
        <div class="lottery-card-body">
          <div class="lottery-draw">
            <span class="draw-label">最新一期</span>
            <span class="draw-num">{{ info.latestDraw || '暂无' }}</span>
          </div>
          <div class="lottery-numbers" v-if="parseNumbers(info.latestNumbers).length">
            <span
              v-for="(n, i) in parseNumbers(info.latestNumbers)"
              :key="i"
              :class="['ball', n.type]"
            >{{ n.value }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 开奖记录表格 -->
    <div class="table-section" v-if="selectedType">
      <div class="table-header">
        <div class="table-title-group">
          <h2 class="table-title">{{ selectedName }} 开奖记录</h2>
          <span class="table-badge">{{ totalResults }} 条数据</span>
        </div>
        <div class="table-controls">
          <div class="select-group">
            <select v-model="selectedType" @change="handleTypeChange" class="select">
              <option v-for="(info, key) in status" :key="key" :value="key">{{ info.name }}</option>
            </select>
            <select v-model="pageSize" @change="handlePageSizeChange" class="select">
              <option v-for="size in pageSizeOptions" :key="size" :value="size">{{ size }} 条/页</option>
            </select>
          </div>
        </div>
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>日期</th>
              <th>期号</th>
              <th>中奖号码</th>
              <th>销售额</th>
              <th>一等奖</th>
              <th>二等奖</th>
              <th>详情</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in results" :key="r.id">
              <td class="date-cell">{{ r.drawDate }}</td>
              <td class="draw-num-cell">{{ r.drawNum }}</td>
              <td>
                <div class="numbers">
                  <span v-for="(n, i) in parseNumbers(r.numbers)" :key="i" :class="['ball', 'ball-sm', n.type]">{{ n.value }}</span>
                </div>
              </td>
              <td class="amount-cell">{{ getSalesAmountText(r) }}</td>
              <td class="prize-cell">{{ getFirstPrizeText(r) }}</td>
              <td class="prize-cell">{{ getSecondPrizeText(r) }}</td>
              <td class="detail-cell">
                <div v-if="hasDetailContent(r)" class="detail-content">
                  <div v-if="getDetailText(r) !== '-'" class="detail-text">{{ getDetailText(r) }}</div>
                  <div v-if="getDetailLinks(r).length" class="detail-links">
                    <a
                      v-for="link in getDetailLinks(r)"
                      :key="link.label + link.url"
                      :href="link.url"
                      class="detail-link"
                      target="_blank"
                      rel="noopener noreferrer"
                    >{{ link.label }} →</a>
                  </div>
                </div>
                <span v-else class="text-muted">-</span>
              </td>
            </tr>
            <tr v-if="!results.length">
              <td colspan="7" class="empty-row">
                <div class="empty-state">
                  <span class="empty-icon">📭</span>
                  <span>暂无开奖记录</span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="pagination" v-if="totalResults > 0">
        <span class="page-info">第 {{ currentPage }} / {{ totalPages }} 页</span>
        <div class="page-btns">
          <button class="page-btn" @click="goToPrevPage" :disabled="currentPage <= 1">← 上一页</button>
          <button class="page-btn" @click="goToNextPage" :disabled="currentPage >= totalPages">下一页 →</button>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div class="empty-hero" v-else-if="!Object.keys(status).length">
      <span class="empty-hero-icon">🎰</span>
      <h3>还没有数据</h3>
      <p>点击上方「演示数据」按钮生成示例数据，或前往管理后台拉取真实数据</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../api'

const status = ref({})
const results = ref([])
const totalResults = ref(0)
const selectedType = ref('ssq')
const currentPage = ref(1)
const pageSize = ref(20)

const icons = { ssq: '🔴🔵', dlt: '🟡🔵', fc3d: '🎲', pl3: '🎯', pl5: '🎯🎯', qlc: '🎱' }
const pageSizeOptions = [20, 100, 200, 500]
const salesAmountKeys = ['salesAmount', 'saleAmount', 'sales', 'salesMoney', 'amount', 'sales_total', '销售额', '销售额(元)']
const firstPrizeKeys = ['firstPrize', 'prize1', '一等奖', '一等奖详情', '一等奖信息']
const secondPrizeKeys = ['secondPrize', 'prize2', '二等奖', '二等奖详情', '二等奖信息']
const detailKeys = ['detail', 'detailText', 'remark', 'memo', 'notes', '详情', '详细']

const selectedName = computed(() => status.value[selectedType.value]?.name || '')
const totalPages = computed(() => Math.max(1, Math.ceil(totalResults.value / pageSize.value)))
const totalCount = computed(() => Object.values(status.value).reduce((s, i) => s + (i.count || 0), 0))

function parseNumbers(str) {
  if (!str) return []
  if (str.includes('+')) {
    const [redPart, bluePart] = str.split('+')
    return [
      ...redPart.split(',').map(v => ({ value: v.trim(), type: 'red' })),
      ...bluePart.split(',').map(v => ({ value: v.trim(), type: 'blue' })),
    ]
  }
  return str.split(/[\s,]+/).filter(Boolean).map(v => ({ value: v.trim(), type: 'gray' }))
}

function parseExtraInfo(raw) {
  if (!raw) return {}
  if (typeof raw === 'object') return raw
  try { const p = JSON.parse(raw); return p && typeof p === 'object' ? p : {} } catch { return {} }
}
function normalizeResult(item) {
  return {
    ...item,
    parsedExtraInfo: parseExtraInfo(item.extraInfo),
    rawExtraInfoText: typeof item.extraInfo === 'string' ? item.extraInfo.trim() : '',
  }
}
function pickExtraValue(info, keys) {
  for (const k of keys) { const v = info?.[k]; if (v !== undefined && v !== null && v !== '') return v }
  return ''
}
function formatExtraValue(value) {
  if (value === undefined || value === null || value === '') return '-'
  if (Array.isArray(value)) return value.map(i => formatExtraValue(i)).filter(i => i !== '-').join('，') || '-'
  if (typeof value === 'object') return Object.entries(value).filter(([,v]) => v !== undefined && v !== null && v !== '').map(([k,v]) => `${k}:${v}`).join('；') || '-'
  return String(value)
}
function getExtraText(row, keys) { return formatExtraValue(pickExtraValue(row.parsedExtraInfo, keys)) }
function getSalesAmountText(row) { return getExtraText(row, salesAmountKeys) }
function getFirstPrizeText(row) { return getExtraText(row, firstPrizeKeys) }
function getSecondPrizeText(row) { return getExtraText(row, secondPrizeKeys) }
function getDetailValue(row) { return pickExtraValue(row.parsedExtraInfo, detailKeys) }
function getDetailText(row) {
  const dv = getDetailValue(row)
  if (dv && typeof dv === 'object' && !Array.isArray(dv)) return formatExtraValue(dv.text) === '-' ? '-' : formatExtraValue(dv.text)
  const dt = formatExtraValue(dv)
  return dt !== '-' ? dt : row.rawExtraInfoText || '-'
}
function getDetailLinks(row) {
  const dv = getDetailValue(row)
  const links = dv && typeof dv === 'object' && !Array.isArray(dv) ? dv.links : null
  if (!links || typeof links !== 'object') return []
  return Object.entries(links).filter(([,url]) => typeof url === 'string' && url.trim()).map(([label, url]) => ({ label, url: url.trim() }))
}
function hasDetailContent(row) { return getDetailText(row) !== '-' || getDetailLinks(row).length > 0 }

function selectType(key) {
  selectedType.value = key
  currentPage.value = 1
  loadResults()
}

async function loadStatus() {
  const { data } = await api.status()
  status.value = data
  if (!status.value[selectedType.value]) selectedType.value = Object.keys(status.value)[0] || ''
}
async function loadResults() {
  if (!selectedType.value) { results.value = []; totalResults.value = 0; return }
  const offset = (currentPage.value - 1) * pageSize.value
  const { data } = await api.results(selectedType.value, pageSize.value, offset)
  results.value = (data.data || []).map(normalizeResult)
  totalResults.value = data.total || 0
  if (totalResults.value > 0 && currentPage.value > totalPages.value) { currentPage.value = totalPages.value; return loadResults() }
}
async function handleTypeChange() { currentPage.value = 1; await loadResults() }
async function handlePageSizeChange() { currentPage.value = 1; await loadResults() }
async function goToPrevPage() { if (currentPage.value <= 1) return; currentPage.value--; await loadResults() }
async function goToNextPage() { if (currentPage.value >= totalPages.value) return; currentPage.value++; await loadResults() }

onMounted(async () => { await loadStatus(); await loadResults() })
</script>

<style scoped>
/* ========== Page Hero ========== */
.page-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 24px;
  gap: 16px;
}
.page-title {
  font-size: 28px;
  font-weight: 800;
  letter-spacing: -0.5px;
  margin-bottom: 4px;
}
.page-subtitle {
  color: var(--text-secondary);
  font-size: 14px;
}
.hero-stats {
  display: flex;
  gap: 24px;
}
.hero-stat {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}
.hero-stat-value {
  font-size: 24px;
  font-weight: 800;
  color: var(--accent);
  line-height: 1.2;
}
.hero-stat-label {
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 500;
}

/* ========== Lottery Cards Grid ========== */
.lottery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  margin-bottom: 32px;
}
.lottery-card {
  background: var(--bg-card);
  border: 2px solid transparent;
  border-radius: var(--radius-lg);
  padding: 20px;
  cursor: pointer;
  transition: all 0.25s ease;
  box-shadow: var(--shadow-sm);
}
.lottery-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
  border-color: var(--border);
}
.lottery-card.is-selected {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-bg), var(--shadow-md);
}
.lottery-card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.lottery-icon {
  font-size: 28px;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg);
  border-radius: var(--radius);
  flex-shrink: 0;
}
.lottery-meta {
  flex: 1;
  min-width: 0;
}
.lottery-name {
  display: block;
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}
.lottery-code {
  display: inline-block;
  font-size: 11px;
  font-weight: 600;
  color: var(--accent);
  background: var(--accent-bg);
  padding: 2px 8px;
  border-radius: 999px;
  margin-top: 2px;
}
.lottery-count {
  font-size: 20px;
  font-weight: 800;
  color: var(--text-primary);
  line-height: 1;
}
.lottery-count small {
  font-size: 11px;
  color: var(--text-muted);
  font-weight: 500;
  margin-left: 2px;
}
.lottery-card-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.lottery-draw {
  display: flex;
  align-items: baseline;
  gap: 8px;
}
.draw-label {
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 500;
}
.draw-num {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-secondary);
  font-variant-numeric: tabular-nums;
}

/* ========== Balls ========== */
.lottery-numbers, .numbers {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.ball {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  font-size: 13px;
  font-weight: 700;
  color: #fff;
  flex-shrink: 0;
}
.ball.red { background: linear-gradient(135deg, #ef4444, #dc2626); }
.ball.blue { background: linear-gradient(135deg, #3b82f6, #2563eb); }
.ball.gray { background: linear-gradient(135deg, #6b7280, #4b5563); }
.ball-sm { width: 28px; height: 28px; font-size: 12px; }

/* ========== Table Section ========== */
.table-section {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}
.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-light);
  gap: 16px;
  flex-wrap: wrap;
}
.table-title-group {
  display: flex;
  align-items: center;
  gap: 12px;
}
.table-title {
  font-size: 18px;
  font-weight: 700;
}
.table-badge {
  font-size: 12px;
  font-weight: 600;
  color: var(--accent);
  background: var(--accent-bg);
  padding: 4px 10px;
  border-radius: 999px;
}
.table-controls { display: flex; gap: 8px; align-items: center; }
.select-group { display: flex; gap: 8px; }
.select {
  background: var(--bg);
  color: var(--text-primary);
  border: 1px solid var(--border);
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 500;
  font-family: var(--font);
  cursor: pointer;
  transition: border-color 0.2s;
}
.select:focus { border-color: var(--accent); outline: none; }

.table-wrap { overflow-x: auto; }
.data-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 900px;
}
.data-table th {
  padding: 12px 20px;
  text-align: left;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  background: var(--bg);
  border-bottom: 1px solid var(--border-light);
  white-space: nowrap;
}
.data-table td {
  padding: 14px 20px;
  border-bottom: 1px solid var(--border-light);
  font-size: 14px;
  vertical-align: middle;
}
.data-table tr:last-child td { border-bottom: none; }
.data-table tr:hover td { background: var(--bg-card-hover); }
.date-cell { color: var(--text-secondary); font-weight: 500; white-space: nowrap; }
.draw-num-cell { font-weight: 600; font-variant-numeric: tabular-nums; }
.amount-cell { color: var(--text-secondary); font-variant-numeric: tabular-nums; }
.prize-cell { color: var(--text-secondary); max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.text-muted { color: var(--text-muted); }
.detail-cell { max-width: 280px; }
.detail-content { display: flex; flex-direction: column; gap: 6px; }
.detail-text { color: var(--text-secondary); font-size: 13px; }
.detail-links { display: flex; flex-wrap: wrap; gap: 6px; }
.detail-link {
  font-size: 12px;
  font-weight: 600;
  color: var(--accent);
  text-decoration: none;
  padding: 3px 10px;
  background: var(--accent-bg);
  border-radius: 999px;
  transition: all 0.2s;
}
.detail-link:hover { background: var(--accent); color: #fff; }
.empty-row { padding: 0; }
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 48px 20px;
  color: var(--text-muted);
  font-size: 14px;
}
.empty-icon { font-size: 32px; }

/* ========== Pagination ========== */
.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-top: 1px solid var(--border-light);
}
.page-info {
  font-size: 13px;
  color: var(--text-muted);
  font-weight: 500;
}
.page-btns { display: flex; gap: 8px; }
.page-btn {
  padding: 8px 16px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  font-family: var(--font);
}
.page-btn:hover:not(:disabled) { border-color: var(--accent); color: var(--accent); }
.page-btn:disabled { opacity: 0.4; cursor: not-allowed; }

/* ========== Empty Hero ========== */
.empty-hero {
  text-align: center;
  padding: 80px 20px;
  color: var(--text-secondary);
}
.empty-hero-icon { font-size: 48px; display: block; margin-bottom: 16px; }
.empty-hero h3 { font-size: 20px; font-weight: 700; color: var(--text-primary); margin-bottom: 8px; }
.empty-hero p { font-size: 14px; max-width: 400px; margin: 0 auto; }

/* ========== Responsive ========== */
@media (max-width: 768px) {
  .page-hero { flex-direction: column; align-items: flex-start; }
  .hero-stats { gap: 16px; }
  .lottery-grid { grid-template-columns: 1fr; }
  .table-header { flex-direction: column; align-items: flex-start; }
  .pagination { flex-direction: column; gap: 12px; }
}
</style>
