<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { courseApi, assignmentApi, gradingApi, classApi } from '@/api'
import { greetingByTime } from '@/utils'

const router = useRouter()
const auth = useAuthStore()
const greeting = greetingByTime(auth.username || '老师')
const loading = ref(true)

const stats = ref({ courses: 0, assignments: 0, pendingReviews: 0, classes: 0, students: 0 })
const pendingReviews = ref<any[]>([])
const classStats = ref<any[]>([])

onMounted(async () => {
  try {
    const results = await Promise.allSettled([
      courseApi.getMyCourses(),
      assignmentApi.getMyAssignments(),
      gradingApi.getPending(),
      classApi.getMyClasses(),
    ])
    const courses = (results[0] as any).value?.data || []
    const assignments = (results[1] as any).value?.data || []
    const pending = (results[2] as any).value?.data || []
    const classes = (results[3] as any).value?.data || []

    stats.value.courses = courses.length
    stats.value.assignments = assignments.filter((a: any) => a.status === 'PUBLISHED' || a.status === 'ACTIVE').length
    stats.value.pendingReviews = pending.length
    stats.value.classes = classes.length
    stats.value.students = classes.reduce((sum: number, c: any) => sum + (c.studentCount || 0), 0)

    pendingReviews.value = pending.slice(0, 5)
    classStats.value = classes.map((c: any) => ({
      ...c,
      completionRate: c.studentCount ? Math.round((c.submittedCount || 0) / Math.max(1, c.studentCount) * 100) : 0,
    })).slice(0, 4)
  } catch { /* degrade */ }
  finally { loading.value = false }
})
</script>

<template>
  <div class="teacher-dashboard" v-loading="loading">
    <div class="welcome-banner">
      <div class="welcome-text">
        <h1>{{ greeting }}</h1>
        <p>掌握教学全局，高效管理您的 C 语言课程</p>
      </div>
    </div>

    <el-row :gutter="16" class="stats-row">
      <el-col :xs="12" :sm="4.8">
        <div class="stat-card card-1" @click="router.push('/teacher/courses')">
          <div class="stat-number">{{ stats.courses }}</div>
          <div class="stat-label">活跃课程</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="4.8">
        <div class="stat-card card-2" @click="router.push('/teacher/assignments')">
          <div class="stat-number">{{ stats.assignments }}</div>
          <div class="stat-label">已发布作业</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="4.8">
        <div class="stat-card card-3" @click="router.push('/teacher/grading-review')">
          <div class="stat-number">{{ stats.pendingReviews }}</div>
          <div class="stat-label">待审核批阅</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="4.8">
        <div class="stat-card card-4">
          <div class="stat-number">{{ stats.classes }}</div>
          <div class="stat-label">班级总数</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="4.8">
        <div class="stat-card card-5">
          <div class="stat-number">{{ stats.students }}</div>
          <div class="stat-label">总学生数</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="content-row">
      <el-col :xs="24" :md="14">
        <el-card shadow="hover" class="section-card">
          <template #header>
            <div class="card-header">
              <span>待审核批阅</span>
              <el-button v-if="pendingReviews.length" size="small" type="primary" text @click="router.push('/teacher/grading-review')">查看全部</el-button>
            </div>
          </template>
          <el-empty v-if="pendingReviews.length === 0" description="暂无待审核批阅" :image-size="60" />
          <div v-else class="review-table">
            <div v-for="r in pendingReviews" :key="r.id" class="review-row">
              <span class="review-student">{{ r.studentName || `学生 #${r.studentId}` }}</span>
              <span class="review-problem">{{ r.problemTitle || `题目 #${r.problemId}` }}</span>
              <span :class="['review-score', r.totalScore >= 80 ? 'score-high' : r.totalScore >= 60 ? 'score-mid' : 'score-low']">{{ r.totalScore ?? '—' }}分</span>
              <el-button size="small" type="primary" @click="router.push('/teacher/grading-review')">审核</el-button>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="10">
        <el-card shadow="hover" class="section-card">
          <template #header><span>班级概况</span></template>
          <el-empty v-if="classStats.length === 0" description="暂无班级数据" :image-size="60" />
          <div v-else class="class-stats">
            <div v-for="c in classStats" :key="c.id" class="class-card" @click="router.push(`/teacher/classes/${c.courseId || c.id}`)">
              <div class="class-name">{{ c.name }}</div>
              <div class="class-info">{{ c.studentCount || 0 }} 学生 · {{ c.courseName || '' }}</div>
              <el-progress :percentage="c.completionRate" :stroke-width="6"
                :color="c.completionRate >= 80 ? 'var(--grade-a)' : c.completionRate >= 50 ? 'var(--color-accent)' : 'var(--grade-f)'" />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <div class="quick-actions">
      <el-button type="primary" @click="router.push('/teacher/assignments')">发布作业</el-button>
      <el-button @click="router.push('/teacher/grading-review')">审核批阅</el-button>
      <el-button @click="router.push('/teacher/analytics')">学情分析</el-button>
      <el-button @click="router.push('/teacher/announcements')">发布公告</el-button>
    </div>
  </div>
</template>

<style scoped>
.teacher-dashboard { max-width: 100%; }
.welcome-banner {
  background: linear-gradient(135deg, var(--primary-dark) 0%, var(--primary) 60%, var(--primary-light) 100%);
  border-radius: var(--radius-xl); padding: var(--space-8); margin-bottom: var(--space-5); color: #fff;
}
.welcome-banner h1 { color: #fff; font-size: var(--font-size-2xl); font-weight: 700; margin-bottom: var(--space-2); }
.welcome-banner p { color: rgba(255,255,255,0.85); font-size: var(--font-size-md); }
.stats-row { margin-bottom: var(--space-4); }
.stat-card {
  background: var(--bg-card); border-radius: var(--radius-lg); padding: var(--space-5);
  text-align: center; box-shadow: var(--shadow-sm); border-left: 4px solid transparent;
  cursor: pointer; transition: transform var(--transition-fast), box-shadow var(--transition-fast);
}
.stat-card:hover { transform: translateY(-2px); box-shadow: var(--shadow-md); }
.stat-card.card-1 { border-left-color: var(--primary); }
.stat-card.card-2 { border-left-color: var(--grade-b); }
.stat-card.card-3 { border-left-color: var(--color-accent); }
.stat-card.card-4 { border-left-color: var(--grade-a); }
.stat-card.card-5 { border-left-color: #7c3aed; }
.stat-number { font-size: var(--font-size-3xl); font-weight: 700; color: var(--text-primary); font-family: var(--font-display); }
.stat-label { font-size: var(--font-size-sm); color: var(--text-secondary); margin-top: var(--space-1); }
.content-row { margin-bottom: var(--space-4); }
.section-card { height: 100%; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.review-table { display: flex; flex-direction: column; }
.review-row {
  display: flex; align-items: center; gap: var(--space-3);
  padding: var(--space-3) 0; border-bottom: 1px solid var(--border-lighter);
}
.review-row:last-child { border-bottom: none; }
.review-student { min-width: 90px; font-weight: 600; font-size: var(--font-size-sm); }
.review-problem { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: var(--font-size-sm); color: var(--text-secondary); }
.review-score { min-width: 50px; font-weight: 700; font-size: var(--font-size-base); }
.class-stats { display: flex; flex-direction: column; gap: var(--space-3); }
.class-card {
  padding: var(--space-3); border-radius: var(--radius-md);
  border: 1px solid var(--border-lighter); cursor: pointer;
  transition: box-shadow var(--transition-fast);
}
.class-card:hover { box-shadow: var(--shadow-sm); }
.class-name { font-weight: 600; font-size: var(--font-size-sm); margin-bottom: 2px; }
.class-info { font-size: var(--font-size-xs); color: var(--text-secondary); margin-bottom: var(--space-2); }
.quick-actions { display: flex; gap: var(--space-3); flex-wrap: wrap; }
@media (max-width: 768px) {
  .welcome-banner { padding: var(--space-5); }
  .welcome-banner h1 { font-size: var(--font-size-xl); }
  .stat-card { padding: var(--space-3); }
  .stat-number { font-size: var(--font-size-2xl); }
  .review-row { flex-wrap: wrap; }
  .quick-actions { flex-direction: column; }
}
</style>
