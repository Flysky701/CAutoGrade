<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loginForm = ref({ username: '', password: '' })
const loading = ref(false)

const handleLogin = async () => {
  if (!loginForm.value.username || !loginForm.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const res = await authApi.login(loginForm.value)
    // axios 拦截器返回 response.data，即 HTTP body = { code, msg, data: {...} }
    // res.data 是业务数据 { token, username, role }
    const inner = res?.data ?? res
    if (!inner?.token) {
      const msg = inner?.msg || res?.msg || '登录失败，请检查用户名和密码'
      throw new Error(msg)
    }

    let role = inner.role || ''
    if (role.startsWith('ROLE_')) role = role.substring(5)

    auth.setAuth(inner.token, role, inner.username)
    // 确保 localStorage 已写入后再跳转
    await new Promise(r => setTimeout(r, 0))
    const routeMap: Record<string, string> = {
      ADMIN: '/admin/dashboard', TEACHER: '/teacher/dashboard', STUDENT: '/student/dashboard',
    }
    await router.replace(routeMap[role] || '/student/dashboard')
  } catch (error: any) {
    ElMessage.error(error.response?.data?.msg || error.message || '登录失败')
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
        <h1>C 语言自动批阅系统</h1>
        <p class="auth-subtitle">AI 驱动的编程作业智能批阅平台</p>
      </div>
      <el-form :model="loginForm" @submit.prevent="handleLogin" label-position="top">
        <el-form-item label="用户名">
          <el-input v-model="loginForm.username" placeholder="用户名 / 学号 / 工号" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="loginForm.password" type="password" placeholder="密码" prefix-icon="Lock" show-password
            size="large" @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" size="large" class="auth-btn" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="auth-footer">
        <router-link to="/register">没有账号？立即注册</router-link>
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
  margin-bottom: var(--space-8);
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
.auth-btn {
  width: 100%;
  font-weight: 600;
  letter-spacing: 1px;
}
.auth-footer {
  text-align: center;
}
.auth-footer a {
  color: var(--primary);
  text-decoration: none;
  font-size: var(--font-size-sm);
}
.auth-footer a:hover { color: var(--primary-light); }
</style>
