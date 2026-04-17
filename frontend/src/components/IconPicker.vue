<template>
  <div class="icon-picker" ref="pickerRef">
    <button type="button" class="icon-trigger" :class="{ active: showPicker }" @click="showPicker = !showPicker">
      <MenuIcon v-if="modelValue" :name="modelValue" :size="18" />
      <span v-else class="icon-placeholder">选择图标</span>
      <span class="icon-name" v-if="modelValue">{{ modelValue }}</span>
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
        <div class="icon-grid">
          <button
            v-for="icon in filteredIcons"
            :key="icon.name"
            type="button"
            class="icon-item"
            :class="{ selected: modelValue === icon.name }"
            :title="icon.name"
            @click="select(icon.name)"
          >
            <component :is="icon.component" />
          </button>
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
import * as ElementPlusIcons from '@element-plus/icons-vue'
import MenuIcon from './MenuIcon.vue'

defineProps<{ modelValue: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()

const showPicker = ref(false)
const keyword = ref('')
const searchInput = ref<HTMLInputElement | null>(null)
const pickerRef = ref<HTMLElement | null>(null)

// All Element Plus icons
const allIcons = computed(() => {
  return Object.entries(ElementPlusIcons)
    .filter(([name]) => name !== 'default' && !name.startsWith('__'))
    .map(([name, component]) => ({ name, component }))
})

const filteredIcons = computed(() => {
  if (!keyword.value) return allIcons.value
  const kw = keyword.value.toLowerCase()
  return allIcons.value.filter(i => i.name.toLowerCase().includes(kw))
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

.icon-grid {
  display: grid; grid-template-columns: repeat(8, 1fr);
  gap: 2px; padding: 8px; max-height: 260px; overflow-y: auto;
}
.icon-item {
  display: flex; align-items: center; justify-content: center;
  width: 36px; height: 36px; border: 1px solid transparent;
  border-radius: var(--radius-sm); background: none; cursor: pointer;
  color: var(--text-secondary); font-size: 18px; transition: all 0.15s;
}
.icon-item:hover {
  background: var(--accent-bg); color: var(--accent);
  border-color: var(--accent); transform: scale(1.1);
}
.icon-item.selected {
  background: var(--accent); color: #fff;
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
