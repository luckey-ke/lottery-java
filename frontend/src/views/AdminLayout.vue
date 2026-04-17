<template>
  <div class="admin-layout">
    <!-- 侧边栏 -->
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
        <template v-for="item in adminMenus" :key="item.menuId">
          <!-- 目录类型：分组标题 + 子菜单 -->
          <template v-if="item.menuType === 'M'">
            <div class="sidebar-group" v-if="!sidebarCollapsed">{{ item.icon || '📁' }} {{ item.menuName }}</div>
            <router-link
              v-for="child in item.children"
              :key="child.menuId"
              :to="resolvePath(child.path)"
              class="sidebar-item indent"
              active-class="active"
            >
              <span class="sidebar-icon">{{ child.icon || '📄' }}</span>
              <span class="sidebar-text" v-if="!sidebarCollapsed">{{ child.menuName }}</span>
            </router-link>
          </template>
          <!-- 菜单类型：直接显示为链接 -->
          <router-link
            v-else
            :to="resolvePath(item.path)"
            class="sidebar-item"
            active-class="active"
          >
            <span class="sidebar-icon">{{ item.icon || '📄' }}</span>
            <span class="sidebar-text" v-if="!sidebarCollapsed">{{ item.menuName }}</span>
          </router-link>
        </template>
      </nav>

      <div class="sidebar-footer">
        <router-link to="/" class="sidebar-item back-btn">
          <span class="sidebar-icon">←</span>
          <span class="sidebar-text" v-if="!sidebarCollapsed">返回前台</span>
        </router-link>
      </div>
    </aside>

    <!-- 主内容区 -->
    <div class="admin-main">
      <header class="admin-header">
        <div class="admin-header-left">
          <h2 class="admin-page-title">{{ currentTitle }}</h2>
        </div>
        <div class="admin-header-right">
          <div class="admin-user">
            <span class="admin-user-avatar">{{ isAdmin ? '👑' : '👤' }}</span>
            <span class="admin-user-name">{{ user?.nickname || user?.username }}</span>
            <router-link to="/" class="admin-back-link">返回前台 →</router-link>
          </div>
        </div>
      </header>

      <div class="admin-content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAuth } from '../composables/useAuth'
import api from '../api'

interface MenuNode {
  menuId: number
  menuName: string
  icon: string
  path: string
  menuType: string
  orderNum: number
  children?: MenuNode[]
}

const { user, isAdmin } = useAuth()
const route = useRoute()
const sidebarCollapsed = ref(false)

const adminMenus = ref<MenuNode[]>([])

async function loadAdminMenus() {
  try {
    const { data } = await api.getMenusByLocation('admin')
    adminMenus.value = data.data || []
  } catch { adminMenus.value = [] }
}

onMounted(loadAdminMenus)

/** 收集所有菜单项用于标题查找 */
function collectAll(menus: MenuNode[]): MenuNode[] {
  const out: MenuNode[] = []
  for (const m of menus) {
    if (m.menuType !== 'M') out.push(m)
    if (m.children) out.push(...collectAll(m.children))
  }
  return out
}

const currentTitle = computed(() => {
  const path = route.path.replace('/admin/', '').replace('/admin', '')
  const all = collectAll(adminMenus.value)
  const item = all.find(i => i.path === path)
  return item ? `${item.icon || '📄'} ${item.menuName}` : '后台管理'
})

function resolvePath(path: string) {
  if (!path || path === '#') return '/admin'
  return '/admin/' + path
}
</script>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
  background: var(--bg);
}

/* ===== 侧边栏 ===== */
.sidebar {
  width: 220px;
  background: var(--bg-card);
  border-right: 1px solid var(--border-light);
  display: flex;
  flex-direction: column;
  transition: width 0.25s ease;
  flex-shrink: 0;
}
.sidebar.collapsed {
  width: 64px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid var(--border-light);
}
.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  color: var(--text-primary);
}
.brand-icon { font-size: 22px; }
.brand-text { font-size: 16px; font-weight: 800; }
.brand-accent { color: var(--accent); }
.sidebar-toggle {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: 1px solid var(--border);
  background: var(--bg);
  color: var(--text-muted);
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}
.sidebar-toggle:hover { border-color: var(--accent); color: var(--accent); }

.sidebar-label {
  padding: 16px 16px 8px;
  font-size: 11px;
  font-weight: 700;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 1px;
}

.sidebar-nav {
  flex: 1;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

/* 分组标题 */
.sidebar-group {
  padding: 16px 12px 6px;
  font-size: 11px;
  font-weight: 700;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.sidebar-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  text-decoration: none;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  transition: all 0.15s;
}
.sidebar-item:hover {
  background: var(--bg);
  color: var(--text-primary);
}
.sidebar-item.active {
  background: var(--accent-bg);
  color: var(--accent);
  font-weight: 600;
}
.sidebar-item.indent {
  padding-left: 24px;
}
.sidebar.collapsed .sidebar-item {
  justify-content: center;
  padding: 10px;
}
.sidebar-icon {
  font-size: 18px;
  flex-shrink: 0;
  width: 24px;
  text-align: center;
}
.sidebar-text {
  white-space: nowrap;
}

.sidebar-footer {
  padding: 8px;
  border-top: 1px solid var(--border-light);
}
.back-btn {
  color: var(--text-muted) !important;
}
.back-btn:hover {
  color: var(--accent) !important;
  background: var(--accent-bg) !important;
}

/* ===== 主内容 ===== */
.admin-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 28px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-light);
  flex-shrink: 0;
}
.admin-page-title {
  font-size: 20px;
  font-weight: 800;
  letter-spacing: -0.3px;
}
.admin-header-right {
  display: flex;
  align-items: center;
}
.admin-user {
  display: flex;
  align-items: center;
  gap: 10px;
}
.admin-user-avatar { font-size: 20px; }
.admin-user-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}
.admin-back-link {
  font-size: 13px;
  color: var(--accent);
  text-decoration: none;
  font-weight: 600;
  padding: 6px 12px;
  border-radius: var(--radius-sm);
  transition: background 0.15s;
}
.admin-back-link:hover {
  background: var(--accent-bg);
}

.admin-content {
  flex: 1;
  padding: 28px;
  overflow-y: auto;
}

@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    z-index: 100;
    height: 100vh;
    left: 0;
    top: 0;
  }
  .sidebar.collapsed {
    width: 0;
    overflow: hidden;
  }
  .admin-content {
    padding: 16px;
  }
}
</style>
