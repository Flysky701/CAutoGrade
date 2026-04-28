<template>
  <div class="empty-state">
    <el-icon :size="48"><Document /></el-icon>
    <p>{{ text }}</p>
    <slot />
  </div>
</template>

<script setup lang="ts">
import { Document } from '@element-plus/icons-vue'
defineProps<{ text?: string }>()
</script>

<style scoped>
.empty-state { text-align: center; padding: 60px 0; color: #c0c4cc; }
.empty-state p { margin: 12px 0; }
</style>
