<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { userApi } from '../../api';

const profile = ref<any>({});
const nickname = ref('');

onMounted(async () => {
  try {
    profile.value = (await userApi.getProfile()).data;
    nickname.value = profile.value.nickname;
  } catch (e) {
    console.error(e);
  }
});

const handleUpdate = async () => {
  try {
    await userApi.updateProfile({ nickname: nickname.value });
    ElMessage.success('更新成功');
  } catch (e) {
    ElMessage.error('更新失败');
  }
};
</script>

<template>
  <div>
    <h2>个人设置</h2>
    <el-card>
      <el-form label-width="100px">
        <el-form-item label="用户名">
          <el-input v-model="profile.username" disabled />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="nickname" />
        </el-form-item>
        <el-form-item label="角色">
          <el-input v-model="profile.role" disabled />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleUpdate">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>
