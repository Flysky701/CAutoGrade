<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElTable, ElTableColumn, ElButton, ElSelect, ElOption, ElTag } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import { assignmentApi, classApi, courseApi } from '@/api'
import * as XLSX from 'xlsx'

const courses = ref<any[]>([])
const classes = ref<any[]>([])
const assignments = ref<any[]>([])

const selectedCourseId = ref<number | null>(null)
const selectedClassId = ref<number | null>(null)
const selectedAssignmentId = ref<number | null>(null)

const scores = ref<any[]>([])
const loading = ref(false)

const loadMeta = async () => {
  try {
    courses.value = (await courseApi.getMyCourses() as any).data || []
  } catch { /* */ }
}

const loadScores = async () => {
  if (!selectedAssignmentId.value) return
  loading.value = true
  try {
    const res = await assignmentApi.getById(selectedAssignmentId.value) as any
    scores.value = res.data?.submissions || []
  } catch {
    scores.value = []
  } finally {
    loading.value = false
  }
}

// Watch courseId to load classes
import { watch } from 'vue'
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

const exportExcel = () => {
  if (scores.value.length === 0) {
    ElMessage.warning('没有可导出的数据')
    return
  }
  const data = scores.value.map((s: any) => ({
    '学号': s.studentId,
    '姓名': s.studentName || s.nickname || '',
    '用户名': s.username || '',
    'AI评分': s.totalScore ?? '',
    '正确性': s.correctnessScore ?? '',
    '规范性': s.styleScore ?? '',
    '效率': s.efficiencyScore ?? '',
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
  const headers = ['学号', '姓名', '用户名', 'AI评分', '正确性', '规范性', '效率', '状态', '提交时间']
  const rows = scores.value.map((s: any) => [
    s.studentId, s.studentName || '', s.username || '',
    s.totalScore ?? '', s.correctnessScore ?? '', s.styleScore ?? '', s.efficiencyScore ?? '',
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
      <h2>成绩导出</h2>
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
      <el-table-column prop="studentId" label="学号" width="100" />
      <el-table-column prop="studentName" label="姓名" width="120" />
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="totalScore" label="AI评分" width="100" align="center" />
      <el-table-column prop="correctnessScore" label="正确性" width="80" align="center" />
      <el-table-column prop="styleScore" label="规范性" width="80" align="center" />
      <el-table-column prop="efficiencyScore" label="效率" width="80" align="center" />
      <el-table-column prop="gradingStatus" label="状态" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.gradingStatus === 'DONE'" type="success" size="small">已完成</el-tag>
          <el-tag v-else-if="row.gradingStatus === 'PROCESSING'" type="warning" size="small">批阅中</el-tag>
          <el-tag v-else type="info" size="small">待批阅</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" width="180">
        <template #default="{ row }">
          {{ row.submittedAt ? new Date(row.submittedAt).toLocaleString('zh-CN') : '—' }}
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && scores.length === 0" description="选择课程和作业后查看成绩" :image-size="120" />
  </div>
</template>
