<template>
  <div class="analysis">
    <div class="page-header">
      <h2>📊 统计分析</h2>
      <div class="selector">
        <select v-model="selectedType" @change="loadAnalysis">
          <option v-for="t in types" :key="t.code" :value="t.code">{{ t.name }}</option>
        </select>
        <button class="btn btn-primary" @click="loadAnalysis" :disabled="loading">
          {{ loading ? '分析中...' : '🔄 刷新' }}
        </button>
      </div>
    </div>

    <div v-if="analysis && !analysis.error">
      <!-- 双色球 / 大乐透 -->
      <div v-if="selectedType === 'ssq' || selectedType === 'dlt'" class="charts-row">
        <div class="chart-card">
          <h3>{{ selectedType === 'ssq' ? '红球' : '前区' }}号码频率</h3>
          <div ref="freqChart" class="chart"></div>
        </div>
        <div class="chart-card">
          <h3>{{ selectedType === 'ssq' ? '蓝球' : '后区' }}号码频率</h3>
          <div ref="extraChart" class="chart"></div>
        </div>
      </div>

      <!-- 位置型 -->
      <div v-if="['fc3d','pl3','pl5'].includes(selectedType)">
        <div class="charts-row" v-for="pos in analysis.positions" :key="pos.position">
          <div class="chart-card full">
            <h3>第 {{ pos.position }} 位 号码频率</h3>
            <div :ref="el => posCharts[pos.position - 1] = el" class="chart"></div>
          </div>
        </div>
        <div class="chart-card full">
          <h3>和值分布</h3>
          <div ref="sumChart" class="chart"></div>
        </div>
      </div>

      <!-- 七乐彩 -->
      <div v-if="selectedType === 'qlc'" class="chart-card full">
        <h3>号码频率</h3>
        <div ref="freqChart" class="chart"></div>
      </div>

      <!-- 概览卡片 -->
      <div class="info-grid">
        <div class="info-card" v-if="analysis.redHot">
          <h4>🔴 红球热号 TOP10</h4>
          <div class="tag-group">
            <span class="tag hot" v-for="n in analysis.redHot" :key="n">{{ n }}</span>
          </div>
        </div>
        <div class="info-card" v-if="analysis.redCold">
          <h4>❄️ 红球冷号</h4>
          <div class="tag-group">
            <span class="tag cold" v-for="n in analysis.redCold" :key="n">{{ n }}</span>
          </div>
        </div>
        <div class="info-card" v-if="analysis.frontHot">
          <h4>🔥 前区热号 TOP10</h4>
          <div class="tag-group">
            <span class="tag hot" v-for="n in analysis.frontHot" :key="n">{{ n }}</span>
          </div>
        </div>
        <div class="info-card" v-if="analysis.hot">
          <h4>🔥 热号 TOP10</h4>
          <div class="tag-group">
            <span class="tag hot" v-for="n in analysis.hot" :key="n">{{ n }}</span>
          </div>
        </div>
        <div class="info-card" v-if="analysis.sumStats">
          <h4>📈 和值统计</h4>
          <p>平均: <strong>{{ analysis.sumStats.avg }}</strong></p>
          <p>范围: <strong>{{ analysis.sumStats.min }} - {{ analysis.sumStats.max }}</strong></p>
        </div>
        <div class="info-card" v-if="analysis.oddEvenRatio">
          <h4>⚖️ 奇偶比分布</h4>
          <div v-for="(count, ratio) in analysis.oddEvenRatio" :key="ratio" class="mini-bar">
            <span class="label">{{ ratio }}</span>
            <div class="bar"><div class="fill" :style="{width: (count / analysis.totalDraws * 100) + '%'}"></div></div>
            <span class="count">{{ count }}</span>
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="analysis?.error" class="empty">{{ analysis.error }}</div>
    <div v-else class="empty">选择彩种后点击刷新进行分析</div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import * as echarts from 'echarts'
import api from '../api'

const types = [
  { code: 'ssq', name: '双色球' },
  { code: 'dlt', name: '大乐透' },
  { code: 'fc3d', name: '福彩3D' },
  { code: 'pl3', name: '排列三' },
  { code: 'pl5', name: '排列五' },
  { code: 'qlc', name: '七乐彩' },
]

const selectedType = ref('ssq')
const analysis = ref(null)
const loading = ref(false)
const freqChart = ref(null)
const extraChart = ref(null)
const sumChart = ref(null)
const posCharts = ref([])

let chartInstances = []

function destroyCharts() {
  chartInstances.forEach(c => c?.dispose())
  chartInstances = []
}

function makeBarChart(el, labels, values, color = '#4a9eff') {
  if (!el) return
  const chart = echarts.init(el)
  chartInstances.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: labels, axisLabel: { color: '#667788', fontSize: 10 }, axisLine: { lineStyle: { color: '#2a4a6a' } } },
    yAxis: { type: 'value', axisLabel: { color: '#667788' }, splitLine: { lineStyle: { color: '#1f2f3f' } } },
    series: [{
      type: 'bar', data: values,
      itemStyle: {
        borderRadius: [4, 4, 0, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: color },
          { offset: 1, color: color + '66' }
        ])
      }
    }]
  })
}

async function loadAnalysis() {
  loading.value = true
  destroyCharts()
  try {
    const { data } = await api.analyze(selectedType.value)
    analysis.value = data
    await nextTick()
    renderCharts(data)
  } catch (e) {
    analysis.value = { error: e.message }
  } finally {
    loading.value = false
  }
}

function renderCharts(data) {
  const t = selectedType.value
  if (t === 'ssq') {
    makeBarChart(freqChart.value, Object.keys(data.redFreq), Object.values(data.redFreq), '#ff4757')
    makeBarChart(extraChart.value, Object.keys(data.blueFreq), Object.values(data.blueFreq), '#4a9eff')
  } else if (t === 'dlt') {
    makeBarChart(freqChart.value, Object.keys(data.frontFreq), Object.values(data.frontFreq), '#ffa502')
    makeBarChart(extraChart.value, Object.keys(data.backFreq), Object.values(data.backFreq), '#4a9eff')
  } else if (['fc3d', 'pl3', 'pl5'].includes(t)) {
    data.positions?.forEach((pos, i) => {
      if (posCharts.value[i]) {
        makeBarChart(posCharts.value[i], Object.keys(pos.freq), Object.values(pos.freq), ['#ff6b6b', '#4a9eff', '#ffa502'][i % 3])
      }
    })
    if (sumChart.value && data.sumDistribution) {
      makeBarChart(sumChart.value, Object.keys(data.sumDistribution), Object.values(data.sumDistribution), '#2ed573')
    }
  } else if (t === 'qlc') {
    makeBarChart(freqChart.value, Object.keys(data.freq), Object.values(data.freq), '#a29bfe')
  }
}

watch(selectedType, () => loadAnalysis())
onMounted(() => loadAnalysis())
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.page-header h2 { font-size: 24px; }
.selector { display: flex; gap: 8px; }
select { background: #1a2a3a; color: #e0e6ed; border: 1px solid #2a4a6a; padding: 8px 12px; border-radius: 8px; }
.btn { padding: 10px 20px; border: none; border-radius: 8px; cursor: pointer; font-size: 14px; font-weight: 600; }
.btn:disabled { opacity: .5; }
.btn-primary { background: linear-gradient(135deg, #4a9eff, #0077cc); color: #fff; }
.charts-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px; }
.chart-card { background: #1a2a3a; border: 1px solid #2a4a6a; border-radius: 12px; padding: 20px; }
.chart-card.full { grid-column: 1 / -1; margin-bottom: 16px; }
.chart-card h3 { font-size: 14px; color: #8899aa; margin-bottom: 12px; }
.chart { width: 100%; height: 280px; }
.info-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; margin-top: 24px; }
.info-card { background: #1a2a3a; border: 1px solid #2a4a6a; border-radius: 12px; padding: 20px; }
.info-card h4 { font-size: 14px; margin-bottom: 12px; }
.info-card p { font-size: 13px; color: #8899aa; margin: 4px 0; }
.info-card strong { color: #4a9eff; }
.tag-group { display: flex; flex-wrap: wrap; gap: 6px; }
.tag { display: inline-block; padding: 4px 10px; border-radius: 6px; font-size: 13px; font-weight: 600; }
.tag.hot { background: rgba(255,71,87,.15); color: #ff4757; }
.tag.cold { background: rgba(74,158,255,.15); color: #4a9eff; }
.mini-bar { display: flex; align-items: center; gap: 8px; margin: 4px 0; }
.mini-bar .label { font-size: 12px; width: 40px; color: #8899aa; }
.mini-bar .bar { flex: 1; height: 6px; background: #0f1923; border-radius: 3px; overflow: hidden; }
.mini-bar .fill { height: 100%; background: linear-gradient(90deg, #4a9eff, #00d4ff); border-radius: 3px; }
.mini-bar .count { font-size: 12px; width: 30px; text-align: right; color: #667788; }
.empty { text-align: center; padding: 80px 20px; color: #667788; font-size: 16px; }
</style>
