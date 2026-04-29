<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { courseApi, classApi } from '@/api'

const router = useRouter()
const courses = ref<any[]>([])
const joinCode = ref('')
const loading = ref(false)

const loadCourses = async () => {
  try {
    courses.value = (await classApi.getMyClassesAsStudent()).data || []
  } catch { /* */ }
}

const handleJoin = async () => {
  if (!joinCode.value) { ElMessage.warning('请输入选课码'); return }
  loading.value = true
  try {
    await classApi.join(joinCode.value)
    ElMessage.success('加入成功')
    joinCode.value = ''
    loadCourses()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.msg || '加入失败')
  } finally { loading.value = false }
}

onMounted(loadCourses)
</script>

<template>
  <div class="course-list">
    <div class="page-header">
      <h2>我的课程</h2>
    </div>

    <el-card shadow="hover" class="join-card">
      <div class="join-row">
        <el-input v-model="joinCode" placeholder="输入选课码加入课程" class="join-input" size="large">
          <template #prefix>选课码</template>
        </el-input>
        <el-button type="primary" :loading="loading" size="large" @click="handleJoin">加入课程</el-button>
      </div>
    </el-card>

    <el-row :gutter="16" class="course-grid" v-if="courses.length">
      <el-col v-for="c in courses" :key="c.id" :xs="24" :sm="12" :lg="8">
        <el-card shadow="hover" class="course-card" @click="router.push(`/student/assignments/${c.courseId}`)">
          <template #header>
            <div class="course-header">
              <span class="course-name">{{ c.courseName || c.name }}</span>
              <el-tag size="small" effect="plain">{{ c.studentCount || 0 }} 人</el-tag>
            </div>
          </template>
          <div class="course-body">
            <div class="course-class">班级：{{ c.name }}</div>
            <div class="course-action">
              <el-button type="primary" size="small">查看作业</el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && courses.length === 0" description="暂无课程，使用选课码加入课程吧" :image-size="80" />
  </div>
</template>

<style scoped>
.join-card { margin-bottom: var(--space-5); }
.join-row { display: flex; gap: var(--space-3); align-items: center; }
.join-input { flex: 1; max-width: 400px; }
.course-grid { margin-bottom: var(--space-4); }
.course-card { cursor: pointer; transition: transform var(--transition-fast), box-shadow var(--transition-fast); border-left: 3px solid var(--primary); }
.course-card:hover { transform: translateY(-2px); }
.course-header { display: flex; justify-content: space-between; align-items: center; }
.course-name { font-weight: 600; }
.course-body { display: flex; justify-content: space-between; align-items: center; }
.course-class { font-size: var(--font-size-sm); color: var(--text-secondary); }
@media (max-width: 768px) {
  .join-row { flex-direction: column; }
  .join-input { max-width: 100%; }
}
</style>
