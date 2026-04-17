<template>
  <span v-if="iconComponent" class="menu-icon-wrap" :style="{ width: size + 'px', height: size + 'px', color }">
    <component :is="iconComponent" />
  </span>
  <span v-else class="menu-icon-fallback">{{ fallback }}</span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import * as ElementPlusIcons from '@element-plus/icons-vue'

const props = withDefaults(defineProps<{
  name?: string | null
  size?: number
  color?: string
  fallback?: string
}>(), {
  name: '',
  size: 18,
  fallback: '📄',
})

function toPascalCase(str: string): string {
  return str
    .replace(/(^|[-_\s])(\w)/g, (_, __, c) => c.toUpperCase())
    .replace(/[-_\s]/g, '')
}

const iconComponent = computed(() => {
  if (!props.name) return null
  const pascal = toPascalCase(props.name)
  return (ElementPlusIcons as Record<string, any>)[pascal] || null
})
</script>

<style scoped>
.menu-icon-wrap {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.menu-icon-wrap svg {
  width: 100%;
  height: 100%;
  fill: currentColor;
}
.menu-icon-fallback {
  font-size: 18px;
  line-height: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
}
</style>
