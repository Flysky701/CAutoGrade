<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const goHome = () => {
  if (auth.isAdmin()) router.push('/admin/dashboard')
  else if (auth.isTeacher()) router.push('/teacher/dashboard')
  else if (auth.isStudent()) router.push('/student/dashboard')
  else router.push('/login')
}
</script>

<template>
  <div class="not-found">
    <div class="not-found-content">
      <h1>404</h1>
      <p>页面不存在</p>
      <el-button type="primary" @click="goHome">返回首页</el-button>
    </div>
  </div>
</template>

<style scoped>
.not-found {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 60vh;
}
.not-found-content {
  text-align: center;
}
.not-found-content h1 {
  font-size: 96px;
  font-weight: 700;
  color: #409eff;
  margin-bottom: 12px;
  line-height: 1;
}
.not-found-content p {
  font-size: 18px;
  color: #909399;
  margin-bottom: 24px;
}
</style>
