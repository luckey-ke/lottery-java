import { ref, computed, readonly } from 'vue'
import api from '../api'

const TOKEN_KEY = 'lottery_token'
const REFRESH_KEY = 'lottery_refresh_token'
const USER_KEY = 'lottery_user'

interface UserInfo {
  id: number
  username: string
  nickname: string
  roles: string[]
}

const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
const refreshToken = ref<string | null>(localStorage.getItem(REFRESH_KEY))
const user = ref<UserInfo | null>(JSON.parse(localStorage.getItem(USER_KEY) || 'null'))

const isLoggedIn = computed(() => !!token.value)
const isAdmin = computed(() => (user.value?.roles ?? []).some(r => r.toLowerCase() === 'admin'))

function saveAuth(tk: string, rtk: string, u: UserInfo) {
  token.value = tk
  refreshToken.value = rtk
  user.value = u
  localStorage.setItem(TOKEN_KEY, tk)
  localStorage.setItem(REFRESH_KEY, rtk)
  localStorage.setItem(USER_KEY, JSON.stringify(u))
}

function clearAuth() {
  token.value = null
  refreshToken.value = null
  user.value = null
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_KEY)
  localStorage.removeItem(USER_KEY)
}

async function login(username: string, password: string) {
  const { data } = await api.login(username, password)
  saveAuth(data.token, data.refreshToken, data.user)
  return data
}

async function register(username: string, password: string, nickname?: string, inviteCode?: string) {
  const { data } = await api.register(username, password, nickname, inviteCode)
  saveAuth(data.token, data.refreshToken, data.user)
  return data
}

async function refresh() {
  if (!refreshToken.value) {
    clearAuth()
    return false
  }
  try {
    const { data } = await api.refreshToken(refreshToken.value)
    saveAuth(data.token, data.refreshToken, user.value!)
    return true
  } catch {
    clearAuth()
    return false
  }
}

function logout() {
  clearAuth()
}

/** 从后端刷新当前用户信息（角色、权限等），保持本地数据同步 */
async function fetchUser() {
  if (!token.value) return
  try {
    const { data } = await api.me()
    if (data?.user) {
      user.value = data.user
      localStorage.setItem(USER_KEY, JSON.stringify(data.user))
    }
  } catch {
    // token 无效等 — 由拦截器处理 401 清理
  }
}

export function useAuth() {
  return {
    token: readonly(token),
    user: readonly(user),
    isLoggedIn,
    isAdmin,
    login,
    register,
    refresh,
    logout,
    fetchUser,
    saveAuth,
    clearAuth,
  }
}
