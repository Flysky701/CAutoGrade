<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage, ElTable, ElTableColumn, ElButton, ElTag } from 'element-plus';
import { userApi } from '../../api';

const users = ref<any[]>([]);

const loadUsers = async () => {
  try {
    users.value = (await userApi.getAllUsers()).data || [];
  } catch (e) {
    console.error(e);
  }
};

const handleDisable = async (id: number) => {
  try {
    await userApi.disableUser(id);
    ElMessage.success('禁用成功');
    loadUsers();
  } catch (e) {
    ElMessage.error('操作失败');
  }
};

const handleEnable = async (id: number) => {
  try {
    await userApi.enableUser(id);
    ElMessage.success('启用成功');
    loadUsers();
  } catch (e) {
    ElMessage.error('操作失败');
  }
};

onMounted(loadUsers);
</script>

<template>
  <div>
    <h2>用户管理</h2>
    <el-table :data="users" stripe style="margin-top: 20px">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="role" label="角色">
        <template #default="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'danger' : row.role === 'TEACHER' ? 'success' : 'primary'">
            {{ row.role }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button v-if="row.status === 1" size="small" @click="handleDisable(row.id)">禁用</el-button>
          <el-button v-else size="small" @click="handleEnable(row.id)">启用</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>
