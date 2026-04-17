<template>
  <div class="menus-page">
    <div class="page-hero">
      <div>
        <h1 class="page-title">菜单管理</h1>
        <p class="page-subtitle">管理系统目录、菜单和按钮权限</p>
      </div>
      <button class="btn-primary" @click="openAdd(null)">➕ 新增菜单</button>
    </div>

    <!-- 菜单树表格 -->
    <div class="table-section">
      <!-- 错误状态 -->
      <div v-if="loadError" class="empty-state">
        <span class="empty-icon">❌</span>
        <span>{{ loadError }}</span>
        <button class="btn-sm btn-outline" @click="loadMenus">重试</button>
      </div>
      <!-- 加载中 -->
      <div v-else-if="loading" class="empty-state">
        <span class="empty-icon">⏳</span>
        <span>加载中...</span>
      </div>
      <!-- 空状态 -->
      <div v-else-if="!menuTree.length" class="empty-state">
        <span class="empty-icon">📂</span>
        <span>暂无菜单数据</span>
      </div>
      <div v-else class="table-scroll">
        <table class="data-table">
          <thead>
            <tr>
              <th>菜单名称</th>
              <th>类型</th>
              <th>路由/权限</th>
              <th>图标</th>
              <th>排序</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <template v-for="m in menuTree" :key="m.menuId">
              <tr>
                <td class="name-cell">
                  <span class="tree-icon">{{ m.menuType === 'M' ? '📁' : '📄' }}</span>
                  {{ m.menuName }}
                </td>
                <td><span class="type-tag" :class="m.menuType">{{ typeLabel(m.menuType) }}</span></td>
                <td class="mono text-muted">{{ m.path || '-' }}</td>
                <td>{{ m.icon || '-' }}</td>
                <td>{{ m.orderNum }}</td>
                <td><span :class="['status-dot', m.status === '0' ? 'active' : 'disabled']">{{ m.status === '0' ? '正常' : '停用' }}</span></td>
                <td class="actions-cell">
                  <button class="btn-xs btn-accent" @click="openAdd(m)" v-if="m.menuType !== 'F'">添加子项</button>
                  <button class="btn-xs" @click="openEdit(m)">编辑</button>
                  <button class="btn-xs btn-danger" @click="confirmDelete(m)">删除</button>
                </td>
              </tr>
              <template v-if="m.children?.length">
                <tr v-for="c in m.children" :key="c.menuId">
                  <td class="name-cell indent-1">
                    <span class="tree-icon">{{ c.menuType === 'M' ? '📁' : c.menuType === 'F' ? '🔘' : '📋' }}</span>
                    {{ c.menuName }}
                  </td>
                  <td><span class="type-tag" :class="c.menuType">{{ typeLabel(c.menuType) }}</span></td>
                  <td class="mono text-muted">{{ c.perms || c.path || '-' }}</td>
                  <td>{{ c.icon || '-' }}</td>
                  <td>{{ c.orderNum }}</td>
                  <td><span :class="['status-dot', c.status === '0' ? 'active' : 'disabled']">{{ c.status === '0' ? '正常' : '停用' }}</span></td>
                  <td class="actions-cell">
                    <button class="btn-xs btn-accent" @click="openAdd(c)" v-if="c.menuType !== 'F'">添加子项</button>
                    <button class="btn-xs" @click="openEdit(c)">编辑</button>
                    <button class="btn-xs btn-danger" @click="confirmDelete(c)">删除</button>
                  </td>
                </tr>
                <template v-if="c.children?.length">
                  <tr v-for="b in c.children" :key="b.menuId">
                    <td class="name-cell indent-2">
                      <span class="tree-icon">🔘</span>
                      {{ b.menuName }}
                    </td>
                    <td><span class="type-tag F">{{ typeLabel(b.menuType) }}</span></td>
                    <td class="mono text-muted">{{ b.perms || '-' }}</td>
                    <td>-</td>
                    <td>{{ b.orderNum }}</td>
                    <td><span :class="['status-dot', b.status === '0' ? 'active' : 'disabled']">{{ b.status === '0' ? '正常' : '停用' }}</span></td>
                    <td class="actions-cell">
                      <button class="btn-xs" @click="openEdit(b)">编辑</button>
                      <button class="btn-xs btn-danger" @click="confirmDelete(b)">删除</button>
                    </td>
                  </tr>
                </template>
              </template>
            </template>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <Transition name="modal">
      <div v-if="editDialog" class="modal-overlay" @click.self="editDialog = null">
        <div class="modal-card">
          <div class="modal-header">
            <h3>{{ editDialog.menuId ? '✏️ 编辑菜单' : '➕ 新增菜单' }}</h3>
            <button class="modal-close" @click="editDialog = null">✕</button>
          </div>
          <form class="modal-body" @submit.prevent="handleSave">
            <div class="form-field">
              <label>上级菜单</label>
              <select v-model="editDialog.parentId" class="input">
                <option :value="0">顶级菜单</option>
                <option v-for="m in flatMenus" :key="m.menuId" :value="m.menuId">{{ m.menuName }}</option>
              </select>
            </div>
            <div class="form-row">
              <div class="form-field">
                <label>菜单类型 <span class="req">*</span></label>
                <select v-model="editDialog.menuType" class="input" required>
                  <option value="M">目录</option>
                  <option value="C">菜单</option>
                  <option value="F">按钮</option>
                </select>
              </div>
              <div class="form-field">
                <label>排序</label>
                <input v-model.number="editDialog.orderNum" type="number" class="input" placeholder="0" />
              </div>
            </div>
            <div class="form-field">
              <label>菜单名称 <span class="req">*</span></label>
              <input v-model="editDialog.menuName" class="input" required placeholder="如：用户管理" />
            </div>
            <div class="form-field" v-if="editDialog.menuType !== 'F'">
              <label>路由地址</label>
              <input v-model="editDialog.path" class="input" placeholder="如：user" />
            </div>
            <div class="form-field" v-if="editDialog.menuType === 'C'">
              <label>组件路径</label>
              <input v-model="editDialog.component" class="input" placeholder="如：system/user/User" />
            </div>
            <div class="form-field" v-if="editDialog.menuType === 'F'">
              <label>权限标识</label>
              <input v-model="editDialog.perms" class="input" placeholder="如：system:user:list" />
            </div>
            <div class="form-field" v-if="editDialog.menuType !== 'F'">
              <label>图标</label>
              <input v-model="editDialog.icon" class="input" placeholder="如：user、setting" />
            </div>
            <div class="form-row">
              <div class="form-field">
                <label>状态</label>
                <select v-model="editDialog.status" class="input">
                  <option value="0">正常</option>
                  <option value="1">停用</option>
                </select>
              </div>
              <div class="form-field">
                <label>可见</label>
                <select v-model="editDialog.visible" class="input">
                  <option value="0">显示</option>
                  <option value="1">隐藏</option>
                </select>
              </div>
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

    <!-- 删除确认 -->
    <Transition name="modal">
      <div v-if="deleteTarget" class="modal-overlay" @click.self="deleteTarget = null">
        <div class="modal-card modal-sm">
          <div class="modal-header"><h3>⚠️ 确认删除</h3></div>
          <div class="modal-body">
            <p>确定要删除菜单 <b>{{ deleteTarget.menuName }}</b> 吗？</p>
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
import { ref, computed, onMounted } from 'vue'
import { useGlobal } from '../composables/useGlobal'
import api from '../api'

interface MenuItem {
  menuId: number
  menuName: string
  parentId: number
  orderNum: number
  path: string
  component: string
  menuType: string
  perms: string
  icon: string
  visible: string
  status: string
  children?: MenuItem[]
}

const { showToast } = useGlobal()

const menuTree = ref<MenuItem[]>([])
const loading = ref(false)
const loadError = ref('')

// 平铺菜单（供上级菜单选择用）
const flatMenus = computed(() => {
  const list: MenuItem[] = []
  for (const m of menuTree.value) {
    if (m.menuType !== 'F') list.push(m)
    if (m.children) {
      for (const c of m.children) {
        if (c.menuType !== 'F') list.push(c)
      }
    }
  }
  return list
})

// 编辑
const editDialog = ref<any>(null)
const editError = ref('')
const saving = ref(false)

// 删除
const deleteTarget = ref<MenuItem | null>(null)
const deleting = ref(false)

function typeLabel(t: string) {
  return { M: '目录', C: '菜单', F: '按钮' }[t] || t
}

async function loadMenus() {
  loading.value = true
  loadError.value = ''
  try {
    const { data } = await api.listMenus()
    menuTree.value = data.data || []
  } catch (e: any) {
    loadError.value = e?.response?.data?.error || '加载菜单失败'
  } finally {
    loading.value = false
  }
}

function openAdd(parent: MenuItem | null) {
  editError.value = ''
  editDialog.value = {
    menuName: '', parentId: parent?.menuId || 0, orderNum: 0,
    path: '', component: '', menuType: parent ? (parent.menuType === 'M' ? 'C' : 'F') : 'M',
    perms: '', icon: '', visible: '0', status: '0',
  }
}

function openEdit(m: MenuItem) {
  editError.value = ''
  editDialog.value = {
    menuId: m.menuId, menuName: m.menuName, parentId: m.parentId, orderNum: m.orderNum,
    path: m.path || '', component: m.component || '', menuType: m.menuType,
    perms: m.perms || '', icon: m.icon || '', visible: m.visible, status: m.status,
  }
}

async function handleSave() {
  if (!editDialog.value) return
  saving.value = true
  editError.value = ''
  try {
    const body = { ...editDialog.value }
    if (body.menuType === 'F') { body.path = '#'; body.component = undefined; body.icon = undefined }
    if (body.menuId) {
      await api.updateMenu(body.menuId, body)
      showToast('菜单更新成功', 'success')
    } else {
      await api.addMenu(body)
      showToast('菜单创建成功', 'success')
    }
    editDialog.value = null
    await loadMenus()
  } catch (e: any) {
    editError.value = e?.response?.data?.error || '保存失败'
  } finally { saving.value = false }
}

function confirmDelete(m: MenuItem) { deleteTarget.value = m }

async function handleDelete() {
  if (!deleteTarget.value) return
  deleting.value = true
  try {
    await api.deleteMenu(deleteTarget.value.menuId)
    showToast(`菜单 ${deleteTarget.value.menuName} 已删除`, 'success')
    deleteTarget.value = null
    await loadMenus()
  } catch { /* interceptor */ }
  finally { deleting.value = false }
}

onMounted(loadMenus)
</script>

<style scoped>
.page-hero { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 24px; flex-wrap: wrap; gap: 12px; }
.page-title { font-size: 28px; font-weight: 800; letter-spacing: -0.5px; margin-bottom: 4px; }
.page-subtitle { color: var(--text-secondary); font-size: 14px; }
.btn-primary { padding: 10px 18px; border: none; border-radius: var(--radius-sm); background: linear-gradient(135deg, var(--accent), var(--purple)); color: #fff; font-size: 13px; font-weight: 600; cursor: pointer; font-family: var(--font); transition: all 0.2s; }
.btn-primary:hover { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(99,102,241,0.4); }

.table-section { background: var(--bg-card); border-radius: var(--radius-lg); box-shadow: var(--shadow-sm); overflow: hidden; }
.table-scroll { overflow-x: auto; }
.data-table { width: 100%; border-collapse: collapse; font-size: 14px; }
.data-table th { padding: 12px 16px; text-align: left; font-weight: 600; color: var(--text-secondary); background: var(--bg); border-bottom: 1px solid var(--border); white-space: nowrap; }
.data-table td { padding: 12px 16px; border-bottom: 1px solid var(--border-light); }
.data-table tr:last-child td { border-bottom: none; }
.data-table tr:hover td { background: var(--bg-card-hover); }
.mono { font-family: 'SF Mono', Consolas, monospace; font-size: 12px; }
.text-muted { color: var(--text-muted); }

.name-cell { display: flex; align-items: center; gap: 8px; font-weight: 500; }
.indent-1 { padding-left: 36px !important; }
.indent-2 { padding-left: 64px !important; }
.tree-icon { font-size: 14px; }

.type-tag { display: inline-block; padding: 2px 8px; border-radius: 999px; font-size: 11px; font-weight: 600; }
.type-tag.M { background: var(--blue-bg); color: var(--blue); }
.type-tag.C { background: var(--green-bg); color: var(--green); }
.type-tag.F { background: var(--orange-bg); color: var(--orange); }

.status-dot { font-size: 12px; font-weight: 600; }
.status-dot.active { color: var(--green); }
.status-dot.disabled { color: var(--red); }

.actions-cell { display: flex; gap: 6px; }
.btn-xs { padding: 3px 8px; border: 1px solid var(--border); border-radius: var(--radius-sm); font-size: 11px; cursor: pointer; transition: all 0.2s; background: var(--bg-card); font-family: var(--font); }
.btn-sm { padding: 4px 10px; border: 1px solid var(--border); border-radius: var(--radius-sm); font-size: 12px; cursor: pointer; transition: all 0.2s; background: var(--bg-card); font-family: var(--font); }
.btn-outline { color: var(--text-secondary); }
.btn-outline:hover { border-color: var(--accent); color: var(--accent); }
.btn-accent { color: var(--accent); border-color: var(--accent); }
.btn-accent:hover { background: var(--accent-bg); }
.btn-danger { color: var(--red); border-color: var(--red); }
.btn-danger:hover { background: var(--red-bg); }

/* Modal */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; z-index: 1000; padding: 24px; }
.modal-card { background: var(--bg-card); border-radius: var(--radius-lg); box-shadow: var(--shadow-lg); width: 100%; max-width: 520px; max-height: 90vh; overflow-y: auto; }
.modal-sm { max-width: 400px; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 20px 24px; border-bottom: 1px solid var(--border-light); }
.modal-header h3 { font-size: 16px; font-weight: 700; }
.modal-close { width: 32px; height: 32px; border-radius: 50%; border: none; background: var(--bg); color: var(--text-muted); font-size: 16px; cursor: pointer; display: flex; align-items: center; justify-content: center; }
.modal-close:hover { background: var(--red-bg); color: var(--red); }
.modal-body { padding: 20px 24px; }
.modal-body p { font-size: 14px; line-height: 1.6; color: var(--text-secondary); }
.modal-footer { display: flex; justify-content: flex-end; gap: 10px; padding: 16px 24px; border-top: 1px solid var(--border-light); }

.form-field { margin-bottom: 16px; flex: 1; }
.form-field label { display: block; font-size: 13px; font-weight: 600; margin-bottom: 6px; }
.form-row { display: flex; gap: 16px; }
.req { color: var(--red); }
.input { width: 100%; padding: 10px 14px; border: 1px solid var(--border); border-radius: var(--radius-sm); font-size: 14px; outline: none; transition: border-color 0.2s; background: var(--bg); color: var(--text-primary); box-sizing: border-box; font-family: var(--font); }
.input:focus { border-color: var(--accent); }
.form-error { padding: 10px 14px; background: var(--red-bg); color: var(--red); border-radius: var(--radius-sm); font-size: 13px; margin-bottom: 16px; }
.btn-cancel { padding: 10px 20px; border: 1px solid var(--border); border-radius: var(--radius-sm); background: var(--bg-card); color: var(--text-secondary); font-size: 14px; cursor: pointer; font-family: var(--font); }
.btn-confirm { padding: 10px 20px; border: none; border-radius: var(--radius-sm); background: var(--accent); color: #fff; font-size: 14px; font-weight: 600; cursor: pointer; font-family: var(--font); }
.btn-confirm:disabled { opacity: 0.5; }
.modal-footer .btn-danger { padding: 10px 20px; border: none; border-radius: var(--radius-sm); background: var(--red); color: #fff; font-size: 14px; font-weight: 600; cursor: pointer; font-family: var(--font); }

.modal-enter-active { animation: fadeIn 0.2s ease; }
.modal-leave-active { animation: fadeIn 0.2s ease reverse; }
.modal-enter-active .modal-card { animation: scaleIn 0.25s ease; }
.modal-leave-active .modal-card { animation: scaleIn 0.2s ease reverse; }
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes scaleIn { from { transform: scale(0.95); opacity: 0; } to { transform: scale(1); opacity: 1; } }

.empty-state { display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 48px 20px; color: var(--text-muted); font-size: 14px; }
.empty-icon { font-size: 32px; }

@media (max-width: 768px) {
  .page-hero { flex-direction: column; align-items: flex-start; }
  .form-row { flex-direction: column; gap: 0; }
  .actions-cell { flex-direction: column; }
}
</style>
