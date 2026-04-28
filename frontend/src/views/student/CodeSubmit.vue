<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { submissionApi, assignmentApi, problemApi } from '@/api'
import CodeEditor from '@/components/CodeEditor/index.vue'

const route = useRoute()
const router = useRouter()
const assignmentId = Number(route.params.assignmentId)
const problemId = Number(route.params.problemId)
const assignment = ref<any>(null)
const problem = ref<any>(null)
const code = ref('#include <stdio.h>\n\nint main() {\n    \n    return 0;\n}')
const loading = ref(false)

onMounted(async () => {
  try {
    const [aRes, pRes] = await Promise.all([
      assignmentApi.getById(assignmentId).catch(() => null),
      problemApi.getById(problemId).catch(() => null),
    ])
    assignment.value = (aRes as any)?.data
    problem.value = (pRes as any)?.data
  } catch { /* ignore */ }
})

const handleSubmit = async () => {
  if (!code.value.trim()) {
    ElMessage.warning('请输入代码')
    return
  }
  loading.value = true
  try {
    await submissionApi.submit({ assignmentId, problemId, code: code.value })
    ElMessage.success('提交成功，正在批阅...')
    router.push('/student/history')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.msg || '提交失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="code-submit">
    <div class="page-header">
      <h2>代码提交</h2>
      <el-button type="primary" :loading="loading" @click="handleSubmit">提交代码</el-button>
    </div>

    <el-card v-if="problem" class="problem-card" shadow="hover">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span style="font-weight:600">{{ problem.title }}</span>
          <el-tag size="small">难度 {{ '★'.repeat(problem.difficulty) + '☆'.repeat(5 - problem.difficulty) }}</el-tag>
        </div>
      </template>
      <div style="white-space:pre-wrap;font-size:var(--font-size-sm);color:var(--text-secondary)">{{ problem.description }}</div>
    </el-card>

    <CodeEditor v-model="code" language="c" style="margin-top:var(--space-4)" />
  </div>
</template>

<style scoped>
.code-submit :deep(.el-card__body) { padding: var(--space-4); }
.problem-card { margin-bottom: var(--space-3); }
</style>
