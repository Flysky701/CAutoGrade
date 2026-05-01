<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { ElMessage, ElTable, ElTableColumn, ElButton, ElSelect, ElOption, ElPopconfirm } from 'element-plus';
import { courseApi, classApi } from '../../api';

const courses = ref<any[]>([]);
const classes = ref<any[]>([]);
const selectedCourseId = ref<number | null>(null);
const loading = ref(false);

const loadCourses = async () => {
  try {
    courses.value = (await courseApi.getAll()).data || [];
  } catch { /* */ }
};

const loadClasses = async () => {
  if (!selectedCourseId.value) {
    classes.value = [];
    return;
  }
  loading.value = true;
  try {
    classes.value = (await classApi.getByCourse(selectedCourseId.value)).data || [];
  } catch {
    ElMessage.error('加载班级列表失败');
  } finally {
    loading.value = false;
  }
};

watch(selectedCourseId, loadClasses);
onMounted(loadCourses);

const handleDeleteClass = async (id: number) => {
  try {
    await classApi.delete(id);
    ElMessage.success('已删除');
    loadClasses();
  } catch {
    ElMessage.error('删除失败');
  }
};
</script>

<template>
  <div>
    <div class="page-header">
      <h2>班级管理</h2>
      <div style="display:flex;gap:8px;align-items:center">
        <el-select v-model="selectedCourseId" placeholder="请选择课程" clearable style="width:240px">
          <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <span style="color:#909399;font-size:14px">班级由教师创建，管理员可查看和删除</span>
      </div>
    </div>

    <el-table :data="classes" stripe v-loading="loading" style="margin-top:16px" v-if="selectedCourseId">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="班级名称" />
      <el-table-column prop="inviteCode" label="选课码" width="120">
        <template #default="{ row }">
          <code style="font-size:14px">{{ row.inviteCode }}</code>
        </template>
      </el-table-column>
      <el-table-column prop="studentCount" label="学生数" width="100" />
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">
          {{ row.createdAt ? new Date(row.createdAt).toLocaleString('zh-CN') : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-popconfirm title="确认删除该班级？" @confirm="handleDeleteClass(row.id)">
            <template #reference>
              <el-button size="small" type="danger" plain>删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!selectedCourseId" description="请先选择一门课程查看班级" />
    <el-empty v-else-if="!loading && classes.length === 0" description="该课程下暂无班级" />
  </div>
</template>

<style scoped>
.page-header {
  display: flex; justify-content: space-between; align-items: center;
}
</style>
