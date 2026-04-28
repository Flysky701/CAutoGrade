<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElCard } from 'element-plus';
import { courseApi, classApi } from '../../api';

const router = useRouter();
const courses = ref<any[]>([]);
const joinCode = ref('');
const loading = ref(false);

const loadCourses = async () => {
  try {
    courses.value = (await classApi.getMyClassesAsStudent()).data || [];
  } catch (e) {
    console.error(e);
  }
};

const handleJoin = async () => {
  if (!joinCode.value) {
    ElMessage.warning('请输入选课码');
    return;
  }
  loading.value = true;
  try {
    await classApi.join(joinCode.value);
    ElMessage.success('加入成功');
    joinCode.value = '';
    loadCourses();
  } catch (e: any) {
    ElMessage.error(e.response?.data?.msg || '加入失败');
  } finally {
    loading.value = false;
  }
};

onMounted(loadCourses);
</script>

<template>
  <div>
    <h2>我的课程</h2>
    <el-card class="join-card">
      <el-input v-model="joinCode" placeholder="请输入选课码" style="width: 300px; margin-right: 10px" />
      <el-button type="primary" :loading="loading" @click="handleJoin">加入课程</el-button>
    </el-card>
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col v-for="c in courses" :key="c.id" :span="8">
        <el-card>
          <template #header>{{ c.name }}</template>
          <p>课程: {{ c.courseName }}</p>
          <p>班级: {{ c.name }}</p>
          <p>学生数: {{ c.studentCount }}</p>
          <el-button type="primary" size="small" @click="router.push(`/student/assignments/${c.courseId}`)">
            查看作业
          </el-button>
        </el-card>
      </el-col>
    </el-row>
    <el-empty v-if="courses.length === 0" description="暂无课程" />
  </div>
</template>
