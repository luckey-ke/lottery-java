<template>
  <div class="roles-page">
    <div class="page-hero">
      <div>
        <h1 class="page-title">角色管理</h1>
        <p class="page-subtitle">管理系统角色和菜单权限分配</p>
      </div>
      <button class="btn-primary" @click="openAdd">➕ 新增角色</button>
    </div>

    <!-- 角色列表 -->
    <div class="roles-grid">
      <div v-for="r in roles" :key="r.roleId" class="role-card">
        <div class="role-card-header">
          <div>
            <span class="role-name">{{ r.roleName }}</span>
            <span class="role-key">{{ r.roleKey }}</span>
          </div>
          <div class="role-actions" v-if="r.roleKey !== 'admin'">
            <button class="btn-sm btn-accent" @click="openEdit(r)">编辑</button>
            <button class="btn-sm btn-danger" @click="confirmDelete(r)">删除</button>
          </div>
          <span v-else class="role-protected">🔒 系统内置</span>
        </div>
        <div class="role-card-body">
          <div class="menu-count">
            <span class="mc-icon">📂</span>
            <span>{{ r.menuIds?.length || 0 }} 个菜单权限</span>
          </div>
          <button class="btn-sm btn-outline" @click="openMenuAssign(r)">分配菜单</button>
        </div>
      </div>
    </div>

    <!-- 新增/编辑角色弹窗 -->
    <Transition name="modal">
      <div v-if="editDialog" class="modal-overlay" @click.self="editDialog = null">
        <div class="modal-card">
          <div class="modal-header">
            <h3>{{ editDialog.roleId ? '✏️ 编辑角色' : '➕ 新增角色' }}</h3>
            <button class="modal-close" @click="editDialog = null">✕</button>
          </div>
          <form class="modal-body" @submit.prevent="handleSave">
            <div class="form-field">
              <label>角色名称 <span class="req">*</span></label>
              <input v-model="editDialog.roleName" class="input" required placeholder="如：编辑员" />
            </div>
            <div v-if="!editDialog.roleId" class="form-field">
              <label>角色标识 <span class="req">*</span></label>
              <input v-model="editDialog.roleKey" class="input" required placeholder="如：editor（英文小写）" pattern="[a-z_-]+" />
            </div>
            <div class="form-field">
              <label>排序</label>
              <input v-model.number="editDialog.sort" type="number" class="input" placeholder="0" />
            </div>
            <div class="form-field">
              <label>备注</label>
              <input v-model="editDialog.remark" class="input" placeholder="选填" />
            </div>
            <div v-if="editError" class="form-error">{{ editError }}</div>
            <div class="modal-footer">
              <button type="button" class="btn-cancel" @click="editDialog = null">取消</button>
              <button type="submit" class="btn-confirm" :disabled="saving">保存</button>
            </div>
          </form>
        </div>
      </div>
    </Transition>

    <!-- 菜单分配弹窗（带折叠） -->
    <Transition name="modal">
      <div v-if="menuDialog" class="modal-overlay" @click.self="menuDialog = null">
        <div class="modal-card modal-lg">
          <div class="modal-header">
            <h3>📂 分配菜单 — {{ menuDialog.roleName }}</h3>
            <button class="modal-close" @click="menuDialog = null">✕</button>
          </div>
          <div class="modal-body menu-tree-body">
            <div class="tree-toolbar">
              <button class="btn-sm btn-outline" @click="toggleAllMenus">全选 / 取消全选</button>
              <button class="btn-sm btn-outline" @click="expandAll">全部展开</button>
              <button class="btn-sm btn-outline" @click="collapseAll">全部折叠</button>
            </div>
            <div class="menu-tree">
              <div v-for="m in menuTree" :key="m.menuId" class="tree-group">
                <div class="tree-label" :class="{ 'is-dir': m.menuType === 'M' }">
                  <button v-if="m.children?.length" class="fold-btn" @click="toggleFold(m.menuId)">
                    {{ foldedIds.has(m.menuId) ? '▶' : '▼' }}
                  </button>
                  <span v-else class="fold-placeholder"></span>
                  <label class="tree-check">
                    <input type="checkbox" :value="m.menuId" v-model="checkedMenuIds" />
                    <span class="tree-icon">{{ m.menuType === 'M' ? '📁' : '📄' }}</span>
                    {{ m.menuName }}
                  </label>
                </div>
                <div v-if="m.children?.length && !foldedIds.has(m.menuId)" class="tree-children">
                  <div v-for="c in m.children" :key="c.menuId" class="tree-group">
                    <div class="tree-label">
                      <button v-if="c.children?.length" class="fold-btn" @click="toggleFold(c.menuId)">
                        {{ foldedIds.has(c.menuId) ? '▶' : '▼' }}
                      </button>
                      <span v-else class="fold-placeholder"></span>
                      <label class="tree-check">
                        <input type="checkbox" :value="c.menuId" v-model="checkedMenuIds" />
                        <span class="tree-icon">📋</span>
                        {{ c.menuName }}
                      </label>
                    </div>
                    <div v-if="c.children?.length && !foldedIds.has(c.menuId)" class="tree-children">
                      <label v-for="b in c.children" :key="b.menuId" class="tree-label tree-leaf">
                        <span class="fold-placeholder"></span>
                        <span class="tree-check">
                          <input type="checkbox" :value="b.menuId" v-model="checkedMenuIds" />
                          <span class="tree-icon">🔘</span>
                          {{ b.menuName }}
                          <span class="perm-tag" v-if="b.perms">{{ b.perms }}</span>
                        </span>
                      </label>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn-cancel" @click="menuDialog = null">取消</button>
            <button class="btn-confirm" @click="handleSaveMenus" :disabled="saving">保存</button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- 删除确认 -->
    <Transition name="modal">
      <div v-if="deleteTarget" class="modal-overlay" @click.self="deleteTarget = null">
        <div class="modal-card modal-sm">
          <div class="modal-header"><h3>⚠️ 确认删除</h3></div>
          <div class="modal-body">
            <p>确定要删除角色 <b>{{ deleteTarget.roleName }}</b> 吗？</p>
            <p class="form-hint">如果该角色下有用户，删除会失败。</p>
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
import { useGlobal } from '../composables/useGlobal'
import api from '../api'

interface RoleInfo {
  roleId: number
  roleName: string
  roleKey: string
  sort: number
  status: string
  remark: string
  menuIds: number[]
}

interface MenuItem {
  menuId: number
  menuName: string
  menuType: string
  perms?: string
  children?: MenuItem[]
}

const { showToast } = useGlobal()

const roles = ref<RoleInfo[]>([])
const menuTree = ref<MenuItem[]>([])
const allMenuIds = ref<number[]>([])

// 编辑
const editDialog = ref<any>(null)
const editError = ref('')
const saving = ref(false)

// 菜单分配
const menuDialog = ref<RoleInfo | null>(null)
const checkedMenuIds = ref<number[]>([])
const foldedIds = ref<Set<number>>(new Set())

// 删除
const deleteTarget = ref<RoleInfo | null>(null)
const deleting = ref(false)

async function loadRoles() {
  try {
    const { data } = await api.listRoles()
    roles.value = data.data || []
  } catch { /* interceptor */ }
}

async function loadMenuTree() {
  try {
    const { data } = await api.listMenus()
    menuTree.value = data.data || []
    allMenuIds.value = flattenMenuIds(data.data || [])
  } catch { /* interceptor */ }
}

function flattenMenuIds(items: MenuItem[]): number[] {
  const ids: number[] = []
  for (const item of items) {
    ids.push(item.menuId)
    if (item.children?.length) ids.push(...flattenMenuIds(item.children))
  }
  return ids
}

// 折叠控制
function toggleFold(menuId: number) {
  const s = new Set(foldedIds.value)
  if (s.has(menuId)) s.delete(menuId)
  else s.add(menuId)
  foldedIds.value = s
}

function expandAll() {
  foldedIds.value = new Set()
}

function collapseAll() {
  const s = new Set<number>()
  for (const m of menuTree.value) {
    if (m.children?.length) {
      s.add(m.menuId)
      for (const c of m.children) {
        if (c.children?.length) s.add(c.menuId)
      }
    }
  }
  foldedIds.value = s
}

function openAdd() {
  editError.value = ''
  editDialog.value = { roleName: '', roleKey: '', sort: 0, remark: '' }
}

function openEdit(r: RoleInfo) {
  editError.value = ''
  editDialog.value = { roleId: r.roleId, roleName: r.roleName, sort: r.sort, remark: r.remark || '' }
}

async function handleSave() {
  if (!editDialog.value) return
  saving.value = true
  editError.value = ''
  try {
    const body = { roleName: editDialog.value.roleName, sort: editDialog.value.sort, remark: editDialog.value.remark || undefined }
    if (editDialog.value.roleId) {
      await api.updateRole(editDialog.value.roleId, body)
      showToast('角色更新成功', 'success')
    } else {
      await api.addRole({ ...body, roleKey: editDialog.value.roleKey })
      showToast('角色创建成功', 'success')
    }
    editDialog.value = null
    await loadRoles()
  } catch (e: any) {
    editError.value = e?.response?.data?.error || '保存失败'
  } finally { saving.value = false }
}

function openMenuAssign(r: RoleInfo) {
  menuDialog.value = r
  checkedMenuIds.value = [...(r.menuIds || [])]
  foldedIds.value = new Set()
}

function toggleAllMenus() {
  if (checkedMenuIds.value.length === allMenuIds.value.length) {
    checkedMenuIds.value = []
  } else {
    checkedMenuIds.value = [...allMenuIds.value]
  }
}

async function handleSaveMenus() {
  if (!menuDialog.value) return
  saving.value = true
  try {
    await api.updateRole(menuDialog.value.roleId, { menuIds: checkedMenuIds.value })
    menuDialog.value.menuIds = [...checkedMenuIds.value]
    showToast('菜单权限已更新', 'success')
    menuDialog.value = null
  } catch { /* interceptor */ }
  finally { saving.value = false }
}

function confirmDelete(r: RoleInfo) { deleteTarget.value = r }

async function handleDelete() {
  if (!deleteTarget.value) return
  deleting.value = true
  try {
    await api.deleteRole(deleteTarget.value.roleId)
    showToast(`角色 ${deleteTarget.value.roleName} 已删除`, 'success')
    deleteTarget.value = null
    await loadRoles()
  } catch { /* interceptor */ }
  finally { deleting.value = false }
}

onMounted(() => { loadRoles(); loadMenuTree() })
</script>

<style scoped>
.page-hero { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 24px; flex-wrap: wrap; gap: 12px; }
.page-title { font-size: 28px; font-weight: 800; letter-spacing: -0.5px; margin-bottom: 4px; }
.page-subtitle { color: var(--text-secondary); font-size: 14px; }
.btn-primary { padding: 10px 18px; border: none; border-radius: var(--radius-sm); background: linear-gradient(135deg, var(--accent), var(--purple)); color: #fff; font-size: 13px; font-weight: 600; cursor: pointer; font-family: var(--font); transition: all 0.2s; }
.btn-primary:hover { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(99,102,241,0.4); }

.roles-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; }
.role-card { background: var(--bg-card); border-radius: var(--radius-lg); box-shadow: var(--shadow-sm); border: 1px solid var(--border-light); overflow: hidden; }
.role-card-header { display: flex; justify-content: space-between; align-items: center; padding: 20px; border-bottom: 1px solid var(--border-light); }
.role-name { font-size: 16px; font-weight: 700; margin-right: 8px; }
.role-key { font-size: 12px; font-weight: 600; color: var(--accent); background: var(--accent-bg); padding: 2px 8px; border-radius: 999px; }
.role-actions { display: flex; gap: 6px; }
.role-protected { font-size: 12px; color: var(--text-muted); }
.role-card-body { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; }
.menu-count { display: flex; align-items: center; gap: 8px; font-size: 13px; color: var(--text-secondary); }
.mc-icon { font-size: 18px; }

.btn-sm { padding: 4px 10px; border: 1px solid var(--border); border-radius: var(--radius-sm); font-size: 12px; cursor: pointer; transition: all 0.2s; background: var(--bg-card); font-family: var(--font); }
.btn-accent { color: var(--accent); border-color: var(--accent); }
.btn-accent:hover { background: var(--accent-bg); }
.btn-outline { color: var(--text-secondary); }
.btn-outline:hover { border-color: var(--accent); color: var(--accent); }
.btn-danger { color: var(--red); border-color: var(--red); }
.btn-danger:hover { background: var(--red-bg); }
.btn-sm:disabled { opacity: 0.5; cursor: not-allowed; }

/* Modal */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; z-index: 1000; padding: 24px; }
.modal-card { background: var(--bg-card); border-radius: var(--radius-lg); box-shadow: var(--shadow-lg); width: 100%; max-width: 480px; max-height: 90vh; overflow-y: auto; }
.modal-sm { max-width: 400px; }
.modal-lg { max-width: 600px; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 20px 24px; border-bottom: 1px solid var(--border-light); }
.modal-header h3 { font-size: 16px; font-weight: 700; }
.modal-close { width: 32px; height: 32px; border-radius: 50%; border: none; background: var(--bg); color: var(--text-muted); font-size: 16px; cursor: pointer; display: flex; align-items: center; justify-content: center; }
.modal-close:hover { background: var(--red-bg); color: var(--red); }
.modal-body { padding: 20px 24px; }
.modal-body p { font-size: 14px; line-height: 1.6; color: var(--text-secondary); }
.modal-footer { display: flex; justify-content: flex-end; gap: 10px; padding: 16px 24px; border-top: 1px solid var(--border-light); }

.form-field { margin-bottom: 16px; }
.form-field label { display: block; font-size: 13px; font-weight: 600; margin-bottom: 6px; }
.form-hint { font-size: 12px; color: var(--text-muted); margin-top: 8px; }
.req { color: var(--red); }
.input { width: 100%; padding: 10px 14px; border: 1px solid var(--border); border-radius: var(--radius-sm); font-size: 14px; outline: none; transition: border-color 0.2s; background: var(--bg); color: var(--text-primary); box-sizing: border-box; }
.input:focus { border-color: var(--accent); }
.form-error { padding: 10px 14px; background: var(--red-bg); color: var(--red); border-radius: var(--radius-sm); font-size: 13px; margin-bottom: 16px; }
.btn-cancel { padding: 10px 20px; border: 1px solid var(--border); border-radius: var(--radius-sm); background: var(--bg-card); color: var(--text-secondary); font-size: 14px; cursor: pointer; font-family: var(--font); }
.btn-confirm { padding: 10px 20px; border: none; border-radius: var(--radius-sm); background: var(--accent); color: #fff; font-size: 14px; font-weight: 600; cursor: pointer; font-family: var(--font); }
.btn-confirm:disabled { opacity: 0.5; }
.modal-footer .btn-danger { padding: 10px 20px; border: none; border-radius: var(--radius-sm); background: var(--red); color: #fff; font-size: 14px; font-weight: 600; cursor: pointer; font-family: var(--font); }

/* Menu Tree with fold */
.menu-tree-body { max-height: 60vh; overflow-y: auto; }
.tree-toolbar { display: flex; gap: 8px; margin-bottom: 12px; }
.menu-tree { display: flex; flex-direction: column; gap: 2px; }
.tree-group { }
.tree-label { display: flex; align-items: center; gap: 6px; padding: 7px 10px; border-radius: var(--radius-sm); cursor: default; font-size: 14px; transition: background 0.15s; }
.tree-label:hover { background: var(--bg); }
.tree-label.is-dir .tree-check { font-weight: 600; }
.fold-btn { width: 20px; height: 20px; border: none; background: none; color: var(--text-muted); font-size: 10px; cursor: pointer; display: flex; align-items: center; justify-content: center; border-radius: 4px; padding: 0; flex-shrink: 0; }
.fold-btn:hover { background: var(--border-light); color: var(--text-primary); }
.fold-placeholder { width: 20px; flex-shrink: 0; }
.tree-check { display: flex; align-items: center; gap: 8px; cursor: pointer; flex: 1; }
.tree-icon { font-size: 14px; }
.tree-children { margin-left: 26px; border-left: 2px solid var(--border-light); padding-left: 6px; }
.tree-leaf { font-size: 13px; color: var(--text-secondary); }
.perm-tag { font-size: 11px; color: var(--text-muted); background: var(--bg); padding: 1px 6px; border-radius: 4px; margin-left: auto; font-family: monospace; }

.modal-enter-active { animation: fadeIn 0.2s ease; }
.modal-leave-active { animation: fadeIn 0.2s ease reverse; }
.modal-enter-active .modal-card { animation: scaleIn 0.25s ease; }
.modal-leave-active .modal-card { animation: scaleIn 0.2s ease reverse; }
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes scaleIn { from { transform: scale(0.95); opacity: 0; } to { transform: scale(1); opacity: 1; } }

@media (max-width: 768px) {
  .page-hero { flex-direction: column; align-items: flex-start; }
  .roles-grid { grid-template-columns: 1fr; }
}
</style>
