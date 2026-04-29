<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { notificationApi, submissionApi, classApi, assignmentApi, announcementApi } from '@/api'
import { useCountdown } from '@/composables/useCountdown'
import { computeGrade, greetingByTime } from '@/utils'

const router = useRouter()
const auth = useAuthStore()
const greeting = greetingByTime(auth.username || '同学')
const loading = ref(true)

const stats = ref({ courses: 0, pending: 0, avgScore: 0, weekly: 0 })
const deadlines = ref<any[]>([])
const recentGrades = ref<any[]>([])
const notifications = ref<any[]>([])
const recentSubmissions = ref<any[]>([])
const announcements = ref<any[]>([])

const urgencyClass = (endTime: string) => {
  const h = (new Date(endTime).getTime() - Date.now()) / 3600000
  if (h < 24) return 'deadline-urgent'
  if (h < 72) return 'deadline-soon'
  return 'deadline-ok'
}

onMounted(async () => {
  try {
    const [classRes, notifRes, subRes] = await Promise.allSettled([
      classApi.getMyClassesAsStudent(),
      notificationApi.getUnread().catch(() => ({ data: [] } as any)),
      submissionApi.getMySubmissions().catch(() => ({ data: [] } as any)),
    ])
    const classes = (classRes as any).value?.data || []
    const notifs = (notifRes as any).value?.data || []
    const subs = (subRes as any).value?.data || []

    // Load announcements from all enrolled courses
    const courseIds = [...new Set(classes.map((c: any) => c.courseId).filter(Boolean))]
    if (courseIds.length > 0) {
      const annPromises = courseIds.map((cid: number) =>
        announcementApi.getByCourse(cid).then((r: any) => r.data || []).catch(() => [])
      )
      const allAnnouncements = (await Promise.all(annPromises)).flat()
      announcements.value = allAnnouncements
        .sort((a: any, b: any) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
        .slice(0, 5)
    }

    stats.value.courses = classes.length
    notifications.value = notifs.slice(0, 5)
    recentSubmissions.value = subs.slice(0, 5)
    stats.value.weekly = subs.filter((s: any) =>
      new Date(s.submittedAt).getTime() > Date.now() - 7 * 86400000
    ).length

    // Gather all assignments from all courses, check pending
    const allAssignments: any[] = []
    for (const c of classes) {
      try {
        const aRes = await assignmentApi.getByCourse(c.courseId || c.id)
        const assignments = (aRes as any).data || []
        allAssignments.push(...assignments.map((a: any) => ({ ...a, className: c.name })))
      } catch { /* skip */ }
    }

    // Pending: published, not expired, student hasn't submitted for all problems
    const submittedIds = new Set(subs.map((s: any) => s.assignmentId))
    deadlines.value = allAssignments
      .filter((a: any) => {
        const notExpired = new Date(a.endTime).getTime() > Date.now()
        const isPublished = a.status === 'PUBLISHED' || a.status === 'ACTIVE'
        return notExpired && isPublished
      })
      .sort((a: any, b: any) => new Date(a.endTime).getTime() - new Date(b.endTime).getTime())
      .slice(0, 5)
    stats.value.pending = deadlines.value.length

    // Recent grades
    const graded = subs
      .filter((s: any) => s.gradingStatus === 'DONE' || s.totalScore != null)
      .slice(0, 5)
    recentGrades.value = graded
    if (graded.length > 0) {
      const total = graded.reduce((sum: number, s: any) => sum + (s.totalScore || s.score || 0), 0)
      stats.value.avgScore = Math.round(total / graded.length)
    }
  } catch { /* gracefully degrade */ }
  finally { loading.value = false }
})
</script>

<template>
  <div class="student-dashboard" v-loading="loading">
    <!-- Welcome Banner -->
    <div class="welcome-banner">
      <div class="welcome-text">
        <h1>{{ greeting }}</h1>
        <p>欢迎回到 C 语言学习平台，今天也请加油</p>
      </div>
    </div>

    <!-- Stats Row -->
    <el-row :gutter="16" class="stats-row">
      <el-col :xs="12" :sm="6">
        <div class="stat-card card-1">
          <div class="stat-number">{{ stats.courses }}</div>
          <div class="stat-label">已选课程</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card card-2">
          <div class="stat-number">{{ stats.pending }}</div>
          <div class="stat-label">待完成作业</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card card-3">
          <div class="stat-number">{{ stats.avgScore }}</div>
          <div class="stat-label">平均分</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card card-4">
          <div class="stat-number">{{ stats.weekly }}</div>
          <div class="stat-label">本周提交</div>
        </div>
      </el-col>
    </el-row>

    <!-- Announcements -->
    <el-card v-if="announcements.length" shadow="hover" class="section-card" style="margin-bottom:16px">
      <template #header><div class="card-header"><span>课程公告</span><el-tag size="small">{{ announcements.length }} 条</el-tag></div></template>
      <div v-for="a in announcements" :key="a.id" class="list-item">
        <div>
          <span v-if="a.pinned" style="color:#f56c6c;margin-right:4px">[置顶]</span>
          <span class="list-title">{{ a.title }}</span>
        </div>
        <span class="list-date">{{ new Date(a.createdAt).toLocaleDateString('zh-CN') }}</span>
      </div>
    </el-card>

    <!-- Upcoming Deadlines + Recent Grades -->
    <el-row :gutter="16" class="content-row">
      <el-col :xs="24" :md="12">
        <el-card shadow="hover" class="section-card">
          <template #header>
            <div class="card-header"><span>即将截止</span><el-tag v-if="deadlines.length" size="small" :type="deadlines[0].urgency === 'urgent' ? 'danger' : 'warning'">{{ deadlines.length }} 项</el-tag></div>
          </template>
          <el-empty v-if="deadlines.length === 0" description="暂无待完成作业" :image-size="60" />
          <div v-else class="deadline-list">
            <div v-for="d in deadlines" :key="d.id" :class="['deadline-item', urgencyClass(d.endTime)]" @click="router.push(`/student/assignment/${d.id}`)">
              <div class="deadline-info">
                <div class="deadline-title">{{ d.title }}</div>
                <div class="deadline-meta">{{ d.className || '' }} · 截止 {{ new Date(d.endTime).toLocaleDateString('zh-CN') }}</div>
              </div>
              <el-tag :type="new Date(d.endTime).getTime() - Date.now() < 86400000 ? 'danger' : 'warning'" size="small" effect="plain">
                {{ new Date(d.endTime).getTime() - Date.now() < 86400000
                   ? Math.ceil((new Date(d.endTime).getTime() - Date.now()) / 3600000) + '时'
                   : Math.ceil((new Date(d.endTime).getTime() - Date.now()) / 86400000) + '天' }}
              </el-tag>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="hover" class="section-card">
          <template #header><div class="card-header"><span>最近成绩</span></div></template>
          <el-empty v-if="recentGrades.length === 0" description="暂无批阅成绩" :image-size="60" />
          <div v-else class="grade-list">
            <div v-for="g in recentGrades" :key="g.id" class="grade-item" @click="router.push(`/student/grading/${g.id}`)">
              <div class="grade-score">
                <span :class="`grade-letter grade-${computeGrade(g.totalScore || g.score || 0).letter.toLowerCase()}`">
                  {{ computeGrade(g.totalScore || g.score || 0).letter }}
                </span>
                <span class="grade-num">{{ g.totalScore || g.score || 0 }} 分</span>
              </div>
              <div class="grade-meta">作业 #{{ g.assignmentId }} · 题 #{{ g.problemId }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Quick Actions -->
    <div class="quick-actions">
      <el-button type="primary" @click="router.push('/student/courses')">浏览课程</el-button>
      <el-button @click="router.push('/student/history')">查看提交历史</el-button>
      <el-button @click="router.push('/student/profile')">个人设置</el-button>
    </div>

    <!-- Notifications + Recent Submissions -->
    <el-row :gutter="16" class="content-row">
      <el-col :xs="24" :md="12">
        <el-card shadow="hover" class="section-card">
          <template #header><div class="card-header"><span>通知消息</span></div></template>
          <el-empty v-if="notifications.length === 0" description="暂无未读通知" :image-size="60" />
          <div v-else>
            <div v-for="n in notifications" :key="n.id" class="list-item">
              <span class="list-title">{{ n.title }}</span>
              <span class="list-date">{{ new Date(n.createdAt).toLocaleDateString('zh-CN') }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="hover" class="section-card">
          <template #header><div class="card-header"><span>最近提交</span></div></template>
          <el-empty v-if="recentSubmissions.length === 0" description="暂无提交记录" :image-size="60" />
          <div v-else>
            <div v-for="s in recentSubmissions" :key="s.id" class="list-item" @click="router.push(`/student/grading/${s.id}`)">
              <span class="list-title">作业 #{{ s.assignmentId }} · 题 #{{ s.problemId }}</span>
              <span class="list-date">{{ new Date(s.submittedAt).toLocaleDateString('zh-CN') }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.student-dashboard { max-width: 100%; }

/* Welcome Banner */
.welcome-banner {
  background: linear-gradient(135deg, var(--primary-dark) 0%, var(--primary) 60%, var(--primary-light) 100%);
  border-radius: var(--radius-xl);
  padding: var(--space-8);
  margin-bottom: var(--space-5);
  color: #fff;
}
.welcome-banner h1 { color: #fff; font-size: var(--font-size-2xl); font-weight: 700; margin-bottom: var(--space-2); }
.welcome-banner p { color: rgba(255,255,255,0.85); font-size: var(--font-size-md); }

/* Stats Row */
.stats-row { margin-bottom: var(--space-4); }
.stat-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  text-align: center;
  box-shadow: var(--shadow-sm);
  border-left: 4px solid transparent;
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
}
.stat-card:hover { transform: translateY(-2px); box-shadow: var(--shadow-md); }
.stat-card.card-1 { border-left-color: var(--primary); }
.stat-card.card-2 { border-left-color: var(--color-accent); }
.stat-card.card-3 { border-left-color: var(--grade-a); }
.stat-card.card-4 { border-left-color: var(--grade-b); }
.stat-number { font-size: var(--font-size-3xl); font-weight: 700; color: var(--text-primary); font-family: var(--font-display); }
.stat-label { font-size: var(--font-size-sm); color: var(--text-secondary); margin-top: var(--space-1); }

/* Content Rows */
.content-row { margin-bottom: var(--space-4); }
.section-card { height: 100%; }

/* Card Header */
.card-header { display: flex; justify-content: space-between; align-items: center; }

/* Deadline List */
.deadline-list { display: flex; flex-direction: column; gap: var(--space-2); }
.deadline-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: var(--space-3); border-radius: var(--radius-md);
  cursor: pointer; transition: background var(--transition-fast);
}
.deadline-item:hover { background: var(--bg-sidebar); }
.deadline-title { font-weight: 600; font-size: var(--font-size-sm); margin-bottom: 2px; }
.deadline-meta { font-size: var(--font-size-xs); color: var(--text-secondary); }
.deadline-urgent { border-left: 3px solid var(--grade-f); background: rgba(220,38,38,0.03); }
.deadline-soon { border-left: 3px solid var(--grade-c); background: rgba(217,119,6,0.03); }
.deadline-ok { border-left: 3px solid transparent; }

/* Grade List */
.grade-list { display: flex; flex-direction: column; gap: var(--space-2); }
.grade-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: var(--space-3); border-radius: var(--radius-md);
  cursor: pointer; transition: background var(--transition-fast);
}
.grade-item:hover { background: var(--bg-sidebar); }
.grade-score { display: flex; align-items: center; gap: var(--space-3); }
.grade-letter { font-size: var(--font-size-xl); font-weight: 800; min-width: 28px; }
.grade-num { font-size: var(--font-size-base); font-weight: 600; color: var(--text-regular); }
.grade-meta { font-size: var(--font-size-xs); color: var(--text-secondary); }

/* Quick Actions */
.quick-actions { display: flex; gap: var(--space-3); margin-bottom: var(--space-5); flex-wrap: wrap; }

/* List Items */
.list-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: var(--space-2) 0; border-bottom: 1px solid var(--border-lighter);
  cursor: pointer; transition: color var(--transition-fast);
}
.list-item:last-child { border-bottom: none; }
.list-item:hover { color: var(--primary); }
.list-title { font-size: var(--font-size-sm); }
.list-date { color: var(--text-secondary); font-size: var(--font-size-xs); }

@media (max-width: 768px) {
  .welcome-banner { padding: var(--space-5); }
  .welcome-banner h1 { font-size: var(--font-size-xl); }
  .stat-card { padding: var(--space-3); }
  .stat-number { font-size: var(--font-size-2xl); }
  .quick-actions { flex-direction: column; }
}
</style>
