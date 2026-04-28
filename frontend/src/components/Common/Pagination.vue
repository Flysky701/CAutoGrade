<template>
  <el-pagination
    v-model:current-page="current"
    v-model:page-size="pageSize"
    :page-sizes="[10, 20, 50]"
    :total="total"
    layout="total, sizes, prev, pager, next, jumper"
    background
  />
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  total: number
  page: number
  size: number
}>()
const emit = defineEmits<{
  'update:page': [v: number]
  'update:size': [v: number]
}>()

const current = computed({
  get: () => props.page,
  set: (v) => emit('update:page', v),
})
const pageSize = computed({
  get: () => props.size,
  set: (v) => emit('update:size', v),
})
</script>
