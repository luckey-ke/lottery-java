<template>
  <div class="icon-picker" ref="pickerRef">
    <button type="button" class="icon-trigger" :class="{ active: showPicker }" @click="showPicker = !showPicker">
      <span class="icon-preview">{{ currentEmoji }}</span>
      <span class="icon-name" v-if="modelValue">{{ modelValue }}</span>
      <span class="icon-placeholder" v-else>选择图标</span>
      <span class="icon-arrow">▾</span>
    </button>

    <Transition name="dropdown">
      <div v-if="showPicker" class="icon-dropdown">
        <div class="icon-search">
          <input
            ref="searchInput"
            v-model="keyword"
            class="icon-search-input"
            placeholder="搜索图标..."
            @keydown.esc="showPicker = false"
          />
        </div>
        <div class="icon-categories">
          <button
            v-for="cat in categories"
            :key="cat.key"
            type="button"
            class="cat-btn"
            :class="{ active: activeCat === cat.key }"
            @click="activeCat = cat.key"
          >{{ cat.label }}</button>
        </div>
        <div class="icon-grid">
          <button
            v-for="icon in filteredIcons"
            :key="icon.name"
            type="button"
            class="icon-item"
            :class="{ selected: modelValue === icon.name }"
            :title="icon.name"
            @click="select(icon.name)"
          >{{ icon.emoji }}</button>
          <div v-if="!filteredIcons.length" class="icon-empty">没有匹配的图标</div>
        </div>
        <div class="icon-footer">
          <button type="button" class="icon-clear" @click="select('')">清除图标</button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'

const props = defineProps<{ modelValue: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()

const showPicker = ref(false)
const keyword = ref('')
const activeCat = ref('all')
const searchInput = ref<HTMLInputElement | null>(null)
const pickerRef = ref<HTMLElement | null>(null)

interface IconEntry { name: string; emoji: string; cat: string }

const icons: IconEntry[] = [
  // 系统
  { name: 'setting', emoji: '⚙️', cat: 'system' },
  { name: 'user', emoji: '👤', cat: 'system' },
  { name: 'users', emoji: '👥', cat: 'system' },
  { name: 'peoples', emoji: '👥', cat: 'system' },
  { name: 'role', emoji: '🛡️', cat: 'system' },
  { name: 'menu', emoji: '📂', cat: 'system' },
  { name: 'tree-table', emoji: '📂', cat: 'system' },
  { name: 'lock', emoji: '🔒', cat: 'system' },
  { name: 'key', emoji: '🔑', cat: 'system' },
  { name: 'password', emoji: '🔑', cat: 'system' },
  { name: 'bell', emoji: '🔔', cat: 'system' },
  { name: 'setting-fill', emoji: '🛠️', cat: 'system' },

  // 数据
  { name: 'dashboard', emoji: '📊', cat: 'data' },
  { name: 'data-board', emoji: '📋', cat: 'data' },
  { name: 'chart', emoji: '📈', cat: 'data' },
  { name: 'chart-bar', emoji: '📊', cat: 'data' },
  { name: 'trend', emoji: '🔥', cat: 'data' },
  { name: 'trend-charts', emoji: '🔥', cat: 'data' },
  { name: 'analysis', emoji: '📈', cat: 'data' },
  { name: 'pie-chart', emoji: '🥧', cat: 'data' },
  { name: 'data', emoji: '📋', cat: 'data' },
  { name: 'database', emoji: '🗄️', cat: 'data' },
  { name: 'table', emoji: '📑', cat: 'data' },
  { name: 'list', emoji: '📝', cat: 'data' },

  // 功能
  { name: 'recommend', emoji: '🎯', cat: 'action' },
  { name: 'magic-stick', emoji: '🎯', cat: 'action' },
  { name: 'history', emoji: '📋', cat: 'action' },
  { name: 'search', emoji: '🔍', cat: 'action' },
  { name: 'add', emoji: '➕', cat: 'action' },
  { name: 'edit', emoji: '✏️', cat: 'action' },
  { name: 'delete', emoji: '🗑️', cat: 'action' },
  { name: 'download', emoji: '⬇️', cat: 'action' },
  { name: 'upload', emoji: '⬆️', cat: 'action' },
  { name: 'refresh', emoji: '🔄', cat: 'action' },
  { name: 'filter', emoji: '🔧', cat: 'action' },
  { name: 'copy', emoji: '📋', cat: 'action' },
  { name: 'share', emoji: '📤', cat: 'action' },
  { name: 'link', emoji: '🔗', cat: 'action' },

  // 导航
  { name: 'home', emoji: '🏠', cat: 'nav' },
  { name: 'admin', emoji: '⚙️', cat: 'nav' },
  { name: 'back', emoji: '🚪', cat: 'nav' },
  { name: 'logout', emoji: '🚪', cat: 'nav' },
  { name: 'profile', emoji: '✏️', cat: 'nav' },
  { name: 'message', emoji: '💬', cat: 'nav' },
  { name: 'email', emoji: '📧', cat: 'nav' },
  { name: 'calendar', emoji: '📅', cat: 'nav' },
  { name: 'clock', emoji: '🕐', cat: 'nav' },
  { name: 'notification', emoji: '🔔', cat: 'nav' },
  { name: 'help', emoji: '❓', cat: 'nav' },
  { name: 'info', emoji: 'ℹ️', cat: 'nav' },

  // 状态
  { name: 'check', emoji: '✅', cat: 'status' },
  { name: 'close', emoji: '❌', cat: 'status' },
  { name: 'warning', emoji: '⚠️', cat: 'status' },
  { name: 'success', emoji: '✅', cat: 'status' },
  { name: 'error', emoji: '❌', cat: 'status' },
  { name: 'star', emoji: '⭐', cat: 'status' },
  { name: 'heart', emoji: '❤️', cat: 'status' },
  { name: 'fire', emoji: '🔥', cat: 'status' },
  { name: 'trophy', emoji: '🏆', cat: 'status' },
  { name: 'medal', emoji: '🏅', cat: 'status' },

  // 彩票
  { name: 'lottery', emoji: '🎱', cat: 'lottery' },
  { name: 'ticket', emoji: '🎫', cat: 'lottery' },
  { name: 'number', emoji: '🔢', cat: 'lottery' },
  { name: 'game', emoji: '🎮', cat: 'lottery' },
  { name: 'rocket', emoji: '🚀', cat: 'lottery' },
  { name: 'dice', emoji: '🎲', cat: 'lottery' },
  { name: 'slot', emoji: '🎰', cat: 'lottery' },
  { name: 'gift', emoji: '🎁', cat: 'lottery' },
]

const categories = [
  { key: 'all', label: '全部' },
  { key: 'system', label: '系统' },
  { key: 'data', label: '数据' },
  { key: 'action', label: '功能' },
  { key: 'nav', label: '导航' },
  { key: 'status', label: '状态' },
  { key: 'lottery', label: '彩票' },
]

const currentEmoji = computed(() => {
  if (!props.modelValue) return '📄'
  const found = icons.find(i => i.name === props.modelValue)
  return found?.emoji || props.modelValue || '📄'
})

const filteredIcons = computed(() => {
  let list = icons
  if (activeCat.value !== 'all') {
    list = list.filter(i => i.cat === activeCat.value)
  }
  if (keyword.value) {
    const kw = keyword.value.toLowerCase()
    list = list.filter(i => i.name.includes(kw) || i.emoji.includes(kw))
  }
  return list
})

function select(name: string) {
  emit('update:modelValue', name)
  showPicker.value = false
  keyword.value = ''
}

watch(showPicker, (v) => {
  if (v) nextTick(() => searchInput.value?.focus())
})

function onClickOutside(e: MouseEvent) {
  if (pickerRef.value && !pickerRef.value.contains(e.target as Node)) {
    showPicker.value = false
    keyword.value = ''
  }
}
onMounted(() => document.addEventListener('mousedown', onClickOutside))
onUnmounted(() => document.removeEventListener('mousedown', onClickOutside))
</script>

<style scoped>
.icon-picker { position: relative; }

.icon-trigger {
  width: 100%; display: flex; align-items: center; gap: 8px;
  padding: 10px 14px; border: 1px solid var(--border);
  border-radius: var(--radius-sm); background: var(--bg);
  color: var(--text-primary); cursor: pointer; font-family: var(--font);
  font-size: 14px; transition: border-color 0.2s; text-align: left;
}
.icon-trigger:hover, .icon-trigger.active { border-color: var(--accent); }
.icon-preview { font-size: 20px; flex-shrink: 0; }
.icon-placeholder { color: var(--text-muted); }
.icon-name { flex: 1; font-family: 'SF Mono', Consolas, monospace; font-size: 12px; color: var(--text-secondary); }
.icon-arrow { font-size: 10px; color: var(--text-muted); margin-left: auto; }

.icon-dropdown {
  position: absolute; top: calc(100% + 6px); left: 0; right: 0;
  background: var(--bg-card); border: 1px solid var(--border-light);
  border-radius: var(--radius-lg); box-shadow: var(--shadow-lg);
  z-index: 500; overflow: hidden; min-width: 320px;
}

.icon-search { padding: 10px 12px; border-bottom: 1px solid var(--border-light); }
.icon-search-input {
  width: 100%; padding: 8px 12px; border: 1px solid var(--border);
  border-radius: var(--radius-sm); font-size: 13px; outline: none;
  background: var(--bg); color: var(--text-primary); font-family: var(--font);
  box-sizing: border-box;
}
.icon-search-input:focus { border-color: var(--accent); }

.icon-categories {
  display: flex; gap: 4px; padding: 8px 12px;
  border-bottom: 1px solid var(--border-light);
  flex-wrap: wrap;
}
.cat-btn {
  padding: 4px 10px; border: 1px solid var(--border);
  border-radius: 999px; background: none; font-size: 12px;
  cursor: pointer; color: var(--text-secondary); font-family: var(--font);
  transition: all 0.15s;
}
.cat-btn:hover { border-color: var(--accent); color: var(--accent); }
.cat-btn.active { background: var(--accent); color: #fff; border-color: var(--accent); }

.icon-grid {
  display: grid; grid-template-columns: repeat(8, 1fr);
  gap: 2px; padding: 8px; max-height: 220px; overflow-y: auto;
}
.icon-item {
  display: flex; align-items: center; justify-content: center;
  width: 36px; height: 36px; border: 2px solid transparent;
  border-radius: var(--radius-sm); background: none; cursor: pointer;
  font-size: 20px; transition: all 0.15s;
}
.icon-item:hover {
  background: var(--accent-bg);
  border-color: var(--accent);
  transform: scale(1.15);
}
.icon-item.selected {
  background: var(--accent-bg);
  border-color: var(--accent);
}
.icon-empty {
  grid-column: 1 / -1; text-align: center; padding: 24px;
  color: var(--text-muted); font-size: 13px;
}

.icon-footer {
  padding: 8px 12px; border-top: 1px solid var(--border-light);
  display: flex; justify-content: flex-end;
}
.icon-clear {
  padding: 4px 12px; border: 1px solid var(--border);
  border-radius: var(--radius-sm); background: none;
  color: var(--text-muted); font-size: 12px; cursor: pointer;
  font-family: var(--font); transition: all 0.15s;
}
.icon-clear:hover { border-color: var(--red); color: var(--red); }

.dropdown-enter-active { animation: dropIn 0.15s ease; }
.dropdown-leave-active { animation: dropIn 0.1s ease reverse; }
@keyframes dropIn { from { opacity: 0; transform: translateY(-6px); } to { opacity: 1; transform: translateY(0); } }
</style>
