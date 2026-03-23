<template>
  <div class="trend">
    <div class="page-header">
      <h2>📈 趋势分析</h2>
      <div class="selector">
        <select v-model="selectedType" @change="loadTrend">
          <option value="ssq">双色球</option>
          <option value="dlt">大乐透</option>
        </select>
        <select v-model="recentN" @change="loadTrend">
          <option :value="20">最近 20 期</option>
          <option :value="30">最近 30 期</option>
          <option :value="50">最近 50 期</option>
          <option :value="100">最近 100 期</option>
        </select>
      </div>
    </div>

    <div v-if="trendData?.trend?.length" class="charts-stack">
      <div class="chart-card">
        <h3>和值走势</h3>
        <div ref="sumChart" class="chart"></div>
      </div>
      <div class="chart-card">
        <h3>奇偶 / 大小走势</h3>
        <div ref="ratioChart" class="chart"></div>
      </div>
      <div class="chart-card" v-if="selectedType === 'ssq'">
        <h3>三区分布走势</h3>
        <div ref="zoneChart" class="chart"></div>
      </div>
    </div>

    <div v-else-if="trendData?.info" class="info-msg">{{ trendData.info }}</div>
    <div v-else class="empty">选择彩种加载趋势数据</div>
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
    tooltip: { trigger: 'axis' },
    legend: { data: series.map(s => s.name), textStyle: { color: '#667788' }, top: 0 },
    grid: { left: 40, right: 20, top: 36, bottom: 30 },
    xAxis: { type: 'category', data: xData, axisLabel: { color: '#667788', fontSize: 10, rotate: 45 }, axisLine: { lineStyle: { color: '#2a4a6a' } } },
    yAxis: { type: 'value', axisLabel: { color: '#667788' }, splitLine: { lineStyle: { color: '#1f2f3f' } } },
    series: series.map((s, i) => ({
      ...s, type: 'line', smooth: true, symbol: 'circle', symbolSize: 4,
      lineStyle: { width: 2, color: colors[i % colors.length] },
      itemStyle: { color: colors[i % colors.length] },
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
      { name: '和值', data: data.trend.map(t => t.sum) },
      { name: '平均线', data: data.trend.map(() => 102), lineStyle: { type: 'dashed' } },
    ], ['#4a9eff', '#ff4757'])
    makeLine(ratioChart.value, xData, [
      { name: '奇数个数', data: data.trend.map(t => t.oddCount) },
      { name: '大号个数', data: data.trend.map(t => t.bigCount) },
    ], ['#ffa502', '#2ed573'])
    makeLine(zoneChart.value, xData, [
      { name: '一区(1-11)', data: data.trend.map(t => t.zone1) },
      { name: '二区(12-22)', data: data.trend.map(t => t.zone2) },
      { name: '三区(23-33)', data: data.trend.map(t => t.zone3) },
    ], ['#ff6b6b', '#4a9eff', '#ffa502'])
  } else if (t === 'dlt') {
    makeLine(sumChart.value, xData, [
      { name: '前区和值', data: data.trend.map(t => t.frontSum) },
      { name: '后区和值', data: data.trend.map(t => t.backSum) },
    ], ['#4a9eff', '#ffa502'])
    makeLine(ratioChart.value, xData, [
      { name: '前区奇数', data: data.trend.map(t => t.frontOdd) },
    ], ['#2ed573'])
  }
}

onMounted(() => loadTrend())
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.page-header h2 { font-size: 24px; }
.selector { display: flex; gap: 8px; }
select { background: #1a2a3a; color: #e0e6ed; border: 1px solid #2a4a6a; padding: 8px 12px; border-radius: 8px; }
.charts-stack { display: flex; flex-direction: column; gap: 16px; }
.chart-card { background: #1a2a3a; border: 1px solid #2a4a6a; border-radius: 12px; padding: 20px; }
.chart-card h3 { font-size: 14px; color: #8899aa; margin-bottom: 12px; }
.chart { width: 100%; height: 300px; }
.empty, .info-msg { text-align: center; padding: 80px 20px; color: #667788; font-size: 16px; }
</style>
