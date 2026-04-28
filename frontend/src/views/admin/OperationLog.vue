<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElTable, ElTableColumn } from 'element-plus';
import api from '../../api/request';

const logs = ref<any[]>([]);

onMounted(async () => {
  try {
    const res = await api.get('/admin/logs');
    logs.value = res.data || [];
  } catch (e) {
    console.error(e);
  }
});
</script>

<template>
  <div>
    <h2>操作日志</h2>
    <el-table :data="logs" stripe style="margin-top: 20px">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="action" label="操作" />
      <el-table-column prop="targetType" label="目标类型" />
      <el-table-column prop="ipAddress" label="IP地址" />
      <el-table-column prop="createdAt" label="时间" />
    </el-table>
  </div>
</template>
