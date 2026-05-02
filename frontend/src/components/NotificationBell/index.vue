<template>
  <el-badge :value="unread" :max="99" :hidden="unread === 0">
    <el-icon :size="22" style="cursor: pointer" @click="$emit('click')">
      <Bell />
    </el-icon>
  </el-badge>
</template>

<script setup lang="ts">
import { Bell } from '@element-plus/icons-vue'

defineProps<{ unread: number }>()
defineEmits<{ click: [] }>()
</script>
