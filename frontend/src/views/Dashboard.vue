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
          <div class="stat">
            <span class="stat-value">{{ info.count }}</span>
            <span class="stat-label">期数据</span>
          </div>
          <div class="stat">
            <span class="stat-value">{{ info.latestDraw || '-' }}</span>
            <span class="stat-label">最新期号</span>
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
      <table class="table">
        <thead>
          <tr><th>期号</th><th>开奖日期</th><th>开奖号码</th></tr>
        </thead>
        <tbody>
          <tr v-for="r in results" :key="r.id">
            <td>{{ r.drawNum }}</td>
            <td>{{ r.drawDate }}</td>
            <td class="numbers">
              <span v-for="(n, i) in parseNumbers(r.numbers)" :key="i" :class="['ball', n.type]">{{ n.value }}</span>
            </td>
          </tr>
          <tr v-if="!results.length">
            <td colspan="3" class="empty-cell">暂无开奖记录</td>
          </tr>
        </tbody>
      </table>
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

const selectedName = computed(() => status.value[selectedType.value]?.name || '')
const totalPages = computed(() => Math.max(1, Math.ceil(totalResults.value / pageSize.value)))

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
  results.value = data.data || []
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
.page-header h2 { font-size: 24px; }
.btn { padding: 10px 20px; border: none; border-radius: 8px; cursor: pointer; font-size: 14px; font-weight: 600; transition: all .2s; }
.btn:disabled { opacity: .5; cursor: not-allowed; }
.btn-secondary { background: #2a3a4a; color: #c7d2dd; border: 1px solid #3a4a5a; }
.btn-secondary:hover:not(:disabled) { background: #33485d; color: #fff; }
.status-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 16px; margin-bottom: 32px; }
.card { background: linear-gradient(145deg, #1a2a3a, #0f1f2f); border: 1px solid #2a4a6a; border-radius: 12px; padding: 20px; transition: all .3s; }
.card:hover { border-color: #4a9eff; transform: translateY(-2px); box-shadow: 0 8px 24px rgba(0,0,0,.3); }
.card-header { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; }
.card-icon { font-size: 24px; }
.card-name { font-size: 16px; font-weight: 600; flex: 1; }
.card-code { font-size: 11px; color: #4a9eff; background: rgba(74,158,255,.1); padding: 2px 8px; border-radius: 4px; }
.card-body { display: flex; gap: 20px; }
.stat { display: flex; flex-direction: column; }
.stat-value { font-size: 28px; font-weight: 700; color: #4a9eff; }
.stat-label { font-size: 12px; color: #667788; margin-top: 2px; }
.results-section { margin-top: 24px; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.section-header h3 { font-size: 18px; }
.section-controls { display: flex; gap: 8px; align-items: center; }
select { background: #1a2a3a; color: #e0e6ed; border: 1px solid #2a4a6a; padding: 8px 12px; border-radius: 8px; font-size: 14px; }
.table { width: 100%; border-collapse: collapse; background: #1a2a3a; border-radius: 12px; overflow: hidden; }
.table th { background: #0f1923; padding: 12px 16px; text-align: left; font-size: 13px; color: #667788; text-transform: uppercase; }
.table td { padding: 12px 16px; border-top: 1px solid #1f2f3f; }
.table tr:hover td { background: rgba(74,158,255,.05); }
.empty-cell { text-align: center; color: #8899aa; }
.numbers { display: flex; gap: 4px; flex-wrap: wrap; }
.ball { display: inline-flex; align-items: center; justify-content: center; width: 30px; height: 30px; border-radius: 50%; font-size: 13px; font-weight: 600; }
.ball.red { background: linear-gradient(135deg, #ff4757, #c0392b); color: #fff; }
.ball.blue { background: linear-gradient(135deg, #4a9eff, #2575d6); color: #fff; }
.ball.gray { background: #2a3a4a; color: #8899aa; border: 1px solid #3a4a5a; }
.pagination { display: flex; justify-content: flex-end; align-items: center; gap: 12px; margin-top: 16px; }
.pagination-info { color: #8899aa; font-size: 14px; }
.msg { margin-top: 16px; padding: 12px 20px; border-radius: 8px; font-size: 14px; }
.admin-hint { margin-top: 16px; padding: 12px 16px; border: 1px solid rgba(74,158,255,.25); border-radius: 8px; color: #9ab6d3; background: rgba(74,158,255,.06); }
</style>
