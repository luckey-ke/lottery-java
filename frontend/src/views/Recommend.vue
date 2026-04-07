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
      <div class="date-badge">
        <span>📅</span> {{ data.date }} · {{ data.name }} · 每日推荐
      </div>

      <div class="groups-grid">
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

async function loadRecommend() {
  data.value = null
  try {
    const { data: res } = await api.recommend(selectedType.value)
    data.value = res
  } catch (e) {
    data.value = { error: e.message }
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

.date-badge {
  display: inline-flex; align-items: center; gap: 8px;
  padding: 10px 18px; border-radius: var(--radius);
  background: var(--accent-bg); color: var(--accent);
  font-size: 14px; font-weight: 600; margin-bottom: 24px;
}

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

@media (max-width: 768px) {
  .page-hero { flex-direction: column; align-items: flex-start; }
  .groups-grid { grid-template-columns: 1fr; }
}
</style>
