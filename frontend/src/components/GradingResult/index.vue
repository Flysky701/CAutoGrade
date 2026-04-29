<template>
  <div class="grading-result">
    <div class="score-overview">
      <el-statistic title="总分" :value="n.totalScore" suffix="分" />
      <div class="score-details">
        <div class="score-item">
          <span class="label">正确性</span>
          <el-progress :percentage="n.correctnessScore" :color="colorByScore(n.correctnessScore)" />
        </div>
        <div class="score-item">
          <span class="label">规范性</span>
          <el-progress :percentage="n.styleScore" :color="colorByScore(n.styleScore)" />
        </div>
        <div class="score-item">
          <span class="label">效率</span>
          <el-progress :percentage="n.efficiencyScore" :color="colorByScore(n.efficiencyScore)" />
        </div>
      </div>
    </div>

    <el-divider />

    <div v-if="n.summary" class="summary">
      <h4>总体评价</h4>
      <p>{{ n.summary }}</p>
    </div>

    <div v-if="n.lineAnnotations.length" class="annotations">
      <h4>逐行批注</h4>
      <div v-for="ann in n.lineAnnotations" :key="ann.line"
           :class="['annotation-item', `severity-${ann.severity}`]">
        <span class="line-num">L{{ ann.line }}</span>
        <el-tag :type="tagType(ann.severity)" size="small">{{ ann.severity }}</el-tag>
        <span class="msg">{{ ann.message }}</span>
      </div>
    </div>

    <div v-if="n.improvements.length" class="improvements">
      <h4>改进建议</h4>
      <div v-for="(imp, idx) in n.improvements" :key="idx" class="improvement-item">
        <div class="imp-header">
          <el-tag :type="imp.priority === 'high' ? 'danger' : imp.priority === 'medium' ? 'warning' : 'info'" size="small">
            {{ imp.priority === 'high' ? '高优' : imp.priority === 'medium' ? '中优' : '低优' }}
          </el-tag>
          <span class="imp-category">{{ imp.category }}</span>
          <strong>{{ imp.title }}</strong>
        </div>
        <p>{{ imp.detail }}</p>
      </div>
    </div>

    <template v-if="n.testCaseResults.length">
      <el-divider />
      <h4>测试用例结果</h4>
      <div v-for="tc in n.testCaseResults" :key="tc.caseId || tc.id" class="test-case-item">
        <el-tag :type="tc.passed ? 'success' : 'danger'" size="small">
          {{ tc.passed ? '通过' : '失败' }}
        </el-tag>
        <span>输入: {{ tc.input }}</span>
        <span>期望: {{ tc.expected }}</span>
        <span v-if="!tc.passed">实际: {{ tc.actual }}</span>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  data: Record<string, any>
}>()

// Normalize field names: accept camelCase (backend) or snake_case (Python)
const n = computed(() => {
  const d = props.data || {}
  return {
    totalScore:       d.totalScore ?? d.total_score ?? 0,
    correctnessScore: d.correctnessScore ?? d.correctness_score ?? 0,
    styleScore:       d.styleScore ?? d.style_score ?? 0,
    efficiencyScore:  d.efficiencyScore ?? d.efficiency_score ?? 0,
    summary:          d.summary || d.feedbackJson || (typeof d.feedbackJson === 'string' ? d.feedbackJson : '') || '',
    lineAnnotations:  d.lineAnnotations ?? d.line_annotations ?? [],
    improvements:     d.improvements ?? [],
    testCaseResults:  d.testCaseResults ?? d.test_case_results ?? [],
  }
})

function colorByScore(score: number) {
  if (score >= 80) return '#67c23a'
  if (score >= 60) return '#e6a23c'
  return '#f56c6c'
}

function tagType(severity: string) {
  const map: Record<string, string> = { error: 'danger', warning: 'warning', info: 'info', praise: 'success' }
  return map[severity] || 'info'
}
</script>

<style scoped>
.grading-result { padding: 16px; }
.score-overview { display: flex; align-items: flex-start; gap: 40px; }
.score-details { flex: 1; }
.score-item { margin-bottom: 12px; }
.score-item .label { display: block; margin-bottom: 4px; color: #606266; }
.summary { margin: 16px 0; }
.summary p { color: #303133; line-height: 1.8; }
.annotation-item { display: flex; align-items: center; gap: 8px; padding: 6px 0; border-bottom: 1px solid #ebeef5; }
.annotation-item.severity-error { background: #fef0f0; }
.annotation-item.severity-warning { background: #fdf6ec; }
.line-num { font-family: monospace; color: #909399; min-width: 40px; }
.msg { flex: 1; color: #303133; }
.imp-header { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.imp-category { color: #909399; font-size: 12px; }
.improvement-item { padding: 8px 0; border-bottom: 1px solid #ebeef5; }
.improvement-item p { margin: 4px 0 0 0; color: #606266; }
.test-case-item { display: flex; gap: 12px; padding: 4px 0; font-size: 13px; flex-wrap: wrap; }
</style>
