<template>
  <div class="app">
    <!-- 顶部导航 -->
    <header class="header">
      <div class="header-inner">
        <div class="brand">
          <span class="brand-icon">🎱</span>
          <span class="brand-text">Lottery<span class="brand-accent">Lab</span></span>
        </div>
        <nav class="nav">
          <router-link to="/" class="nav-item" active-class="active">
            <span class="nav-icon">📊</span> 总览
          </router-link>
          <router-link to="/admin" class="nav-item" active-class="active">
            <span class="nav-icon">⚙️</span> 管理
          </router-link>
          <router-link to="/history" class="nav-item" active-class="active">
            <span class="nav-icon">📋</span> 历史
          </router-link>
          <router-link to="/analysis" class="nav-item" active-class="active">
            <span class="nav-icon">📈</span> 分析
          </router-link>
          <router-link to="/trend" class="nav-item" active-class="active">
            <span class="nav-icon">🔥</span> 趋势
          </router-link>
          <router-link to="/recommend" class="nav-item" active-class="active">
            <span class="nav-icon">🎯</span> 推荐
          </router-link>
        </nav>
        <div class="header-actions">
        </div>
      </div>
    </header>

    <!-- 主内容 -->
    <main class="main">
      <router-view />
    </main>

    <!-- 全局提示 -->
    <Transition name="toast">
      <div v-if="message" class="toast" :class="messageType">
        <span class="toast-icon">{{ messageType === 'success' ? '✅' : '❌' }}</span>
        {{ message }}
        <button class="toast-close" @click="message = ''">×</button>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const message = ref('')
const messageType = ref('success')
</script>

<style>
/* ========== CSS Variables ========== */
:root {
  --bg: #f5f7fa;
  --bg-card: #ffffff;
  --bg-card-hover: #fafbfc;
  --bg-nav: rgba(255,255,255,0.85);
  --border: #e8ecf1;
  --border-light: #f0f2f5;
  --text-primary: #1a1d23;
  --text-secondary: #6b7280;
  --text-muted: #9ca3af;
  --accent: #6366f1;
  --accent-light: #818cf8;
  --accent-bg: #eef2ff;
  --red: #ef4444;
  --red-bg: #fef2f2;
  --blue: #3b82f6;
  --blue-bg: #eff6ff;
  --orange: #f59e0b;
  --orange-bg: #fffbeb;
  --green: #10b981;
  --green-bg: #ecfdf5;
  --purple: #8b5cf6;
  --purple-bg: #f5f3ff;
  --pink: #ec4899;
  --shadow-sm: 0 1px 3px rgba(0,0,0,0.04), 0 1px 2px rgba(0,0,0,0.06);
  --shadow: 0 4px 6px -1px rgba(0,0,0,0.05), 0 2px 4px -2px rgba(0,0,0,0.05);
  --shadow-md: 0 10px 25px -5px rgba(0,0,0,0.08), 0 8px 10px -6px rgba(0,0,0,0.04);
  --shadow-lg: 0 20px 40px -10px rgba(0,0,0,0.1);
  --radius-sm: 8px;
  --radius: 12px;
  --radius-lg: 16px;
  --radius-xl: 20px;
  --font: 'Inter', -apple-system, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* ========== Reset ========== */
*, *::before, *::after { margin: 0; padding: 0; box-sizing: border-box; }
body {
  font-family: var(--font);
  background: var(--bg);
  color: var(--text-primary);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  line-height: 1.6;
}

/* ========== Header ========== */
.header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--bg-nav);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border-bottom: 1px solid var(--border-light);
}
.header-inner {
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  padding: 0 24px;
  gap: 16px;
}

/* Brand */
.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.brand-icon { font-size: 24px; }
.brand-text {
  font-size: 20px;
  font-weight: 800;
  letter-spacing: -0.5px;
  color: var(--text-primary);
}
.brand-accent { color: var(--accent); }

/* Nav */
.nav {
  display: flex;
  gap: 4px;
  align-items: center;
  background: var(--bg);
  padding: 4px;
  border-radius: var(--radius-lg);
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: var(--radius);
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
  white-space: nowrap;
}
.nav-item:hover {
  color: var(--text-primary);
  background: var(--bg-card);
}
.nav-item.active {
  color: var(--accent);
  background: var(--bg-card);
  box-shadow: var(--shadow-sm);
  font-weight: 600;
}
.nav-icon { font-size: 15px; }

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* Main */
.main {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}

/* Toast */
.toast {
  position: fixed;
  top: 80px;
  right: 24px;
  z-index: 1000;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  border-radius: var(--radius);
  font-size: 14px;
  font-weight: 500;
  box-shadow: var(--shadow-lg);
}
.toast.success {
  background: var(--bg-card);
  color: var(--green);
  border: 1px solid #d1fae5;
}
.toast.error {
  background: var(--bg-card);
  color: var(--red);
  border: 1px solid #fecaca;
}
.toast-close {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  color: var(--text-muted);
  margin-left: 8px;
  line-height: 1;
}
.toast-enter-active { animation: slideIn 0.3s ease; }
.toast-leave-active { animation: slideIn 0.3s ease reverse; }
@keyframes slideIn {
  from { transform: translateX(100%); opacity: 0; }
  to { transform: translateX(0); opacity: 1; }
}

/* ========== Responsive ========== */
@media (max-width: 768px) {
  .header-inner { padding: 0 16px; }
  .nav-item .nav-icon { display: none; }
  .nav-item { padding: 8px 12px; font-size: 13px; }
  .main { padding: 16px; }
  .brand-text { font-size: 17px; }
}
@media (max-width: 600px) {
  .nav { gap: 2px; padding: 3px; }
  .nav-item { padding: 6px 10px; font-size: 12px; }
}
</style>
