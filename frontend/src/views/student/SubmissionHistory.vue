<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElTable, ElTableColumn } from 'element-plus';
import { submissionApi } from '../../api';

const router = useRouter();
const submissions = ref<any[]>([]);

onMounted(async () => {
  try {
    submissions.value = (await submissionApi.getMySubmissions()).data || [];
  } catch (e) {
    console.error(e);
  }
});
</script>

<template>
  <div>
    <h2>提交历史</h2>
    <el-table :data="submissions" stripe @row-click="(row) => router.push(`/student/grading/${row.id}`)">
      <el-table-column prop="id" label="提交ID" width="100" />
      <el-table-column prop="assignmentId" label="作业ID" width="100" />
      <el-table-column prop="problemId" label="题目ID" width="100" />
      <el-table-column prop="submittedAt" label="提交时间" />
      <el-table-column prop="isLate" label="是否迟交" width="100">
        <template #default="{ row }">
          <el-tag :type="row.isLate ? 'warning' : 'success'">
            {{ row.isLate ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>
