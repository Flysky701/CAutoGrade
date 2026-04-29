<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const registerForm = ref({ username: '', password: '', passwordConfirm: '', nickname: '', role: 'STUDENT' })
const loading = ref(false)

const handleRegister = async () => {
  const f = registerForm.value
  if (!f.username || !f.password || !f.nickname) {
    ElMessage.warning('请填写所有必填项')
    return
  }
  if (f.password !== f.passwordConfirm) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  loading.value = true
  try {
    const res = await authApi.register({ username: f.username, password: f.password, nickname: f.nickname, role: f.role })
    const inner = res?.data ?? res
    if (!inner?.token) {
      const msg = inner?.msg || res?.msg || '注册失败'
      throw new Error(msg)
    }
    let role = inner.role || ''
    if (role.startsWith('ROLE_')) role = role.substring(5)
    auth.setAuth(inner.token, role, inner.username)
    ElMessage.success('注册成功')
    const routeMap: Record<string, string> = {
      ADMIN: '/admin/dashboard', TEACHER: '/teacher/dashboard', STUDENT: '/student/dashboard',
    }
    router.push(routeMap[role] || '/student/dashboard')
  } catch (error: any) {
    ElMessage.error(error.response?.data?.msg || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-container">
    <div class="auth-card">
      <div class="auth-brand">
        <div class="auth-logo">C</div>
        <h1>创建账号</h1>
        <p class="auth-subtitle">加入 C 语言自动批阅系统</p>
      </div>
      <el-form :model="registerForm" @submit.prevent="handleRegister" label-position="top">
        <el-form-item label="用户名">
          <el-input v-model="registerForm.username" placeholder="用户名 / 学号 / 工号" size="large" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="registerForm.nickname" placeholder="你的姓名或昵称" size="large" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="registerForm.password" type="password" placeholder="密码" show-password size="large" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="registerForm.passwordConfirm" type="password" placeholder="再次输入密码" show-password
            size="large" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="registerForm.role" size="large" class="auth-select">
            <el-option label="学生" value="STUDENT" />
            <el-option label="教师" value="TEACHER" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" size="large" class="auth-btn" @click="handleRegister">
            注 册
          </el-button>
        </el-form-item>
      </el-form>
      <div class="auth-footer">
        <router-link to="/login">已有账号？立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--primary-dark) 0%, var(--primary) 50%, var(--primary-light) 100%);
}
.auth-card {
  width: 420px;
  padding: var(--space-10);
  background: var(--bg-card);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-xl);
}
.auth-brand {
  text-align: center;
  margin-bottom: var(--space-6);
}
.auth-logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  background: var(--primary);
  color: #fff;
  font-size: var(--font-size-xl);
  font-weight: 700;
  border-radius: var(--radius-lg);
  margin-bottom: var(--space-4);
}
.auth-brand h1 {
  font-size: var(--font-size-xl);
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: var(--space-1);
}
.auth-subtitle {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}
.auth-btn { width: 100%; font-weight: 600; letter-spacing: 1px; }
.auth-select { width: 100%; }
.auth-footer { text-align: center; }
.auth-footer a { color: var(--primary); text-decoration: none; font-size: var(--font-size-sm); }
.auth-footer a:hover { color: var(--primary-light); }
</style>
