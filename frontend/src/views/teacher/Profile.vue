<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { roleLabel } from '@/utils'

const auth = useAuthStore()
const profile = ref<any>({})
const nickname = ref('')

const pwForm = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const savingPw = ref(false)

onMounted(async () => {
  try {
    const res = await userApi.getProfile()
    profile.value = (res as any)?.data || {}
    nickname.value = profile.value.nickname || ''
  } catch { /* */ }
})

const handleUpdate = async () => {
  try {
    await userApi.updateProfile({ nickname: nickname.value })
    ElMessage.success('更新成功')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '更新失败')
  }
}

const handleChangePassword = async () => {
  if (!pwForm.oldPassword || !pwForm.newPassword) {
    ElMessage.warning('请填写旧密码和新密码')
    return
  }
  if (pwForm.newPassword !== pwForm.confirm) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  savingPw.value = true
  try {
    await userApi.changePassword({ oldPassword: pwForm.oldPassword, newPassword: pwForm.newPassword })
    ElMessage.success('密码修改成功，请重新登录')
    pwForm.oldPassword = ''; pwForm.newPassword = ''; pwForm.confirm = ''
    setTimeout(() => auth.logout(), 1500)
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '修改失败')
  } finally { savingPw.value = false }
}

const handleLogout = () => auth.logout()
</script>

<template>
  <div class="profile-page">
    <h2>个人设置</h2>

    <el-row :gutter="20">
      <el-col :xs="24" :md="14">
        <el-card shadow="hover" class="profile-card">
          <template #header><span>个人信息</span></template>
          <el-form label-width="80px" class="profile-form">
            <el-form-item label="用户名">
              <el-input :model-value="profile.username" disabled />
            </el-form-item>
            <el-form-item label="角色">
              <el-input :model-value="roleLabel(profile.role)" disabled />
            </el-form-item>
            <el-form-item label="昵称">
              <el-input v-model="nickname" placeholder="请输入昵称" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleUpdate">保存</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="10">
        <el-card shadow="hover" class="profile-card">
          <template #header><span>修改密码</span></template>
          <el-form :model="pwForm" label-width="80px">
            <el-form-item label="旧密码">
              <el-input v-model="pwForm.oldPassword" type="password" show-password placeholder="原密码" />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input v-model="pwForm.newPassword" type="password" show-password placeholder="新密码" />
            </el-form-item>
            <el-form-item label="确认密码">
              <el-input v-model="pwForm.confirm" type="password" show-password placeholder="再次输入新密码" />
            </el-form-item>
            <el-form-item>
              <el-button type="warning" :loading="savingPw" @click="handleChangePassword">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover" class="logout-card">
      <div class="logout-row">
        <span>退出当前登录的教师账号</span>
        <el-button type="danger" plain @click="handleLogout">退出登录</el-button>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.profile-page { max-width: 800px; }
.profile-card { margin-bottom: var(--space-5); }
.profile-form :deep(.el-form-item) { margin-bottom: var(--space-4); }
.logout-card { margin-top: var(--space-5); }
.logout-row { display: flex; justify-content: space-between; align-items: center; }
</style>
