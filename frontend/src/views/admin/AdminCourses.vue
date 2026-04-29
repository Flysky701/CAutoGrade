<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage, ElTable, ElTableColumn, ElTag, ElButton } from 'element-plus';
import { courseApi } from '../../api';

const courses = ref<any[]>([]);
const loading = ref(false);

const loadCourses = async () => {
  loading.value = true;
  try {
    courses.value = (await courseApi.getAll()).data || [];
  } catch {
    ElMessage.error('加载课程列表失败');
  } finally {
    loading.value = false;
  }
};

const statusTagType = (s: string) => s === 'ACTIVE' ? 'success' : 'info';

onMounted(loadCourses);
</script>

<template>
  <div>
    <div class="page-header">
      <h2>课程管理</h2>
      <span style="color:#909399;font-size:14px">共 {{ courses.length }} 门课程</span>
    </div>
    <el-table :data="courses" stripe v-loading="loading" style="margin-top:16px">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="课程名称" />
      <el-table-column prop="semester" label="学期" width="140" />
      <el-table-column prop="teacherName" label="教师" width="120" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">
          {{ row.createdAt ? new Date(row.createdAt).toLocaleString('zh-CN') : '-' }}
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && courses.length === 0" description="暂无课程" />
  </div>
</template>

<style scoped>
.page-header {
  display: flex; justify-content: space-between; align-items: center;
}
</style>
