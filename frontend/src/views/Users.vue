<template>
  <div class="users-page">
    <div class="page-hero">
      <div>
        <h1 class="page-title">用户管理</h1>
        <p class="page-subtitle">管理系统用户和角色权限</p>
      </div>
      <div class="hero-actions">
        <span class="hero-badge" v-if="total > 0">共 {{ total }} 人</span>
        <button class="btn-primary" @click="showAddDialog = true">➕ 新增用户</button>
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
              <th>状态</th>
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
              <td>
                <span :class="['status-dot', u.status === '0' ? 'active' : 'disabled']">
                  {{ u.status === '0' ? '正常' : '停用' }}
                </span>
              </td>
              <td class="text-muted">{{ u.createdAt || '-' }}</td>
              <td class="actions-cell">
                <button
                  v-if="u.username !== currentUser"
                  class="btn-sm"
                  :class="isAdminUser(u) ? 'btn-warn' : 'btn-accent'"
                  @click="toggleRole(u)"
                  :disabled="updating === u.id"
                >{{ isAdminUser(u) ? '降级' : '升级' }}</button>
                <button
                  v-if="u.username !== currentUser"
                  class="btn-sm btn-danger"
                  @click="confirmDelete(u)"
                  :disabled="updating === u.id"
                >删除</button>
                <span v-else class="text-muted text-sm">当前用户</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="pagination" v-if="total > pageSize">
        <button class="page-btn" :disabled="offset === 0" @click="prevPage">上一页</button>
        <span class="page-info">{{ offset + 1 }}-{{ Math.min(offset + pageSize, total) }} / {{ total }}</span>
        <button class="page-btn" :disabled="offset + pageSize >= total" @click="nextPage">下一页</button>
      </div>
    </div>

    <!-- 新增用户弹窗 -->
    <Transition name="modal">
      <div v-if="showAddDialog" class="modal-overlay" @click.self="showAddDialog = false">
        <div class="modal-card">
          <div class="modal-header">
            <h3>➕ 新增用户</h3>
            <button class="modal-close" @click="showAddDialog = false">✕</button>
          </div>
          <form class="modal-body" @submit.prevent="handleAddUser">
            <div class="form-field">
              <label>用户名 <span class="req">*</span></label>
              <input v-model="newUser.username" class="input" placeholder="3-32 个字符" required minlength="3" maxlength="32" />
            </div>
            <div class="form-field">
              <label>昵称</label>
              <input v-model="newUser.nickname" class="input" placeholder="默认使用用户名" />
            </div>
            <div class="form-field">
              <label>密码 <span class="req">*</span></label>
              <input v-model="newUser.password" type="password" class="input" placeholder="至少 6 位" required minlength="6" />
            </div>
            <div class="form-field">
              <label>邮箱</label>
              <input v-model="newUser.email" type="email" class="input" placeholder="选填" />
            </div>
            <div class="form-field">
              <label>手机号</label>
              <input v-model="newUser.phone" class="input" placeholder="选填" />
            </div>
            <div class="form-field">
              <label>角色</label>
              <div class="checkbox-group">
                <label v-for="r in allRoles" :key="r.roleId" class="checkbox-label">
                  <input type="checkbox" :value="r.roleId" v-model="newUser.roleIds" />
                  {{ r.roleName }}
                </label>
              </div>
            </div>
            <div v-if="addError" class="form-error">{{ addError }}</div>
            <div class="modal-footer">
              <button type="button" class="btn-cancel" @click="showAddDialog = false">取消</button>
              <button type="submit" class="btn-confirm" :disabled="adding">创建</button>
            </div>
          </form>
        </div>
      </div>
    </Transition>

    <!-- 删除确认 -->
    <Transition name="modal">
      <div v-if="deleteTarget" class="modal-overlay" @click.self="deleteTarget = null">
        <div class="modal-card modal-sm">
          <div class="modal-header">
            <h3>⚠️ 确认删除</h3>
          </div>
          <div class="modal-body">
            <p>确定要删除用户 <b>{{ deleteTarget.username }}</b> 吗？此操作不可撤销。</p>
          </div>
          <div class="modal-footer">
            <button class="btn-cancel" @click="deleteTarget = null">取消</button>
            <button class="btn-danger" @click="handleDelete" :disabled="deleting">确认删除</button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useAuth } from '../composables/useAuth'
import { useGlobal } from '../composables/useGlobal'
import api from '../api'

interface UserInfo {
  id: number
  username: string
  nickname: string
  roles: string[]
  roleIds: number[]
  status: string
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

// 角色列表
const allRoles = ref<Array<{ roleId: number; roleName: string; roleKey: string }>>([])

// 新增用户
const showAddDialog = ref(false)
const adding = ref(false)
const addError = ref('')
const newUser = reactive({ username: '', nickname: '', password: '', email: '', phone: '', roleIds: [] as number[] })

// 删除用户
const deleteTarget = ref<UserInfo | null>(null)
const deleting = ref(false)

function isAdminUser(u: UserInfo): boolean {
  return (u.roles ?? []).some(r => r.toLowerCase() === 'admin')
}

async function loadUsers() {
  try {
    const { data } = await api.listUsers(pageSize, offset.value)
    users.value = data.data
    total.value = data.total
  } catch { /* interceptor */ }
}

async function loadRoles() {
  try {
    const { data } = await api.listRoles()
    allRoles.value = data.data
  } catch { /* interceptor */ }
}

async function handleAddUser() {
  addError.value = ''
  adding.value = true
  try {
    await api.addUser({
      username: newUser.username,
      password: newUser.password,
      nickname: newUser.nickname || undefined,
      email: newUser.email || undefined,
      phone: newUser.phone || undefined,
      roleIds: newUser.roleIds,
    })
    showToast('用户创建成功', 'success')
    showAddDialog.value = false
    Object.assign(newUser, { username: '', nickname: '', password: '', email: '', phone: '', roleIds: [] })
    await loadUsers()
  } catch (e: any) {
    addError.value = e?.response?.data?.error || '创建失败'
  } finally { adding.value = false }
}

async function toggleRole(u: UserInfo) {
  const wasAdmin = isAdminUser(u)
  updating.value = u.id
  try {
    const adminRoleId = allRoles.value.find(r => r.roleKey === 'admin')?.roleId
    const userRoleId = allRoles.value.find(r => r.roleKey === 'user')?.roleId
    const newRoleIds = wasAdmin
      ? (userRoleId ? [userRoleId] : [])
      : (adminRoleId ? [adminRoleId] : u.roleIds)
    await api.updateUser(u.id, { roleIds: newRoleIds })
    u.roles = [wasAdmin ? 'user' : 'admin']
    u.roleIds = newRoleIds
    showToast(`已将 ${u.username} ${wasAdmin ? '降为普通用户' : '提升为管理员'}`, 'success')
  } catch { /* interceptor */ }
  finally { updating.value = null }
}

function confirmDelete(u: UserInfo) { deleteTarget.value = u }

async function handleDelete() {
  if (!deleteTarget.value) return
  deleting.value = true
  try {
    await api.deleteUser(deleteTarget.value.id)
    showToast(`用户 ${deleteTarget.value.username} 已删除`, 'success')
    deleteTarget.value = null
    await loadUsers()
  } catch { /* interceptor */ }
  finally { deleting.value = false }
}

function prevPage() { offset.value = Math.max(0, offset.value - pageSize); loadUsers() }
function nextPage() { offset.value += pageSize; loadUsers() }

onMounted(() => { loadUsers(); loadRoles() })
</script>

<style scoped>
.page-hero { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 24px; flex-wrap: wrap; gap: 12px; }
.page-title { font-size: 28px; font-weight: 800; letter-spacing: -0.5px; margin-bottom: 4px; }
.page-subtitle { color: var(--text-secondary); font-size: 14px; }
.hero-actions { display: flex; align-items: center; gap: 12px; }
.hero-badge { font-size: 13px; font-weight: 600; color: var(--accent); background: var(--accent-bg); padding: 6px 14px; border-radius: 999px; }
.btn-primary { padding: 10px 18px; border: none; border-radius: var(--radius-sm); background: linear-gradient(135deg, var(--accent), var(--purple)); color: #fff; font-size: 13px; font-weight: 600; cursor: pointer; font-family: var(--font); transition: all 0.2s; }
.btn-primary:hover { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(99,102,241,0.4); }

.table-section { background: var(--bg-card); border-radius: var(--radius-lg); box-shadow: var(--shadow-sm); overflow: hidden; }
.table-scroll { overflow-x: auto; }
.data-table { width: 100%; border-collapse: collapse; font-size: 14px; }
.data-table th { padding: 12px 16px; text-align: left; font-weight: 600; color: var(--text-secondary); background: var(--bg); border-bottom: 1px solid var(--border); white-space: nowrap; }
.data-table td { padding: 12px 16px; border-bottom: 1px solid var(--border-light); }
.data-table tr:last-child td { border-bottom: none; }
.data-table tr:hover td { background: var(--bg-card-hover); }
.font-medium { font-weight: 600; }
.text-muted { color: var(--text-muted); }
.text-sm { font-size: 12px; }

.role-badge { display: inline-flex; padding: 4px 10px; border-radius: 20px; font-size: 12px; font-weight: 600; }
.role-admin { background: var(--accent-bg); color: var(--accent); }
.role-user { background: var(--bg); color: var(--text-secondary); }
.status-dot { font-size: 12px; font-weight: 600; }
.status-dot.active { color: var(--green); }
.status-dot.disabled { color: var(--red); }

.actions-cell { display: flex; gap: 6px; }
.btn-sm { padding: 4px 10px; border: 1px solid var(--border); border-radius: var(--radius-sm); font-size: 12px; cursor: pointer; transition: all 0.2s; background: var(--bg-card); font-family: var(--font); }
.btn-accent { color: var(--accent); border-color: var(--accent); }
.btn-accent:hover { background: var(--accent-bg); }
.btn-warn { color: var(--orange); border-color: var(--orange); }
.btn-warn:hover { background: var(--orange-bg); }
.btn-danger { color: var(--red); border-color: var(--red); }
.btn-danger:hover { background: var(--red-bg); }
.btn-sm:disabled { opacity: 0.5; cursor: not-allowed; }

.pagination { display: flex; align-items: center; justify-content: center; gap: 16px; padding: 16px; border-top: 1px solid var(--border-light); }
.page-btn { padding: 8px 16px; border: 1px solid var(--border); border-radius: var(--radius-sm); background: var(--bg-card); color: var(--text-primary); font-size: 13px; cursor: pointer; transition: all 0.2s; }
.page-btn:hover:not(:disabled) { border-color: var(--accent); color: var(--accent); }
.page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.page-info { font-size: 13px; color: var(--text-secondary); }

/* Modal */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; z-index: 1000; padding: 24px; }
.modal-card { background: var(--bg-card); border-radius: var(--radius-lg); box-shadow: var(--shadow-lg); width: 100%; max-width: 480px; max-height: 90vh; overflow-y: auto; }
.modal-sm { max-width: 400px; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 20px 24px; border-bottom: 1px solid var(--border-light); }
.modal-header h3 { font-size: 16px; font-weight: 700; }
.modal-close { width: 32px; height: 32px; border-radius: 50%; border: none; background: var(--bg); color: var(--text-muted); font-size: 16px; cursor: pointer; display: flex; align-items: center; justify-content: center; }
.modal-close:hover { background: var(--red-bg); color: var(--red); }
.modal-body { padding: 20px 24px; }
.modal-body p { font-size: 14px; line-height: 1.6; color: var(--text-secondary); }
.modal-footer { display: flex; justify-content: flex-end; gap: 10px; padding: 16px 24px; border-top: 1px solid var(--border-light); }

.form-field { margin-bottom: 16px; }
.form-field label { display: block; font-size: 13px; font-weight: 600; margin-bottom: 6px; color: var(--text-primary); }
.req { color: var(--red); }
.input { width: 100%; padding: 10px 14px; border: 1px solid var(--border); border-radius: var(--radius-sm); font-size: 14px; outline: none; transition: border-color 0.2s; background: var(--bg); color: var(--text-primary); box-sizing: border-box; }
.input:focus { border-color: var(--accent); }
.checkbox-group { display: flex; gap: 16px; flex-wrap: wrap; }
.checkbox-label { display: flex; align-items: center; gap: 6px; font-size: 14px; cursor: pointer; }
.form-error { padding: 10px 14px; background: var(--red-bg); color: var(--red); border-radius: var(--radius-sm); font-size: 13px; margin-bottom: 16px; }
.btn-cancel { padding: 10px 20px; border: 1px solid var(--border); border-radius: var(--radius-sm); background: var(--bg-card); color: var(--text-secondary); font-size: 14px; cursor: pointer; font-family: var(--font); }
.btn-confirm { padding: 10px 20px; border: none; border-radius: var(--radius-sm); background: var(--accent); color: #fff; font-size: 14px; font-weight: 600; cursor: pointer; font-family: var(--font); }
.btn-confirm:disabled { opacity: 0.5; }
.modal-footer .btn-danger { padding: 10px 20px; border: none; border-radius: var(--radius-sm); background: var(--red); color: #fff; font-size: 14px; font-weight: 600; cursor: pointer; font-family: var(--font); }
.modal-footer .btn-danger:disabled { opacity: 0.5; }

.modal-enter-active { animation: fadeIn 0.2s ease; }
.modal-leave-active { animation: fadeIn 0.2s ease reverse; }
.modal-enter-active .modal-card { animation: scaleIn 0.25s ease; }
.modal-leave-active .modal-card { animation: scaleIn 0.2s ease reverse; }
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes scaleIn { from { transform: scale(0.95); opacity: 0; } to { transform: scale(1); opacity: 1; } }

@media (max-width: 768px) {
  .page-hero { flex-direction: column; align-items: flex-start; }
  .actions-cell { flex-direction: column; }
}
</style>
