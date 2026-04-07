<template>
  <div class="trend-page">
    <div class="page-hero">
      <div>
        <h1 class="page-title">趋势分析</h1>
        <p class="page-subtitle">和值、跨度、AC值、连号、重号、奇偶比、区间分布等趋势可视化</p>
      </div>
      <div class="hero-controls">
        <div class="type-pills">
          <button
            v-for="t in types"
            :key="t.code"
            :class="['type-pill', { active: selectedType === t.code }]"
            @click="selectedType = t.code"
          >{{ t.icon }} {{ t.name }}</button>
        </div>
        <select v-model="recentN" @change="loadTrend" class="select">
          <option :value="20">最近 20 期</option>
          <option :value="30">最近 30 期</option>
          <option :value="50">最近 50 期</option>
          <option :value="100">最近 100 期</option>
        </select>
      </div>
    </div>

    <div v-if="trendData?.trend?.length" class="charts-stack">
      <!-- 双色球 -->
      <template v-if="selectedType === 'ssq'">
        <div class="chart-panel">
          <div class="chart-header">
            <h3>📈 红球和值走势</h3>
            <span class="chart-hint">红线为平均值参考</span>
          </div>
          <div ref="sumChart" class="chart-area"></div>
        </div>
        <div class="chart-grid">
          <div class="chart-panel">
            <div class="chart-header"><h3>📏 跨度走势</h3></div>
            <div ref="spanChart" class="chart-area"></div>
          </div>
          <div class="chart-panel">
            <div class="chart-header"><h3>🧮 AC值走势</h3></div>
            <div ref="acChart" class="chart-area"></div>
          </div>
        </div>
        <div class="chart-grid">
          <div class="chart-panel">
            <div class="chart-header"><h3>⚖️ 奇数 / 大号 / 质数个数</h3></div>
            <div ref="ratioChart" class="chart-area"></div>
          </div>
          <div class="chart-panel">
            <div class="chart-header"><h3>🔵 蓝球走势</h3></div>
            <div ref="blueChart" class="chart-area"></div>
          </div>
        </div>
        <div class="chart-grid">
          <div class="chart-panel">
            <div class="chart-header"><h3>🔗 连号 / 重号走势</h3></div>
            <div ref="consecChart" class="chart-area"></div>
          </div>
          <div class="chart-panel">
            <div class="chart-header">
              <h3>🎯 三区分布走势</h3>
              <span class="chart-hint">一区(1-11) / 二区(12-22) / 三区(23-33)</span>
            </div>
            <div ref="zoneChart" class="chart-area"></div>
          </div>
        </div>
      </template>

      <!-- 大乐透 -->
      <template v-if="selectedType === 'dlt'">
        <div class="chart-grid">
          <div class="chart-panel">
            <div class="chart-header"><h3>📈 前区和值走势</h3></div>
            <div ref="sumChart" class="chart-area"></div>
          </div>
          <div class="chart-panel">
            <div class="chart-header"><h3>🔵 后区和值走势</h3></div>
            <div ref="backSumChart" class="chart-area"></div>
          </div>
        </div>
        <div class="chart-grid">
          <div class="chart-panel">
            <div class="chart-header"><h3>📏 前区跨度走势</h3></div>
            <div ref="spanChart" class="chart-area"></div>
          </div>
          <div class="chart-panel">
            <div class="chart-header"><h3>🧮 前区AC值走势</h3></div>
            <div ref="acChart" class="chart-area"></div>
          </div>
        </div>
        <div class="chart-grid">
          <div class="chart-panel">
            <div class="chart-header"><h3>⚖️ 奇数 / 质数个数走势</h3></div>
            <div ref="ratioChart" class="chart-area"></div>
          </div>
          <div class="chart-panel">
            <div class="chart-header"><h3>🔁 重号走势</h3></div>
            <div ref="consecChart" class="chart-area"></div>
          </div>
        </div>
      </template>

      <!-- 位置型: 福彩3D/排列三/排列五 -->
      <template v-if="['fc3d','pl3','pl5'].includes(selectedType)">
        <div class="chart-grid">
          <div class="chart-panel">
            <div class="chart-header"><h3>📈 和值走势</h3></div>
            <div ref="sumChart" class="chart-area"></div>
          </div>
          <div class="chart-panel">
            <div class="chart-header"><h3>📏 跨度走势</h3></div>
            <div ref="spanChart" class="chart-area"></div>
          </div>
        </div>
        <div class="chart-grid">
          <div class="chart-panel">
            <div class="chart-header"><h3>⚖️ 奇偶比走势</h3></div>
            <div ref="oddEvenChart" class="chart-area"></div>
          </div>
          <div class="chart-panel">
            <div class="chart-header"><h3>🔁 重号走势</h3></div>
            <div ref="consecChart" class="chart-area"></div>
          </div>
        </div>
        <!-- 各位号码走势 -->
        <div class="chart-panel" v-for="pos in positionCount" :key="pos">
          <div class="chart-header"><h3>📍 第 {{ pos }} 位号码走势</h3></div>
          <div :ref="el => posTrendCharts[pos - 1] = el" class="chart-area"></div>
        </div>
      </template>

      <!-- 七乐彩 -->
      <template v-if="selectedType === 'qlc'">
        <div class="chart-grid">
          <div class="chart-panel">
            <div class="chart-header"><h3>📈 和值走势</h3></div>
            <div ref="sumChart" class="chart-area"></div>
          </div>
          <div class="chart-panel">
            <div class="chart-header"><h3>📏 跨度走势</h3></div>
            <div ref="spanChart" class="chart-area"></div>
          </div>
        </div>
        <div class="chart-grid">
          <div class="chart-panel">
            <div class="chart-header"><h3>⚖️ 奇数 / 质数个数走势</h3></div>
            <div ref="ratioChart" class="chart-area"></div>
          </div>
          <div class="chart-panel">
            <div class="chart-header"><h3>🔁 重号走势</h3></div>
            <div ref="consecChart" class="chart-area"></div>
          </div>
        </div>
      </template>

      <!-- 开奖号码表 -->
      <div class="chart-panel">
        <div class="chart-header"><h3>📋 近期开奖号码</h3></div>
        <div class="draw-table">
          <div class="draw-row" v-for="item in trendData.trend.slice().reverse()" :key="item.drawNum">
            <span class="draw-date">{{ item.drawDate }}</span>
            <span class="draw-num">{{ item.drawNum }}</span>
            <span class="draw-nums" v-if="item.numbers">{{ item.numbers }}</span>
            <span class="draw-sum" v-if="item.sum != null">和值 {{ item.sum }}</span>
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="trendData?.info" class="empty-hero">
      <span class="empty-icon">ℹ️</span>
      <h3>提示</h3>
      <p>{{ trendData.info }}</p>
    </div>
    <div v-else class="empty-hero">
      <span class="empty-icon">📈</span>
      <h3>加载趋势数据</h3>
      <p>选择彩种和期数范围查看趋势分析</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import * as echarts from 'echarts'
import api from '../api'

const types = [
  { code: 'ssq', name: '双色球', icon: '🔴' },
  { code: 'dlt', name: '大乐透', icon: '🟡' },
  { code: 'fc3d', name: '福彩3D', icon: '🎲' },
  { code: 'pl3', name: '排列三', icon: '🎯' },
  { code: 'pl5', name: '排列五', icon: '🎯' },
  { code: 'qlc', name: '七乐彩', icon: '🎱' },
]

const selectedType = ref('ssq')
const recentN = ref(30)
const trendData = ref(null)
const sumChart = ref(null)
const spanChart = ref(null)
const acChart = ref(null)
const ratioChart = ref(null)
const zoneChart = ref(null)
const blueChart = ref(null)
const backSumChart = ref(null)
const oddEvenChart = ref(null)
const sizeChart = ref(null)
const consecChart = ref(null)
const posTrendCharts = ref([])

const positionCount = computed(() => {
  if (selectedType.value === 'pl5') return 5
  if (['fc3d', 'pl3'].includes(selectedType.value)) return 3
  return 0
})

let charts = []
function dispose() { charts.forEach(c => c?.dispose()); charts = [] }

function makeLine(el, xData, series, colors) {
  if (!el) return
  const chart = echarts.init(el)
  charts.push(chart)
  chart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#fff', borderColor: '#e8ecf1',
      textStyle: { color: '#1a1d23', fontSize: 12 }, borderRadius: 8,
    },
    legend: {
      data: series.map(s => s.name),
      textStyle: { color: '#6b7280', fontSize: 12 },
      top: 0, icon: 'roundRect', itemWidth: 16, itemHeight: 4,
    },
    grid: { left: 48, right: 20, top: 40, bottom: 36 },
    xAxis: {
      type: 'category', data: xData,
      axisLabel: { color: '#9ca3af', fontSize: 10, rotate: xData.length > 40 ? 60 : 45 },
      axisLine: { lineStyle: { color: '#e8ecf1' } }, axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#9ca3af' },
      splitLine: { lineStyle: { color: '#f0f2f5' } }, axisLine: { show: false },
    },
    series: series.map((s, i) => ({
      ...s, type: 'line', smooth: true,
      symbol: 'circle', symbolSize: s.data?.length > 50 ? 3 : 6,
      lineStyle: { width: 2, color: colors[i % colors.length] },
      itemStyle: { color: colors[i % colors.length] },
      areaStyle: s.area ? {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: colors[i % colors.length] + '20' },
          { offset: 1, color: colors[i % colors.length] + '05' },
        ])
      } : undefined,
    }))
  })
}

async function loadTrend() {
  dispose()
  const { data } = await api.trend(selectedType.value, recentN.value)
  trendData.value = data
  if (!data?.trend?.length) return
  await nextTick()
  renderTrend(data)
}

function renderTrend(data) {
  const xData = data.trend.map(t => t.drawNum?.slice(-4) || t.drawDate)
  const t = selectedType.value

  if (t === 'ssq') {
    const avgSum = Math.round(data.trend.reduce((a, b) => a + (b.sum || 0), 0) / data.trend.length)
    makeLine(sumChart.value, xData, [
      { name: '和值', data: data.trend.map(t => t.sum), area: true },
      { name: '平均(' + avgSum + ')', data: data.trend.map(() => avgSum), lineStyle: { type: 'dashed', width: 1.5 } },
    ], ['#6366f1', '#ef4444'])
    makeLine(spanChart.value, xData, [
      { name: '跨度', data: data.trend.map(t => t.span), area: true },
    ], ['#f59e0b'])
    makeLine(acChart.value, xData, [
      { name: 'AC值', data: data.trend.map(t => t.ac), area: true },
    ], ['#10b981'])
    makeLine(ratioChart.value, xData, [
      { name: '奇数个数', data: data.trend.map(t => t.oddCount), area: true },
      { name: '大号个数', data: data.trend.map(t => t.bigCount), area: true },
      { name: '质数个数', data: data.trend.map(t => t.primeCount), area: true },
    ], ['#f59e0b', '#8b5cf6', '#ec4899'])
    makeLine(blueChart.value, xData, [
      { name: '蓝球', data: data.trend.map(t => t.blue), area: true },
    ], ['#3b82f6'])
    makeLine(consecChart.value, xData, [
      { name: '连号', data: data.trend.map(t => t.consecutive), area: true },
      { name: '重号', data: data.trend.map(t => t.repeats ?? 0), area: true },
    ], ['#8b5cf6', '#ef4444'])
    makeLine(zoneChart.value, xData, [
      { name: '一区(1-11)', data: data.trend.map(t => t.zone1), area: true },
      { name: '二区(12-22)', data: data.trend.map(t => t.zone2), area: true },
      { name: '三区(23-33)', data: data.trend.map(t => t.zone3), area: true },
    ], ['#ef4444', '#3b82f6', '#f59e0b'])
  } else if (t === 'dlt') {
    makeLine(sumChart.value, xData, [
      { name: '前区和值', data: data.trend.map(t => t.frontSum), area: true },
    ], ['#6366f1'])
    makeLine(backSumChart.value, xData, [
      { name: '后区和值', data: data.trend.map(t => t.backSum), area: true },
    ], ['#3b82f6'])
    makeLine(spanChart.value, xData, [
      { name: '前区跨度', data: data.trend.map(t => t.frontSpan), area: true },
    ], ['#f59e0b'])
    makeLine(acChart.value, xData, [
      { name: '前区AC值', data: data.trend.map(t => t.frontAC), area: true },
    ], ['#10b981'])
    makeLine(ratioChart.value, xData, [
      { name: '前区奇数', data: data.trend.map(t => t.frontOdd), area: true },
      { name: '前区质数', data: data.trend.map(t => t.frontPrime), area: true },
    ], ['#f59e0b', '#ec4899'])
    makeLine(consecChart.value, xData, [
      { name: '重号', data: data.trend.map(t => t.repeats ?? 0), area: true },
    ], ['#ef4444'])
  } else if (['fc3d', 'pl3', 'pl5'].includes(t)) {
    makeLine(sumChart.value, xData, [
      { name: '和值', data: data.trend.map(t => t.sum), area: true },
    ], ['#6366f1'])
    makeLine(spanChart.value, xData, [
      { name: '跨度', data: data.trend.map(t => t.span), area: true },
    ], ['#f59e0b'])
    if (oddEvenChart.value) makeLine(oddEvenChart.value, xData, [
      { name: '奇数个数', data: data.trend.map(t => t.oddCount), area: true },
    ], ['#f59e0b'])
    if (consecChart.value) makeLine(consecChart.value, xData, [
      { name: '重号', data: data.trend.map(t => t.repeats ?? 0), area: true },
    ], ['#ef4444'])
    const colors = ['#ef4444', '#3b82f6', '#f59e0b', '#10b981', '#8b5cf6']
    for (let i = 0; i < positionCount.value; i++) {
      if (posTrendCharts.value[i]) {
        makeLine(posTrendCharts.value[i], xData, [
          { name: `第${i+1}位`, data: data.trend.map(t => t.positions?.[i] ?? 0), area: true },
        ], [colors[i % colors.length]])
      }
    }
  } else if (t === 'qlc') {
    makeLine(sumChart.value, xData, [
      { name: '和值', data: data.trend.map(t => t.sum), area: true },
    ], ['#8b5cf6'])
    makeLine(spanChart.value, xData, [
      { name: '跨度', data: data.trend.map(t => t.span), area: true },
    ], ['#f59e0b'])
    makeLine(ratioChart.value, xData, [
      { name: '奇数个数', data: data.trend.map(t => t.oddCount), area: true },
      { name: '质数个数', data: data.trend.map(t => t.primeCount), area: true },
    ], ['#f59e0b', '#ec4899'])
    makeLine(consecChart.value, xData, [
      { name: '重号', data: data.trend.map(t => t.repeats ?? 0), area: true },
    ], ['#ef4444'])
  }
}

watch(selectedType, () => loadTrend())
onMounted(() => loadTrend())
</script>

<style scoped>
.page-hero {
  display: flex; justify-content: space-between; align-items: flex-end;
  margin-bottom: 28px; gap: 16px; flex-wrap: wrap;
}
.page-title { font-size: 28px; font-weight: 800; letter-spacing: -0.5px; margin-bottom: 4px; }
.page-subtitle { color: var(--text-secondary); font-size: 14px; }

.hero-controls { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.type-pills { display: flex; gap: 6px; flex-wrap: wrap; }
.type-pill {
  padding: 8px 14px; border-radius: 999px; border: 1px solid var(--border);
  background: var(--bg-card); color: var(--text-secondary);
  font-size: 13px; font-weight: 500; cursor: pointer;
  font-family: var(--font); transition: all 0.2s;
}
.type-pill:hover { border-color: var(--accent); color: var(--accent); }
.type-pill.active {
  background: var(--accent); color: #fff; border-color: var(--accent);
  box-shadow: 0 2px 8px rgba(99,102,241,0.3);
}
.select {
  background: var(--bg-card); color: var(--text-primary); border: 1px solid var(--border);
  padding: 8px 12px; border-radius: var(--radius-sm); font-size: 13px;
  font-weight: 500; font-family: var(--font); cursor: pointer;
}
.select:focus { border-color: var(--accent); outline: none; }

.charts-stack { display: flex; flex-direction: column; gap: 16px; }
.chart-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.chart-panel {
  background: var(--bg-card); border-radius: var(--radius-lg);
  padding: 24px; box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-light);
}
.chart-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px;
}
.chart-header h3 { font-size: 15px; font-weight: 600; }
.chart-hint { font-size: 12px; color: var(--text-muted); }
.chart-area { width: 100%; height: 300px; }

/* Draw Table */
.draw-table {
  max-height: 400px; overflow-y: auto;
  display: flex; flex-direction: column; gap: 4px;
}
.draw-row {
  display: flex; align-items: center; gap: 16px; padding: 8px 12px;
  border-radius: var(--radius-sm);
  font-size: 13px;
}
.draw-row:nth-child(odd) { background: var(--bg); }
.draw-date { color: var(--text-muted); min-width: 80px; font-size: 12px; }
.draw-num { font-weight: 600; color: var(--text-secondary); min-width: 60px; font-family: monospace; }
.draw-nums { font-weight: 700; color: var(--text-primary); font-family: monospace; letter-spacing: 0.5px; }
.draw-sum { color: var(--accent); font-weight: 600; font-size: 12px; margin-left: auto; }

.empty-hero {
  text-align: center; padding: 80px 20px; color: var(--text-secondary);
}
.empty-icon { font-size: 48px; display: block; margin-bottom: 16px; }
.empty-hero h3 { font-size: 20px; font-weight: 700; color: var(--text-primary); margin-bottom: 8px; }
.empty-hero p { font-size: 14px; }

@media (max-width: 768px) {
  .page-hero { flex-direction: column; align-items: flex-start; }
  .chart-grid { grid-template-columns: 1fr; }
}
</style>
