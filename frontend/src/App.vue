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
          <!-- 公开页面 -->
          <router-link to="/" class="nav-item" active-class="active">
            <span class="nav-icon">📊</span> 总览
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

          <!-- 管理页面（仅管理员可见） -->
          <template v-if="isAdmin">
            <div class="nav-divider"></div>
            <router-link to="/admin" class="nav-item" active-class="active">
              <span class="nav-icon">⚙️</span> 管理
            </router-link>
            <router-link to="/history" class="nav-item" active-class="active">
              <span class="nav-icon">📋</span> 历史
            </router-link>
            <router-link to="/users" class="nav-item" active-class="active">
              <span class="nav-icon">👥</span> 用户
            </router-link>
            <router-link to="/roles" class="nav-item" active-class="active">
              <span class="nav-icon">🛡️</span> 角色
            </router-link>
            <router-link to="/menus" class="nav-item" active-class="active">
              <span class="nav-icon">📂</span> 菜单
            </router-link>
          </template>
        </nav>

        <!-- 登录/用户信息 -->
        <div class="header-actions">
          <template v-if="isLoggedIn">
            <span class="user-badge" :class="{ 'is-admin': isAdmin }">
              {{ isAdmin ? '👑' : '👤' }} {{ user?.nickname || user?.username }}
            </span>
            <button class="btn-logout" @click="handleLogout">退出</button>
          </template>
          <template v-else>
            <router-link to="/login" class="btn-login">登录</router-link>
          </template>
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
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useGlobal } from './composables/useGlobal'
import { useAuth } from './composables/useAuth'

const router = useRouter()
const { message, messageType, isLoading, dismissToast } = useGlobal()
const { isLoggedIn, isAdmin, user, logout, fetchUser } = useAuth()

// 应用启动时从后端同步用户信息
onMounted(() => { fetchUser() })

const toastIcon = computed(() => {
  switch (messageType.value) {
    case 'success': return '✅'
    case 'error': return '❌'
    case 'warning': return '⚠️'
    default: return 'ℹ️'
  }
})

function handleLogout() {
  logout()
  router.push('/login')
}
</script>

<style>
@import './styles/variables.css';
@import './styles/global.css';

/* Nav divider */
.nav-divider {
  width: 1px;
  height: 24px;
  background: var(--border);
  margin: 0 4px;
}

/* User badge */
.user-badge {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  padding: 6px 12px;
  background: var(--bg);
  border-radius: var(--radius);
}
.user-badge.is-admin {
  color: var(--accent);
  background: var(--accent-bg);
}

/* Auth buttons */
.btn-login {
  padding: 8px 16px;
  background: var(--accent);
  color: white;
  border: none;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
  transition: opacity 0.2s;
}
.btn-login:hover { opacity: 0.9; }

.btn-logout {
  padding: 6px 12px;
  background: none;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
}
.btn-logout:hover {
  border-color: var(--red);
  color: var(--red);
}
</style>
