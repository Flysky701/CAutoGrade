<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage, ElButton, ElCard, ElForm, ElFormItem, ElInput } from 'element-plus';
import { userApi } from '../../api';

const profile = ref<any>({});
const nickname = ref('');

onMounted(async () => {
  try {
    const res = await userApi.getProfile();
    profile.value = res?.data || {};
    nickname.value = profile.value.nickname || '';
  } catch (e) {
    console.error(e);
  }
});

const handleUpdate = async () => {
  try {
    await userApi.updateProfile({ nickname: nickname.value });
    ElMessage.success('更新成功');
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '更新失败');
  }
};

const handleLogout = () => {
  localStorage.clear();
  window.location.href = '/login';
};
</script>

<template>
  <div>
    <h2>个人设置</h2>
    <el-card>
      <el-form label-width="80px" style="max-width: 480px;">
        <el-form-item label="用户名">
          <el-input :model-value="profile.username" disabled />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="角色">
          <el-input :model-value="profile.role" disabled />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleUpdate">保存</el-button>
          <el-button type="danger" @click="handleLogout" style="margin-left: 12px;">退出登录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>
