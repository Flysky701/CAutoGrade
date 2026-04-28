<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { submissionApi } from '@/api'
import GradingResultComp from '@/components/GradingResult/index.vue'

const route = useRoute()
const submission = ref<any>(null)
const gradingResult = ref<any>(null)
const loading = ref(true)

onMounted(async () => {
  const submissionId = Number(route.params.submissionId)
  try {
    const [subRes, gradRes] = await Promise.all([
      submissionApi.getById(submissionId),
      submissionApi.getGradingResult(submissionId),
    ])
    submission.value = (subRes as any).data
    gradingResult.value = (gradRes as any).data
  } catch {
    ElMessage.error('加载批阅结果失败')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="grading-result-page" v-loading="loading">
    <h2 class="page-header__title">批改结果</h2>

    <GradingResultComp v-if="gradingResult" :data="gradingResult" />

    <el-card v-if="submission?.codeContent" class="code-card">
      <template #header>提交的代码</template>
      <pre class="code-block">{{ submission.codeContent }}</pre>
    </el-card>

    <el-empty v-if="!loading && !gradingResult" description="暂无批阅结果" />
  </div>
</template>

<style scoped>
.page-header__title {
  margin-bottom: var(--space-5);
  font-size: var(--font-size-xl);
  font-weight: 600;
}
.code-card {
  margin-top: var(--space-5);
}
.code-block {
  background: var(--bg-sidebar);
  padding: var(--space-4);
  border-radius: var(--radius-md);
  overflow-x: auto;
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: var(--font-size-sm);
  line-height: 1.6;
}
</style>
