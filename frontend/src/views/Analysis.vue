<template>
  <div class="analysis-page">
    <div class="page-hero">
      <div>
        <h1 class="page-title">统计分析</h1>
        <p class="page-subtitle">号码频率、遗漏值、AC值、跨度、连号、质合比等多维分析</p>
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
      <!-- 总览条 -->
      <div class="overview-bar">
        <div class="overview-item">
          <span class="ov-num">{{ analysis.totalDraws }}</span>
          <span class="ov-label">分析期数</span>
        </div>
        <div class="overview-item" v-if="analysis.sumStats">
          <span class="ov-num">{{ analysis.sumStats.avg }}</span>
          <span class="ov-label">和值均值</span>
        </div>
        <div class="overview-item" v-if="analysis.spanStats">
          <span class="ov-num">{{ analysis.spanStats.avg }}</span>
          <span class="ov-label">平均跨度</span>
        </div>
        <div class="overview-item" v-if="analysis.acStats">
          <span class="ov-num">{{ analysis.acStats.avg }}</span>
          <span class="ov-label">平均AC值</span>
        </div>
        <div class="overview-item" v-if="analysis.avgRepeats != null">
          <span class="ov-num">{{ analysis.avgRepeats }}</span>
          <span class="ov-label">平均重号</span>
        </div>
      </div>

      <!-- ========== 双色球 ========== -->
      <div v-if="selectedType === 'ssq'" class="section">
        <div class="chart-grid">
          <div class="chart-panel">
            <div class="chart-header"><h3>🔴 红球号码频率</h3></div>
            <div ref="freqChart" class="chart-area"></div>
          </div>
          <div class="chart-panel">
            <div class="chart-header"><h3>🔵 蓝球号码频率</h3></div>
            <div ref="extraChart" class="chart-area"></div>
          </div>
        </div>
        <div class="chart-grid">
          <div class="chart-panel">
            <div class="chart-header"><h3>⏳ 红球遗漏值</h3><span class="chart-hint">数字越大 = 越久未出现</span></div>
            <div ref="redMissingChart" class="chart-area"></div>
          </div>
          <div class="chart-panel">
            <div class="chart-header"><h3>⏳ 蓝球遗漏值</h3></div>
            <div ref="blueMissingChart" class="chart-area"></div>
          </div>
        </div>
        <div class="chart-grid">
          <div class="chart-panel">
            <div class="chart-header"><h3>📏 跨度分布</h3><span class="chart-hint">最大号 - 最小号</span></div>
            <div ref="spanChart" class="chart-area"></div>
          </div>
          <div class="chart-panel">
            <div class="chart-header"><h3>🧮 AC值分布</h3><span class="chart-hint">号码复杂度指数</span></div>
            <div ref="acChart" class="chart-area"></div>
          </div>
        </div>
        <div class="chart-grid">
          <div class="chart-panel">
            <div class="chart-header"><h3>🔗 连号分布</h3><span class="chart-hint">相邻号码对数</span></div>
            <div ref="consecutiveChart" class="chart-area-sm"></div>
          </div>
          <div class="chart-panel">
            <div class="chart-header"><h3>🎯 和尾分布</h3><span class="chart-hint">和值个位数</span></div>
            <div ref="sumTailChart" class="chart-area-sm"></div>
          </div>
        </div>
        <div class="chart-grid">
          <div class="chart-panel">
            <div class="chart-header"><h3>⚖️ 奇偶比分布</h3></div>
            <div ref="oddEvenChart" class="chart-area-sm"></div>
          </div>
          <div class="chart-panel">
            <div class="chart-header"><h3>📐 大小比分布</h3><span class="chart-hint">≥17 为大号</span></div>
            <div ref="sizeRatioChart" class="chart-area-sm"></div>
          </div>
        </div>
        <div class="chart-grid">
          <div class="chart-panel">
            <div class="chart-header"><h3>🔢 质合比分布</h3><span class="chart-hint">质数: 2,3,5,7,11,13,17,19,23,29,31</span></div>
            <div ref="primeChart" class="chart-area-sm"></div>
          </div>
          <div class="chart-panel">
            <div class="chart-header"><h3>🔀 012路分布</h3><span class="chart-hint">号码 ÷ 3 的余数</span></div>
            <div ref="mod012Chart" class="chart-area-sm"></div>
          </div>
        </div>
      </div>

      <!-- ========== 大乐透 ========== -->
      <div v-if="selectedType === 'dlt'" class="section">
        <div class="chart-grid">
          <div class="chart-panel">
            <div class="chart-header"><h3>🟡 前区号码频率</h3></div>
            <div ref="freqChart" class="chart-area"></div>
          </div>
          <div class="chart-panel">
            <div class="chart-header"><h3>🔵 后区号码频率</h3></div>
            <div ref="extraChart" class="chart-area"></div>
          </div>
        </div>
        <div class="chart-grid">
          <div class="chart-panel">
            <div class="chart-header"><h3>📏 前区跨度分布</h3></div>
            <div ref="spanChart" class="chart-area"></div>
          </div>
          <div class="chart-panel">
            <div class="chart-header"><h3>🧮 前区AC值分布</h3></div>
            <div ref="acChart" class="chart-area"></div>
          </div>
        </div>
        <div class="chart-grid">
          <div class="chart-panel">
            <div class="chart-header"><h3>🔗 连号分布</h3></div>
            <div ref="consecutiveChart" class="chart-area-sm"></div>
          </div>
          <div class="chart-panel">
            <div class="chart-header"><h3>🎯 和尾分布</h3></div>
            <div ref="sumTailChart" class="chart-area-sm"></div>
          </div>
        </div>
        <div class="chart-grid">
          <div class="chart-panel">
            <div class="chart-header"><h3>⚖️ 奇偶比分布</h3></div>
            <div ref="oddEvenChart" class="chart-area-sm"></div>
          </div>
          <div class="chart-panel">
            <div class="chart-header"><h3>📐 大小比分布</h3><span class="chart-hint">≥18 为大号</span></div>
            <div ref="sizeRatioChart" class="chart-area-sm"></div>
          </div>
        </div>
        <div class="chart-grid">
          <div class="chart-panel">
            <div class="chart-header"><h3>🔢 质合比分布</h3></div>
            <div ref="primeChart" class="chart-area-sm"></div>
          </div>
          <div class="chart-panel">
            <div class="chart-header"><h3>🔀 012路分布</h3></div>
            <div ref="mod012Chart" class="chart-area-sm"></div>
          </div>
        </div>
        <div class="chart-grid">
          <div class="chart-panel full">
            <div class="chart-header"><h3>🎯 前区三区比分布</h3><span class="chart-hint">1-12 / 13-24 / 25-35</span></div>
            <div ref="zoneChart" class="chart-area-sm"></div>
          </div>
        </div>
      </div>

      <!-- ========== 位置型 (3D/排列) ========== -->
      <div v-if="['fc3d','pl3','pl5'].includes(selectedType)" class="section">
        <div class="chart-grid" v-for="pos in analysis.positions" :key="pos.position">
          <div class="chart-panel full">
            <div class="chart-header"><h3>📍 第 {{ pos.position }} 位 号码频率</h3></div>
            <div :ref="el => posCharts[pos.position - 1] = el" class="chart-area"></div>
          </div>
        </div>
        <div class="chart-grid">
          <div class="chart-panel full" v-if="analysis.sumDistribution">
            <div class="chart-header"><h3>📊 和值分布</h3></div>
            <div ref="sumChart" class="chart-area"></div>
          </div>
        </div>
        <div class="chart-grid">
          <div class="chart-panel">
            <div class="chart-header"><h3>📏 跨度分布</h3></div>
            <div ref="spanChart" class="chart-area"></div>
          </div>
          <div class="chart-panel">
            <div class="chart-header"><h3>🎯 和尾分布</h3></div>
            <div ref="sumTailChart" class="chart-area-sm"></div>
          </div>
        </div>
        <div class="chart-grid">
          <div class="chart-panel">
            <div class="chart-header"><h3>🔗 连号分布</h3></div>
            <div ref="consecutiveChart" class="chart-area-sm"></div>
          </div>
          <div class="chart-panel">
            <div class="chart-header"><h3>⚖️ 奇偶比分布</h3></div>
            <div ref="oddEvenChart" class="chart-area-sm"></div>
          </div>
        </div>
        <div class="chart-grid">
          <div class="chart-panel">
            <div class="chart-header"><h3>🔢 质合比分布</h3></div>
            <div ref="primeChart" class="chart-area-sm"></div>
          </div>
          <div class="chart-panel">
            <div class="chart-header"><h3>🔀 012路分布</h3></div>
            <div ref="mod012Chart" class="chart-area-sm"></div>
          </div>
        </div>
      </div>

      <!-- ========== 七乐彩 ========== -->
      <div v-if="selectedType === 'qlc'" class="section">
        <div class="chart-panel full">
          <div class="chart-header"><h3>🎱 号码频率</h3></div>
          <div ref="freqChart" class="chart-area"></div>
        </div>
        <div class="chart-grid">
          <div class="chart-panel">
            <div class="chart-header"><h3>📏 跨度分布</h3></div>
            <div ref="spanChart" class="chart-area"></div>
          </div>
          <div class="chart-panel">
            <div class="chart-header"><h3>🧮 AC值分布</h3></div>
            <div ref="acChart" class="chart-area"></div>
          </div>
        </div>
        <div class="chart-grid">
          <div class="chart-panel">
            <div class="chart-header"><h3>🔗 连号分布</h3></div>
            <div ref="consecutiveChart" class="chart-area-sm"></div>
          </div>
          <div class="chart-panel">
            <div class="chart-header"><h3>🎯 和尾分布</h3></div>
            <div ref="sumTailChart" class="chart-area-sm"></div>
          </div>
        </div>
        <div class="chart-grid">
          <div class="chart-panel">
            <div class="chart-header"><h3>⚖️ 奇偶比分布</h3></div>
            <div ref="oddEvenChart" class="chart-area-sm"></div>
          </div>
          <div class="chart-panel">
            <div class="chart-header"><h3>📐 大小比分布</h3><span class="chart-hint">≥16 为大号</span></div>
            <div ref="sizeRatioChart" class="chart-area-sm"></div>
          </div>
        </div>
        <div class="chart-grid">
          <div class="chart-panel">
            <div class="chart-header"><h3>🔢 质合比分布</h3></div>
            <div ref="primeChart" class="chart-area-sm"></div>
          </div>
          <div class="chart-panel">
            <div class="chart-header"><h3>🔀 012路分布</h3></div>
            <div ref="mod012Chart" class="chart-area-sm"></div>
          </div>
        </div>
        <div class="chart-grid">
          <div class="chart-panel full">
            <div class="chart-header"><h3>🎯 三区比分布</h3><span class="chart-hint">1-10 / 11-20 / 21-30</span></div>
            <div ref="zoneChart" class="chart-area-sm"></div>
          </div>
        </div>
      </div>

      <!-- ========== 信息卡片 ========== -->
      <div class="info-grid">
        <!-- 双色球 红球 -->
        <div class="info-card" v-if="analysis.redHot">
          <div class="info-card-header">
            <span class="info-icon hot">🔥</span>
            <h4>红球热号 TOP10</h4>
          </div>
          <div class="tag-group">
            <span class="tag hot" v-for="n in analysis.redHot" :key="'rh'+n">{{ n }}</span>
          </div>
        </div>
        <div class="info-card" v-if="analysis.redCold">
          <div class="info-card-header">
            <span class="info-icon cold">❄️</span>
            <h4>红球冷号 TOP10</h4>
          </div>
          <div class="tag-group">
            <span class="tag cold" v-for="n in analysis.redCold" :key="'rc'+n">{{ n }}</span>
          </div>
        </div>
        <div class="info-card" v-if="analysis.blueHot">
          <div class="info-card-header">
            <span class="info-icon hot">🔥</span>
            <h4>蓝球热号 TOP5</h4>
          </div>
          <div class="tag-group">
            <span class="tag hot" v-for="n in analysis.blueHot" :key="'bh'+n">{{ n }}</span>
          </div>
        </div>
        <div class="info-card" v-if="analysis.blueCold">
          <div class="info-card-header">
            <span class="info-icon cold">❄️</span>
            <h4>蓝球冷号 TOP5</h4>
          </div>
          <div class="tag-group">
            <span class="tag cold" v-for="n in analysis.blueCold" :key="'bc'+n">{{ n }}</span>
          </div>
        </div>

        <!-- 大乐透 -->
        <div class="info-card" v-if="analysis.frontHot">
          <div class="info-card-header">
            <span class="info-icon hot">🔥</span>
            <h4>前区热号 TOP10</h4>
          </div>
          <div class="tag-group">
            <span class="tag hot" v-for="n in analysis.frontHot" :key="'fh'+n">{{ n }}</span>
          </div>
        </div>
        <div class="info-card" v-if="analysis.frontCold">
          <div class="info-card-header">
            <span class="info-icon cold">❄️</span>
            <h4>前区冷号 TOP10</h4>
          </div>
          <div class="tag-group">
            <span class="tag cold" v-for="n in analysis.frontCold" :key="'fc'+n">{{ n }}</span>
          </div>
        </div>
        <div class="info-card" v-if="analysis.backHot">
          <div class="info-card-header">
            <span class="info-icon hot">🔥</span>
            <h4>后区热号 TOP5</h4>
          </div>
          <div class="tag-group">
            <span class="tag hot" v-for="n in analysis.backHot" :key="'bkh'+n">{{ n }}</span>
          </div>
        </div>
        <div class="info-card" v-if="analysis.backCold">
          <div class="info-card-header">
            <span class="info-icon cold">❄️</span>
            <h4>后区冷号 TOP5</h4>
          </div>
          <div class="tag-group">
            <span class="tag cold" v-for="n in analysis.backCold" :key="'bkc'+n">{{ n }}</span>
          </div>
        </div>

        <!-- 通用热冷号 -->
        <div class="info-card" v-if="analysis.hot">
          <div class="info-card-header">
            <span class="info-icon hot">🔥</span>
            <h4>热号 TOP10</h4>
          </div>
          <div class="tag-group">
            <span class="tag hot" v-for="n in analysis.hot" :key="'h'+n">{{ n }}</span>
          </div>
        </div>
        <div class="info-card" v-if="analysis.cold">
          <div class="info-card-header">
            <span class="info-icon cold">❄️</span>
            <h4>冷号 TOP10</h4>
          </div>
          <div class="tag-group">
            <span class="tag cold" v-for="n in analysis.cold" :key="'c'+n">{{ n }}</span>
          </div>
        </div>

        <!-- 位置型 各位热冷号 -->
        <div class="info-card" v-for="pos in analysis.positions" :key="'pos'+pos.position">
          <div class="info-card-header">
            <span class="info-icon sum">📍</span>
            <h4>第 {{ pos.position }} 位热冷号</h4>
          </div>
          <div class="pos-hot-cold">
            <div class="pos-row">
              <span class="pos-label hot-label">🔥 热</span>
              <div class="tag-group">
                <span class="tag hot" v-for="n in pos.hot" :key="n">{{ n }}</span>
              </div>
            </div>
            <div class="pos-row">
              <span class="pos-label cold-label">❄️ 冷</span>
              <div class="tag-group">
                <span class="tag cold" v-for="n in pos.cold" :key="n">{{ n }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 和值统计 -->
        <div class="info-card" v-if="analysis.sumStats">
          <div class="info-card-header">
            <span class="info-icon sum">📈</span>
            <h4>和值统计</h4>
          </div>
          <div class="stat-grid">
            <div class="stat-item">
              <span class="stat-num">{{ analysis.sumStats.avg }}</span>
              <span class="stat-desc">平均值</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">{{ analysis.sumStats.min }}</span>
              <span class="stat-desc">最小值</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">{{ analysis.sumStats.max }}</span>
              <span class="stat-desc">最大值</span>
            </div>
            <div class="stat-item" v-if="analysis.sumStats.max != null && analysis.sumStats.min != null">
              <span class="stat-num">{{ analysis.sumStats.max - analysis.sumStats.min }}</span>
              <span class="stat-desc">极差</span>
            </div>
          </div>
        </div>

        <!-- 跨度统计 -->
        <div class="info-card" v-if="analysis.spanStats">
          <div class="info-card-header">
            <span class="info-icon ratio">📏</span>
            <h4>跨度统计</h4>
          </div>
          <div class="stat-grid">
            <div class="stat-item">
              <span class="stat-num">{{ analysis.spanStats.avg }}</span>
              <span class="stat-desc">平均值</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">{{ analysis.spanStats.min }}</span>
              <span class="stat-desc">最小值</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">{{ analysis.spanStats.max }}</span>
              <span class="stat-desc">最大值</span>
            </div>
          </div>
        </div>

        <!-- AC值统计 -->
        <div class="info-card" v-if="analysis.acStats">
          <div class="info-card-header">
            <span class="info-icon sum">🧮</span>
            <h4>AC值统计</h4>
          </div>
          <div class="stat-grid">
            <div class="stat-item">
              <span class="stat-num">{{ analysis.acStats.avg }}</span>
              <span class="stat-desc">平均值</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">{{ analysis.acStats.min }}</span>
              <span class="stat-desc">最小值</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">{{ analysis.acStats.max }}</span>
              <span class="stat-desc">最大值</span>
            </div>
          </div>
        </div>

        <!-- 重号 -->
        <div class="info-card" v-if="analysis.avgRepeats != null">
          <div class="info-card-header">
            <span class="info-icon ratio">🔁</span>
            <h4>重号统计</h4>
          </div>
          <div class="stat-grid">
            <div class="stat-item">
              <span class="stat-num">{{ analysis.avgRepeats }}</span>
              <span class="stat-desc">平均每期重号数</span>
            </div>
          </div>
        </div>

        <!-- 奇偶比 -->
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
            <span class="bar-count">{{ count }} <small>({{ (count / analysis.totalDraws * 100).toFixed(1) }}%)</small></span>
          </div>
        </div>

        <!-- 大小比 -->
        <div class="info-card" v-if="analysis.sizeRatio">
          <div class="info-card-header">
            <span class="info-icon ratio">📐</span>
            <h4>大小比分布</h4>
          </div>
          <div v-for="(count, ratio) in analysis.sizeRatio" :key="ratio" class="bar-row">
            <span class="bar-label">{{ ratio }}</span>
            <div class="bar-track">
              <div class="bar-fill orange" :style="{ width: (count / analysis.totalDraws * 100) + '%' }"></div>
            </div>
            <span class="bar-count">{{ count }} <small>({{ (count / analysis.totalDraws * 100).toFixed(1) }}%)</small></span>
          </div>
        </div>

        <!-- 质合比 -->
        <div class="info-card" v-if="analysis.primeCompositeRatio">
          <div class="info-card-header">
            <span class="info-icon sum">🔢</span>
            <h4>质合比分布</h4>
          </div>
          <div v-for="(count, ratio) in analysis.primeCompositeRatio" :key="ratio" class="bar-row">
            <span class="bar-label">{{ ratio }}</span>
            <div class="bar-track">
              <div class="bar-fill green" :style="{ width: (count / analysis.totalDraws * 100) + '%' }"></div>
            </div>
            <span class="bar-count">{{ count }} <small>({{ (count / analysis.totalDraws * 100).toFixed(1) }}%)</small></span>
          </div>
        </div>

        <!-- 012路 -->
        <div class="info-card" v-if="analysis.mod012Ratio">
          <div class="info-card-header">
            <span class="info-icon ratio">🔀</span>
            <h4>012路分布</h4>
          </div>
          <div v-for="(count, ratio) in topMod012(analysis.mod012Ratio)" :key="ratio" class="bar-row">
            <span class="bar-label">{{ ratio }}</span>
            <div class="bar-track">
              <div class="bar-fill purple" :style="{ width: (count / analysis.totalDraws * 100) + '%' }"></div>
            </div>
            <span class="bar-count">{{ count }} <small>({{ (count / analysis.totalDraws * 100).toFixed(1) }}%)</small></span>
          </div>
        </div>

        <!-- 龙虎和 (位置型) -->
        <div class="info-card" v-if="analysis.dragonTiger">
          <div class="info-card-header">
            <span class="info-icon hot">🐲</span>
            <h4>龙虎和 (首尾比较)</h4>
          </div>
          <div class="stat-grid">
            <div class="stat-item">
              <span class="stat-num" style="color: var(--red)">{{ analysis.dragonTiger.dragon }}</span>
              <span class="stat-desc">龙 {{ analysis.dragonTiger.dragonPct }}%</span>
            </div>
            <div class="stat-item">
              <span class="stat-num" style="color: var(--blue)">{{ analysis.dragonTiger.tiger }}</span>
              <span class="stat-desc">虎 {{ analysis.dragonTiger.tigerPct }}%</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">{{ analysis.dragonTiger.draw }}</span>
              <span class="stat-desc">和</span>
            </div>
          </div>
        </div>

        <!-- 高频组合 (位置型) -->
        <div class="info-card wide" v-if="analysis.topCombos?.length">
          <div class="info-card-header">
            <span class="info-icon sum">🏆</span>
            <h4>高频组合 TOP20</h4>
          </div>
          <div class="combo-table">
            <div class="combo-row" v-for="(c, i) in analysis.topCombos" :key="i">
              <span class="combo-rank" :class="{ gold: i < 3 }">{{ i + 1 }}</span>
              <span class="combo-num">{{ c.combo }}</span>
              <span class="combo-bar-wrap">
                <span class="combo-bar" :style="{ width: (c.count / analysis.topCombos[0].count * 100) + '%' }"></span>
              </span>
              <span class="combo-count">{{ c.count }}次</span>
            </div>
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
const spanChart = ref(null)
const acChart = ref(null)
const consecutiveChart = ref(null)
const sumTailChart = ref(null)
const redMissingChart = ref(null)
const blueMissingChart = ref(null)
const oddEvenChart = ref(null)
const sizeRatioChart = ref(null)
const primeChart = ref(null)
const mod012Chart = ref(null)
const zoneChart = ref(null)
const posCharts = ref([])

let chartInstances = []

function destroyCharts() { chartInstances.forEach(c => c?.dispose()); chartInstances = [] }

function makeBarChart(el, labels, values, color = '#6366f1') {
  if (!el) return
  const chart = echarts.init(el)
  chartInstances.push(chart)
  chart.setOption({
    tooltip: {
      trigger: 'axis', backgroundColor: '#fff', borderColor: '#e8ecf1',
      textStyle: { color: '#1a1d23', fontSize: 12 }, borderRadius: 8, padding: [8, 12],
    },
    grid: { left: 48, right: 16, top: 16, bottom: 36 },
    xAxis: {
      type: 'category', data: labels,
      axisLabel: { color: '#9ca3af', fontSize: 10 },
      axisLine: { lineStyle: { color: '#e8ecf1' } }, axisTick: { show: false },
    },
    yAxis: {
      type: 'value', axisLabel: { color: '#9ca3af' },
      splitLine: { lineStyle: { color: '#f0f2f5' } }, axisLine: { show: false },
    },
    series: [{
      type: 'bar', data: values, barMaxWidth: 24,
      itemStyle: {
        borderRadius: [6, 6, 0, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color }, { offset: 1, color: color + '66' }
        ])
      },
    }]
  })
}

function makeRatioChart(el, data) {
  if (!el) return
  const labels = Object.keys(data)
  const values = Object.values(data)
  const total = values.reduce((a, b) => a + b, 0)
  const max = Math.max(...values)
  const chart = echarts.init(el)
  chartInstances.push(chart)
  chart.setOption({
    tooltip: {
      trigger: 'axis', backgroundColor: '#fff', borderColor: '#e8ecf1',
      textStyle: { color: '#1a1d23', fontSize: 12 }, borderRadius: 8,
      formatter: (params) => { const p = params[0]; return `${p.name}<br/>${p.value} 期 (${(p.value / total * 100).toFixed(1)}%)` }
    },
    grid: { left: 48, right: 32, top: 16, bottom: 36 },
    xAxis: {
      type: 'category', data: labels,
      axisLabel: { color: '#9ca3af', fontSize: 10 },
      axisLine: { lineStyle: { color: '#e8ecf1' } }, axisTick: { show: false },
    },
    yAxis: {
      type: 'value', axisLabel: { color: '#9ca3af' },
      splitLine: { lineStyle: { color: '#f0f2f5' } }, axisLine: { show: false },
    },
    series: [{
      type: 'bar', data: values, barMaxWidth: 40,
      itemStyle: {
        borderRadius: [6, 6, 0, 0],
        color: (params) => {
          const ratio = params.value / max
          return new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: ratio > 0.7 ? '#6366f1' : ratio > 0.4 ? '#818cf8' : '#c7d2fe' },
            { offset: 1, color: ratio > 0.7 ? '#6366f166' : ratio > 0.4 ? '#818cf866' : '#c7d2fe66' },
          ])
        }
      },
      label: {
        show: true, position: 'top', fontSize: 10, color: '#6b7280',
        formatter: (p) => `${p.value} (${(p.value / total * 100).toFixed(1)}%)`
      }
    }]
  })
}

function topMod012(data) {
  const entries = Object.entries(data)
  entries.sort((a, b) => b[1] - a[1])
  return Object.fromEntries(entries.slice(0, 12))
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

  // Common charts for all types
  if (data.spanDistribution) makeBarChart(spanChart.value, Object.keys(data.spanDistribution), Object.values(data.spanDistribution), '#f59e0b')
  if (data.acDistribution) makeBarChart(acChart.value, Object.keys(data.acDistribution), Object.values(data.acDistribution), '#10b981')
  if (data.consecutiveDistribution) makeBarChart(consecutiveChart.value, Object.keys(data.consecutiveDistribution), Object.values(data.consecutiveDistribution), '#8b5cf6')
  if (data.sumTails) makeBarChart(sumTailChart.value, Object.keys(data.sumTails), Object.values(data.sumTails), '#ec4899')
  if (data.oddEvenRatio) makeRatioChart(oddEvenChart.value, data.oddEvenRatio)
  if (data.sizeRatio) makeRatioChart(sizeRatioChart.value, data.sizeRatio)
  if (data.primeCompositeRatio) makeRatioChart(primeChart.value, data.primeCompositeRatio)
  if (data.mod012Ratio) makeRatioChart(mod012Chart.value, topMod012(data.mod012Ratio))
  if (data.zoneRatio3) makeRatioChart(zoneChart.value, data.zoneRatio3)

  if (t === 'ssq') {
    makeBarChart(freqChart.value, Object.keys(data.redFreq), Object.values(data.redFreq), '#ef4444')
    makeBarChart(extraChart.value, Object.keys(data.blueFreq), Object.values(data.blueFreq), '#3b82f6')
    if (data.redMissing) makeBarChart(redMissingChart.value, Object.keys(data.redMissing), Object.values(data.redMissing), '#f59e0b')
    if (data.blueMissing) makeBarChart(blueMissingChart.value, Object.keys(data.blueMissing), Object.values(data.blueMissing), '#8b5cf6')
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

.section { margin-bottom: 8px; }

/* Overview Bar */
.overview-bar { display: flex; gap: 16px; margin-bottom: 24px; flex-wrap: wrap; }
.overview-item {
  background: var(--bg-card); border-radius: var(--radius-lg);
  padding: 14px 20px; box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-light);
  display: flex; flex-direction: column; min-width: 100px;
}
.ov-num { font-size: 20px; font-weight: 800; color: var(--accent); }
.ov-label { font-size: 11px; color: var(--text-muted); margin-top: 2px; white-space: nowrap; }

/* Chart */
.chart-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px; }
.chart-panel {
  background: var(--bg-card); border-radius: var(--radius-lg);
  padding: 20px; box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-light); margin-bottom: 16px;
}
.chart-panel.full { grid-column: 1 / -1; }
.chart-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.chart-header h3 { font-size: 15px; font-weight: 600; color: var(--text-secondary); }
.chart-hint { font-size: 12px; color: var(--text-muted); }
.chart-area { width: 100%; height: 280px; }
.chart-area-sm { width: 100%; height: 240px; }

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
.info-card.wide { grid-column: 1 / -1; }
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

.pos-hot-cold { display: flex; flex-direction: column; gap: 10px; }
.pos-row { display: flex; align-items: center; gap: 10px; }
.pos-label { font-size: 12px; font-weight: 600; min-width: 50px; }
.hot-label { color: var(--red); }
.cold-label { color: var(--blue); }

.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.stat-item { display: flex; flex-direction: column; align-items: center; }
.stat-num { font-size: 20px; font-weight: 800; color: var(--accent); }
.stat-desc { font-size: 12px; color: var(--text-muted); margin-top: 2px; text-align: center; }

.bar-row { display: flex; align-items: center; gap: 10px; margin: 6px 0; }
.bar-label { font-size: 12px; font-weight: 500; width: 40px; color: var(--text-secondary); }
.bar-track { flex: 1; height: 8px; background: var(--bg); border-radius: 4px; overflow: hidden; }
.bar-fill {
  height: 100%; border-radius: 4px;
  background: linear-gradient(90deg, var(--accent), var(--accent-light));
  transition: width 0.6s ease;
}
.bar-fill.orange { background: linear-gradient(90deg, #f59e0b, #fbbf24); }
.bar-fill.green { background: linear-gradient(90deg, #10b981, #34d399); }
.bar-fill.purple { background: linear-gradient(90deg, #8b5cf6, #a78bfa); }
.bar-count { font-size: 12px; font-weight: 600; min-width: 60px; text-align: right; color: var(--text-muted); }
.bar-count small { font-size: 10px; color: var(--text-muted); }

.combo-table { display: flex; flex-direction: column; gap: 6px; }
.combo-row { display: flex; align-items: center; gap: 10px; }
.combo-rank {
  width: 24px; height: 24px; border-radius: 50%; display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 700; background: var(--bg); color: var(--text-muted);
}
.combo-rank.gold { background: #fef3c7; color: #d97706; }
.combo-num { font-size: 13px; font-weight: 600; font-family: monospace; min-width: 80px; }
.combo-bar-wrap { flex: 1; height: 6px; background: var(--bg); border-radius: 3px; overflow: hidden; }
.combo-bar { height: 100%; border-radius: 3px; background: linear-gradient(90deg, var(--accent), var(--accent-light)); }
.combo-count { font-size: 12px; font-weight: 600; color: var(--text-muted); min-width: 50px; text-align: right; }

.empty-hero { text-align: center; padding: 80px 20px; color: var(--text-secondary); }
.empty-icon { font-size: 48px; display: block; margin-bottom: 16px; }
.empty-hero h3 { font-size: 20px; font-weight: 700; color: var(--text-primary); margin-bottom: 8px; }
.empty-hero p { font-size: 14px; }

@media (max-width: 768px) {
  .page-hero { flex-direction: column; align-items: flex-start; }
  .chart-grid { grid-template-columns: 1fr; }
  .info-grid { grid-template-columns: 1fr; }
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
