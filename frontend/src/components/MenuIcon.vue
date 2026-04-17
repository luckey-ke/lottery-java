<template>
  <span class="menu-icon" :style="{ fontSize: size + 'px' }">{{ emoji }}</span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  name?: string | null
  size?: number
  fallback?: string
}>(), {
  name: '',
  size: 18,
  fallback: '📄',
})

// icon name → emoji 映射
const iconMap: Record<string, string> = {
  // 系统管理
  setting: '⚙️',
  settings: '⚙️',
  user: '👤',
  users: '👥',
  peoples: '👥',
  people: '👥',
  role: '🛡️',
  roles: '🛡️',
  menu: '📂',
  'tree-table': '📂',

  // 数据相关
  dashboard: '📊',
  'data-board': '📋',
  data: '📋',
  chart: '📈',
  charts: '📈',
  analysis: '📈',
  trend: '🔥',
  'trend-charts': '🔥',

  // 功能
  recommend: '🎯',
  'magic-stick': '🎯',
  history: '📋',
  search: '🔍',
  add: '➕',
  edit: '✏️',
  delete: '🗑️',
  download: '⬇️',
  upload: '⬆️',
  refresh: '🔄',
  filter: '🔧',

  // 导航
  home: '🏠',
  admin: '⚙️',
  back: '🚪',
  logout: '🚪',
  lock: '🔑',
  password: '🔑',
  profile: '✏️',
  bell: '🔔',
  notification: '🔔',
  message: '💬',
  email: '📧',
  calendar: '📅',
  clock: '🕐',
  star: '⭐',
  heart: '❤️',
  link: '🔗',
  share: '📤',
  copy: '📋',
  check: '✅',
  close: '❌',
  warning: '⚠️',
  info: 'ℹ️',
  help: '❓',

  // 前台页面
  ForegroundPage: '🌐',
  foreground: '🌐',

  // 彩票相关
  lottery: '🎱',
  ticket: '🎫',
  number: '🔢',
  game: '🎮',
  trophy: '🏆',
  medal: '🏅',
  fire: '🔥',
  rocket: '🚀',
}

const emoji = computed(() => {
  if (!props.name) return props.fallback
  // 先精确匹配，再小写匹配
  return iconMap[props.name] || iconMap[props.name.toLowerCase()] || props.name || props.fallback
})
</script>

<style scoped>
.menu-icon {
  line-height: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
</style>
