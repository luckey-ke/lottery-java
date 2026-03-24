<template>
  <div class="trend-page">
    <div class="page-hero">
      <div>
        <h1 class="page-title">趋势分析</h1>
        <p class="page-subtitle">和值走势、奇偶比变化、三区分布等可视化趋势</p>
      </div>
      <div class="hero-controls">
        <div class="type-pills">
          <button
            :class="['type-pill', { active: selectedType === 'ssq' }]"
            @click="selectedType = 'ssq'"
          >🔴 双色球</button>
          <button
            :class="['type-pill', { active: selectedType === 'dlt' }]"
            @click="selectedType = 'dlt'"
          >🟡 大乐透</button>
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
      <div class="chart-panel">
        <div class="chart-header">
          <h3>📈 和值走势</h3>
          <span class="chart-hint">红线为平均值参考</span>
        </div>
        <div ref="sumChart" class="chart-area"></div>
      </div>
      <div class="chart-panel">
        <div class="chart-header">
          <h3>⚖️ 奇偶 / 大小走势</h3>
        </div>
        <div ref="ratioChart" class="chart-area"></div>
      </div>
      <div class="chart-panel" v-if="selectedType === 'ssq'">
        <div class="chart-header">
          <h3>🎯 三区分布走势</h3>
          <span class="chart-hint">一区(1-11) / 二区(12-22) / 三区(23-33)</span>
        </div>
        <div ref="zoneChart" class="chart-area"></div>
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
import { ref, onMounted, nextTick, watch } from 'vue'
import * as echarts from 'echarts'
import api from '../api'

const selectedType = ref('ssq')
const recentN = ref(30)
const trendData = ref(null)
const sumChart = ref(null)
const ratioChart = ref(null)
const zoneChart = ref(null)

let charts = []
function dispose() { charts.forEach(c => c?.dispose()); charts = [] }

function makeLine(el, xData, series, colors) {
  if (!el) return
  const chart = echarts.init(el)
  charts.push(chart)
  chart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#fff',
      borderColor: '#e8ecf1',
      textStyle: { color: '#1a1d23', fontSize: 12 },
      borderRadius: 8,
    },
    legend: {
      data: series.map(s => s.name),
      textStyle: { color: '#6b7280', fontSize: 12 },
      top: 0, icon: 'roundRect', itemWidth: 16, itemHeight: 4,
    },
    grid: { left: 48, right: 20, top: 40, bottom: 36 },
    xAxis: {
      type: 'category', data: xData,
      axisLabel: { color: '#9ca3af', fontSize: 10, rotate: 45 },
      axisLine: { lineStyle: { color: '#e8ecf1' } },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#9ca3af' },
      splitLine: { lineStyle: { color: '#f0f2f5' } },
      axisLine: { show: false },
    },
    series: series.map((s, i) => ({
      ...s, type: 'line', smooth: true,
      symbol: 'circle', symbolSize: 6,
      lineStyle: { width: 2.5, color: colors[i % colors.length] },
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

  const xData = data.trend.map(t => t.drawNum?.slice(-3) || t.drawDate)
  const t = selectedType.value

  if (t === 'ssq') {
    makeLine(sumChart.value, xData, [
      { name: '和值', data: data.trend.map(t => t.sum), area: true },
      { name: '平均线', data: data.trend.map(() => 102), lineStyle: { type: 'dashed', width: 1.5 } },
    ], ['#6366f1', '#ef4444'])
    makeLine(ratioChart.value, xData, [
      { name: '奇数个数', data: data.trend.map(t => t.oddCount), area: true },
      { name: '大号个数', data: data.trend.map(t => t.bigCount), area: true },
    ], ['#f59e0b', '#10b981'])
    makeLine(zoneChart.value, xData, [
      { name: '一区(1-11)', data: data.trend.map(t => t.zone1), area: true },
      { name: '二区(12-22)', data: data.trend.map(t => t.zone2), area: true },
      { name: '三区(23-33)', data: data.trend.map(t => t.zone3), area: true },
    ], ['#ef4444', '#3b82f6', '#f59e0b'])
  } else if (t === 'dlt') {
    makeLine(sumChart.value, xData, [
      { name: '前区和值', data: data.trend.map(t => t.frontSum), area: true },
      { name: '后区和值', data: data.trend.map(t => t.backSum), area: true },
    ], ['#6366f1', '#f59e0b'])
    makeLine(ratioChart.value, xData, [
      { name: '前区奇数', data: data.trend.map(t => t.frontOdd), area: true },
    ], ['#10b981'])
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
.type-pills { display: flex; gap: 6px; }
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

.empty-hero {
  text-align: center; padding: 80px 20px; color: var(--text-secondary);
}
.empty-icon { font-size: 48px; display: block; margin-bottom: 16px; }
.empty-hero h3 { font-size: 20px; font-weight: 700; color: var(--text-primary); margin-bottom: 8px; }
.empty-hero p { font-size: 14px; }

@media (max-width: 768px) {
  .page-hero { flex-direction: column; align-items: flex-start; }
}
</style>
