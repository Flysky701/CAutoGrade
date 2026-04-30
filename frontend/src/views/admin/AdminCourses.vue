<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage, ElTable, ElTableColumn, ElTag, ElButton, ElPopconfirm, ElDialog, ElForm, ElFormItem, ElInput, ElAlert } from 'element-plus';
import { courseApi } from '../../api';

const courses = ref<any[]>([]);
const loading = ref(false);
const dialogVisible = ref(false);
const saving = ref(false);
const form = ref({ name: '', description: '', semester: '' });
const createdInviteCode = ref('');

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

const handleCreate = async () => {
  if (!form.value.name.trim()) { ElMessage.warning('请输入课程名称'); return }
  saving.value = true;
  try {
    const res: any = await courseApi.create(form.value);
    const course = res.data || res;
    createdInviteCode.value = course.inviteCode || '';
    ElMessage.success('创建成功');
    form.value = { name: '', description: '', semester: '' };
    loadCourses();
  } catch (e: any) {
    ElMessage.error(e?.message || e?.response?.data?.msg || '创建失败');
  } finally { saving.value = false }
};

const handleDelete = async (id: number) => {
  try {
    await courseApi.delete(id);
    ElMessage.success('已删除');
    loadCourses();
  } catch {
    ElMessage.error('删除失败');
  }
};

onMounted(loadCourses);
</script>

<template>
  <div>
    <div class="page-header">
      <h2>课程管理</h2>
      <div style="display:flex;gap:12px;align-items:center">
        <span style="color:#909399;font-size:14px">共 {{ courses.length }} 门课程</span>
        <el-button type="primary" @click="dialogVisible = true; createdInviteCode = ''">创建课程</el-button>
      </div>
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
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-popconfirm title="确认删除该课程？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button size="small" type="danger" plain>删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && courses.length === 0" description="暂无课程" />

    <el-dialog v-model="dialogVisible" title="创建课程">
      <el-alert v-if="createdInviteCode" type="success" :closable="false" show-icon style="margin-bottom:16px">
        <template #title>选课码：<strong>{{ createdInviteCode }}</strong>（请复制保存）</template>
      </el-alert>
      <el-form :model="form">
        <el-form-item label="课程名称">
          <el-input v-model="form.name" placeholder="例如：C语言程序设计" />
        </el-form-item>
        <el-form-item label="课程描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="课程简介" />
        </el-form-item>
        <el-form-item label="学期">
          <el-input v-model="form.semester" placeholder="如：2026-Spring" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-header {
  display: flex; justify-content: space-between; align-items: center;
}
</style>
