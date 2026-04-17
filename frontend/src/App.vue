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
            <div class="user-menu" ref="userMenuRef">
              <button class="user-badge" :class="{ 'is-admin': isAdmin }" @click="showUserMenu = !showUserMenu">
                {{ isAdmin ? '👑' : '👤' }} {{ user?.nickname || user?.username }}
                <span class="dropdown-arrow">▾</span>
              </button>
              <Transition name="dropdown">
                <div v-if="showUserMenu" class="user-dropdown">
                  <div class="dropdown-header">
                    <div class="dropdown-avatar">{{ isAdmin ? '👑' : '👤' }}</div>
                    <div class="dropdown-info">
                      <div class="dropdown-name">{{ user?.nickname || user?.username }}</div>
                      <div class="dropdown-meta">{{ user?.username }} · {{ isAdmin ? '管理员' : '普通用户' }}</div>
                      <div class="dropdown-meta" v-if="user?.email">📧 {{ user?.email }}</div>
                    </div>
                  </div>
                  <div class="dropdown-divider"></div>
                  <button class="dropdown-item" @click="openProfileDialog">
                    <span>✏️</span> 修改昵称
                  </button>
                  <button class="dropdown-item" @click="openPasswordDialog">
                    <span>🔑</span> 修改密码
                  </button>
                  <div class="dropdown-divider"></div>
                  <button class="dropdown-item dropdown-logout" @click="handleLogout">
                    <span>🚪</span> 退出登录
                  </button>
                </div>
              </Transition>
            </div>
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

    <!-- 修改昵称弹窗 -->
    <Transition name="modal">
      <div v-if="profileDialog" class="modal-overlay" @click.self="profileDialog = false">
        <div class="modal-card modal-sm">
          <div class="modal-header">
            <h3>✏️ 修改昵称</h3>
            <button class="modal-close" @click="profileDialog = false">✕</button>
          </div>
          <form class="modal-body" @submit.prevent="handleUpdateProfile">
            <div class="form-field">
              <label>新昵称 <span class="req">*</span></label>
              <input v-model="profileNickname" class="input" required placeholder="输入新昵称" />
            </div>
            <div v-if="profileError" class="form-error">{{ profileError }}</div>
            <div class="modal-footer">
              <button type="button" class="btn-cancel" @click="profileDialog = false">取消</button>
              <button type="submit" class="btn-confirm" :disabled="saving">保存</button>
            </div>
          </form>
        </div>
      </div>
    </Transition>

    <!-- 修改密码弹窗 -->
    <Transition name="modal">
      <div v-if="passwordDialog" class="modal-overlay" @click.self="passwordDialog = false">
        <div class="modal-card modal-sm">
          <div class="modal-header">
            <h3>🔑 修改密码</h3>
            <button class="modal-close" @click="passwordDialog = false">✕</button>
          </div>
          <form class="modal-body" @submit.prevent="handleChangePassword">
            <div class="form-field">
              <label>当前密码 <span class="req">*</span></label>
              <input v-model="oldPassword" type="password" class="input" required placeholder="输入当前密码" />
            </div>
            <div class="form-field">
              <label>新密码 <span class="req">*</span></label>
              <input v-model="newPassword" type="password" class="input" required minlength="6" placeholder="至少 6 位" />
            </div>
            <div class="form-field">
              <label>确认新密码 <span class="req">*</span></label>
              <input v-model="confirmPassword" type="password" class="input" required placeholder="再次输入新密码" />
            </div>
            <div v-if="passwordError" class="form-error">{{ passwordError }}</div>
            <div class="modal-footer">
              <button type="button" class="btn-cancel" @click="passwordDialog = false">取消</button>
              <button type="submit" class="btn-confirm" :disabled="saving">确认修改</button>
            </div>
          </form>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useGlobal } from './composables/useGlobal'
import { useAuth } from './composables/useAuth'
import api from './api'

const router = useRouter()
const { message, messageType, isLoading, dismissToast } = useGlobal()
const { isLoggedIn, isAdmin, user, logout, fetchUser, saveAuth } = useAuth()

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

// ===== 用户下拉菜单 =====
const showUserMenu = ref(false)
const userMenuRef = ref<HTMLElement | null>(null)

function onClickOutside(e: MouseEvent) {
  if (userMenuRef.value && !userMenuRef.value.contains(e.target as Node)) {
    showUserMenu.value = false
  }
}
onMounted(() => document.addEventListener('click', onClickOutside))
onUnmounted(() => document.removeEventListener('click', onClickOutside))

// ===== 修改昵称 =====
const profileDialog = ref(false)
const profileNickname = ref('')
const profileError = ref('')
const saving = ref(false)

function openProfileDialog() {
  showUserMenu.value = false
  profileNickname.value = user.value?.nickname || ''
  profileError.value = ''
  profileDialog.value = true
}

async function handleUpdateProfile() {
  saving.value = true
  profileError.value = ''
  try {
    const { data } = await api.updateProfile({ nickname: profileNickname.value })
    // 本地更新用户信息
    const token = localStorage.getItem('lottery_token')!
    const refreshToken = localStorage.getItem('lottery_refresh_token')!
    saveAuth(token, refreshToken, { ...user.value!, nickname: data.user.nickname })
    profileDialog.value = false
    fetchUser() // 同步后端数据
  } catch (e: any) {
    profileError.value = e?.response?.data?.error || '修改失败'
  } finally { saving.value = false }
}

// ===== 修改密码 =====
const passwordDialog = ref(false)
const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const passwordError = ref('')

function openPasswordDialog() {
  showUserMenu.value = false
  oldPassword.value = ''
  newPassword.value = ''
  confirmPassword.value = ''
  passwordError.value = ''
  passwordDialog.value = true
}

async function handleChangePassword() {
  passwordError.value = ''
  if (newPassword.value !== confirmPassword.value) {
    passwordError.value = '两次输入的新密码不一致'
    return
  }
  saving.value = true
  try {
    await api.changePassword({ oldPassword: oldPassword.value, newPassword: newPassword.value })
    passwordDialog.value = false
    // 密码修改后需要重新登录
    logout()
    router.push('/login')
  } catch (e: any) {
    passwordError.value = e?.response?.data?.error || '修改失败'
  } finally { saving.value = false }
}

// ===== 退出 =====
function handleLogout() {
  showUserMenu.value = false
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

/* User menu */
.user-menu { position: relative; }
.user-badge {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  padding: 6px 12px;
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
  font-family: var(--font);
}
.user-badge:hover { border-color: var(--accent); }
.user-badge.is-admin { color: var(--accent); background: var(--accent-bg); border-color: var(--accent); }
.dropdown-arrow { font-size: 10px; opacity: 0.6; }

/* Dropdown */
.user-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 240px;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  z-index: 1000;
  overflow: hidden;
}
.dropdown-header { display: flex; gap: 12px; padding: 16px; }
.dropdown-avatar { font-size: 28px; }
.dropdown-info { flex: 1; min-width: 0; }
.dropdown-name { font-size: 14px; font-weight: 700; margin-bottom: 2px; }
.dropdown-meta { font-size: 12px; color: var(--text-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.dropdown-divider { height: 1px; background: var(--border-light); }
.dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 16px;
  border: none;
  background: none;
  color: var(--text-primary);
  font-size: 14px;
  cursor: pointer;
  transition: background 0.15s;
  font-family: var(--font);
  text-align: left;
}
.dropdown-item:hover { background: var(--bg); }
.dropdown-logout { color: var(--red); }
.dropdown-logout:hover { background: var(--red-bg); }

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

/* Dropdown transition */
.dropdown-enter-active { animation: dropIn 0.15s ease; }
.dropdown-leave-active { animation: dropIn 0.1s ease reverse; }
@keyframes dropIn { from { opacity: 0; transform: translateY(-6px); } to { opacity: 1; transform: translateY(0); } }

/* Modal styles */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; z-index: 1000; padding: 24px; }
.modal-card { background: var(--bg-card); border-radius: var(--radius-lg); box-shadow: var(--shadow-lg); width: 100%; max-width: 480px; max-height: 90vh; overflow-y: auto; }
.modal-sm { max-width: 400px; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 20px 24px; border-bottom: 1px solid var(--border-light); }
.modal-header h3 { font-size: 16px; font-weight: 700; }
.modal-close { width: 32px; height: 32px; border-radius: 50%; border: none; background: var(--bg); color: var(--text-muted); font-size: 16px; cursor: pointer; display: flex; align-items: center; justify-content: center; }
.modal-close:hover { background: var(--red-bg); color: var(--red); }
.modal-body { padding: 20px 24px; }
.modal-footer { display: flex; justify-content: flex-end; gap: 10px; padding: 16px 24px; border-top: 1px solid var(--border-light); }

.form-field { margin-bottom: 16px; }
.form-field label { display: block; font-size: 13px; font-weight: 600; margin-bottom: 6px; color: var(--text-primary); }
.req { color: var(--red); }
.input { width: 100%; padding: 10px 14px; border: 1px solid var(--border); border-radius: var(--radius-sm); font-size: 14px; outline: none; transition: border-color 0.2s; background: var(--bg); color: var(--text-primary); box-sizing: border-box; font-family: var(--font); }
.input:focus { border-color: var(--accent); }
.form-error { padding: 10px 14px; background: var(--red-bg); color: var(--red); border-radius: var(--radius-sm); font-size: 13px; margin-bottom: 16px; }
.btn-cancel { padding: 10px 20px; border: 1px solid var(--border); border-radius: var(--radius-sm); background: var(--bg-card); color: var(--text-secondary); font-size: 14px; cursor: pointer; font-family: var(--font); }
.btn-confirm { padding: 10px 20px; border: none; border-radius: var(--radius-sm); background: var(--accent); color: #fff; font-size: 14px; font-weight: 600; cursor: pointer; font-family: var(--font); }
.btn-confirm:disabled { opacity: 0.5; }

.modal-enter-active { animation: fadeIn 0.2s ease; }
.modal-leave-active { animation: fadeIn 0.2s ease reverse; }
.modal-enter-active .modal-card { animation: scaleIn 0.25s ease; }
.modal-leave-active .modal-card { animation: scaleIn 0.2s ease reverse; }
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes scaleIn { from { transform: scale(0.95); opacity: 0; } to { transform: scale(1); opacity: 1; } }
</style>
