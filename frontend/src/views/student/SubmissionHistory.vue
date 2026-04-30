<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { submissionApi, assignmentApi } from '@/api'
import { formatDate, computeGrade, statusTag } from '@/utils'

const router = useRouter()
const submissions = ref<any[]>([])
const assignmentNames = ref<Record<number, string>>({})
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const res = await submissionApi.getMySubmissions()
    submissions.value = (res?.data || []).reverse()

    const ids = [...new Set(submissions.value.map((s: any) => s.assignmentId))]
    const names: Record<number, string> = {}
    await Promise.all(ids.map(async (aid) => {
      try {
        const aRes = await assignmentApi.getById(aid) as any
        names[aid as number] = aRes?.data?.title || `作业 #${aid}`
      } catch {
        names[aid as number] = `作业 #${aid}`
      }
    }))
    assignmentNames.value = names
  } catch { /* */ }
  finally { loading.value = false }
})
</script>

<template>
  <div class="submission-history">
    <h2>提交历史</h2>
    <el-table :data="submissions" stripe v-loading="loading" highlight-current-row style="cursor:pointer">
      <el-table-column prop="id" label="#" width="60" />
      <el-table-column label="所属作业" min-width="160">
        <template #default="{ row }">
          {{ assignmentNames[row.assignmentId] || `作业 #${row.assignmentId}` }}
        </template>
      </el-table-column>
      <el-table-column label="题目" width="80">
        <template #default="{ row }">#{{ row.problemId }}</template>
      </el-table-column>
      <el-table-column label="分数" width="80" align="center">
        <template #default="{ row }">
          <span v-if="row.totalScore != null" :class="`grade-${computeGrade(row.totalScore || row.score || 0).letter.toLowerCase()}`">
            {{ row.totalScore || row.score || '—' }}
          </span>
          <span v-else style="color:var(--text-placeholder)">—</span>
        </template>
      </el-table-column>
      <el-table-column label="等级" width="70" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.totalScore != null || row.score != null"
            :type="(row.totalScore || row.score) >= 80 ? 'success' : (row.totalScore || row.score) >= 60 ? 'warning' : 'danger'"
            size="small">
            {{ computeGrade(row.totalScore || row.score || 0).letter }}
          </el-tag>
          <span v-else style="color:var(--text-placeholder)">—</span>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" min-width="150">
        <template #default="{ row }">{{ formatDate(row.submittedAt) }}</template>
      </el-table-column>
      <el-table-column label="迟交" width="65">
        <template #default="{ row }">
          <el-tag :type="row.isLate ? 'warning' : 'success'" size="small">{{ row.isLate ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="85">
        <template #default="{ row }">
          <el-tag v-if="row.gradingStatus" :type="statusTag(row.gradingStatus).type" size="small">
            {{ statusTag(row.gradingStatus).label }}
          </el-tag>
          <el-tag v-else size="small" type="info">已提交</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click.stop="router.push(`/student/grading/${row.id}`)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && submissions.length === 0" description="暂无提交记录" :image-size="60" />
  </div>
</template>
