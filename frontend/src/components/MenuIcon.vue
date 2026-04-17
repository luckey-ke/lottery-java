<template>
  <el-icon v-if="iconComponent" :size="size" :color="color">
    <component :is="iconComponent" />
  </el-icon>
  <span v-else class="menu-icon-fallback">{{ fallback }}</span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import * as ElementPlusIcons from '@element-plus/icons-vue'

const props = withDefaults(defineProps<{
  name?: string | null
  size?: number | string
  color?: string
  fallback?: string
}>(), {
  name: '',
  size: 18,
  fallback: '📄',
})

// kebab-case → PascalCase: setting → Setting, tree-table → TreeTable, trend-charts → TrendCharts
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
.menu-icon-fallback {
  font-size: 18px;
  line-height: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
}
</style>
