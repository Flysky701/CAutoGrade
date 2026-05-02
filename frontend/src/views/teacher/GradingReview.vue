<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElTable, ElTableColumn, ElButton, ElDialog, ElTag, ElSelect, ElOption, ElInput, ElInputNumber, ElCard, ElRow, ElCol, ElStatistic, ElDivider } from 'element-plus'
import { gradingApi, assignmentApi, submissionApi, testCaseApi } from '@/api'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const assignments = ref<any[]>([])
const selectedAssignmentId = ref<number | null>(null)
const unreviewedList = ref<any[]>([])
const loading = ref(false)

const detailDialogVisible = ref(false)
const currentDetail = ref<any>(null)
const adjustedScore = ref(0)
const reviewFeedback = ref('')
const reviewing = ref(false)
const testCases = ref<any[]>([])

const loadAssignments = async () => {
  try {
    assignments.value = (await assignmentApi.getMyAssignments() as any).data || []
  } catch { /* ignore */ }
}

const loadUnreviewed = async () => {
  loading.value = true
  try {
    const res = await gradingApi.getUnreviewed()
    let data = (res as any).data || []
    if (selectedAssignmentId.value) {
      data = data.filter((g: any) => g.assignmentId === selectedAssignmentId.value)
    }
    unreviewedList.value = data
  } catch {
    ElMessage.error('加载批阅列表失败')
  } finally {
    loading.value = false
  }
}

const parseFeedbackJson = (json: string | null): any => {
  if (!json) return null
  try {
    return JSON.parse(json)
  } catch {
    return null
  }
}

const testCaseResults = computed(() => {
  if (!currentDetail.value?.grading?.feedbackJson) return []
  const parsed = parseFeedbackJson(currentDetail.value.grading.feedbackJson)
  return parsed?.test_case_results || parsed?.testCaseResults || []
})

const aiSummary = computed(() => {
  if (!currentDetail.value?.grading?.feedbackJson) return ''
  const parsed = parseFeedbackJson(currentDetail.value.grading.feedbackJson)
  return parsed?.summary || ''
})

const codeLines = computed(() => {
  const code = currentDetail.value?.submission?.codeContent || ''
  return code.split('\n')
})

const openDetail = async (row: any) => {
  try {
    const subId = row.submissionId
    const [subRes, gradRes, tcRes] = await Promise.all([
      submissionApi.getById(subId),
      submissionApi.getGradingResult(subId),
      row.problemId ? testCaseApi.getByProblem(row.problemId) : Promise.resolve(null),
    ])
    currentDetail.value = {
      submission: (subRes as any).data,
      grading: (gradRes as any).data,
      row,
    }
    testCases.value = (tcRes as any)?.data || []
    adjustedScore.value = currentDetail.value.grading?.totalScore || 0
    reviewFeedback.value = ''
    detailDialogVisible.value = true
  } catch {
    ElMessage.error('加载详情失败')
  }
}

const handleReview = async () => {
  if (!currentDetail.value) return
  reviewing.value = true
  try {
    const gradingId = currentDetail.value.grading?.id
    await gradingApi.review(gradingId, adjustedScore.value, reviewFeedback.value)
    ElMessage.success('审核完成')
    detailDialogVisible.value = false
    loadUnreviewed()
  } catch (e: any) {
    ElMessage.error(e?.message || e?.response?.data?.msg || '审核失败')
  } finally {
    reviewing.value = false
  }
}

const handleBatchApprove = async () => {
  let count = 0
  for (const item of unreviewedList.value) {
    try {
      const gradingId = item.id
      await gradingApi.review(gradingId, item.totalScore ?? 60, '')
      count++
    } catch { /* skip */ }
  }
  if (count > 0) {
    ElMessage.success(`批量通过 ${count} 条`)
    loadUnreviewed()
  } else {
    ElMessage.warning('没有可操作的数据')
  }
}

const scoreColor = (score: number) => {
  if (score >= 90) return '#67c23a'
  if (score >= 60) return '#e6a23c'
  return '#f56c6c'
}

onMounted(() => {
  loadAssignments()
  loadUnreviewed()
})
</script>

<template>
  <div class="grading-review">
    <div class="page-header">
      <h2>批阅审核</h2>
      <div style="display:flex;gap:12px">
        <el-select v-model="selectedAssignmentId" placeholder="选择作业" clearable style="width:240px" @change="loadUnreviewed">
          <el-option v-for="a in assignments" :key="a.id" :label="a.title" :value="a.id" />
        </el-select>
        <el-button v-if="unreviewedList.length" type="success" @click="handleBatchApprove">批量通过</el-button>
      </div>
    </div>

    <el-table :data="unreviewedList" stripe v-loading="loading">
      <el-table-column label="学生" min-width="120">
        <template #default="{ row }">
          {{ row.studentName || `学生#${row.studentId}` }}
        </template>
      </el-table-column>
      <el-table-column label="题目" min-width="160">
        <template #default="{ row }">
          {{ row.problemTitle || `题目#${row.problemId}` }}
        </template>
      </el-table-column>
      <el-table-column label="AI评分" width="100" align="center">
        <template #default="{ row }">
          <span :style="{ color: scoreColor(row.totalScore), fontWeight: 700, fontSize: '16px' }">
            {{ row.totalScore ?? '—' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="gradingStatus" label="状态" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.gradingStatus === 'DONE'" type="success" size="small">已完成</el-tag>
          <el-tag v-else-if="row.gradingStatus === 'PROCESSING'" type="warning" size="small">批阅中</el-tag>
          <el-tag v-else-if="row.gradingStatus === 'PENDING'" type="info" size="small">排队中</el-tag>
          <el-tag v-else type="danger" size="small">失败</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="openDetail(row)">查看详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && unreviewedList.length === 0" description="暂无待审核的批阅">
      <template v-if="!selectedAssignmentId">
        <p style="color:#909399">请先选择一个作业</p>
      </template>
    </el-empty>

    <el-dialog v-model="detailDialogVisible" title="批阅详情" width="1000px" top="4vh" destroy-on-close v-if="currentDetail">
      <template v-if="currentDetail.grading">
        <div class="detail-top">
          <div class="detail-code">
            <div class="section-title">提交代码</div>
            <div class="code-block">
              <div class="code-line" v-for="(line, idx) in codeLines" :key="idx">
                <span class="line-num">{{ idx + 1 }}</span>
                <span class="line-content">{{ line }}</span>
              </div>
            </div>
          </div>
          <div class="detail-review">
            <div class="section-title">评分详情</div>
            <el-row :gutter="12" style="margin-bottom:16px">
              <el-col :span="8">
                <el-card shadow="hover" body-style="padding:12px">
                  <el-statistic title="正确性" :value="currentDetail.grading.correctnessScore || 0" suffix="分" />
                </el-card>
              </el-col>
              <el-col :span="8">
                <el-card shadow="hover" body-style="padding:12px">
                  <el-statistic title="规范性" :value="currentDetail.grading.styleScore || 0" suffix="分" />
                </el-card>
              </el-col>
              <el-col :span="8">
                <el-card shadow="hover" body-style="padding:12px">
                  <el-statistic title="效率" :value="currentDetail.grading.efficiencyScore || 0" suffix="分" />
                </el-card>
              </el-col>
            </el-row>

            <div v-if="aiSummary" style="margin-bottom:16px">
              <h4 style="margin:0 0 6px;font-size:13px;color:#909399">AI 评语</h4>
              <p style="color:#606266;line-height:1.6;font-size:13px;margin:0">{{ aiSummary }}</p>
            </div>

            <el-divider style="margin:12px 0" />

            <h4 style="margin:0 0 12px;font-size:13px;color:#909399">人工审核</h4>
            <el-form label-width="72px" size="small">
              <el-form-item label="修正分数">
                <el-input-number v-model="adjustedScore" :min="0" :max="100" :step="1" />
              </el-form-item>
              <el-form-item label="审核评语">
                <el-input v-model="reviewFeedback" type="textarea" :rows="3" placeholder="可选：给学生的额外反馈" />
              </el-form-item>
            </el-form>
          </div>
        </div>

        <div class="detail-bottom">
          <div class="section-title">测试样例</div>
          <div v-if="testCaseResults.length > 0 || testCases.length > 0" class="test-cases">
            <div v-for="(tc, idx) in (testCaseResults.length > 0 ? testCaseResults : testCases)" :key="idx" class="test-case-item">
              <div class="tc-header">
                <el-tag :type="tc.passed ? 'success' : 'danger'" size="small">
                  {{ tc.passed ? '通过' : '失败' }}
                </el-tag>
                <span class="tc-label">样例 {{ idx + 1 }}</span>
              </div>
              <div class="tc-body">
                <div class="tc-field">
                  <span class="tc-key">输入：</span>
                  <code class="tc-val">{{ tc.input || tc.inputData || '(空)' }}</code>
                </div>
                <div class="tc-field">
                  <span class="tc-key">期望输出：</span>
                  <code class="tc-val">{{ tc.expected || tc.expectedOutput || '(空)' }}</code>
                </div>
                <div v-if="!tc.passed && (tc.actual || tc.actualOutput)" class="tc-field">
                  <span class="tc-key">实际输出：</span>
                  <code class="tc-val tc-actual">{{ tc.actual || tc.actualOutput }}</code>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="no-test-cases">暂无测试样例数据</div>
        </div>
      </template>
      <el-empty v-else description="暂无批阅数据" />

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="reviewing" @click="handleReview">提交审核</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.detail-top {
  display: flex;
  gap: 16px;
  min-height: 360px;
}

.detail-code {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.detail-review {
  width: 340px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
  padding-bottom: 6px;
  border-bottom: 2px solid #409eff;
}

.code-block {
  flex: 1;
  background: #1e1e1e;
  border-radius: 6px;
  padding: 12px;
  overflow: auto;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
  max-height: 340px;
}

.code-line {
  display: flex;
  min-height: 20px;
}

.line-num {
  display: inline-block;
  width: 36px;
  text-align: right;
  padding-right: 12px;
  color: #858585;
  user-select: none;
  flex-shrink: 0;
}

.line-content {
  color: #d4d4d4;
  white-space: pre;
}

.detail-bottom {
  margin-top: 16px;
  border-top: 1px solid #ebeef5;
  padding-top: 16px;
}

.test-cases {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.test-case-item {
  flex: 1 1 calc(50% - 6px);
  min-width: 280px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  overflow: hidden;
}

.tc-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-bottom: 1px solid #ebeef5;
}

.tc-label {
  font-size: 13px;
  font-weight: 500;
  color: #606266;
}

.tc-body {
  padding: 10px 12px;
}

.tc-field {
  margin-bottom: 6px;
  font-size: 12px;
}

.tc-field:last-child {
  margin-bottom: 0;
}

.tc-key {
  color: #909399;
  margin-right: 4px;
}

.tc-val {
  font-family: 'Consolas', 'Monaco', monospace;
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 12px;
  color: #303133;
  word-break: break-all;
}

.tc-actual {
  background: #fef0f0;
  color: #f56c6c;
}

.no-test-cases {
  color: #909399;
  font-size: 13px;
  padding: 12px 0;
}
</style>
