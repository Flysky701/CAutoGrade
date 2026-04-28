<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElCard } from 'element-plus';
import { notificationApi, submissionApi } from '../../api';

const notifications = ref<any[]>([]);
const recentSubmissions = ref<any[]>([]);

onMounted(async () => {
  try {
    notifications.value = (await notificationApi.getUnread()).data?.slice(0, 5) || [];
    recentSubmissions.value = (await submissionApi.getMySubmissions()).data?.slice(0, 5) || [];
  } catch (e) {
    console.error(e);
  }
});
</script>

<template>
  <div>
    <h2>学生首页</h2>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card title="待办事项">
          <template #header>待处理通知</template>
          <el-empty v-if="notifications.length === 0" description="暂无未读通知" />
          <el-list v-else>
            <el-list-item v-for="n in notifications" :key="n.id">
              {{ n.title }}
            </el-list-item>
          </el-list>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>最近提交</template>
          <el-empty v-if="recentSubmissions.length === 0" description="暂无提交记录" />
          <el-list v-else>
            <el-list-item v-for="s in recentSubmissions" :key="s.id">
              作业{{ s.assignmentId }} - 题{{ s.problemId }}
            </el-list-item>
          </el-list>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
