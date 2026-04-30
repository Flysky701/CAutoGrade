<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { ElMessage, ElMessageBox, ElTable, ElTableColumn, ElButton, ElTag, ElDialog, ElForm, ElFormItem, ElInput, ElSelect, ElOption, ElPopconfirm } from 'element-plus';
import { userApi, adminApi } from '../../api';

const users = ref<any[]>([]);
const loading = ref(false);

const searchKeyword = ref('');
const filterRole = ref('');

const filteredUsers = computed(() => {
  let list = users.value;
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase();
    list = list.filter((u: any) =>
      u.username?.toLowerCase().includes(kw) ||
      u.nickname?.toLowerCase().includes(kw) ||
      u.code?.toLowerCase().includes(kw)
    );
  }
  if (filterRole.value) {
    list = list.filter((u: any) => u.role === filterRole.value);
  }
  return list;
});

const loadUsers = async () => {
  loading.value = true;
  try {
    const res: any = await adminApi.getUsers();
    users.value = res.data || [];
  } catch (e) {
    console.error(e);
  } finally {
    loading.value = false;
  }
};

const handleDisable = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确认禁用用户「${row.username}」？`, '确认操作', { type: 'warning' });
    await userApi.disableUser(row.id);
    ElMessage.success('已禁用');
    loadUsers();
  } catch { /* cancelled */ }
};

const handleEnable = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确认启用用户「${row.username}」？`, '确认操作', { type: 'info' });
    await userApi.enableUser(row.id);
    ElMessage.success('已启用');
    loadUsers();
  } catch { /* cancelled */ }
};

// Add user dialog
const showAddDialog = ref(false);
const addForm = ref({ username: '', code: '', password: '', nickname: '', role: 'STUDENT' });
const adding = ref(false);

const handleAddUser = async () => {
  if (!addForm.value.username || !addForm.value.password || !addForm.value.nickname) {
    ElMessage.warning('请填写完整信息');
    return;
  }
  adding.value = true;
  try {
    await adminApi.createUser({
      username: addForm.value.username,
      code: addForm.value.code || null,
      passwordHash: addForm.value.password,
      nickname: addForm.value.nickname,
      role: addForm.value.role,
      status: 1,
    });
    ElMessage.success('用户创建成功');
    showAddDialog.value = false;
    addForm.value = { username: '', code: '', password: '', nickname: '', role: 'STUDENT' };
    loadUsers();
  } catch (e: any) {
    ElMessage.error(e?.message || e?.response?.data?.msg || '创建失败');
  } finally {
    adding.value = false;
  }
};

// Edit user dialog
const showEditDialog = ref(false);
const editForm = ref({ id: 0, username: '', code: '', nickname: '', role: 'STUDENT', status: 1 });
const editing = ref(false);

const openEdit = (row: any) => {
  editForm.value = {
    id: row.id,
    username: row.username,
    code: row.code || '',
    nickname: row.nickname || '',
    role: row.role,
    status: row.status ?? 1,
  };
  showEditDialog.value = true;
};

const handleEditUser = async () => {
  if (!editForm.value.username || !editForm.value.nickname) {
    ElMessage.warning('请填写完整信息');
    return;
  }
  editing.value = true;
  try {
    await adminApi.updateUser(editForm.value.id, {
      username: editForm.value.username,
      code: editForm.value.code || null,
      nickname: editForm.value.nickname,
      role: editForm.value.role,
      status: editForm.value.status,
    });
    ElMessage.success('用户信息更新成功');
    showEditDialog.value = false;
    loadUsers();
  } catch (e: any) {
    ElMessage.error(e?.message || e?.response?.data?.msg || '更新失败');
  } finally {
    editing.value = false;
  }
};

// Delete user
const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确认删除用户「${row.username}」？此操作不可恢复。`, '确认删除', { type: 'warning' });
    await adminApi.deleteUser(row.id);
    ElMessage.success('已删除');
    loadUsers();
  } catch { /* cancelled */ }
};

// Password reset dialog
const showPwdDialog = ref(false);
const pwdTarget = ref<any>(null);
const newPassword = ref('');
const resetting = ref(false);

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
    ElMessage.error(e?.message || e?.response?.data?.msg || '重置失败');
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
        <el-input v-model="searchKeyword" placeholder="搜索用户名/昵称/学号" clearable style="width:220px" />
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
      <el-table-column label="操作" width="340">
        <template #default="{ row }">
          <el-button size="small" type="primary" plain @click="openEdit(row)">编辑</el-button>
          <el-button v-if="row.status === 1" size="small" type="warning" plain @click="handleDisable(row)">禁用</el-button>
          <el-button v-else size="small" type="success" plain @click="handleEnable(row)">启用</el-button>
          <el-button size="small" type="info" plain @click="openResetPwd(row)">重置密码</el-button>
          <el-popconfirm title="确认删除该用户？" @confirm="handleDelete(row)">
            <template #reference>
              <el-button size="small" type="danger" plain>删除</el-button>
            </template>
          </el-popconfirm>
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

    <!-- Edit User Dialog -->
    <el-dialog v-model="showEditDialog" title="编辑用户" width="480px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="editForm.username" placeholder="登录用户名" />
        </el-form-item>
        <el-form-item label="学号/工号">
          <el-input v-model="editForm.code" placeholder="学号或教工号" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" placeholder="姓名" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="editForm.role" style="width:100%">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="学生" value="STUDENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="editForm.status" style="width:100%">
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="editing" @click="handleEditUser">保存</el-button>
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
