<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElTable, ElTableColumn, ElButton, ElDialog, ElTag, ElSelect, ElOption, ElInput, ElInputNumber, ElCard, ElRow, ElCol, ElStatistic, ElPopconfirm } from 'element-plus'
import { gradingApi, assignmentApi, submissionApi } from '@/api'
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

const loadAssignments = async () => {
  try {
    assignments.value = (await assignmentApi.getMyAssignments() as any).data || []
  } catch { /* ignore */ }
}

const loadUnreviewed = async () => {
  if (!selectedAssignmentId.value) {
    unreviewedList.value = []
    return
  }
  loading.value = true
  try {
    const res = await gradingApi.getByAssignment(selectedAssignmentId.value)
    const data = (res as any).data || []
    unreviewedList.value = data.filter((g: any) => g.gradingStatus !== 'DONE' || g.totalScore === null)
  } catch {
    ElMessage.error('加载批阅列表失败')
  } finally {
    loading.value = false
  }
}

const openDetail = async (row: any) => {
  try {
    const [subRes, gradRes] = await Promise.all([
      submissionApi.getById(row.submissionId || row.id),
      submissionApi.getGradingResult(row.submissionId || row.id),
    ])
    currentDetail.value = {
      submission: (subRes as any).data,
      grading: (gradRes as any).data,
    }
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
    ElMessage.error(e.response?.data?.msg || '审核失败')
  } finally {
    reviewing.value = false
  }
}

const handleBatchApprove = async () => {
  // Batch approve all currently shown items
  let count = 0
  for (const item of unreviewedList.value) {
    try {
      const gradingId = item.id
      await gradingApi.review(gradingId, item.totalScore || 60, '')
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

onMounted(loadAssignments)
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

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="批阅详情" width="800px" v-if="currentDetail">
      <template v-if="currentDetail.grading">
        <el-row :gutter="20" style="margin-bottom:20px">
          <el-col :span="8">
            <el-card shadow="hover">
              <el-statistic title="正确性" :value="currentDetail.grading.correctnessScore || 0" suffix="分" />
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="hover">
              <el-statistic title="规范性" :value="currentDetail.grading.styleScore || 0" suffix="分" />
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="hover">
              <el-statistic title="效率" :value="currentDetail.grading.efficiencyScore || 0" suffix="分" />
            </el-card>
          </el-col>
        </el-row>

        <div style="margin-bottom:16px" v-if="currentDetail.grading.feedbackJson">
          <h4 style="margin-bottom:8px">AI 评语</h4>
          <p style="color:#606266;line-height:1.6">
            {{ typeof currentDetail.grading.feedbackJson === 'string'
                ? currentDetail.grading.feedbackJson
                : currentDetail.grading.feedbackJson?.summary || JSON.stringify(currentDetail.grading.feedbackJson) }}
          </p>
        </div>

        <div style="border-top:1px solid #ebeef5;padding-top:16px">
          <h4 style="margin-bottom:12px">人工审核</h4>
          <el-form label-width="80px">
            <el-form-item label="修正分数">
              <el-input-number v-model="adjustedScore" :min="0" :max="100" :step="1" />
            </el-form-item>
            <el-form-item label="审核评语">
              <el-input v-model="reviewFeedback" type="textarea" :rows="3" placeholder="可选：给学生的额外反馈" />
            </el-form-item>
          </el-form>
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
