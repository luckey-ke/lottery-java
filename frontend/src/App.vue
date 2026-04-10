<template>
  <div class="app">
    <!-- 全局 Loading -->
    <div v-if="isLoading" class="global-loading">
      <div class="global-loading-bar"></div>
    </div>

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
        <span class="toast-icon">{{ toastIcon }}</span>
        {{ message }}
        <button class="toast-close" @click="dismissToast">×</button>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useGlobal } from './composables/useGlobal'

const { message, messageType, isLoading, dismissToast } = useGlobal()

const toastIcon = computed(() => {
  switch (messageType.value) {
    case 'success': return '✅'
    case 'error': return '❌'
    case 'warning': return '⚠️'
    default: return 'ℹ️'
  }
})
</script>

<style>
@import './styles/variables.css';
@import './styles/global.css';
</style>
