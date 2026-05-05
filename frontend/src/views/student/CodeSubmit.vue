<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { submissionApi, assignmentApi, problemApi } from '@/api'
import CodeEditor from '@/components/CodeEditor/index.vue'
import { marked } from 'marked'
import { ArrowLeft } from '@element-plus/icons-vue'

marked.setOptions({ breaks: true, gfm: true })

const route = useRoute()
const router = useRouter()
const assignmentId = Number(route.params.assignmentId)
const problemId = Number(route.params.problemId)
const assignment = ref<any>(null)
const problem = ref<any>(null)
const language = ref('c')
const loading = ref(false)

const codeTemplates: Record<string, string> = {
  c: '#include <stdio.h>\n\nint main() {\n    \n    return 0;\n}',
  cpp: '#include <bits/stdc++.h>\nusing namespace std;\n\nint main() {\n    \n    return 0;\n}',
}

const code = ref(codeTemplates['c'])

const editorLanguage = computed(() => language.value === 'cpp' ? 'cpp' : 'c')

const onLanguageChange = (val: string) => {
  if (val === 'cpp' && code.value === codeTemplates['c']) {
    code.value = codeTemplates['cpp']
  } else if (val === 'c' && code.value === codeTemplates['cpp']) {
    code.value = codeTemplates['c']
  }
}

const renderedDescription = computed(() => {
  if (!problem.value?.description) return ''
  return marked.parse(problem.value.description) as string
})

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
    await submissionApi.submit({ assignmentId, problemId, code: code.value, language: language.value })
    ElMessage.success('提交成功，正在批阅...')
    router.push('/student/history')
  } catch (e: any) {
    ElMessage.error(e?.message || e?.response?.data?.msg || '提交失败')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.push(`/student/assignment/${assignmentId}`)
}
</script>

<template>
  <div class="code-submit">
    <div class="top-bar">
      <el-button text @click="goBack">
        <el-icon><ArrowLeft /></el-icon>返回作业
      </el-button>
      <span class="assignment-title">{{ assignment?.title || '代码提交' }}</span>
      <div class="lang-select">
        <span class="lang-label">语言：</span>
        <el-select v-model="language" size="small" style="width:100px" @change="onLanguageChange">
          <el-option label="C" value="c" />
          <el-option label="C++" value="cpp" />
        </el-select>
      </div>
    </div>

    <div class="main-area">
      <aside class="problem-panel">
        <template v-if="problem">
          <div class="problem-header">
            <h3 class="problem-title">{{ problem.title }}</h3>
            <el-tag size="small" :type="problem.difficulty <= 2 ? 'success' : problem.difficulty <= 3 ? 'warning' : 'danger'">
              {{ '★'.repeat(problem.difficulty) + '☆'.repeat(5 - problem.difficulty) }}
            </el-tag>
          </div>

          <section class="problem-section">
            <h4 class="section-label">题目描述</h4>
            <div class="markdown-content" v-html="renderedDescription" />
          </section>

          <section v-if="problem.inputDesc" class="problem-section">
            <h4 class="section-label">输入说明</h4>
            <p class="desc-text">{{ problem.inputDesc }}</p>
          </section>

          <section v-if="problem.outputDesc" class="problem-section">
            <h4 class="section-label">输出说明</h4>
            <p class="desc-text">{{ problem.outputDesc }}</p>
          </section>

          <section class="problem-section">
            <h4 class="section-label">限制条件</h4>
            <ul class="limits-list">
              <li>时间限制：{{ problem.timeLimitMs || 1000 }} ms</li>
              <li>内存限制：{{ (problem.memoryLimitKb || 256000) / 1024 }} MB</li>
              <li v-if="problem.knowledgeTags">知识点：{{ problem.knowledgeTags }}</li>
            </ul>
          </section>
        </template>
        <el-empty v-else description="加载题目中..." />
      </aside>

      <section class="editor-panel">
        <CodeEditor v-model="code" :language="editorLanguage" />
        <div class="submit-row">
          <el-button type="primary" size="large" :loading="loading" @click="handleSubmit">
            提交代码
          </el-button>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.code-submit {
  height: calc(100vh - 80px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.top-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  border-bottom: 1px solid var(--el-border-color-light);
  flex-shrink: 0;
  background: var(--el-bg-color);
}

.assignment-title {
  flex: 1;
  font-weight: 600;
  font-size: 16px;
}

.lang-select {
  display: flex;
  align-items: center;
  gap: 6px;
}

.lang-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.main-area {
  flex: 1;
  display: flex;
  overflow: hidden;
  min-height: 0;
}

.problem-panel {
  width: 42%;
  min-width: 320px;
  overflow-y: auto;
  padding: 20px;
  border-right: 1px solid var(--el-border-color-light);
  background: var(--el-bg-color);
}

.problem-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.problem-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.problem-section {
  margin-bottom: 20px;
}

.section-label {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  padding-bottom: 4px;
  border-bottom: 1px dashed var(--el-border-color-light);
}

.desc-text {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: var(--el-text-color-regular);
  white-space: pre-wrap;
}

.limits-list {
  margin: 0;
  padding-left: 18px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  line-height: 1.8;
}

.editor-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

.editor-panel :deep(.code-editor-wrapper) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.editor-panel :deep(.editor-container) {
  flex: 1;
  height: auto !important;
  min-height: 0;
}

.submit-row {
  flex-shrink: 0;
  padding: 12px 20px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid var(--el-border-color-light);
  background: var(--el-bg-color);
}

.markdown-content :deep(h1),
.markdown-content :deep(h2),
.markdown-content :deep(h3) {
  margin-top: 12px;
  margin-bottom: 8px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.markdown-content :deep(h1) { font-size: 18px; }
.markdown-content :deep(h2) { font-size: 16px; }
.markdown-content :deep(h3) { font-size: 14px; }

.markdown-content :deep(p) {
  margin: 6px 0;
  line-height: 1.7;
  font-size: 14px;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  padding-left: 20px;
  margin: 6px 0;
  line-height: 1.7;
}

.markdown-content :deep(li) {
  margin: 2px 0;
  font-size: 14px;
}

.markdown-content :deep(code) {
  background: var(--el-fill-color-light);
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
}

.markdown-content :deep(pre) {
  background: var(--el-fill-color);
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  padding: 12px 16px;
  overflow-x: auto;
  margin: 8px 0;
}

.markdown-content :deep(pre code) {
  background: transparent;
  padding: 0;
  border-radius: 0;
  font-size: 13px;
  line-height: 1.6;
}

.markdown-content :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 8px 0;
  font-size: 13px;
}

.markdown-content :deep(th),
.markdown-content :deep(td) {
  border: 1px solid var(--el-border-color-light);
  padding: 6px 12px;
  text-align: left;
}

.markdown-content :deep(th) {
  background: var(--el-fill-color-light);
  font-weight: 600;
}

.markdown-content :deep(blockquote) {
  border-left: 3px solid var(--el-color-primary);
  padding: 4px 12px;
  margin: 8px 0;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-lighter);
}

.markdown-content :deep(strong) {
  font-weight: 600;
}

.markdown-content :deep(hr) {
  border: none;
  border-top: 1px solid var(--el-border-color-light);
  margin: 12px 0;
}

@media (max-width: 768px) {
  .main-area {
    flex-direction: column;
  }

  .problem-panel {
    width: 100%;
    min-width: 0;
    max-height: 40%;
    border-right: none;
    border-bottom: 1px solid var(--el-border-color-light);
  }

  .editor-panel {
    flex: 1;
    min-height: 0;
  }
}
</style>
