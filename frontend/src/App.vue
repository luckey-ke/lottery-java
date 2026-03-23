<template>
  <div class="app">
    <header class="header">
      <div class="header-inner">
        <h1 class="logo">🎰 彩票统计分析系统</h1>
        <nav class="nav">
          <router-link to="/">总览</router-link>
          <router-link to="/admin">管理后台</router-link>
          <router-link to="/history">抓取历史</router-link>
          <router-link to="/analysis">统计分析</router-link>
          <router-link to="/trend">趋势</router-link>
          <button class="nav-action" @click="fetchDemo" :disabled="demoLoading">
            {{ demoLoading ? '生成中...' : '演示数据' }}
          </button>
        </nav>
      </div>
    </header>
    <main class="main">
      <router-view />
    </main>
    <div v-if="message" class="global-msg" :class="messageType">{{ message }}</div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import api from './api'

const route = useRoute()
const demoLoading = ref(false)
const message = ref('')
const messageType = ref('success')

async function fetchDemo() {
  demoLoading.value = true
  message.value = ''
  try {
    await api.fetchDemoAll(100)
    message.value = route.path === '/' ? '演示数据生成完成，当前总览仍显示真实数据。' : '演示数据生成完成。'
    messageType.value = 'success'
  } catch (e) {
    message.value = '生成失败: ' + (e.response?.data?.message || e.message)
    messageType.value = 'error'
  } finally {
    demoLoading.value = false
  }
}
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: -apple-system, 'Microsoft YaHei', sans-serif; background: #0f1923; color: #e0e6ed; }
.header { background: linear-gradient(135deg, #1a2a3a 0%, #0d1b2a 100%); border-bottom: 1px solid #2a4a6a; padding: 0 24px; }
.header-inner { max-width: 1400px; margin: 0 auto; display: flex; align-items: center; justify-content: space-between; height: 60px; }
.logo { font-size: 20px; background: linear-gradient(90deg, #4a9eff, #00d4ff); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.nav { display: flex; gap: 4px; align-items: center; }
.nav a { padding: 8px 20px; border-radius: 8px; color: #8899aa; text-decoration: none; font-size: 14px; transition: all .2s; }
.nav a:hover { color: #e0e6ed; background: rgba(74,158,255,.1); }
.nav a.router-link-active { color: #4a9eff; background: rgba(74,158,255,.15); }
.nav-action { padding: 8px 20px; border-radius: 8px; border: 1px solid #3a4a5a; background: #2a3a4a; color: #c7d2dd; font-size: 14px; cursor: pointer; transition: all .2s; }
.nav-action:hover:not(:disabled) { color: #e0e6ed; background: #3a4a5a; }
.nav-action:disabled { opacity: .6; cursor: not-allowed; }
.main { max-width: 1400px; margin: 0 auto; padding: 24px; }
.global-msg { position: fixed; top: 72px; right: 24px; z-index: 1000; padding: 12px 16px; border-radius: 8px; font-size: 14px; box-shadow: 0 8px 24px rgba(0,0,0,.25); }
.global-msg.success { background: rgba(46,213,115,.12); color: #2ed573; border: 1px solid rgba(46,213,115,.35); }
.global-msg.error { background: rgba(255,71,87,.12); color: #ff4757; border: 1px solid rgba(255,71,87,.35); }
</style>
