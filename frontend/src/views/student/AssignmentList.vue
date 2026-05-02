<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElTable, ElTableColumn, ElButton, ElTag, ElEmpty } from 'element-plus'
import { assignmentApi } from '@/api'

const route = useRoute()
const router = useRouter()
const assignments = ref<any[]>([])
const loading = ref(false)

const typeLabel = (t: string) => ({ EXAM: '考试', LAB: '实验', PRACTICE: '练习' } as Record<string, string>)[t] || t
const formatTime = (t: string) => new Date(t).toLocaleString('zh-CN')

onMounted(async () => {
  loading.value = true
  try {
    assignments.value = (await assignmentApi.getByCourse(Number(route.params.courseId))).data || []
  } catch { /* fail silently */ }
  finally { loading.value = false }
})
</script>

<template>
  <div class="assignment-list">
    <h2>作业列表</h2>
    <el-table :data="assignments" stripe v-loading="loading">
      <el-table-column prop="title" label="作业标题" min-width="160" />
      <el-table-column label="类型" width="80">
        <template #default="{ row }">
          <el-tag size="small">{{ typeLabel(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="截止时间" width="180">
        <template #default="{ row }">{{ formatTime(row.endTime) }}</template>
      </el-table-column>
      <el-table-column prop="maxScore" label="满分" width="70" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="router.push(`/student/assignment/${row.id}`)">
            查看详情
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && assignments.length === 0" description="暂无作业" />
  </div>
</template>
