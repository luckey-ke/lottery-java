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

      <div class="groups-grid" ref="cardsContainer">
        <div
          v-for="(group, i) in data.groups"
          :key="i"
          :class="['group-card', { highlight: i === 0 }]"
        >
          <div class="group-header">
            <span class="group-strategy">{{ strategyIcons[i] }} {{ group.strategy }}</span>
            <span class="group-tag" v-if="i === 0">推荐</span>
          </div>

          <!-- 双色球 -->
          <div v-if="selectedType === 'ssq'" class="numbers-display">
            <div class="ball-row">
              <span class="ball red" v-for="n in group.reds" :key="'r'+n">{{ n }}</span>
              <span class="ball-sep">+</span>
              <span class="ball blue" v-for="n in group.blues" :key="'b'+n">{{ n }}</span>
            </div>
          </div>

          <!-- 大乐透 -->
          <div v-else-if="selectedType === 'dlt'" class="numbers-display">
            <div class="ball-row">
              <span class="ball yellow" v-for="n in group.fronts" :key="'f'+n">{{ n }}</span>
              <span class="ball-sep">+</span>
              <span class="ball blue" v-for="n in group.backs" :key="'bk'+n">{{ n }}</span>
            </div>
          </div>

          <!-- 位置型 -->
          <div v-else-if="['fc3d','pl3','pl5'].includes(selectedType)" class="numbers-display">
            <div class="ball-row">
              <span class="ball purple" v-for="(d, di) in group.digits" :key="di">{{ d }}</span>
            </div>
          </div>

          <!-- 七乐彩 -->
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
      <span class="empty-icon">⚠️</span>
      <h3>推荐出错</h3>
      <p>{{ data.error }}</p>
    </div>
    <div v-else class="empty-hero">
      <span class="empty-icon">🎯</span>
      <h3>选择彩种查看推荐</h3>
      <p>点击上方彩种标签加载每日推荐号码</p>
    </div>

    <!-- 隐藏的图片预览 -->
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
import { ref, watch, onMounted } from 'vue'
import api from '../api'

const types = [
  { code: 'ssq', name: '双色球', icon: '🔴' },
  { code: 'dlt', name: '大乐透', icon: '🟡' },
  { code: 'fc3d', name: '福彩3D', icon: '🎲' },
  { code: 'pl3', name: '排列三', icon: '🎯' },
  { code: 'pl5', name: '排列五', icon: '🎯' },
  { code: 'qlc', name: '七乐彩', icon: '🎱' },
]

const strategyIcons = ['⚖️', '🔥', '❄️', '📊', '🎲']

const selectedType = ref('ssq')
const data = ref(null)
const copied = ref(false)
const generating = ref(false)
const canvasEl = ref(null)
const cardsContainer = ref(null)
const previewUrl = ref('')
const showPreview = ref(false)

async function loadRecommend() {
  data.value = null
  try {
    const { data: res } = await api.recommend(selectedType.value)
    data.value = res
  } catch (e) {
    data.value = { error: e.message }
  }
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
    // fallback
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

// ========== 生成图片 ==========
function getGroupNumbers(group) {
  const t = selectedType.value
  if (t === 'ssq') return { main: group.reds, extra: group.blues, mainColor: '#ef4444', extraColor: '#3b82f6' }
  if (t === 'dlt') return { main: group.fronts, extra: group.backs, mainColor: '#f59e0b', extraColor: '#3b82f6' }
  if (['fc3d', 'pl3', 'pl5'].includes(t)) return { main: group.digits, extra: null, mainColor: '#8b5cf6', extraColor: null }
  if (t === 'qlc') return { main: group.numbers, extra: null, mainColor: '#8b5cf6', extraColor: null }
  return { main: [], extra: null, mainColor: '#666', extraColor: null }
}

function drawBall(ctx, x, y, radius, text, color) {
  // gradient ball
  const grad = ctx.createRadialGradient(x - radius * 0.3, y - radius * 0.3, radius * 0.1, x, y, radius)
  grad.addColorStop(0, lightenColor(color, 30))
  grad.addColorStop(1, color)
  ctx.beginPath()
  ctx.arc(x, y, radius, 0, Math.PI * 2)
  ctx.fillStyle = grad
  ctx.fill()

  // text
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
  ctx.moveTo(x + r, y)
  ctx.lineTo(x + w - r, y)
  ctx.quadraticCurveTo(x + w, y, x + w, y + r)
  ctx.lineTo(x + w, y + h - r)
  ctx.quadraticCurveTo(x + w, y + h, x + w - r, y + h)
  ctx.lineTo(x + r, y + h)
  ctx.quadraticCurveTo(x, y + h, x, y + h - r)
  ctx.lineTo(x, y + r)
  ctx.quadraticCurveTo(x, y, x + r, y)
  ctx.closePath()
}

async function generateImage() {
  if (!data.value) return
  generating.value = true

  await new Promise(r => setTimeout(r, 50))

  try {
    const canvas = canvasEl.value
    const ctx = canvas.getContext('2d')
    const dpr = 2 // 高清

    const W = 800
    const groups = data.value.groups
    const cardH = 120
    const padding = 32
    const headerH = 100
    const footerH = 60
    const gap = 16
    const H = padding + headerH + groups.length * (cardH + gap) + footerH + padding

    canvas.width = W * dpr
    canvas.height = H * dpr
    ctx.scale(dpr, dpr)

    // Background
    ctx.fillStyle = '#f5f7fa'
    ctx.fillRect(0, 0, W, H)

    // Header area
    roundRect(ctx, padding, padding, W - padding * 2, headerH - 8, 16)
    ctx.fillStyle = '#6366f1'
    ctx.fill()

    // Title
    ctx.fillStyle = '#fff'
    ctx.font = 'bold 28px "Inter", -apple-system, "PingFang SC", sans-serif'
    ctx.textAlign = 'left'
    ctx.textBaseline = 'top'
    ctx.fillText(`🎯 ${data.value.name} 每日推荐`, padding + 24, padding + 20)

    // Date & subtitle
    ctx.font = '15px "Inter", -apple-system, "PingFang SC", sans-serif'
    ctx.fillStyle = 'rgba(255,255,255,0.8)'
    ctx.fillText(`${data.date}  ·  基于历史数据与统计分析`, padding + 24, padding + 58)

    // Cards
    const cardY0 = padding + headerH
    const cardW = W - padding * 2
    const ballR = 18
    const ballGap = 8

    groups.forEach((group, i) => {
      const cy = cardY0 + i * (cardH + gap)

      // Card background
      roundRect(ctx, padding, cy, cardW, cardH, 12)
      ctx.fillStyle = i === 0 ? '#faf5ff' : '#fff'
      ctx.fill()
      ctx.strokeStyle = i === 0 ? '#6366f1' : '#e8ecf1'
      ctx.lineWidth = i === 0 ? 2 : 1
      ctx.stroke()

      // Strategy label
      ctx.fillStyle = '#1a1d23'
      ctx.font = 'bold 15px "Inter", -apple-system, "PingFang SC", sans-serif'
      ctx.textAlign = 'left'
      ctx.textBaseline = 'top'
      ctx.fillText(`${strategyIcons[i]} ${group.strategy}`, padding + 20, cy + 16)

      // "推荐" tag
      if (i === 0) {
        const tagX = padding + cardW - 70
        roundRect(ctx, tagX, cy + 14, 50, 22, 11)
        ctx.fillStyle = '#eef2ff'
        ctx.fill()
        ctx.fillStyle = '#6366f1'
        ctx.font = 'bold 11px "Inter", -apple-system, "PingFang SC", sans-serif'
        ctx.textAlign = 'center'
        ctx.fillText('推荐', tagX + 25, cy + 20)
      }

      // Balls
      const nums = getGroupNumbers(group)
      const ballY = cy + 68
      let ballX = padding + 28

      nums.main.forEach(n => {
        drawBall(ctx, ballX, ballY, ballR, n, nums.mainColor)
        ballX += ballR * 2 + ballGap
      })

      if (nums.extra && nums.extra.length) {
        // "+" separator
        ctx.fillStyle = '#9ca3af'
        ctx.font = 'bold 16px sans-serif'
        ctx.textAlign = 'center'
        ctx.textBaseline = 'middle'
        ctx.fillText('+', ballX + 4, ballY)
        ballX += ballR + ballGap + 4

        nums.extra.forEach(n => {
          drawBall(ctx, ballX, ballY, ballR, n, nums.extraColor)
          ballX += ballR * 2 + ballGap
        })
      }

      // Display text
      ctx.fillStyle = '#9ca3af'
      ctx.font = '12px monospace'
      ctx.textAlign = 'right'
      ctx.textBaseline = 'bottom'
      ctx.fillText(group.display, padding + cardW - 20, cy + cardH - 10)
    })

    // Footer disclaimer
    const fy = cardY0 + groups.length * (cardH + gap) + 12
    ctx.fillStyle = '#92400e'
    ctx.font = '12px "Inter", -apple-system, "PingFang SC", sans-serif'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'top'
    ctx.fillText('⚠️ 以上推荐基于历史统计分析，仅供参考娱乐，不构成任何购买建议。', W / 2, fy + 8)

    // Watermark
    ctx.fillStyle = '#d1d5db'
    ctx.font = '11px "Inter", -apple-system, "PingFang SC", sans-serif'
    ctx.fillText('LotteryLab · lottery-java', W / 2, fy + 30)

    // Show preview instead of direct download
    canvas.toBlob(blob => {
      const url = URL.createObjectURL(blob)
      previewUrl.value = url
      showPreview.value = true
      generating.value = false
    }, 'image/png')
  } catch (e) {
    console.error('Image generation failed:', e)
    generating.value = false
  }
}

function downloadImage() {
  if (!previewUrl.value || !data.value) return
  const a = document.createElement('a')
  a.href = previewUrl.value
  a.download = `${data.value.name}_推荐_${data.value.date}.png`
  a.click()
}

function closePreview() {
  showPreview.value = false
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = ''
  }
}

watch(selectedType, () => loadRecommend())
onMounted(() => loadRecommend())
</script>

<style scoped>
.page-hero {
  display: flex; justify-content: space-between; align-items: flex-end;
  margin-bottom: 28px; gap: 16px; flex-wrap: wrap;
}
.page-title { font-size: 28px; font-weight: 800; letter-spacing: -0.5px; margin-bottom: 4px; }
.page-subtitle { color: var(--text-secondary); font-size: 14px; }

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

/* Action Bar */
.action-bar {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 24px; gap: 16px; flex-wrap: wrap;
}
.date-badge {
  display: inline-flex; align-items: center; gap: 8px;
  padding: 10px 18px; border-radius: var(--radius);
  background: var(--accent-bg); color: var(--accent);
  font-size: 14px; font-weight: 600;
}
.export-btns { display: flex; gap: 8px; flex-wrap: wrap; }
.btn-export {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 14px; border-radius: var(--radius-sm);
  border: 1px solid var(--border); background: var(--bg-card);
  color: var(--text-secondary); font-size: 13px; font-weight: 500;
  cursor: pointer; font-family: var(--font); transition: all 0.2s;
}
.btn-export:hover { border-color: var(--accent); color: var(--accent); }
.btn-export:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-export.primary {
  background: linear-gradient(135deg, var(--accent), var(--purple));
  color: #fff; border: none;
}
.btn-export.primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(99,102,241,0.4);
}

.spinner-sm {
  display: inline-block; width: 14px; height: 14px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff; border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.groups-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.group-card {
  background: var(--bg-card); border-radius: var(--radius-lg);
  padding: 24px; box-shadow: var(--shadow-sm);
  border: 2px solid transparent;
  transition: all 0.25s ease;
}
.group-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}
.group-card.highlight {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-bg), var(--shadow-md);
}

.group-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 16px;
}
.group-strategy {
  font-size: 15px; font-weight: 700; color: var(--text-primary);
}
.group-tag {
  font-size: 11px; font-weight: 700; color: var(--accent);
  background: var(--accent-bg); padding: 3px 10px;
  border-radius: 999px;
}

.numbers-display { margin-bottom: 12px; }
.ball-row {
  display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
}
.ball {
  display: inline-flex; align-items: center; justify-content: center;
  width: 38px; height: 38px; border-radius: 50%;
  font-size: 15px; font-weight: 800; color: #fff;
}
.ball.red { background: linear-gradient(135deg, #ef4444, #dc2626); }
.ball.blue { background: linear-gradient(135deg, #3b82f6, #2563eb); }
.ball.yellow { background: linear-gradient(135deg, #f59e0b, #d97706); }
.ball.purple { background: linear-gradient(135deg, #8b5cf6, #7c3aed); }
.ball-sep {
  font-size: 18px; font-weight: 700; color: var(--text-muted);
  margin: 0 4px;
}

.group-display-text {
  font-size: 12px; color: var(--text-muted); font-family: monospace;
  padding-top: 8px; border-top: 1px solid var(--border-light);
}

.disclaimer {
  margin-top: 32px; padding: 16px 20px;
  background: var(--orange-bg); border-radius: var(--radius);
  color: #92400e; font-size: 13px; line-height: 1.6;
  border: 1px solid #fde68a;
}

.empty-hero { text-align: center; padding: 80px 20px; color: var(--text-secondary); }
.empty-icon { font-size: 48px; display: block; margin-bottom: 16px; }
.empty-hero h3 { font-size: 20px; font-weight: 700; color: var(--text-primary); margin-bottom: 8px; }
.empty-hero p { font-size: 14px; }

/* ========== Modal ========== */
.modal-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center;
  z-index: 1000; padding: 24px;
}
.modal-content {
  background: var(--bg-card); border-radius: var(--radius-xl);
  box-shadow: 0 25px 60px rgba(0, 0, 0, 0.2);
  max-width: 860px; width: 100%; max-height: 90vh;
  display: flex; flex-direction: column; overflow: hidden;
}
.modal-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 18px 24px; border-bottom: 1px solid var(--border-light);
}
.modal-header h3 { font-size: 16px; font-weight: 700; }
.modal-close {
  width: 32px; height: 32px; border-radius: 50%; border: none;
  background: var(--bg); color: var(--text-muted); font-size: 16px;
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: all 0.2s;
}
.modal-close:hover { background: var(--red-bg); color: var(--red); }
.modal-body {
  padding: 20px 24px; overflow-y: auto; flex: 1;
  display: flex; justify-content: center; background: #e5e7eb;
}
.preview-img {
  max-width: 100%; max-height: 60vh;
  border-radius: var(--radius); box-shadow: var(--shadow-md);
}
.modal-footer {
  display: flex; justify-content: flex-end; gap: 10px;
  padding: 16px 24px; border-top: 1px solid var(--border-light);
}
.btn-modal {
  padding: 10px 20px; border-radius: var(--radius-sm); border: none;
  font-size: 14px; font-weight: 600; cursor: pointer;
  font-family: var(--font); transition: all 0.2s;
  display: flex; align-items: center; gap: 6px;
}
.btn-modal.cancel {
  background: var(--bg); color: var(--text-secondary);
}
.btn-modal.cancel:hover { background: var(--border); }
.btn-modal.confirm {
  background: linear-gradient(135deg, var(--accent), var(--purple));
  color: #fff;
}
.btn-modal.confirm:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(99,102,241,0.4);
}

/* Modal transition */
.modal-enter-active { animation: fadeIn 0.2s ease; }
.modal-leave-active { animation: fadeIn 0.2s ease reverse; }
.modal-enter-active .modal-content { animation: scaleIn 0.25s ease; }
.modal-leave-active .modal-content { animation: scaleIn 0.2s ease reverse; }
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes scaleIn { from { transform: scale(0.95); opacity: 0; } to { transform: scale(1); opacity: 1; } }

@media (max-width: 768px) {
  .page-hero { flex-direction: column; align-items: flex-start; }
  .action-bar { flex-direction: column; align-items: flex-start; }
  .groups-grid { grid-template-columns: 1fr; }
}
</style>
