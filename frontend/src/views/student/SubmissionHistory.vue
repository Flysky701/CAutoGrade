<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElTable, ElTableColumn, ElTag, ElButton } from 'element-plus';
import { submissionApi, assignmentApi } from '../../api';

const router = useRouter();
const submissions = ref<any[]>([]);
const assignmentNames = ref<Record<number, string>>({});
const loading = ref(false);

onMounted(async () => {
  loading.value = true;
  try {
    const res = await submissionApi.getMySubmissions();
    submissions.value = (res?.data || []).reverse();

    // Batch pre-fetch assignment titles
    const ids = [...new Set(submissions.value.map((s: any) => s.assignmentId))];
    await Promise.all(ids.map(async (aid) => {
      try {
        const aRes = await assignmentApi.getById(aid);
        assignmentNames.value[aid] = aRes?.data?.title || `作业 #${aid}`;
      } catch {
        assignmentNames.value[aid] = `作业 #${aid}`;
      }
    }));
  } catch (e) {
    console.error(e);
  } finally {
    loading.value = false;
  }
});

function fmtTime(t: string) {
  if (!t) return '';
  return new Date(t).toLocaleString('zh-CN');
}
</script>

<template>
  <div>
    <h2>提交历史</h2>
    <el-table :data="submissions" stripe v-loading="loading"
              highlight-current-row
              style="cursor: pointer">
      <el-table-column prop="id" label="#" width="60" />
      <el-table-column label="所属作业" min-width="180">
        <template #default="{ row }">
          {{ assignmentNames[row.assignmentId] || `作业 #${row.assignmentId}` }}
        </template>
      </el-table-column>
      <el-table-column prop="problemId" label="题目ID" width="80" />
      <el-table-column label="提交时间" min-width="160">
        <template #default="{ row }">{{ fmtTime(row.submittedAt) }}</template>
      </el-table-column>
      <el-table-column label="迟交" width="70">
        <template #default="{ row }">
          <el-tag :type="row.isLate ? 'warning' : 'success'" size="small">
            {{ row.isLate ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button size="small" type="primary"
                     @click.stop="router.push(`/student/grading/${row.id}`)">
            查看
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>
