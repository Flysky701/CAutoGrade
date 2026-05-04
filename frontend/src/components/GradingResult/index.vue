<template>
  <div class="grading-result">
    <!-- Grade + Score Overview -->
    <div class="grade-overview">
      <div class="grade-letter" :style="{ color: grade.color }">{{ grade.letter }}</div>
      <div class="grade-info">
        <div class="grade-score">{{ n.totalScore }} <span class="grade-unit">/ 100 分</span></div>
        <div class="grade-label">{{ gradeLabel }}</div>
      </div>
    </div>

    <div class="score-details">
      <div class="score-item">
        <div class="score-label">
          <span>正确性</span>
          <span class="score-val">{{ n.correctnessScore }}分</span>
        </div>
        <el-progress :percentage="n.correctnessScore" :color="barColor(n.correctnessScore)" :stroke-width="8" />
      </div>
      <div class="score-item">
        <div class="score-label">
          <span>规范性</span>
          <span class="score-val">{{ n.styleScore }}分</span>
        </div>
        <el-progress :percentage="n.styleScore" :color="barColor(n.styleScore)" :stroke-width="8" />
      </div>
      <div class="score-item">
        <div class="score-label">
          <span>效率</span>
          <span class="score-val">{{ n.efficiencyScore }}分</span>
        </div>
        <el-progress :percentage="n.efficiencyScore" :color="barColor(n.efficiencyScore)" :stroke-width="8" />
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
        <el-tag :type="tagType(ann.severity)" size="small">{{ severityLabel(ann.severity) }}</el-tag>
        <span class="msg">{{ ann.message }}</span>
      </div>
    </div>

    <div v-if="n.improvements.length" class="improvements">
      <h4>改进建议</h4>
      <div v-for="(imp, idx) in n.improvements" :key="idx" class="improvement-item">
        <div class="imp-header">
          <el-tag :type="priorityType(imp.priority)" size="small">
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
        <span class="tc-field">输入: <code>{{ tc.input }}</code></span>
        <span class="tc-field">期望: <code>{{ tc.expected }}</code></span>
        <span v-if="!tc.passed" class="tc-field">实际: <code>{{ tc.actual }}</code></span>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ data: Record<string, any> }>()

const n = computed(() => {
  const d = props.data || {}
  const fb = typeof d.feedbackJson === 'string'
    ? (() => { try { return JSON.parse(d.feedbackJson) } catch { return {} } })()
    : (d.feedbackJson || {})
  return {
    totalScore:       d.totalScore ?? d.total_score ?? 0,
    correctnessScore: d.correctnessScore ?? d.correctness_score ?? 0,
    styleScore:       d.styleScore ?? d.style_score ?? 0,
    efficiencyScore:  d.efficiencyScore ?? d.efficiency_score ?? 0,
    summary:          typeof d.summary === 'string' ? d.summary
                      : (fb?.summary || ''),
    lineAnnotations:  d.lineAnnotations ?? d.line_annotations ?? fb?.line_annotations ?? [],
    improvements:     d.improvements ?? fb?.improvements ?? [],
    testCaseResults:  (() => {
      if (d.testCaseResults) return d.testCaseResults
      if (d.test_case_results) return d.test_case_results
      const raw = d.testCaseResult ?? d.test_case_result
      if (raw) {
        if (typeof raw === 'string') {
          try { return JSON.parse(raw) } catch { /* fall through */ }
        }
        if (Array.isArray(raw)) return raw
      }
      return fb?.test_case_results ?? []
    })(),
  }
})

const grade = computed(() => {
  const s = n.value.totalScore
  if (s >= 90) return { letter: 'A', color: 'var(--grade-a)' }
  if (s >= 80) return { letter: 'B', color: 'var(--grade-b)' }
  if (s >= 70) return { letter: 'C', color: 'var(--grade-c)' }
  if (s >= 60) return { letter: 'D', color: 'var(--grade-d)' }
  return { letter: 'F', color: 'var(--grade-f)' }
})

const gradeLabel = computed(() => {
  const m: Record<string, string> = { A: '优秀', B: '良好', C: '中等', D: '及格', F: '不及格' }
  return m[grade.value.letter] || ''
})

function barColor(score: number) {
  if (score >= 80) return 'var(--grade-a)'
  if (score >= 60) return 'var(--grade-c)'
  return 'var(--grade-f)'
}

function tagType(severity: string) {
  return { error: 'danger', warning: 'warning', info: 'info', praise: 'success' }[severity] || 'info'
}

function severityLabel(s: string) {
  return { error: '错误', warning: '警告', info: '提示', praise: '优秀' }[s] || s
}

function priorityType(p: string) {
  return { high: 'danger', medium: 'warning', low: 'info' }[p] || 'info'
}
</script>

<style scoped>
.grading-result { padding: var(--space-4); }

/* Grade Overview */
.grade-overview {
  display: flex; align-items: center; gap: var(--space-5); margin-bottom: var(--space-5);
}
.grade-letter { font-size: 64px; font-weight: 800; line-height: 1; font-family: var(--font-display); }
.grade-score { font-size: var(--font-size-2xl); font-weight: 700; color: var(--text-primary); }
.grade-unit { font-size: var(--font-size-base); font-weight: 400; color: var(--text-secondary); }
.grade-label { font-size: var(--font-size-sm); color: var(--text-secondary); margin-top: var(--space-1); }

/* Score Details */
.score-details { display: flex; flex-direction: column; gap: var(--space-3); }
.score-label { display: flex; justify-content: space-between; margin-bottom: var(--space-1); font-size: var(--font-size-sm); color: var(--text-secondary); }
.score-val { font-weight: 600; color: var(--text-regular); }

/* Summary */
.summary { margin: var(--space-4) 0; }
.summary p { color: var(--text-regular); line-height: var(--line-height-base); }

/* Annotations */
.annotation-item {
  display: flex; align-items: center; gap: var(--space-2);
  padding: var(--space-2) var(--space-3); border-radius: var(--radius-sm);
}
.annotation-item + .annotation-item { margin-top: var(--space-1); }
.severity-error { background: rgba(220,38,38,0.06); border-left: 2px solid var(--grade-f); }
.severity-warning { background: rgba(217,119,6,0.06); border-left: 2px solid var(--grade-c); }
.severity-praise { background: rgba(5,150,105,0.06); border-left: 2px solid var(--grade-a); }
.line-num { font-family: var(--font-mono); color: var(--text-placeholder); min-width: 40px; font-size: var(--font-size-xs); }
.msg { flex: 1; color: var(--text-regular); font-size: var(--font-size-sm); }

/* Improvements */
.improvement-item { padding: var(--space-3) 0; border-bottom: 1px solid var(--border-lighter); }
.improvement-item:last-child { border-bottom: none; }
.imp-header { display: flex; align-items: center; gap: var(--space-2); margin-bottom: var(--space-1); }
.imp-category { color: var(--text-placeholder); font-size: var(--font-size-xs); }
.improvement-item p { color: var(--text-secondary); font-size: var(--font-size-sm); }

/* Test Cases */
.test-case-item { display: flex; gap: var(--space-3); padding: var(--space-2) 0; font-size: var(--font-size-sm); flex-wrap: wrap; }
.tc-field { font-size: var(--font-size-xs); }
.tc-field code { font-family: var(--font-mono); font-size: var(--font-size-xs); background: var(--bg-sidebar); padding: 1px 4px; border-radius: 2px; }

h4 { font-size: var(--font-size-md); margin-bottom: var(--space-3); color: var(--text-primary); }
</style>
