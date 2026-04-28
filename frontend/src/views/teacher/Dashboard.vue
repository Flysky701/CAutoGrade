<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElCard, ElRow, ElCol } from 'element-plus';
import { courseApi, assignmentApi } from '../../api';

const stats = ref({
  courseCount: 0,
  assignmentCount: 0,
  studentCount: 0,
});

onMounted(async () => {
  try {
    const courses = (await courseApi.getMyCourses()).data || [];
    stats.value.courseCount = courses.length;
    const assignments = (await assignmentApi.getMyAssignments()).data || [];
    stats.value.assignmentCount = assignments.length;
  } catch (e) {
    console.error(e);
  }
});
</script>

<template>
  <div>
    <h2>教师首页</h2>
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card>
          <el-statistic title="我的课程" :value="stats.courseCount" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <el-statistic title="作业总数" :value="stats.assignmentCount" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
