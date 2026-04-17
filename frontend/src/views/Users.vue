<template>
  <div class="users-page">
    <div class="page-hero">
      <div>
        <h1 class="page-title">用户管理</h1>
        <p class="page-subtitle">管理系统用户和角色权限</p>
      </div>
      <div class="hero-stats" v-if="total > 0">
        <div class="hero-stat">
          <span class="hero-stat-value">{{ total }}</span>
          <span class="hero-stat-label">用户总数</span>
        </div>
      </div>
    </div>

    <!-- 用户列表 -->
    <div class="table-section">
      <div class="table-scroll">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>用户名</th>
              <th>昵称</th>
              <th>角色</th>
              <th>注册时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in users" :key="u.id">
              <td class="text-muted">{{ u.id }}</td>
              <td class="font-medium">{{ u.username }}</td>
              <td>{{ u.nickname || '-' }}</td>
              <td>
                <span class="role-badge" :class="isAdminUser(u) ? 'role-admin' : 'role-user'">
                  {{ isAdminUser(u) ? '👑 管理员' : '👤 普通用户' }}
                </span>
              </td>
              <td class="text-muted">{{ u.createdAt || '-' }}</td>
              <td>
                <button
                  v-if="u.username !== currentUser"
                  class="btn-role"
                  :class="isAdminUser(u) ? 'btn-demote' : 'btn-promote'"
                  @click="toggleRole(u)"
                  :disabled="updating === u.id"
                >
                  {{ isAdminUser(u) ? '降为用户' : '提升为管理员' }}
                </button>
                <span v-else class="text-muted text-sm">当前用户</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页 -->
      <div class="pagination" v-if="total > pageSize">
        <button class="page-btn" :disabled="offset === 0" @click="prevPage">上一页</button>
        <span class="page-info">{{ offset + 1 }}-{{ Math.min(offset + pageSize, total) }} / {{ total }}</span>
        <button class="page-btn" :disabled="offset + pageSize >= total" @click="nextPage">下一页</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuth } from '../composables/useAuth'
import { useGlobal } from '../composables/useGlobal'
import api from '../api'

interface UserInfo {
  id: number
  username: string
  nickname: string
  roles: string[]
  roleIds: number[]
  createdAt: string
}

const { user } = useAuth()
const { showToast } = useGlobal()

const users = ref<UserInfo[]>([])
const total = ref(0)
const offset = ref(0)
const pageSize = 20
const updating = ref<number | null>(null)
const currentUser = user.value?.username

function isAdminUser(u: UserInfo): boolean {
  return (u.roles ?? []).some(r => r.toLowerCase() === 'admin')
}

async function loadUsers() {
  try {
    const { data } = await api.listUsers(pageSize, offset.value)
    users.value = data.data
    total.value = data.total
  } catch { /* handled by interceptor */ }
}

async function toggleRole(u: UserInfo) {
  const wasAdmin = isAdminUser(u)
  updating.value = u.id
  try {
    // 获取角色列表，找到 admin / user 的 roleId
    const { data: rolesData } = await api.listRoles()
    const adminRoleId = rolesData.data.find(r => r.roleKey === 'admin')?.roleId
    const userRoleId = rolesData.data.find(r => r.roleKey === 'user')?.roleId
    const newRoleIds = wasAdmin
      ? (userRoleId ? [userRoleId] : [])
      : (adminRoleId ? [adminRoleId] : u.roleIds)

    await api.updateUser(u.id, { roleIds: newRoleIds })
    // 本地更新
    const roleName = wasAdmin ? 'user' : 'admin'
    u.roles = [roleName]
    u.roleIds = newRoleIds
    const label = wasAdmin ? '普通用户' : '管理员'
    showToast(`已将 ${u.username} 的角色更新为 ${label}`, 'success')
  } catch { /* handled by interceptor */ }
  finally { updating.value = null }
}

function prevPage() { offset.value = Math.max(0, offset.value - pageSize); loadUsers() }
function nextPage() { offset.value += pageSize; loadUsers() }

onMounted(loadUsers)
</script>

<style scoped>
.page-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 24px;
}
.page-title { font-size: 28px; font-weight: 800; letter-spacing: -0.5px; margin-bottom: 4px; }
.page-subtitle { color: var(--text-secondary); font-size: 14px; }
.hero-stat { display: flex; flex-direction: column; align-items: flex-end; }
.hero-stat-value { font-size: 24px; font-weight: 800; color: var(--accent); line-height: 1.2; }
.hero-stat-label { font-size: 12px; color: var(--text-muted); font-weight: 500; }

.table-section {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}
.table-scroll { overflow-x: auto; }
.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}
.data-table th {
  padding: 12px 16px;
  text-align: left;
  font-weight: 600;
  color: var(--text-secondary);
  background: var(--bg);
  border-bottom: 1px solid var(--border);
  white-space: nowrap;
}
.data-table td {
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-light);
}
.data-table tr:last-child td { border-bottom: none; }
.data-table tr:hover td { background: var(--bg-card-hover); }

.font-medium { font-weight: 600; }
.text-muted { color: var(--text-muted); }
.text-sm { font-size: 12px; }

.role-badge {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}
.role-admin {
  background: var(--accent-bg);
  color: var(--accent);
}
.role-user {
  background: var(--bg);
  color: var(--text-secondary);
}

.btn-role {
  padding: 6px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  background: var(--bg-card);
}
.btn-promote {
  color: var(--accent);
  border-color: var(--accent);
}
.btn-promote:hover { background: var(--accent-bg); }
.btn-demote {
  color: var(--orange);
  border-color: var(--orange);
}
.btn-demote:hover { background: var(--orange-bg); }
.btn-role:disabled { opacity: 0.5; cursor: not-allowed; }

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 16px;
  border-top: 1px solid var(--border-light);
}
.page-btn {
  padding: 8px 16px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-card);
  color: var(--text-primary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.page-btn:hover:not(:disabled) { border-color: var(--accent); color: var(--accent); }
.page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.page-info { font-size: 13px; color: var(--text-secondary); }
</style>
