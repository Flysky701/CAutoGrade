<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { ElMessage, ElMessageBox, ElTable, ElTableColumn, ElButton, ElTag, ElDialog, ElForm, ElFormItem, ElInput, ElSelect, ElOption, ElInputNumber, ElPopconfirm } from 'element-plus';
import { userApi, authApi } from '../../api';

const users = ref<any[]>([]);
const loading = ref(false);

// Search & filter
const searchKeyword = ref('');
const filterRole = ref('');

const filteredUsers = computed(() => {
  let list = users.value;
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase();
    list = list.filter((u: any) =>
      u.username?.toLowerCase().includes(kw) ||
      u.nickname?.toLowerCase().includes(kw)
    );
  }
  if (filterRole.value) {
    list = list.filter((u: any) => u.role === filterRole.value);
  }
  return list;
});

// Add user dialog
const showAddDialog = ref(false);
const addForm = ref({ username: '', code: '', password: '', nickname: '', role: 'STUDENT' });
const adding = ref(false);

// Password reset dialog
const showPwdDialog = ref(false);
const pwdTarget = ref<any>(null);
const newPassword = ref('');
const resetting = ref(false);

const loadUsers = async () => {
  loading.value = true;
  try {
    users.value = (await userApi.getAllUsers()).data || [];
  } catch (e) {
    console.error(e);
  } finally {
    loading.value = false;
  }
};

// Disable user
const handleDisable = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确认禁用用户「${row.username}」？`, '确认操作', { type: 'warning' });
    await userApi.disableUser(row.id);
    ElMessage.success('已禁用');
    loadUsers();
  } catch { /* cancelled */ }
};

// Enable user
const handleEnable = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确认启用用户「${row.username}」？`, '确认操作', { type: 'info' });
    await userApi.enableUser(row.id);
    ElMessage.success('已启用');
    loadUsers();
  } catch { /* cancelled */ }
};

// Add user
const handleAddUser = async () => {
  if (!addForm.value.username || !addForm.value.password || !addForm.value.nickname) {
    ElMessage.warning('请填写完整信息');
    return;
  }
  adding.value = true;
  try {
    await authApi.register(addForm.value);
    ElMessage.success('用户创建成功');
    showAddDialog.value = false;
    addForm.value = { username: '', code: '', password: '', nickname: '', role: 'STUDENT' };
    loadUsers();
  } catch (e: any) {
    ElMessage.error(e.response?.data?.msg || '创建失败');
  } finally {
    adding.value = false;
  }
};

// Reset password
const openResetPwd = (row: any) => {
  pwdTarget.value = row;
  newPassword.value = '';
  showPwdDialog.value = true;
};

const handleResetPwd = async () => {
  if (!newPassword.value || newPassword.value.length < 6) {
    ElMessage.warning('密码至少6位');
    return;
  }
  resetting.value = true;
  try {
    await userApi.resetPassword(pwdTarget.value.id, newPassword.value);
    ElMessage.success('密码已重置');
    showPwdDialog.value = false;
  } catch (e: any) {
    ElMessage.error(e.response?.data?.msg || '重置失败');
  } finally {
    resetting.value = false;
  }
};

const roleTagType = (role: string) => {
  const m: Record<string, string> = { ADMIN: 'danger', TEACHER: 'success', STUDENT: 'primary' };
  return m[role] || 'info';
};

onMounted(loadUsers);
</script>

<template>
  <div>
    <div class="page-header">
      <h2>用户管理</h2>
      <div style="display:flex;gap:12px;align-items:center">
        <el-input v-model="searchKeyword" placeholder="搜索用户名/昵称" clearable style="width:220px" />
        <el-select v-model="filterRole" placeholder="按角色筛选" clearable style="width:140px">
          <el-option label="管理员" value="ADMIN" />
          <el-option label="教师" value="TEACHER" />
          <el-option label="学生" value="STUDENT" />
        </el-select>
        <el-button type="primary" @click="showAddDialog = true">添加用户</el-button>
      </div>
    </div>

    <el-table :data="filteredUsers" stripe v-loading="loading" style="margin-top: 16px">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="code" label="学号/工号" width="120" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="role" label="角色" width="100">
        <template #default="{ row }">
          <el-tag :type="roleTagType(row.role)" size="small">{{ row.role }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280">
        <template #default="{ row }">
          <el-button v-if="row.status === 1" size="small" type="warning" plain @click="handleDisable(row)">禁用</el-button>
          <el-button v-else size="small" type="success" plain @click="handleEnable(row)">启用</el-button>
          <el-button size="small" type="primary" plain @click="openResetPwd(row)">重置密码</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Add User Dialog -->
    <el-dialog v-model="showAddDialog" title="添加用户" width="480px">
      <el-form :model="addForm" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="addForm.username" placeholder="登录用户名" />
        </el-form-item>
        <el-form-item label="学号/工号">
          <el-input v-model="addForm.code" placeholder="学号或教工号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="addForm.password" type="password" placeholder="至少6位" show-password />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="addForm.nickname" placeholder="姓名" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="addForm.role" style="width:100%">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="学生" value="STUDENT" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" :loading="adding" @click="handleAddUser">创建</el-button>
      </template>
    </el-dialog>

    <!-- Reset Password Dialog -->
    <el-dialog v-model="showPwdDialog" title="重置密码" width="400px">
      <p style="margin-bottom:16px;color:#606266">用户：<b>{{ pwdTarget?.username }}</b> ({{ pwdTarget?.nickname }})</p>
      <el-form label-width="80px">
        <el-form-item label="新密码">
          <el-input v-model="newPassword" type="password" placeholder="至少6位" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPwdDialog = false">取消</el-button>
        <el-button type="primary" :loading="resetting" @click="handleResetPwd">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
