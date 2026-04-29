<template>
  <div class="empty-state">
    <el-icon :size="52"><component :is="icon" /></el-icon>
    <p class="empty-text">{{ text }}</p>
    <p v-if="description" class="empty-description">{{ description }}</p>
    <slot />
    <el-button v-if="action" :type="action.type || 'primary'" size="small"
      @click="action.handler ? action.handler() : $router?.push(action.to || '/')">
      {{ action.label }}
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { Document } from '@element-plus/icons-vue'
defineProps<{
  text?: string
  description?: string
  icon?: any
  action?: { label: string; to?: string; type?: string; handler?: () => void }
}>()
</script>

<style scoped>
.empty-state { text-align: center; padding: var(--space-12) var(--space-4); color: var(--text-placeholder); }
.empty-state :deep(.el-icon) { margin-bottom: var(--space-3); }
.empty-text { margin: var(--space-3) 0; font-size: var(--font-size-md); color: var(--text-secondary); }
.empty-description { font-size: var(--font-size-sm); color: var(--text-placeholder); margin-bottom: var(--space-4); }
</style>
