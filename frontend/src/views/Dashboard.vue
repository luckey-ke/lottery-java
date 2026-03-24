<template>
  <div class="dashboard">
    <div class="page-header">
      <h2>数据总览</h2>
    </div>

    <div class="status-grid">
      <div class="card" v-for="(info, key) in status" :key="key">
        <div class="card-header">
          <span class="card-icon">{{ icons[key] || '🎱' }}</span>
          <span class="card-name">{{ info.name }}</span>
          <span class="card-code">{{ key.toUpperCase() }}</span>
        </div>
        <div class="card-body">
          <div class="card-meta-row">
            <div class="draw-block">
              <span class="draw-label">最新开奖</span>
              <span :class="['draw-value', getStatValueClass(info.latestDraw || '-') ]">{{ info.latestDraw || '-' }}</span>
            </div>
            <div class="count-pill">
              <span class="count-pill-value">{{ info.count }}</span>
              <span class="count-pill-label">期数据</span>
            </div>
          </div>
          <div class="latest-numbers-row">
            <span class="latest-numbers-label">中奖号码</span>
            <div v-if="parseNumbers(info.latestNumbers).length" class="numbers summary-numbers">
              <span v-for="(n, i) in parseNumbers(info.latestNumbers)" :key="i" :class="['ball', 'summary-ball', n.type]">{{ n.value }}</span>
            </div>
            <span v-else class="latest-numbers-empty">-</span>
          </div>
        </div>
      </div>
    </div>

    <div class="results-section" v-if="selectedType">
      <div class="section-header">
        <h3>{{ selectedName }} 最新开奖</h3>
        <div class="section-controls">
          <select v-model="selectedType" @change="handleTypeChange">
            <option v-for="(info, key) in status" :key="key" :value="key">{{ info.name }}</option>
          </select>
          <select v-model="pageSize" @change="handlePageSizeChange">
            <option v-for="size in pageSizeOptions" :key="size" :value="size">每页 {{ size }} 条</option>
          </select>
        </div>
      </div>
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>开奖日期</th>
              <th>期号</th>
              <th>中奖号码</th>
              <th>销售额(元)</th>
              <th>一等奖</th>
              <th>二等奖</th>
              <th>详细</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in results" :key="r.id">
              <td>{{ r.drawDate }}</td>
              <td>{{ r.drawNum }}</td>
              <td class="numbers-cell">
                <div class="numbers">
                  <span v-for="(n, i) in parseNumbers(r.numbers)" :key="i" :class="['ball', n.type]">{{ n.value }}</span>
                </div>
              </td>
              <td>{{ getSalesAmountText(r) }}</td>
              <td>{{ getFirstPrizeText(r) }}</td>
              <td>{{ getSecondPrizeText(r) }}</td>
              <td class="detail-cell">
                <div v-if="hasDetailContent(r)" class="detail-content">
                  <div v-if="getDetailText(r) !== '-'" class="detail-text">{{ getDetailText(r) }}</div>
                  <div v-if="getDetailLinks(r).length" class="detail-links">
                    <a
                      v-for="link in getDetailLinks(r)"
                      :key="link.label + link.url"
                      :href="link.url"
                      class="detail-link-btn"
                      target="_blank"
                      rel="noopener noreferrer"
                    >
                      {{ link.label }}
                    </a>
                  </div>
                </div>
                <span v-else>-</span>
              </td>
            </tr>
            <tr v-if="!results.length">
              <td colspan="7" class="empty-cell">暂无开奖记录</td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="pagination" v-if="totalResults > 0">
        <span class="pagination-info">共 {{ totalResults }} 条，第 {{ currentPage }} / {{ totalPages }} 页</span>
        <button class="btn btn-secondary" @click="goToPrevPage" :disabled="currentPage <= 1">上一页</button>
        <button class="btn btn-secondary" @click="goToNextPage" :disabled="currentPage >= totalPages">下一页</button>
      </div>
    </div>

    <div class="msg" v-else-if="!Object.keys(status).length">暂无数据</div>
    <div class="admin-hint">抓取数据请前往“管理后台”。</div>
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

function getStatValueClass(value) {
  const text = String(value ?? '').trim()
  if (text.length >= 18) return 'draw-value-xxs'
  if (text.length >= 14) return 'draw-value-xs'
  if (text.length >= 10) return 'draw-value-sm'
  return ''
}

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
  try {
    const parsed = JSON.parse(raw)
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    return {}
  }
}

function normalizeResult(item) {
  return {
    ...item,
    parsedExtraInfo: parseExtraInfo(item.extraInfo),
    rawExtraInfoText: typeof item.extraInfo === 'string' ? item.extraInfo.trim() : '',
  }
}

function pickExtraValue(info, keys) {
  for (const key of keys) {
    const value = info?.[key]
    if (value !== undefined && value !== null && value !== '') {
      return value
    }
  }
  return ''
}

function formatExtraValue(value) {
  if (value === undefined || value === null || value === '') return '-'
  if (Array.isArray(value)) {
    const text = value.map(item => formatExtraValue(item)).filter(item => item !== '-').join('，')
    return text || '-'
  }
  if (typeof value === 'object') {
    const text = Object.entries(value)
      .filter(([, item]) => item !== undefined && item !== null && item !== '')
      .map(([key, item]) => `${key}:${item}`)
      .join('；')
    return text || '-'
  }
  return String(value)
}

function getExtraText(row, keys) {
  return formatExtraValue(pickExtraValue(row.parsedExtraInfo, keys))
}

function getSalesAmountText(row) {
  return getExtraText(row, salesAmountKeys)
}

function getFirstPrizeText(row) {
  return getExtraText(row, firstPrizeKeys)
}

function getSecondPrizeText(row) {
  return getExtraText(row, secondPrizeKeys)
}

function getDetailValue(row) {
  return pickExtraValue(row.parsedExtraInfo, detailKeys)
}

function getDetailText(row) {
  const detailValue = getDetailValue(row)
  if (detailValue && typeof detailValue === 'object' && !Array.isArray(detailValue)) {
    const text = formatExtraValue(detailValue.text)
    return text === '-' ? '-' : text
  }
  const detailText = formatExtraValue(detailValue)
  if (detailText !== '-') {
    return detailText
  }
  return row.rawExtraInfoText || '-'
}

function getDetailLinks(row) {
  const detailValue = getDetailValue(row)
  const links = detailValue && typeof detailValue === 'object' && !Array.isArray(detailValue)
    ? detailValue.links
    : null
  if (!links || typeof links !== 'object') {
    return []
  }
  return Object.entries(links)
    .filter(([, url]) => typeof url === 'string' && url.trim())
    .map(([label, url]) => ({ label, url: url.trim() }))
}

function hasDetailContent(row) {
  return getDetailText(row) !== '-' || getDetailLinks(row).length > 0
}

async function loadStatus() {
  const { data } = await api.status()
  status.value = data
  if (!status.value[selectedType.value]) {
    selectedType.value = Object.keys(status.value)[0] || ''
  }
}

async function loadResults() {
  if (!selectedType.value) {
    results.value = []
    totalResults.value = 0
    return
  }

  const offset = (currentPage.value - 1) * pageSize.value
  const { data } = await api.results(selectedType.value, pageSize.value, offset)
  results.value = (data.data || []).map(normalizeResult)
  totalResults.value = data.total || 0

  if (totalResults.value > 0 && currentPage.value > totalPages.value) {
    currentPage.value = totalPages.value
    return loadResults()
  }
}

async function handleTypeChange() {
  currentPage.value = 1
  await loadResults()
}

async function handlePageSizeChange() {
  currentPage.value = 1
  await loadResults()
}

async function goToPrevPage() {
  if (currentPage.value <= 1) return
  currentPage.value--
  await loadResults()
}

async function goToNextPage() {
  if (currentPage.value >= totalPages.value) return
  currentPage.value++
  await loadResults()
}

onMounted(async () => {
  await loadStatus()
  await loadResults()
})
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.page-header h2 { font-size: 24px; line-height: 1.3; white-space: nowrap; }
.btn { padding: 10px 20px; border: none; border-radius: 8px; cursor: pointer; font-size: 14px; font-weight: 600; transition: all .2s; }
.btn:disabled { opacity: .5; cursor: not-allowed; }
.btn-secondary { background: #2a3a4a; color: #c7d2dd; border: 1px solid #3a4a5a; }
.btn-secondary:hover:not(:disabled) { background: #33485d; color: #fff; }
.status-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 14px; margin-bottom: 28px; }
.card { background: linear-gradient(145deg, #1a2a3a, #0f1f2f); border: 1px solid #2a4a6a; border-radius: 14px; padding: 16px 18px; transition: all .25s; }
.card:hover { border-color: #63adff; transform: translateY(-2px); box-shadow: 0 10px 26px rgba(0,0,0,.28); }
.card-header { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; flex-wrap: nowrap; }
.card-icon { font-size: 22px; flex-shrink: 0; }
.card-name { font-size: 15px; font-weight: 600; flex: 1; min-width: 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; color: #edf4fb; }
.card-code { font-size: 11px; color: #7db7ff; background: rgba(74,158,255,.12); padding: 3px 8px; border-radius: 999px; flex-shrink: 0; }
.card-body { display: flex; flex-direction: column; gap: 12px; }
.card-meta-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.draw-block { display: flex; flex-direction: column; min-width: 0; gap: 4px; }
.draw-label { font-size: 12px; color: #7f95ac; white-space: nowrap; }
.draw-value { font-size: 24px; font-weight: 700; color: #4a9eff; line-height: 1.2; white-space: nowrap; overflow: hidden; text-overflow: clip; font-variant-numeric: tabular-nums; }
.draw-value.draw-value-sm { font-size: 20px; }
.draw-value.draw-value-xs { font-size: 17px; }
.draw-value.draw-value-xxs { font-size: 15px; }
.count-pill { display: inline-flex; flex-direction: column; align-items: flex-end; justify-content: center; gap: 2px; padding: 8px 10px; border-radius: 12px; background: rgba(74,158,255,.1); border: 1px solid rgba(74,158,255,.18); flex-shrink: 0; }
.count-pill-value { font-size: 18px; line-height: 1; font-weight: 700; color: #d8ebff; font-variant-numeric: tabular-nums; white-space: nowrap; }
.count-pill-label { font-size: 11px; color: #89a8c6; white-space: nowrap; }
.latest-numbers-row { display: flex; flex-direction: column; gap: 8px; min-width: 0; }
.latest-numbers-label { font-size: 12px; color: #7f95ac; white-space: nowrap; }
.summary-numbers { gap: 6px; }
.summary-ball { width: 24px; height: 24px; font-size: 11px; box-shadow: inset 0 0 0 1px rgba(255,255,255,.04); }
.latest-numbers-empty { color: #6e8398; font-size: 13px; }
.results-section { margin-top: 24px; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.section-header h3 { font-size: 18px; }
.section-controls { display: flex; gap: 8px; align-items: center; }
select { background: #1a2a3a; color: #e0e6ed; border: 1px solid #2a4a6a; padding: 8px 12px; border-radius: 8px; font-size: 14px; }
.table-wrap { overflow-x: auto; }
.table { width: 100%; min-width: 1080px; border-collapse: collapse; background: #1a2a3a; border-radius: 12px; overflow: hidden; }
.table th { background: #0f1923; padding: 12px 16px; text-align: left; font-size: 13px; color: #667788; text-transform: uppercase; white-space: nowrap; }
.table td { padding: 12px 16px; border-top: 1px solid #1f2f3f; vertical-align: top; }
.table tr:hover td { background: rgba(74,158,255,.05); }
.empty-cell { text-align: center; color: #8899aa; }
.numbers-cell { min-width: 210px; }
.numbers { display: flex; gap: 4px; flex-wrap: wrap; }
.ball { display: inline-flex; align-items: center; justify-content: center; width: 30px; height: 30px; border-radius: 50%; font-size: 13px; font-weight: 600; }
.ball.red { background: linear-gradient(135deg, #ff4757, #c0392b); color: #fff; }
.ball.blue { background: linear-gradient(135deg, #4a9eff, #2575d6); color: #fff; }
.ball.gray { background: #2a3a4a; color: #8899aa; border: 1px solid #3a4a5a; }
.detail-cell { max-width: 320px; color: #9ab6d3; word-break: break-word; white-space: normal; }
.detail-content { display: flex; flex-direction: column; gap: 8px; }
.detail-text { color: #9ab6d3; }
.detail-links { display: flex; flex-wrap: wrap; gap: 8px; }
.detail-link-btn { display: inline-flex; align-items: center; justify-content: center; padding: 4px 10px; border-radius: 999px; border: 1px solid rgba(74,158,255,.4); background: rgba(74,158,255,.12); color: #7db7ff; text-decoration: none; font-size: 12px; line-height: 1.4; }
.detail-link-btn:hover { background: rgba(74,158,255,.2); color: #fff; }
.pagination { display: flex; justify-content: flex-end; align-items: center; gap: 12px; margin-top: 16px; }
.pagination-info { color: #8899aa; font-size: 14px; }
.msg { margin-top: 16px; padding: 12px 20px; border-radius: 8px; font-size: 14px; }
.admin-hint { margin-top: 16px; padding: 12px 16px; border: 1px solid rgba(74,158,255,.25); border-radius: 8px; color: #9ab6d3; background: rgba(74,158,255,.06); }
@media (max-width: 640px) {
  .page-header h2 { font-size: 22px; }
  .status-grid { grid-template-columns: 1fr; }
  .card { padding: 15px 16px; }
  .card-meta-row { align-items: flex-start; gap: 10px; }
  .draw-value { font-size: 20px; }
  .draw-value.draw-value-sm { font-size: 18px; }
  .draw-value.draw-value-xs { font-size: 16px; }
  .draw-value.draw-value-xxs { font-size: 14px; }
  .count-pill { padding: 7px 9px; }
  .summary-ball { width: 22px; height: 22px; font-size: 10px; }
}
</style>
