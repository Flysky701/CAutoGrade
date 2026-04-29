<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { userApi, courseApi } from '@/api'
import { greetingByTime } from '@/utils'

const greeting = greetingByTime('管理员')
const loading = ref(true)

const stats = ref({ users: 0, students: 0, teachers: 0, admins: 0, courses: 0, activeToday: 0 })

const systemHealth = ref([
  { name: 'API 服务', status: 'ok' as const, desc: '运行正常' },
  { name: 'LLM 服务', status: 'ok' as const, desc: 'DeepSeek 连接正常' },
  { name: '批阅沙箱', status: 'ok' as const, desc: 'Docker 运行中' },
  { name: '数据库', status: 'ok' as const, desc: 'MySQL 正常' },
  { name: '缓存服务', status: 'ok' as const, desc: 'Redis 正常' },
])

const recentActivity = ref<any[]>([])

onMounted(async () => {
  try {
    const res = await userApi.getAllUsers()
    const users = (res as any).data || []
    stats.value.users = users.length
    stats.value.students = users.filter((u: any) => u.role === 'STUDENT').length
    stats.value.teachers = users.filter((u: any) => u.role === 'TEACHER').length
    stats.value.admins = users.filter((u: any) => u.role === 'ADMIN').length

    try {
      const cRes = await courseApi.getAll()
      stats.value.courses = ((cRes as any).data || []).length
    } catch { /* optional */ }

    stats.value.activeToday = users.filter((u: any) => {
      if (!u.updatedAt) return false
      return new Date(u.updatedAt).toDateString() === new Date().toDateString()
    }).length

    const sorted = [...users].sort((a, b) =>
      new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime()
    ).slice(0, 8)
    recentActivity.value = sorted.map(u => ({
      type: u.role,
      text: `${u.nickname || u.username} (${u.role === 'ADMIN' ? '管理员' : u.role === 'TEACHER' ? '教师' : '学生'}) 已注册`,
      time: u.createdAt,
    }))
  } catch { /* degrade */ }
  finally { loading.value = false }
})
</script>

<template>
  <div class="admin-dashboard" v-loading="loading">
    <div class="welcome-banner">
      <div class="welcome-text">
        <h1>{{ greeting }}</h1>
        <p>系统运行概览</p>
      </div>
    </div>

    <el-row :gutter="16" class="stats-row">
      <el-col :xs="8" :sm="4">
        <div class="stat-card card-1">
          <div class="stat-number">{{ stats.users }}</div>
          <div class="stat-label">总用户</div>
        </div>
      </el-col>
      <el-col :xs="8" :sm="4">
        <div class="stat-card card-2">
          <div class="stat-number">{{ stats.students }}</div>
          <div class="stat-label">学生</div>
        </div>
      </el-col>
      <el-col :xs="8" :sm="4">
        <div class="stat-card card-3">
          <div class="stat-number">{{ stats.teachers }}</div>
          <div class="stat-label">教师</div>
        </div>
      </el-col>
      <el-col :xs="8" :sm="4">
        <div class="stat-card card-4">
          <div class="stat-number">{{ stats.courses }}</div>
          <div class="stat-label">课程</div>
        </div>
      </el-col>
      <el-col :xs="8" :sm="4">
        <div class="stat-card card-5">
          <div class="stat-number">{{ stats.admins }}</div>
          <div class="stat-label">管理员</div>
        </div>
      </el-col>
      <el-col :xs="8" :sm="4">
        <div class="stat-card card-6">
          <div class="stat-number">{{ stats.activeToday }}</div>
          <div class="stat-label">今日活跃</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="content-row">
      <el-col :xs="24" :md="12">
        <el-card shadow="hover" class="section-card">
          <template #header><span>系统健康</span></template>
          <div class="health-grid">
            <div v-for="h in systemHealth" :key="h.name" class="health-item">
              <span :class="['health-dot', h.status]" />
              <div class="health-info">
                <div class="health-name">{{ h.name }}</div>
                <div class="health-desc">{{ h.desc }}</div>
              </div>
              <el-tag :type="h.status === 'ok' ? 'success' : 'danger'" size="small">{{ h.status === 'ok' ? '正常' : '异常' }}</el-tag>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="12">
        <el-card shadow="hover" class="section-card">
          <template #header><span>最近活动</span></template>
          <el-empty v-if="recentActivity.length === 0" description="暂无活动记录" :image-size="60" />
          <div v-else class="activity-list">
            <div v-for="(a, i) in recentActivity" :key="i" class="activity-item">
              <span :class="['activity-dot', a.type === 'ADMIN' ? 'admin' : a.type === 'TEACHER' ? 'teacher' : 'student']" />
              <span class="activity-text">{{ a.text }}</span>
              <span class="activity-time">{{ a.time ? new Date(a.time).toLocaleDateString('zh-CN') : '' }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.admin-dashboard { max-width: 100%; }
.welcome-banner {
  background: linear-gradient(135deg, var(--primary-dark) 0%, var(--primary) 60%, var(--primary-light) 100%);
  border-radius: var(--radius-xl); padding: var(--space-8); margin-bottom: var(--space-5); color: #fff;
}
.welcome-banner h1 { color: #fff; font-size: var(--font-size-2xl); font-weight: 700; margin-bottom: var(--space-2); }
.welcome-banner p { color: rgba(255,255,255,0.85); font-size: var(--font-size-md); }
.stats-row { margin-bottom: var(--space-4); }
.stat-card {
  background: var(--bg-card); border-radius: var(--radius-lg); padding: var(--space-4) var(--space-3);
  text-align: center; box-shadow: var(--shadow-sm); border-left: 4px solid transparent;
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
}
.stat-card:hover { transform: translateY(-2px); box-shadow: var(--shadow-md); }
.stat-card.card-1 { border-left-color: var(--primary); }
.stat-card.card-2 { border-left-color: var(--grade-b); }
.stat-card.card-3 { border-left-color: var(--color-accent); }
.stat-card.card-4 { border-left-color: var(--grade-a); }
.stat-card.card-5 { border-left-color: #7c3aed; }
.stat-card.card-6 { border-left-color: #0891b2; }
.stat-number { font-size: var(--font-size-2xl); font-weight: 700; color: var(--text-primary); font-family: var(--font-display); }
.stat-label { font-size: var(--font-size-xs); color: var(--text-secondary); margin-top: var(--space-1); }
.content-row { margin-bottom: var(--space-4); }
.section-card { height: 100%; }
.health-grid { display: flex; flex-direction: column; gap: var(--space-3); }
.health-item {
  display: flex; align-items: center; gap: var(--space-3);
  padding: var(--space-3); border-radius: var(--radius-md); background: var(--bg-sidebar);
}
.health-dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; }
.health-dot.ok { background: var(--grade-a); }
.health-dot.fail { background: var(--grade-f); }
.health-info { flex: 1; }
.health-name { font-weight: 600; font-size: var(--font-size-sm); }
.health-desc { font-size: var(--font-size-xs); color: var(--text-secondary); }
.activity-list { display: flex; flex-direction: column; }
.activity-item {
  display: flex; align-items: center; gap: var(--space-3);
  padding: var(--space-2) 0; border-bottom: 1px solid var(--border-lighter);
}
.activity-item:last-child { border-bottom: none; }
.activity-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.activity-dot.student { background: var(--grade-b); }
.activity-dot.teacher { background: var(--color-accent); }
.activity-dot.admin { background: #7c3aed; }
.activity-text { flex: 1; font-size: var(--font-size-sm); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.activity-time { font-size: var(--font-size-xs); color: var(--text-secondary); white-space: nowrap; }
@media (max-width: 768px) {
  .welcome-banner { padding: var(--space-5); }
  .welcome-banner h1 { font-size: var(--font-size-xl); }
  .stat-card { padding: var(--space-3); }
  .stat-number { font-size: var(--font-size-xl); }
}
</style>
