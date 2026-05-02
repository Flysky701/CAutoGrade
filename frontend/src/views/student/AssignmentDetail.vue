<script setup lang="ts">
import { ref, onMounted, watch, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElTable, ElTableColumn, ElButton, ElTag, ElCard, ElDescriptions, ElDescriptionsItem } from 'element-plus'
import { assignmentApi, submissionApi } from '@/api'
import { statusTag } from '@/utils'

const route = useRoute()
const router = useRouter()
const assignmentId = Number(route.params.assignmentId)

const assignment = ref<any>(null)
const problems = ref<any[]>([])
const submissions = ref<Record<number, any>>({})
const loading = ref(false)

const countdownDisplay = ref('')
const remaining = ref({ expired: false, days: 0, hours: 0, minutes: 0, seconds: 0 })
let countdownTimer: ReturnType<typeof setInterval> | null = null

const startCountdown = (deadline: string) => {
  if (countdownTimer) clearInterval(countdownTimer)
  const update = () => {
    const diff = new Date(deadline).getTime() - Date.now()
    if (diff <= 0) {
      remaining.value = { expired: true, days: 0, hours: 0, minutes: 0, seconds: 0 }
      countdownDisplay.value = '已截止'
      if (countdownTimer) clearInterval(countdownTimer)
      return
    }
    remaining.value = {
      expired: false,
      days: Math.floor(diff / 86400000),
      hours: Math.floor((diff % 86400000) / 3600000),
      minutes: Math.floor((diff % 3600000) / 60000),
      seconds: Math.floor((diff % 60000) / 1000),
    }
    const r = remaining.value
    if (r.days > 0) countdownDisplay.value = `${r.days}天${r.hours}时`
    else countdownDisplay.value = `${r.hours}时${r.minutes}分${r.seconds}秒`
  }
  update()
  countdownTimer = setInterval(update, 1000)
}
onBeforeUnmount(() => { if (countdownTimer) clearInterval(countdownTimer) })

const loadDetail = async () => {
  loading.value = true
  try {
    const [aRes, pRes] = await Promise.all([
      assignmentApi.getById(assignmentId),
      assignmentApi.getProblemDetails(assignmentId),
    ])
    assignment.value = (aRes as any).data
    problems.value = (pRes as any).data || []

    // Load submissions for each problem (use problemId, not join-table id)
    const subRes: any = await submissionApi.getMySubmissions()
    const allSubs = subRes.data || []
    for (const p of problems.value) {
      const subs = allSubs.filter((s: any) => s.assignmentId === assignmentId && s.problemId === p.problemId)
      if (subs.length) {
        submissions.value[p.problemId] = subs[0]
      }
    }
  } catch {
    ElMessage.error('加载作业详情失败')
  } finally {
    loading.value = false
  }
  if (assignment.value?.endTime) startCountdown(assignment.value.endTime)
}

const typeLabel = (type: string) => {
  const m: Record<string, string> = { EXAM: '考试', LAB: '实验', PRACTICE: '练习' }
  return m[type] || type
}

const difficultyStars = (level: number) => '★'.repeat(level) + '☆'.repeat(5 - level)

const parseTagList = (tags: any): string[] => {
  if (!tags) return []
  if (Array.isArray(tags)) return tags
  if (typeof tags === 'string') {
    try { const arr = JSON.parse(tags); if (Array.isArray(arr)) return arr } catch { return [tags] }
  }
  return []
}

onMounted(loadDetail)
</script>

<template>
  <div class="assignment-detail" v-loading="loading">
    <template v-if="assignment">
      <el-card style="margin-bottom:20px">
        <template #header>
          <div style="display:flex;justify-content:space-between;align-items:center">
            <span style="font-size:18px;font-weight:600">{{ assignment.title }}</span>
            <el-tag :type="statusTag(assignment.status).type" size="small">{{ statusTag(assignment.status).label }}</el-tag>
          </div>
        </template>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="类型">{{ typeLabel(assignment.type) }}</el-descriptions-item>
          <el-descriptions-item label="满分">{{ assignment.maxScore }} 分</el-descriptions-item>
          <el-descriptions-item label="截止时间">
            <span :style="{ color: remaining.expired ? '#f56c6c' : '#303133' }">
              {{ countdownDisplay }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="开始时间" :span="3">
            {{ new Date(assignment.startTime).toLocaleString('zh-CN') }}
          </el-descriptions-item>
          <el-descriptions-item label="作业说明" :span="3" v-if="assignment.description">
            <div v-html="assignment.description" style="white-space:pre-wrap" />
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <h3 style="margin-bottom:16px">题目列表 ({{ problems.length }})</h3>
      <el-table :data="problems" stripe>
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="difficulty" label="难度" width="150">
          <template #default="{ row }">
            <span style="color:#e6a23c;letter-spacing:2px">{{ difficultyStars(row.difficulty) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="knowledgeTags" label="知识点" min-width="180">
          <template #default="{ row }">
            <template v-if="row.knowledgeTags">
              <el-tag v-for="tag in parseTagList(row.knowledgeTags)" :key="tag" size="small" style="margin:2px">{{ tag }}</el-tag>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="140">
          <template #default="{ row }">
            <template v-if="submissions[row.problemId]">
              <el-tag v-if="submissions[row.problemId].gradingStatus === 'DONE'" type="success" size="small">已批阅</el-tag>
              <el-tag v-else-if="submissions[row.problemId].gradingStatus === 'PROCESSING'" type="warning" size="small">批阅中</el-tag>
              <el-tag v-else type="info" size="small">已提交</el-tag>
            </template>
            <span v-else style="color:#909399">未提交</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="router.push(`/student/submit/${assignmentId}/${row.problemId}`)">
              {{ submissions[row.problemId] ? '再次提交' : '写代码' }}
            </el-button>
            <el-button v-if="submissions[row.problemId]?.gradingStatus === 'DONE'" size="small"
              @click="router.push(`/student/grading/${submissions[row.problemId].id}`)">
              查看结果
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </div>
</template>
