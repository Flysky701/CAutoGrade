<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { ElMessage, ElTable, ElTableColumn, ElButton, ElSelect, ElOption, ElTag, ElDialog, ElDescriptions, ElDescriptionsItem, ElTooltip } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import { assignmentApi, classApi, courseApi, submissionApi } from '@/api'
import * as XLSX from 'xlsx'

const courses = ref<any[]>([])
const classes = ref<any[]>([])
const assignments = ref<any[]>([])

const selectedCourseId = ref<number | null>(null)
const selectedClassId = ref<number | null>(null)
const selectedAssignmentId = ref<number | null>(null)

const scores = ref<any[]>([])
const loading = ref(false)

const detailVisible = ref(false)
const detailRow = ref<any>(null)

const loadMeta = async () => {
  try {
    courses.value = (await courseApi.getMyCourses() as any).data || []
  } catch { /* */ }
}

const loadScores = async () => {
  if (!selectedAssignmentId.value) return
  loading.value = true
  try {
    const res = await submissionApi.getScoresByAssignment(selectedAssignmentId.value) as any
    scores.value = res.data || []
  } catch {
    scores.value = []
  } finally {
    loading.value = false
  }
}

watch(selectedCourseId, async (cid) => {
  classes.value = []
  assignments.value = []
  if (!cid) return
  try {
    const [cr, ar] = await Promise.all([
      classApi.getByCourse(cid),
      assignmentApi.getByCourse(cid),
    ])
    classes.value = (cr as any).data || []
    assignments.value = (ar as any).data || []
  } catch { /* */ }
})

const showDetail = (row: any) => {
  detailRow.value = row
  detailVisible.value = true
}

const parseFeedbackJson = (json: string | null): any => {
  if (!json) return null
  try {
    return JSON.parse(json)
  } catch {
    return null
  }
}

const exportExcel = () => {
  if (scores.value.length === 0) {
    ElMessage.warning('没有可导出的数据')
    return
  }
  const data = scores.value.map((s: any) => ({
    '学号': s.studentId,
    '姓名': s.studentName || s.nickname || '',
    '用户名': s.username || '',
    '正式成绩': s.effectiveScore ?? '',
    'AI评分': s.totalScore ?? '',
    '正确性': s.correctnessScore ?? '',
    '规范性': s.styleScore ?? '',
    '效率': s.efficiencyScore ?? '',
    '教师调整分': s.humanAdjustedScore ?? '',
    '评语': s.effectiveFeedback || '',
    '状态': s.gradingStatus === 'DONE' ? '已完成' : s.gradingStatus === 'PROCESSING' ? '批阅中' : '待批阅',
    '提交时间': s.submittedAt ? new Date(s.submittedAt).toLocaleString('zh-CN') : '',
  }))
  const ws = XLSX.utils.json_to_sheet(data)
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '成绩单')
  const assignment = assignments.value.find((a: any) => a.id === selectedAssignmentId.value)
  const filename = `${assignment?.title || '成绩导出'}.xlsx`
  XLSX.writeFile(wb, filename)
  ElMessage.success('导出成功')
}

const exportCSV = () => {
  if (scores.value.length === 0) {
    ElMessage.warning('没有可导出的数据')
    return
  }
  const headers = ['学号', '姓名', '用户名', '正式成绩', 'AI评分', '正确性', '规范性', '效率', '教师调整分', '评语', '状态', '提交时间']
  const rows = scores.value.map((s: any) => [
    s.studentId, s.studentName || '', s.username || '',
    s.effectiveScore ?? '', s.totalScore ?? '', s.correctnessScore ?? '',
    s.styleScore ?? '', s.efficiencyScore ?? '', s.humanAdjustedScore ?? '',
    `"${(s.effectiveFeedback || '').replace(/"/g, '""')}"`,
    s.gradingStatus, s.submittedAt ? new Date(s.submittedAt).toLocaleString('zh-CN') : '',
  ])
  const csv = [headers.join(','), ...rows.map((r: any[]) => r.join(','))].join('\n')
  const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  const assignment = assignments.value.find((a: any) => a.id === selectedAssignmentId.value)
  a.download = `${assignment?.title || '成绩导出'}.csv`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('导出成功')
}

onMounted(loadMeta)
</script>

<template>
  <div class="score-export">
    <div class="page-header">
      <h2>成绩查看与导出</h2>
      <div style="display:flex;gap:8px" v-if="scores.length">
        <el-button type="primary" :icon="Download" @click="exportExcel">导出 Excel</el-button>
        <el-button :icon="Download" @click="exportCSV">导出 CSV</el-button>
      </div>
    </div>

    <div style="display:flex;gap:12px;margin-bottom:20px">
      <el-select v-model="selectedCourseId" placeholder="选择课程" clearable style="width:200px">
        <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-select v-model="selectedAssignmentId" placeholder="选择作业" clearable style="width:240px" @change="loadScores">
        <el-option v-for="a in assignments" :key="a.id" :label="a.title" :value="a.id" />
      </el-select>
    </div>

    <el-table :data="scores" stripe v-loading="loading">
      <el-table-column prop="studentId" label="学号" width="80" />
      <el-table-column prop="studentName" label="姓名" width="90" />
      <el-table-column prop="username" label="用户名" width="100" />
      <el-table-column label="正式成绩" width="110" align="center">
        <template #default="{ row }">
          <span v-if="row.effectiveScore != null"
                :style="{ fontWeight: 'bold', color: row.humanAdjustedScore != null ? '#e6a23c' : '' }">
            {{ row.effectiveScore }}
          </span>
          <span v-else style="color:var(--text-placeholder)">—</span>
          <el-tag v-if="row.humanAdjustedScore != null" type="warning" size="small" style="margin-left:4px">已调整</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="totalScore" label="AI评分" width="80" align="center">
        <template #default="{ row }">
          <span v-if="row.totalScore != null">{{ row.totalScore }}</span>
          <span v-else style="color:var(--text-placeholder)">—</span>
        </template>
      </el-table-column>
      <el-table-column prop="correctnessScore" label="正确性" width="70" align="center" />
      <el-table-column prop="styleScore" label="规范性" width="70" align="center" />
      <el-table-column prop="efficiencyScore" label="效率" width="70" align="center" />
      <el-table-column label="评语" min-width="180">
        <template #default="{ row }">
          <el-tooltip v-if="row.effectiveFeedback" :content="row.effectiveFeedback" placement="top" :show-after="300">
            <span style="cursor:pointer">{{ row.effectiveFeedback.length > 40 ? row.effectiveFeedback.substring(0, 40) + '...' : row.effectiveFeedback }}</span>
          </el-tooltip>
          <span v-else style="color:var(--text-placeholder)">—</span>
        </template>
      </el-table-column>
      <el-table-column prop="gradingStatus" label="状态" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.gradingStatus === 'DONE'" type="success" size="small">已完成</el-tag>
          <el-tag v-else-if="row.gradingStatus === 'PROCESSING'" type="warning" size="small">批阅中</el-tag>
          <el-tag v-else-if="row.gradingStatus === 'FAILED'" type="danger" size="small">失败</el-tag>
          <el-tag v-else type="info" size="small">待批阅</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="showDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && scores.length === 0" description="选择课程和作业后查看成绩" :image-size="120" />

    <el-dialog v-model="detailVisible" title="批阅详情" width="640px" destroy-on-close>
      <template v-if="detailRow">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="学生">{{ detailRow.studentName }} ({{ detailRow.username }})</el-descriptions-item>
          <el-descriptions-item label="学号">{{ detailRow.studentId }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ detailRow.submittedAt ? new Date(detailRow.submittedAt).toLocaleString('zh-CN') : '—' }}</el-descriptions-item>
          <el-descriptions-item label="提交次数">{{ detailRow.submitCount }}</el-descriptions-item>
          <el-descriptions-item label="是否迟交">
            <el-tag :type="detailRow.isLate ? 'warning' : 'success'" size="small">{{ detailRow.isLate ? '是' : '否' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="批阅状态">
            <el-tag v-if="detailRow.gradingStatus === 'DONE'" type="success" size="small">已完成</el-tag>
            <el-tag v-else-if="detailRow.gradingStatus === 'FAILED'" type="danger" size="small">失败</el-tag>
            <el-tag v-else type="info" size="small">待批阅</el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <el-descriptions :column="2" border style="margin-top:16px" title="评分详情">
          <el-descriptions-item label="正式成绩">
            <span style="font-size:18px;font-weight:bold" :style="{ color: detailRow.humanAdjustedScore != null ? '#e6a23c' : '#409eff' }">
              {{ detailRow.effectiveScore ?? '—' }}
            </span>
            <el-tag v-if="detailRow.humanAdjustedScore != null" type="warning" size="small" style="margin-left:6px">教师调整</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="AI评分">{{ detailRow.totalScore ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="正确性">{{ detailRow.correctnessScore ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="规范性">{{ detailRow.styleScore ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="效率">{{ detailRow.efficiencyScore ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="教师调整分">{{ detailRow.humanAdjustedScore ?? '—' }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="detailRow.effectiveFeedback" style="margin-top:16px">
          <h4 style="margin:0 0 8px">{{ detailRow.reviewFeedback ? '教师评语' : 'AI评语' }}</h4>
          <div style="background:#f5f7fa;padding:12px;border-radius:4px;white-space:pre-wrap;line-height:1.6">{{ detailRow.effectiveFeedback }}</div>
        </div>

        <div v-if="detailRow.reviewFeedback && detailRow.feedbackJson" style="margin-top:12px">
          <h4 style="margin:0 0 8px">AI原始评语</h4>
          <div style="background:#f0f9eb;padding:12px;border-radius:4px;white-space:pre-wrap;line-height:1.6;font-size:13px;color:#666">
            {{ parseFeedbackJson(detailRow.feedbackJson)?.summary || detailRow.feedbackJson }}
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
