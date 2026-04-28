<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage, ElButton } from 'element-plus';

const handleLogout = () => {
  localStorage.clear();
  window.location.href = '/login';
};
</script>

<template>
  <div>
    <h2>个人设置</h2>
    <el-button type="primary" @click="handleLogout">退出登录</el-button>
  </div>
</template>
