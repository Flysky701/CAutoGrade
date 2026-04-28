<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage, ElTable, ElTableColumn, ElButton, ElDialog, ElForm, ElFormItem, ElInput } from 'element-plus';
import { courseApi } from '../../api';

const courses = ref<any[]>([]);
const dialogVisible = ref(false);
const form = ref({
  name: '',
  description: '',
  semester: '',
});

const loadCourses = async () => {
  try {
    courses.value = (await courseApi.getMyCourses()).data || [];
  } catch (e) {
    console.error(e);
  }
};

const handleCreate = async () => {
  try {
    await courseApi.create(form.value);
    ElMessage.success('创建成功');
    dialogVisible.value = false;
    loadCourses();
  } catch (e: any) {
    ElMessage.error(e.response?.data?.msg || '创建失败');
  }
};

onMounted(loadCourses);
</script>

<template>
  <div>
    <h2>课程管理</h2>
    <el-button type="primary" @click="dialogVisible = true">创建课程</el-button>
    <el-table :data="courses" stripe style="margin-top: 20px">
      <el-table-column prop="name" label="课程名称" />
      <el-table-column prop="semester" label="学期" />
      <el-table-column prop="status" label="状态" />
    </el-table>

    <el-dialog v-model="dialogVisible" title="创建课程">
      <el-form :model="form">
        <el-form-item label="课程名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="课程描述">
          <el-input v-model="form.description" type="textarea" />
        </el-form-item>
        <el-form-item label="学期">
          <el-input v-model="form.semester" placeholder="如: 2026-Spring" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>
