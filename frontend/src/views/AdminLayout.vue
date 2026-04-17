<template>
  <div class="admin-layout">
    <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <router-link to="/" class="sidebar-brand">
          <span class="brand-icon">🎱</span>
          <span class="brand-text" v-if="!sidebarCollapsed">Lottery<span class="brand-accent">Lab</span></span>
        </router-link>
        <button class="sidebar-toggle" @click="sidebarCollapsed = !sidebarCollapsed">
          {{ sidebarCollapsed ? '›' : '‹' }}
        </button>
      </div>

      <div class="sidebar-label" v-if="!sidebarCollapsed">后台管理</div>

      <nav class="sidebar-nav">
        <router-link to="/admin" class="sidebar-item" active-class="active" :exact="true">
          <span class="sidebar-icon">⚙️</span>
          <span class="sidebar-text" v-if="!sidebarCollapsed">管理</span>
        </router-link>
        <router-link to="/admin/history" class="sidebar-item" active-class="active">
          <span class="sidebar-icon">📋</span>
          <span class="sidebar-text" v-if="!sidebarCollapsed">历史</span>
        </router-link>
        <router-link to="/admin/user" class="sidebar-item" active-class="active">
          <span class="sidebar-icon">👥</span>
          <span class="sidebar-text" v-if="!sidebarCollapsed">用户</span>
        </router-link>
        <router-link to="/admin/role" class="sidebar-item" active-class="active">
          <span class="sidebar-icon">🛡️</span>
          <span class="sidebar-text" v-if="!sidebarCollapsed">角色</span>
        </router-link>
        <router-link to="/admin/menu" class="sidebar-item" active-class="active">
          <span class="sidebar-icon">📂</span>
          <span class="sidebar-text" v-if="!sidebarCollapsed">菜单</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <router-link to="/" class="sidebar-item back-btn">
          <span class="sidebar-icon">←</span>
          <span class="sidebar-text" v-if="!sidebarCollapsed">返回前台</span>
        </router-link>
      </div>
    </aside>

    <div class="admin-main">
      <header class="admin-header">
        <h2 class="admin-page-title">{{ currentTitle }}</h2>
        <div class="admin-user">
          <span class="admin-user-avatar">{{ isAdmin ? '👑' : '👤' }}</span>
          <span class="admin-user-name">{{ user?.nickname || user?.username }}</span>
          <router-link to="/" class="admin-back-link">返回前台 →</router-link>
        </div>
      </header>
      <div class="admin-content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuth } from '../composables/useAuth'

const { user, isAdmin } = useAuth()
const route = useRoute()
const sidebarCollapsed = ref(false)

const menuMap: Record<string, { icon: string; label: string }> = {
  '': { icon: '⚙️', label: '管理' },
  history: { icon: '📋', label: '历史' },
  user: { icon: '👥', label: '用户' },
  role: { icon: '🛡️', label: '角色' },
  menu: { icon: '📂', label: '菜单' },
}

const currentTitle = computed(() => {
  const path = route.path.replace('/admin/', '').replace('/admin', '')
  const m = menuMap[path]
  return m ? `${m.icon} ${m.label}` : '后台管理'
})
</script>

<style scoped>
.admin-layout { display: flex; min-height: 100vh; background: var(--bg); }

.sidebar {
  width: 220px; background: var(--bg-card); border-right: 1px solid var(--border-light);
  display: flex; flex-direction: column; transition: width 0.25s ease; flex-shrink: 0;
}
.sidebar.collapsed { width: 64px; }

.sidebar-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px; border-bottom: 1px solid var(--border-light);
}
.sidebar-brand { display: flex; align-items: center; gap: 8px; text-decoration: none; color: var(--text-primary); }
.brand-icon { font-size: 22px; }
.brand-text { font-size: 16px; font-weight: 800; }
.brand-accent { color: var(--accent); }
.sidebar-toggle {
  width: 28px; height: 28px; border-radius: 6px; border: 1px solid var(--border);
  background: var(--bg); color: var(--text-muted); font-size: 14px; cursor: pointer;
  display: flex; align-items: center; justify-content: center; transition: all 0.2s;
}
.sidebar-toggle:hover { border-color: var(--accent); color: var(--accent); }

.sidebar-label {
  padding: 16px 16px 8px; font-size: 11px; font-weight: 700;
  color: var(--text-muted); text-transform: uppercase; letter-spacing: 1px;
}

.sidebar-nav { flex: 1; padding: 8px; display: flex; flex-direction: column; gap: 2px; }

.sidebar-item {
  display: flex; align-items: center; gap: 12px; padding: 10px 12px;
  border-radius: var(--radius-sm); text-decoration: none; color: var(--text-secondary);
  font-size: 14px; font-weight: 500; transition: all 0.15s;
}
.sidebar-item:hover { background: var(--bg); color: var(--text-primary); }
.sidebar-item.active { background: var(--accent-bg); color: var(--accent); font-weight: 600; }
.sidebar.collapsed .sidebar-item { justify-content: center; padding: 10px; }
.sidebar-icon { font-size: 18px; flex-shrink: 0; width: 24px; text-align: center; }
.sidebar-text { white-space: nowrap; }

.sidebar-footer { padding: 8px; border-top: 1px solid var(--border-light); }
.back-btn { color: var(--text-muted) !important; }
.back-btn:hover { color: var(--accent) !important; background: var(--accent-bg) !important; }

.admin-main { flex: 1; display: flex; flex-direction: column; min-width: 0; }

.admin-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 28px; background: var(--bg-card);
  border-bottom: 1px solid var(--border-light); flex-shrink: 0;
}
.admin-page-title { font-size: 20px; font-weight: 800; letter-spacing: -0.3px; }
.admin-user { display: flex; align-items: center; gap: 10px; }
.admin-user-avatar { font-size: 20px; }
.admin-user-name { font-size: 14px; font-weight: 600; color: var(--text-primary); }
.admin-back-link {
  font-size: 13px; color: var(--accent); text-decoration: none; font-weight: 600;
  padding: 6px 12px; border-radius: var(--radius-sm); transition: background 0.15s;
}
.admin-back-link:hover { background: var(--accent-bg); }

.admin-content { flex: 1; padding: 28px; overflow-y: auto; }

@media (max-width: 768px) {
  .sidebar { position: fixed; z-index: 100; height: 100vh; left: 0; top: 0; }
  .sidebar.collapsed { width: 0; overflow: hidden; }
  .admin-content { padding: 16px; }
}
</style>
