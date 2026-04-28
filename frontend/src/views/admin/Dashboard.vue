<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElRow, ElCol, ElCard } from 'element-plus';
import { userApi } from '../../api';

const stats = ref({
  totalUsers: 0,
  totalStudents: 0,
  totalTeachers: 0,
});

onMounted(async () => {
  try {
    const users = (await userApi.getAllUsers()).data || [];
    stats.value.totalUsers = users.length;
    stats.value.totalStudents = users.filter((u: any) => u.role === 'STUDENT').length;
    stats.value.totalTeachers = users.filter((u: any) => u.role === 'TEACHER').length;
  } catch (e) {
    console.error(e);
  }
});
</script>

<template>
  <div>
    <h2>系统概览</h2>
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card>
          <el-statistic title="用户总数" :value="stats.totalUsers" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <el-statistic title="学生数" :value="stats.totalStudents" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <el-statistic title="教师数" :value="stats.totalTeachers" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
