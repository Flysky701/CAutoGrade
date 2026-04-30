<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage, ElTable, ElTableColumn, ElTag } from 'element-plus';
import { adminApi } from '@/api';

const logs = ref<any[]>([]);
const loading = ref(false);

onMounted(async () => {
  loading.value = true;
  try {
    const res: any = await adminApi.getLogs(200);
    logs.value = res.data || [];
  } catch (e: any) {
    ElMessage.error(e?.message || e?.response?.data?.msg || '加载日志失败');
  } finally {
    loading.value = false;
  }
});

const formatTime = (v: string) => {
  if (!v) return '-';
  return new Date(v).toLocaleString('zh-CN');
};
</script>

<template>
  <div>
    <h2>操作日志</h2>
    <el-table :data="logs" stripe v-loading="loading" style="margin-top: 20px">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="action" label="操作" width="140">
        <template #default="{ row }">
          <el-tag size="small">{{ row.action }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="targetType" label="目标类型" width="120" />
      <el-table-column prop="detail" label="详情" min-width="200" show-overflow-tooltip />
      <el-table-column prop="ipAddress" label="IP地址" width="140" />
      <el-table-column prop="createdAt" label="时间" width="180">
        <template #default="{ row }">
          {{ formatTime(row.createdAt) }}
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && logs.length === 0" description="暂无操作日志" :image-size="80" />
  </div>
</template>
