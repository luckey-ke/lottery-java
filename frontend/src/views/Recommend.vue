<template>
  <div class="recommend-page">
    <div class="page-hero">
      <div>
        <h1 class="page-title">🎯 号码推荐</h1>
        <p class="page-subtitle">基于历史数据与统计分析，每日推荐5组号码</p>
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

    <!-- Tabs -->
    <div class="tab-bar">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        :class="['tab-btn', { active: activeTab === tab.key }]"
        @click="activeTab = tab.key"
      >{{ tab.icon }} {{ tab.label }}</button>
    </div>

    <!-- ===== 今日推荐 ===== -->
    <div v-if="activeTab === 'today'">
      <div v-if="data && !data.error">
        <div class="action-bar">
          <div class="date-badge">
            <span>📅</span> {{ data.date }} · {{ data.name }} · 每日推荐
          </div>
          <div class="export-btns">
            <button class="btn-export" @click="copyText">
              <span>📋</span> {{ copied ? '已复制' : '复制文本' }}
            </button>
            <button class="btn-export" @click="downloadText">
              <span>📄</span> 导出文本
            </button>
            <button class="btn-export primary" @click="generateImage" :disabled="generating">
              <span v-if="generating" class="spinner-sm"></span>
              <span v-else>🖼️</span>
              {{ generating ? '生成中...' : '生成图片' }}
            </button>
          </div>
        </div>

        <div class="groups-grid">
          <div
            v-for="(group, i) in data.groups"
            :key="i"
            :class="['group-card', { highlight: i === 0 }]"
          >
            <div class="group-header">
              <span class="group-strategy">{{ strategyIcons[i] }} {{ group.strategy }}</span>
              <div class="group-header-right">
                <span class="group-weight" v-if="group.weight">{{ group.weight }}</span>
                <span class="group-tag" v-if="i === 0">推荐</span>
              </div>
            </div>

            <div v-if="selectedType === 'ssq'" class="numbers-display">
              <div class="ball-row">
                <span class="ball red" v-for="n in group.reds" :key="'r'+n">{{ n }}</span>
                <span class="ball-sep">+</span>
                <span class="ball blue" v-for="n in group.blues" :key="'b'+n">{{ n }}</span>
              </div>
            </div>
            <div v-else-if="selectedType === 'dlt'" class="numbers-display">
              <div class="ball-row">
                <span class="ball yellow" v-for="n in group.fronts" :key="'f'+n">{{ n }}</span>
                <span class="ball-sep">+</span>
                <span class="ball blue" v-for="n in group.backs" :key="'bk'+n">{{ n }}</span>
              </div>
            </div>
            <div v-else-if="['fc3d','pl3','pl5'].includes(selectedType)" class="numbers-display">
              <div class="ball-row">
                <span class="ball purple" v-for="(d, di) in group.digits" :key="di">{{ d }}</span>
              </div>
            </div>
            <div v-else-if="selectedType === 'qlc'" class="numbers-display">
              <div class="ball-row">
                <span class="ball purple" v-for="n in group.numbers" :key="n">{{ n }}</span>
              </div>
            </div>
            <div class="group-display-text">{{ group.display }}</div>
          </div>
        </div>

        <div class="disclaimer">
          ⚠️ 以上推荐基于历史统计分析，仅供参考娱乐，不构成任何购买建议。彩票开奖为随机事件，请理性投注。
        </div>
      </div>
      <div v-else-if="data?.error" class="empty-hero">
        <span class="empty-icon">⚠️</span><h3>推荐出错</h3><p>{{ data.error }}</p>
      </div>
      <div v-else class="empty-hero">
        <span class="empty-icon">⏳</span><h3>加载中...</h3>
      </div>
    </div>

    <!-- ===== 历史记录 ===== -->
    <div v-if="activeTab === 'history'">
      <div v-if="historyData?.data?.length">
        <div class="history-list">
          <div class="history-day" v-for="day in historyData.data" :key="day.date">
            <div class="history-date">{{ day.date }}</div>
            <div class="history-groups">
              <div class="history-row" v-for="(g, gi) in day.groups" :key="gi">
                <span class="history-strategy">{{ strategyIcons[g.strategyIndex] || '📋' }} {{ g.strategy }}</span>
                <span class="history-numbers">{{ g.numbers }}</span>
                <span class="history-hits" v-if="g.actual">
                  <span class="hit-badge" :class="{ good: g.hitMain >= 3, great: g.hitMain >= 5 }">
                    {{ g.hitMain }}+{{ g.hitExtra }}
                  </span>
                </span>
                <span class="history-actual" v-if="g.actual">开奖: {{ g.actual }}</span>
                <span class="history-pending" v-else>待开奖</span>
              </div>
            </div>
          </div>
        </div>
        <div class="pagination" v-if="historyData.total > historyPageSize">
          <button class="page-btn" @click="historyPage--" :disabled="historyPage <= 1">← 上一页</button>
          <span class="page-info">第 {{ historyPage }} / {{ Math.ceil(historyData.total / historyPageSize) }} 页</span>
          <button class="page-btn" @click="historyPage++" :disabled="historyPage >= Math.ceil(historyData.total / historyPageSize)">下一页 →</button>
        </div>
      </div>
      <div v-else-if="historyLoaded" class="empty-hero">
        <span class="empty-icon">📭</span><h3>暂无历史推荐</h3><p>开始使用每日推荐后会自动记录</p>
      </div>
      <div v-else class="empty-hero">
        <span class="empty-icon">⏳</span><h3>加载中...</h3>
      </div>
    </div>

    <!-- ===== 命中统计 ===== -->
    <div v-if="activeTab === 'stats'">
      <div v-if="statsData?.stats?.length">
        <div class="stats-summary">
          <div class="stats-overview-card">
            <span class="sov-num">{{ statsData.totalDays }}</span>
            <span class="sov-label">累计天数</span>
          </div>
        </div>
        <div class="stats-grid">
          <div class="stat-card" v-for="s in statsData.stats" :key="s.strategy">
            <div class="stat-card-header">
              <span class="stat-strategy">{{ s.strategy }}</span>
              <span class="stat-hit-rate" :class="{ good: s.hitRate >= 40, great: s.hitRate >= 60 }">
                {{ s.hitRate }}%
              </span>
            </div>
            <div class="stat-bar-track">
              <div class="stat-bar-fill" :style="{ width: s.hitRate + '%' }"
                :class="{ good: s.hitRate >= 40, great: s.hitRate >= 60 }"></div>
            </div>
            <div class="stat-details">
              <div class="stat-detail">
                <span class="sd-label">命中次数</span>
                <span class="sd-value">{{ s.hitCount }} / {{ s.total }}</span>
              </div>
              <div class="stat-detail">
                <span class="sd-label">平均命中主号</span>
                <span class="sd-value">{{ s.avgHitMain }}</span>
              </div>
              <div class="stat-detail">
                <span class="sd-label">最多命中主号</span>
                <span class="sd-value highlight">{{ s.maxHitMain }}</span>
              </div>
              <div class="stat-detail" v-if="s.maxHitExtra > 0">
                <span class="sd-label">最多命中副号</span>
                <span class="sd-value highlight">{{ s.maxHitExtra }}</span>
              </div>
            </div>
          </div>
        </div>
        <div class="stats-note">
          💡 系统会根据各策略历史命中率自动调整推荐权重，命中率高的策略会获得更大的候选池。
        </div>
      </div>
      <div v-else-if="statsLoaded" class="empty-hero">
        <span class="empty-icon">📊</span><h3>暂无命中数据</h3><p>推荐记录需要开奖后才能统计命中率</p>
      </div>
      <div v-else class="empty-hero">
        <span class="empty-icon">⏳</span><h3>加载中...</h3>
      </div>
    </div>

    <!-- 隐藏 Canvas -->
    <canvas ref="canvasEl" style="display:none"></canvas>

    <!-- 图片预览弹窗 -->
    <Transition name="modal">
      <div v-if="showPreview" class="modal-overlay" @click.self="closePreview">
        <div class="modal-content">
          <div class="modal-header">
            <h3>🖼️ 图片预览</h3>
            <button class="modal-close" @click="closePreview">✕</button>
          </div>
          <div class="modal-body">
            <img :src="previewUrl" class="preview-img" alt="推荐图片预览" />
          </div>
          <div class="modal-footer">
            <button class="btn-modal cancel" @click="closePreview">关闭</button>
            <button class="btn-modal confirm" @click="downloadImage">
              <span>💾</span> 保存图片
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, computed } from 'vue'
import api from '../api'

const types = [
  { code: 'ssq', name: '双色球', icon: '🔴' },
  { code: 'dlt', name: '大乐透', icon: '🟡' },
  { code: 'fc3d', name: '福彩3D', icon: '🎲' },
  { code: 'pl3', name: '排列三', icon: '🎯' },
  { code: 'pl5', name: '排列五', icon: '🎯' },
  { code: 'qlc', name: '七乐彩', icon: '🎱' },
]

const tabs = [
  { key: 'today', label: '今日推荐', icon: '🎯' },
  { key: 'history', label: '历史记录', icon: '📋' },
  { key: 'stats', label: '命中统计', icon: '📊' },
]

const strategyIcons = ['⚖️', '🔥', '❄️', '📊', '🎲']

const selectedType = ref('ssq')
const activeTab = ref('today')
const data = ref(null)
const copied = ref(false)
const generating = ref(false)
const canvasEl = ref(null)
const previewUrl = ref('')
const showPreview = ref(false)

const historyData = ref(null)
const historyLoaded = ref(false)
const historyPage = ref(1)
const historyPageSize = 20

const statsData = ref(null)
const statsLoaded = ref(false)

async function loadRecommend() {
  data.value = null
  try {
    const { data: res } = await api.recommend(selectedType.value)
    data.value = res
  } catch (e) {
    data.value = { error: e.message }
  }
}

async function loadHistory() {
  historyLoaded.value = false
  historyData.value = null
  try {
    const offset = (historyPage.value - 1) * historyPageSize
    const { data: res } = await api.recommendHistory(selectedType.value, historyPageSize, offset)
    historyData.value = res
    historyLoaded.value = true
  } catch (e) {
    historyLoaded.value = true
  }
}

async function loadStats() {
  statsLoaded.value = false
  statsData.value = null
  try {
    const { data: res } = await api.recommendStats(selectedType.value)
    statsData.value = res
    statsLoaded.value = true
  } catch (e) {
    statsLoaded.value = true
  }
}

function onTabChange() {
  if (activeTab.value === 'today') loadRecommend()
  else if (activeTab.value === 'history') loadHistory()
  else if (activeTab.value === 'stats') loadStats()
}

// ========== 导出文本 ==========
function buildTextContent() {
  if (!data.value) return ''
  const lines = []
  lines.push(`${data.value.name} 每日推荐 - ${data.value.date}`)
  lines.push('='.repeat(36))
  lines.push('')
  data.value.groups.forEach((g, i) => {
    lines.push(`${strategyIcons[i]} ${g.strategy}`)
    lines.push(`  ${g.display}`)
    lines.push('')
  })
  lines.push('⚠️ 以上推荐基于历史统计分析，仅供参考娱乐，不构成任何购买建议。')
  return lines.join('\n')
}

async function copyText() {
  try {
    await navigator.clipboard.writeText(buildTextContent())
    copied.value = true
    setTimeout(() => { copied.value = false }, 2000)
  } catch {
    const ta = document.createElement('textarea')
    ta.value = buildTextContent()
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
    copied.value = true
    setTimeout(() => { copied.value = false }, 2000)
  }
}

function downloadText() {
  const text = buildTextContent()
  const blob = new Blob([text], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${data.value.name}_推荐_${data.value.date}.txt`
  a.click()
  URL.revokeObjectURL(url)
}

// ========== 图片生成 ==========
function getGroupNumbers(group) {
  const t = selectedType.value
  if (t === 'ssq') return { main: group.reds, extra: group.blues, mainColor: '#ef4444', extraColor: '#3b82f6' }
  if (t === 'dlt') return { main: group.fronts, extra: group.backs, mainColor: '#f59e0b', extraColor: '#3b82f6' }
  if (['fc3d', 'pl3', 'pl5'].includes(t)) return { main: group.digits, extra: null, mainColor: '#8b5cf6', extraColor: null }
  if (t === 'qlc') return { main: group.numbers, extra: null, mainColor: '#8b5cf6', extraColor: null }
  return { main: [], extra: null, mainColor: '#666', extraColor: null }
}

function drawBall(ctx, x, y, radius, text, color) {
  const grad = ctx.createRadialGradient(x - radius * 0.3, y - radius * 0.3, radius * 0.1, x, y, radius)
  grad.addColorStop(0, lightenColor(color, 30))
  grad.addColorStop(1, color)
  ctx.beginPath()
  ctx.arc(x, y, radius, 0, Math.PI * 2)
  ctx.fillStyle = grad
  ctx.fill()
  ctx.fillStyle = '#fff'
  ctx.font = `bold ${radius * 0.9}px "Inter", -apple-system, "PingFang SC", sans-serif`
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText(text, x, y + 1)
}

function lightenColor(hex, percent) {
  const num = parseInt(hex.slice(1), 16)
  const r = Math.min(255, (num >> 16) + percent)
  const g = Math.min(255, ((num >> 8) & 0xff) + percent)
  const b = Math.min(255, (num & 0xff) + percent)
  return `rgb(${r},${g},${b})`
}

function roundRect(ctx, x, y, w, h, r) {
  ctx.beginPath()
  ctx.moveTo(x + r, y); ctx.lineTo(x + w - r, y); ctx.quadraticCurveTo(x + w, y, x + w, y + r)
  ctx.lineTo(x + w, y + h - r); ctx.quadraticCurveTo(x + w, y + h, x + w - r, y + h)
  ctx.lineTo(x + r, y + h); ctx.quadraticCurveTo(x, y + h, x, y + h - r)
  ctx.lineTo(x, y + r); ctx.quadraticCurveTo(x, y, x + r, y)
  ctx.closePath()
}

async function generateImage() {
  if (!data.value) return
  generating.value = true
  await new Promise(r => setTimeout(r, 50))
  try {
    const canvas = canvasEl.value
    const ctx = canvas.getContext('2d')
    const dpr = 2
    const W = 800, groups = data.value.groups, cardH = 120, padding = 32, headerH = 100, footerH = 60, gap = 16
    const H = padding + headerH + groups.length * (cardH + gap) + footerH + padding
    canvas.width = W * dpr; canvas.height = H * dpr; ctx.scale(dpr, dpr)
    ctx.fillStyle = '#f5f7fa'; ctx.fillRect(0, 0, W, H)
    roundRect(ctx, padding, padding, W - padding * 2, headerH - 8, 16); ctx.fillStyle = '#6366f1'; ctx.fill()
    ctx.fillStyle = '#fff'; ctx.font = 'bold 28px "Inter", -apple-system, "PingFang SC", sans-serif'
    ctx.textAlign = 'left'; ctx.textBaseline = 'top'
    ctx.fillText(`🎯 ${data.value.name} 每日推荐`, padding + 24, padding + 20)
    ctx.font = '15px "Inter", -apple-system, "PingFang SC", sans-serif'; ctx.fillStyle = 'rgba(255,255,255,0.8)'
    ctx.fillText(`${data.value.date}  ·  基于历史数据与统计分析`, padding + 24, padding + 58)
    const cardY0 = padding + headerH, cardW = W - padding * 2, ballR = 18, ballGap = 8
    groups.forEach((group, i) => {
      const cy = cardY0 + i * (cardH + gap)
      roundRect(ctx, padding, cy, cardW, cardH, 12); ctx.fillStyle = i === 0 ? '#faf5ff' : '#fff'; ctx.fill()
      ctx.strokeStyle = i === 0 ? '#6366f1' : '#e8ecf1'; ctx.lineWidth = i === 0 ? 2 : 1; ctx.stroke()
      ctx.fillStyle = '#1a1d23'; ctx.font = 'bold 15px "Inter", -apple-system, "PingFang SC", sans-serif'
      ctx.textAlign = 'left'; ctx.textBaseline = 'top'
      ctx.fillText(`${strategyIcons[i]} ${group.strategy}`, padding + 20, cy + 16)
      if (i === 0) {
        const tagX = padding + cardW - 70; roundRect(ctx, tagX, cy + 14, 50, 22, 11)
        ctx.fillStyle = '#eef2ff'; ctx.fill(); ctx.fillStyle = '#6366f1'
        ctx.font = 'bold 11px sans-serif'; ctx.textAlign = 'center'; ctx.fillText('推荐', tagX + 25, cy + 20)
      }
      const nums = getGroupNumbers(group); const ballY = cy + 68; let ballX = padding + 28
      nums.main.forEach(n => { drawBall(ctx, ballX, ballY, ballR, n, nums.mainColor); ballX += ballR * 2 + ballGap })
      if (nums.extra?.length) {
        ctx.fillStyle = '#9ca3af'; ctx.font = 'bold 16px sans-serif'; ctx.textAlign = 'center'; ctx.textBaseline = 'middle'
        ctx.fillText('+', ballX + 4, ballY); ballX += ballR + ballGap + 4
        nums.extra.forEach(n => { drawBall(ctx, ballX, ballY, ballR, n, nums.extraColor); ballX += ballR * 2 + ballGap })
      }
      ctx.fillStyle = '#9ca3af'; ctx.font = '12px monospace'; ctx.textAlign = 'right'; ctx.textBaseline = 'bottom'
      ctx.fillText(group.display, padding + cardW - 20, cy + cardH - 10)
    })
    const fy = cardY0 + groups.length * (cardH + gap) + 12
    ctx.fillStyle = '#92400e'; ctx.font = '12px "Inter", -apple-system, "PingFang SC", sans-serif'
    ctx.textAlign = 'center'; ctx.textBaseline = 'top'
    ctx.fillText('⚠️ 以上推荐基于历史统计分析，仅供参考娱乐，不构成任何购买建议。', W / 2, fy + 8)
    ctx.fillStyle = '#d1d5db'; ctx.font = '11px sans-serif'
    ctx.fillText('LotteryLab · lottery-java', W / 2, fy + 30)
    canvas.toBlob(blob => {
      previewUrl.value = URL.createObjectURL(blob); showPreview.value = true; generating.value = false
    }, 'image/png')
  } catch (e) { console.error(e); generating.value = false }
}

function downloadImage() {
  if (!previewUrl.value || !data.value) return
  const a = document.createElement('a'); a.href = previewUrl.value
  a.download = `${data.value.name}_推荐_${data.value.date}.png`; a.click()
}

function closePreview() {
  showPreview.value = false
  if (previewUrl.value) { URL.revokeObjectURL(previewUrl.value); previewUrl.value = '' }
}

watch(selectedType, () => { historyPage.value = 1; onTabChange() })
watch(activeTab, () => onTabChange())
watch(historyPage, () => { if (activeTab.value === 'history') loadHistory() })
onMounted(() => loadRecommend())
</script>

<style scoped>
.page-hero {
  display: flex; justify-content: space-between; align-items: flex-end;
  margin-bottom: 20px; gap: 16px; flex-wrap: wrap;
}
.page-title { font-size: 28px; font-weight: 800; letter-spacing: -0.5px; margin-bottom: 4px; }
.page-subtitle { color: var(--text-secondary); font-size: 14px; }
.type-pills { display: flex; gap: 6px; flex-wrap: wrap; }
.type-pill {
  padding: 8px 14px; border-radius: 999px; border: 1px solid var(--border);
  background: var(--bg-card); color: var(--text-secondary);
  font-size: 13px; font-weight: 500; cursor: pointer; font-family: var(--font); transition: all 0.2s;
}
.type-pill:hover { border-color: var(--accent); color: var(--accent); }
.type-pill.active { background: var(--accent); color: #fff; border-color: var(--accent); box-shadow: 0 2px 8px rgba(99,102,241,0.3); }

/* Tabs */
.tab-bar { display: flex; gap: 4px; margin-bottom: 24px; background: var(--bg); padding: 4px; border-radius: var(--radius-lg); width: fit-content; }
.tab-btn {
  padding: 8px 18px; border-radius: var(--radius); border: none; background: transparent;
  color: var(--text-secondary); font-size: 13px; font-weight: 500; cursor: pointer;
  font-family: var(--font); transition: all 0.2s;
}
.tab-btn:hover { color: var(--text-primary); }
.tab-btn.active { background: var(--bg-card); color: var(--accent); box-shadow: var(--shadow-sm); font-weight: 600; }

/* Action Bar */
.action-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; gap: 16px; flex-wrap: wrap; }
.date-badge { display: inline-flex; align-items: center; gap: 8px; padding: 10px 18px; border-radius: var(--radius); background: var(--accent-bg); color: var(--accent); font-size: 14px; font-weight: 600; }
.export-btns { display: flex; gap: 8px; flex-wrap: wrap; }
.btn-export { display: flex; align-items: center; gap: 6px; padding: 8px 14px; border-radius: var(--radius-sm); border: 1px solid var(--border); background: var(--bg-card); color: var(--text-secondary); font-size: 13px; font-weight: 500; cursor: pointer; font-family: var(--font); transition: all 0.2s; }
.btn-export:hover { border-color: var(--accent); color: var(--accent); }
.btn-export:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-export.primary { background: linear-gradient(135deg, var(--accent), var(--purple)); color: #fff; border: none; }
.btn-export.primary:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(99,102,241,0.4); }
.spinner-sm { display: inline-block; width: 14px; height: 14px; border: 2px solid rgba(255,255,255,0.3); border-top-color: #fff; border-radius: 50%; animation: spin 0.6s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.groups-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; }
.group-card { background: var(--bg-card); border-radius: var(--radius-lg); padding: 24px; box-shadow: var(--shadow-sm); border: 2px solid transparent; transition: all 0.25s ease; }
.group-card:hover { box-shadow: var(--shadow-md); transform: translateY(-2px); }
.group-card.highlight { border-color: var(--accent); box-shadow: 0 0 0 3px var(--accent-bg), var(--shadow-md); }
.group-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.group-strategy { font-size: 15px; font-weight: 700; color: var(--text-primary); }
.group-header-right { display: flex; align-items: center; gap: 8px; }
.group-tag { font-size: 11px; font-weight: 700; color: var(--accent); background: var(--accent-bg); padding: 3px 10px; border-radius: 999px; }
.numbers-display { margin-bottom: 12px; }
.ball-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.ball { display: inline-flex; align-items: center; justify-content: center; width: 38px; height: 38px; border-radius: 50%; font-size: 15px; font-weight: 800; color: #fff; }
.ball.red { background: linear-gradient(135deg, #ef4444, #dc2626); }
.ball.blue { background: linear-gradient(135deg, #3b82f6, #2563eb); }
.ball.yellow { background: linear-gradient(135deg, #f59e0b, #d97706); }
.ball.purple { background: linear-gradient(135deg, #8b5cf6, #7c3aed); }
.ball-sep { font-size: 18px; font-weight: 700; color: var(--text-muted); margin: 0 4px; }
.group-display-text { font-size: 12px; color: var(--text-muted); font-family: monospace; padding-top: 8px; border-top: 1px solid var(--border-light); }
.disclaimer { margin-top: 32px; padding: 16px 20px; background: var(--orange-bg); border-radius: var(--radius); color: #92400e; font-size: 13px; line-height: 1.6; border: 1px solid #fde68a; }

/* History */
.history-list { display: flex; flex-direction: column; gap: 20px; }
.history-day { background: var(--bg-card); border-radius: var(--radius-lg); padding: 20px; box-shadow: var(--shadow-sm); border: 1px solid var(--border-light); }
.history-date { font-size: 15px; font-weight: 700; color: var(--accent); margin-bottom: 14px; padding-bottom: 10px; border-bottom: 1px solid var(--border-light); }
.history-groups { display: flex; flex-direction: column; gap: 8px; }
.history-row { display: flex; align-items: center; gap: 12px; padding: 8px 12px; border-radius: var(--radius-sm); font-size: 13px; flex-wrap: wrap; }
.history-row:nth-child(odd) { background: var(--bg); }
.history-strategy { font-weight: 600; color: var(--text-primary); min-width: 80px; white-space: nowrap; }
.history-numbers { font-family: monospace; font-weight: 600; color: var(--text-secondary); flex: 1; }
.history-hits { display: flex; align-items: center; }
.hit-badge { padding: 2px 10px; border-radius: 999px; font-size: 12px; font-weight: 700; background: var(--bg); color: var(--text-muted); }
.hit-badge.good { background: #fef3c7; color: #d97706; }
.hit-badge.great { background: #dcfce7; color: #16a34a; }
.history-actual { font-size: 12px; color: var(--text-muted); font-family: monospace; }
.history-pending { font-size: 12px; color: var(--text-muted); font-style: italic; }
.pagination { display: flex; justify-content: center; align-items: center; gap: 12px; margin-top: 24px; }
.page-btn { padding: 8px 16px; border: 1px solid var(--border); border-radius: var(--radius-sm); background: var(--bg-card); color: var(--text-secondary); font-size: 13px; font-weight: 500; cursor: pointer; font-family: var(--font); transition: all 0.2s; }
.page-btn:hover:not(:disabled) { border-color: var(--accent); color: var(--accent); }
.page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.page-info { font-size: 13px; color: var(--text-muted); }

/* Stats */
.stats-summary { display: flex; gap: 16px; margin-bottom: 24px; }
.stats-overview-card { background: var(--bg-card); border-radius: var(--radius-lg); padding: 16px 24px; box-shadow: var(--shadow-sm); border: 1px solid var(--border-light); display: flex; flex-direction: column; }
.sov-num { font-size: 24px; font-weight: 800; color: var(--accent); }
.sov-label { font-size: 12px; color: var(--text-muted); }
.stats-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; }
.stat-card { background: var(--bg-card); border-radius: var(--radius-lg); padding: 20px; box-shadow: var(--shadow-sm); border: 1px solid var(--border-light); }
.stat-card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.stat-strategy { font-size: 15px; font-weight: 700; }
.stat-hit-rate { font-size: 22px; font-weight: 800; color: var(--text-muted); }
.stat-hit-rate.good { color: #d97706; }
.stat-hit-rate.great { color: #16a34a; }
.stat-bar-track { height: 8px; background: var(--bg); border-radius: 4px; margin-bottom: 16px; overflow: hidden; }
.stat-bar-fill { height: 100%; border-radius: 4px; background: var(--border); transition: width 0.6s ease; }
.stat-bar-fill.good { background: linear-gradient(90deg, #f59e0b, #fbbf24); }
.stat-bar-fill.great { background: linear-gradient(90deg, #10b981, #34d399); }
.stat-details { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.stat-detail { display: flex; flex-direction: column; }
.sd-label { font-size: 11px; color: var(--text-muted); }
.sd-value { font-size: 15px; font-weight: 700; color: var(--text-secondary); }
.sd-value.highlight { color: var(--accent); }
.stats-note { margin-top: 24px; padding: 14px 18px; background: var(--accent-bg); border-radius: var(--radius); color: var(--accent); font-size: 13px; line-height: 1.6; }

.empty-hero { text-align: center; padding: 80px 20px; color: var(--text-secondary); }
.empty-icon { font-size: 48px; display: block; margin-bottom: 16px; }
.empty-hero h3 { font-size: 20px; font-weight: 700; color: var(--text-primary); margin-bottom: 8px; }
.empty-hero p { font-size: 14px; }

/* Modal */
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; z-index: 1000; padding: 24px; }
.modal-content { background: var(--bg-card); border-radius: var(--radius-xl); box-shadow: 0 25px 60px rgba(0,0,0,0.2); max-width: 860px; width: 100%; max-height: 90vh; display: flex; flex-direction: column; overflow: hidden; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 18px 24px; border-bottom: 1px solid var(--border-light); }
.modal-header h3 { font-size: 16px; font-weight: 700; }
.modal-close { width: 32px; height: 32px; border-radius: 50%; border: none; background: var(--bg); color: var(--text-muted); font-size: 16px; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all 0.2s; }
.modal-close:hover { background: var(--red-bg); color: var(--red); }
.modal-body { padding: 20px 24px; overflow-y: auto; flex: 1; display: flex; justify-content: center; background: #e5e7eb; }
.preview-img { max-width: 100%; max-height: 60vh; border-radius: var(--radius); box-shadow: var(--shadow-md); }
.modal-footer { display: flex; justify-content: flex-end; gap: 10px; padding: 16px 24px; border-top: 1px solid var(--border-light); }
.btn-modal { padding: 10px 20px; border-radius: var(--radius-sm); border: none; font-size: 14px; font-weight: 600; cursor: pointer; font-family: var(--font); transition: all 0.2s; display: flex; align-items: center; gap: 6px; }
.btn-modal.cancel { background: var(--bg); color: var(--text-secondary); }
.btn-modal.cancel:hover { background: var(--border); }
.btn-modal.confirm { background: linear-gradient(135deg, var(--accent), var(--purple)); color: #fff; }
.btn-modal.confirm:hover { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(99,102,241,0.4); }
.modal-enter-active { animation: fadeIn 0.2s ease; }
.modal-leave-active { animation: fadeIn 0.2s ease reverse; }
.modal-enter-active .modal-content { animation: scaleIn 0.25s ease; }
.modal-leave-active .modal-content { animation: scaleIn 0.2s ease reverse; }
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes scaleIn { from { transform: scale(0.95); opacity: 0; } to { transform: scale(1); opacity: 1; } }

@media (max-width: 768px) {
  .page-hero { flex-direction: column; align-items: flex-start; }
  .action-bar { flex-direction: column; align-items: flex-start; }
  .groups-grid, .stats-grid { grid-template-columns: 1fr; }
  .tab-bar { width: 100%; }
  .tab-btn { flex: 1; text-align: center; }
  .stat-details { grid-template-columns: 1fr; }
}
</style>
