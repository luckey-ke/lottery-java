<template>
  <div class="admin-home">
    <div v-if="loading" class="loading-state">
      <div class="spinner-lg"></div>
      <p>加载中...</p>
    </div>
    <div v-else-if="error" class="error-state">
      <span class="error-icon">⚠️</span>
      <p>{{ error }}</p>
      <button class="btn-retry" @click="loadComponent">重试</button>
    </div>
    <component v-else-if="currentComponent" :is="currentComponent" />
    <div v-else class="empty-state">
      <span class="empty-icon">📂</span>
      <p>暂未配置后台首页</p>
      <router-link to="/admin/menu" class="link">前往菜单管理 →</router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, shallowRef, onMounted, defineAsyncComponent, type Component } from 'vue'
import api from '../api'

// 组件映射表：菜单 component 字段 → 实际 Vue 组件
const componentMap: Record<string, () => Promise<Component>> = {
  Admin: () => import('./Admin.vue'),
  FetchHistory: () => import('./FetchHistory.vue'),
  Users: () => import('./Users.vue'),
  Roles: () => import('./Roles.vue'),
  Menus: () => import('./Menus.vue'),
  Dashboard: () => import('./Dashboard.vue'),
  Analysis: () => import('./Analysis.vue'),
  Trend: () => import('./Trend.vue'),
  Recommend: () => import('./Recommend.vue'),
}

const currentComponent = shallowRef<Component | null>(null)
const loading = ref(true)
const error = ref('')

async function loadComponent() {
  loading.value = true
  error.value = ''
  currentComponent.value = null
  try {
    const { data } = await api.getMenusByLocation('admin')
    const menus: Array<{ path: string; component: string }> = data.data || []
    if (menus.length === 0) {
      loading.value = false
      return
    }

    // 找到 path 为空的菜单（管理首页），否则取第一个
    const homeMenu = menus.find(m => !m.path) || menus[0]
    const componentName = homeMenu.component

    if (!componentName || !componentMap[componentName]) {
      error.value = `未找到组件: ${componentName || '空'}`
      loading.value = false
      return
    }

    // 异步加载组件
    const mod = await componentMap[componentName]()
    currentComponent.value = (mod as any).default || mod
  } catch (e: any) {
    error.value = e?.response?.data?.error || '加载页面失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadComponent)
</script>

<style scoped>
.admin-home { min-height: 400px; }

.loading-state, .error-state, .empty-state {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  min-height: 400px; gap: 12px; color: var(--text-muted);
}
.error-icon, .empty-icon { font-size: 48px; }
.error-state p, .empty-state p { font-size: 15px; }

.spinner-lg {
  width: 36px; height: 36px;
  border: 3px solid var(--border-light); border-top-color: var(--accent);
  border-radius: 50%; animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.btn-retry {
  padding: 8px 20px; border: 1px solid var(--border); border-radius: var(--radius-sm);
  background: var(--bg-card); color: var(--accent); font-size: 13px; font-weight: 600;
  cursor: pointer; font-family: var(--font); transition: all 0.2s;
}
.btn-retry:hover { border-color: var(--accent); background: var(--accent-bg); }

.link { color: var(--accent); text-decoration: none; font-size: 13px; font-weight: 600; }
.link:hover { text-decoration: underline; }
</style>
