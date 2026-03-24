<template>
  <div class="analysis-page">
    <div class="page-hero">
      <div>
        <h1 class="page-title">统计分析</h1>
        <p class="page-subtitle">号码频率、热冷号、奇偶比、和值分布等多维分析</p>
      </div>
      <div class="hero-actions">
        <div class="type-pills">
          <button
            v-for="t in types"
            :key="t.code"
            :class="['type-pill', { active: selectedType === t.code }]"
            @click="selectedType = t.code"
          >{{ t.icon }} {{ t.name }}</button>
        </div>
      </div>
    </div>

    <div v-if="analysis && !analysis.error">
      <!-- 双色球 / 大乐透 -->
      <div v-if="selectedType === 'ssq' || selectedType === 'dlt'" class="chart-grid">
        <div class="chart-panel">
          <div class="chart-header">
            <h3>{{ selectedType === 'ssq' ? '🔴 红球' : '🟡 前区' }}号码频率</h3>
          </div>
          <div ref="freqChart" class="chart-area"></div>
        </div>
        <div class="chart-panel">
          <div class="chart-header">
            <h3>{{ selectedType === 'ssq' ? '🔵 蓝球' : '🔵 后区' }}号码频率</h3>
          </div>
          <div ref="extraChart" class="chart-area"></div>
        </div>
      </div>

      <!-- 位置型 -->
      <div v-if="['fc3d','pl3','pl5'].includes(selectedType)">
        <div class="chart-grid" v-for="pos in analysis.positions" :key="pos.position">
          <div class="chart-panel full">
            <div class="chart-header">
              <h3>📍 第 {{ pos.position }} 位 号码频率</h3>
            </div>
            <div :ref="el => posCharts[pos.position - 1] = el" class="chart-area"></div>
          </div>
        </div>
        <div class="chart-panel full" v-if="analysis.sumDistribution">
          <div class="chart-header">
            <h3>📊 和值分布</h3>
          </div>
          <div ref="sumChart" class="chart-area"></div>
        </div>
      </div>

      <!-- 七乐彩 -->
      <div v-if="selectedType === 'qlc'" class="chart-panel full">
        <div class="chart-header"><h3>🎱 号码频率</h3></div>
        <div ref="freqChart" class="chart-area"></div>
      </div>

      <!-- 信息卡片 -->
      <div class="info-grid">
        <div class="info-card" v-if="analysis.redHot">
          <div class="info-card-header">
            <span class="info-icon hot">🔥</span>
            <h4>红球热号 TOP10</h4>
          </div>
          <div class="tag-group">
            <span class="tag hot" v-for="n in analysis.redHot" :key="n">{{ n }}</span>
          </div>
        </div>
        <div class="info-card" v-if="analysis.redCold">
          <div class="info-card-header">
            <span class="info-icon cold">❄️</span>
            <h4>红球冷号</h4>
          </div>
          <div class="tag-group">
            <span class="tag cold" v-for="n in analysis.redCold" :key="n">{{ n }}</span>
          </div>
        </div>
        <div class="info-card" v-if="analysis.frontHot">
          <div class="info-card-header">
            <span class="info-icon hot">🔥</span>
            <h4>前区热号 TOP10</h4>
          </div>
          <div class="tag-group">
            <span class="tag hot" v-for="n in analysis.frontHot" :key="n">{{ n }}</span>
          </div>
        </div>
        <div class="info-card" v-if="analysis.hot">
          <div class="info-card-header">
            <span class="info-icon hot">🔥</span>
            <h4>热号 TOP10</h4>
          </div>
          <div class="tag-group">
            <span class="tag hot" v-for="n in analysis.hot" :key="n">{{ n }}</span>
          </div>
        </div>
        <div class="info-card" v-if="analysis.sumStats">
          <div class="info-card-header">
            <span class="info-icon sum">📈</span>
            <h4>和值统计</h4>
          </div>
          <div class="stat-row">
            <div class="stat-item">
              <span class="stat-num">{{ analysis.sumStats.avg }}</span>
              <span class="stat-desc">平均值</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">{{ analysis.sumStats.min }} - {{ analysis.sumStats.max }}</span>
              <span class="stat-desc">范围</span>
            </div>
          </div>
        </div>
        <div class="info-card" v-if="analysis.oddEvenRatio">
          <div class="info-card-header">
            <span class="info-icon ratio">⚖️</span>
            <h4>奇偶比分布</h4>
          </div>
          <div v-for="(count, ratio) in analysis.oddEvenRatio" :key="ratio" class="bar-row">
            <span class="bar-label">{{ ratio }}</span>
            <div class="bar-track">
              <div class="bar-fill" :style="{ width: (count / analysis.totalDraws * 100) + '%' }"></div>
            </div>
            <span class="bar-count">{{ count }}</span>
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="analysis?.error" class="empty-hero">
      <span class="empty-icon">⚠️</span>
      <h3>分析出错</h3>
      <p>{{ analysis.error }}</p>
    </div>
    <div v-else class="empty-hero">
      <span class="empty-icon">📊</span>
      <h3>选择彩种开始分析</h3>
      <p>点击上方彩种标签加载统计分析数据</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
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
const analysis = ref(null)
const loading = ref(false)
const freqChart = ref(null)
const extraChart = ref(null)
const sumChart = ref(null)
const posCharts = ref([])

let chartInstances = []

function destroyCharts() { chartInstances.forEach(c => c?.dispose()); chartInstances = [] }

function makeBarChart(el, labels, values, color = '#6366f1') {
  if (!el) return
  const chart = echarts.init(el)
  chartInstances.push(chart)
  chart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#fff',
      borderColor: '#e8ecf1',
      textStyle: { color: '#1a1d23', fontSize: 12 },
      borderRadius: 8,
      padding: [8, 12],
    },
    grid: { left: 48, right: 16, top: 16, bottom: 36 },
    xAxis: {
      type: 'category', data: labels,
      axisLabel: { color: '#9ca3af', fontSize: 10 },
      axisLine: { lineStyle: { color: '#e8ecf1' } },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#9ca3af' },
      splitLine: { lineStyle: { color: '#f0f2f5' } },
      axisLine: { show: false },
    },
    series: [{
      type: 'bar', data: values, barMaxWidth: 24,
      itemStyle: {
        borderRadius: [6, 6, 0, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color },
          { offset: 1, color: color + '66' }
        ])
      },
      emphasis: {
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color },
            { offset: 1, color }
          ])
        }
      }
    }]
  })
}

async function loadAnalysis() {
  loading.value = true; destroyCharts()
  try {
    const { data } = await api.analyze(selectedType.value)
    analysis.value = data
    await nextTick()
    renderCharts(data)
  } catch (e) { analysis.value = { error: e.message } }
  finally { loading.value = false }
}

function renderCharts(data) {
  const t = selectedType.value
  if (t === 'ssq') {
    makeBarChart(freqChart.value, Object.keys(data.redFreq), Object.values(data.redFreq), '#ef4444')
    makeBarChart(extraChart.value, Object.keys(data.blueFreq), Object.values(data.blueFreq), '#3b82f6')
  } else if (t === 'dlt') {
    makeBarChart(freqChart.value, Object.keys(data.frontFreq), Object.values(data.frontFreq), '#f59e0b')
    makeBarChart(extraChart.value, Object.keys(data.backFreq), Object.values(data.backFreq), '#3b82f6')
  } else if (['fc3d', 'pl3', 'pl5'].includes(t)) {
    const colors = ['#ef4444', '#3b82f6', '#f59e0b', '#10b981', '#8b5cf6']
    data.positions?.forEach((pos, i) => {
      if (posCharts.value[i]) makeBarChart(posCharts.value[i], Object.keys(pos.freq), Object.values(pos.freq), colors[i % colors.length])
    })
    if (sumChart.value && data.sumDistribution) makeBarChart(sumChart.value, Object.keys(data.sumDistribution), Object.values(data.sumDistribution), '#10b981')
  } else if (t === 'qlc') {
    makeBarChart(freqChart.value, Object.keys(data.freq), Object.values(data.freq), '#8b5cf6')
  }
}

watch(selectedType, () => loadAnalysis())
onMounted(() => loadAnalysis())
</script>

<style scoped>
.page-hero {
  display: flex; justify-content: space-between; align-items: flex-end;
  margin-bottom: 28px; gap: 16px; flex-wrap: wrap;
}
.page-title { font-size: 28px; font-weight: 800; letter-spacing: -0.5px; margin-bottom: 4px; }
.page-subtitle { color: var(--text-secondary); font-size: 14px; }

/* Type Pills */
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

/* Chart */
.chart-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px; }
.chart-panel {
  background: var(--bg-card); border-radius: var(--radius-lg);
  padding: 20px; box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-light); margin-bottom: 16px;
}
.chart-panel.full { grid-column: 1 / -1; }
.chart-header { margin-bottom: 16px; }
.chart-header h3 { font-size: 15px; font-weight: 600; color: var(--text-secondary); }
.chart-area { width: 100%; height: 280px; }

/* Info Grid */
.info-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px; margin-top: 8px;
}
.info-card {
  background: var(--bg-card); border-radius: var(--radius-lg);
  padding: 20px; box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-light);
}
.info-card-header { display: flex; align-items: center; gap: 10px; margin-bottom: 16px; }
.info-card-header h4 { font-size: 14px; font-weight: 600; }
.info-icon {
  width: 36px; height: 36px; display: flex; align-items: center; justify-content: center;
  border-radius: var(--radius-sm); font-size: 18px;
}
.info-icon.hot { background: var(--red-bg); }
.info-icon.cold { background: var(--blue-bg); }
.info-icon.sum { background: var(--green-bg); }
.info-icon.ratio { background: var(--purple-bg); }

.tag-group { display: flex; flex-wrap: wrap; gap: 6px; }
.tag {
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 32px; padding: 4px 10px; border-radius: 999px;
  font-size: 13px; font-weight: 700;
}
.tag.hot { background: var(--red-bg); color: var(--red); }
.tag.cold { background: var(--blue-bg); color: var(--blue); }

.stat-row { display: flex; gap: 24px; }
.stat-item { display: flex; flex-direction: column; }
.stat-num { font-size: 20px; font-weight: 800; color: var(--accent); }
.stat-desc { font-size: 12px; color: var(--text-muted); margin-top: 2px; }

.bar-row { display: flex; align-items: center; gap: 10px; margin: 6px 0; }
.bar-label { font-size: 12px; font-weight: 500; width: 40px; color: var(--text-secondary); }
.bar-track { flex: 1; height: 8px; background: var(--bg); border-radius: 4px; overflow: hidden; }
.bar-fill {
  height: 100%; border-radius: 4px;
  background: linear-gradient(90deg, var(--accent), var(--accent-light));
  transition: width 0.6s ease;
}
.bar-count { font-size: 12px; font-weight: 600; width: 32px; text-align: right; color: var(--text-muted); }

/* Empty */
.empty-hero {
  text-align: center; padding: 80px 20px; color: var(--text-secondary);
}
.empty-icon { font-size: 48px; display: block; margin-bottom: 16px; }
.empty-hero h3 { font-size: 20px; font-weight: 700; color: var(--text-primary); margin-bottom: 8px; }
.empty-hero p { font-size: 14px; }

@media (max-width: 768px) {
  .page-hero { flex-direction: column; align-items: flex-start; }
  .chart-grid { grid-template-columns: 1fr; }
  .info-grid { grid-template-columns: 1fr; }
}
</style>
