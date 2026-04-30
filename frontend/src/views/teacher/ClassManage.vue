<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, ElTable, ElTableColumn, ElButton, ElDialog, ElForm, ElFormItem, ElInput, ElTag, ElTooltip } from 'element-plus'
import { CopyDocument } from '@element-plus/icons-vue'
import { classApi, userApi } from '@/api'

const route = useRoute()
const courseId = Number(route.params.courseId)
const classes = ref<any[]>([])
const loading = ref(false)

const showCreateDialog = ref(false)
const createForm = ref({ name: '' })
const creating = ref(false)

const showAddStudentDialog = ref(false)
const currentClassId = ref(0)
const searchUsername = ref('')
const searchResults = ref<any[]>([])
const searching = ref(false)

const expandedClass = ref<number | null>(null)
const classStudents = ref<Record<number, any[]>>({})

const loadClasses = async () => {
  loading.value = true
  try {
    const res = await classApi.getByCourse(courseId)
    classes.value = (res as any).data || []
  } catch {
    ElMessage.error('加载班级列表失败')
  } finally {
    loading.value = false
  }
}

const handleCreate = async () => {
  if (!createForm.value.name.trim()) return
  creating.value = true
  try {
    await classApi.create({ courseId, name: createForm.value.name })
    ElMessage.success('班级创建成功')
    showCreateDialog.value = false
    createForm.value.name = ''
    loadClasses()
  } catch (e: any) {
    ElMessage.error(e?.message || e?.response?.data?.msg || '创建失败')
  } finally {
    creating.value = false
  }
}

const toggleStudents = async (classId: number) => {
  if (expandedClass.value === classId) {
    expandedClass.value = null
    return
  }
  expandedClass.value = classId
  if (!classStudents.value[classId]) {
    try {
      const res = await classApi.getClassStudents(classId)
      classStudents.value = { ...classStudents.value, [classId]: (res as any).data || [] }
    } catch {
      ElMessage.error('加载学生列表失败')
      classStudents.value = { ...classStudents.value, [classId]: [] }
    }
  }
}

const searchStudent = async () => {
  const kw = searchUsername.value.trim()
  if (!kw) { searchResults.value = []; return }
  searching.value = true
  try {
    // Search all students by username/nickname
    const res = await userApi.getUsersByRole('STUDENT')
    const allStudents = (res as any).data || []
    searchResults.value = allStudents.filter((u: any) =>
      u.username?.toLowerCase().includes(kw.toLowerCase()) ||
      u.nickname?.toLowerCase().includes(kw.toLowerCase())
    ).slice(0, 10)
  } catch {
    searchResults.value = []
  } finally {
    searching.value = false
  }
}

const handleAddStudent = async (student: any) => {
  try {
    await classApi.addStudent(currentClassId.value, student.id)
    ElMessage.success(`已添加学生 ${student.nickname || student.username}`)
    searchUsername.value = ''
    searchResults.value = []
    showAddStudentDialog.value = false
    // 重新加载该班级学生列表以刷新UI
    const res = await classApi.getClassStudents(currentClassId.value)
    classStudents.value = { ...classStudents.value, [currentClassId.value]: (res as any).data || [] }
  } catch (e: any) {
    ElMessage.error(e?.message || e?.response?.data?.msg || '添加失败')
  }
}

const handleRemoveStudent = async (classId: number, studentId: number, name: string) => {
  try {
    await ElMessageBox.confirm(`确认将学生 ${name} 移出班级？`, '确认', { type: 'warning' })
    await classApi.removeStudent(classId, studentId)
    ElMessage.success('已移除')
    const res = await classApi.getClassStudents(classId)
    classStudents.value = { ...classStudents.value, [classId]: (res as any).data || [] }
    expandedClass.value = null
  } catch { /* cancelled */ }
}

const copyInviteCode = (code: string) => {
  navigator.clipboard.writeText(code)
  ElMessage.success('选课码已复制')
}

const openAddStudent = (classId: number) => {
  currentClassId.value = classId
  searchUsername.value = ''
  searchResults.value = []
  showAddStudentDialog.value = true
}

onMounted(loadClasses)
</script>

<template>
  <div class="class-manage">
    <div class="page-header">
      <h2>班级管理</h2>
      <el-button type="primary" @click="showCreateDialog = true">创建班级</el-button>
    </div>

    <el-table :data="classes" stripe v-loading="loading" row-key="id">
      <el-table-column type="expand">
        <template #default="{ row }">
          <div style="padding: 12px 48px">
            <div class="page-header" style="margin-bottom: 12px">
              <span style="font-weight: 600">学生列表 ({{ (classStudents[row.id] || []).length }}人)</span>
              <el-button size="small" type="primary" @click="openAddStudent(row.id)">
                添加学生
              </el-button>
            </div>
            <el-table v-if="classStudents[row.id]?.length" :data="classStudents[row.id]" size="small" stripe>
              <el-table-column prop="code" label="学号/工号" width="120" />
              <el-table-column prop="username" label="用户名" />
              <el-table-column prop="nickname" label="姓名" />
              <el-table-column label="操作" width="100">
                <template #default="{ row: stu }">
                  <el-button size="small" type="danger" plain @click="handleRemoveStudent(row.id, stu.id, stu.nickname || stu.username)">
                    移除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-else description="暂无学生，点击「添加学生」加入" :image-size="60" />
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="班级名称" />
      <el-table-column prop="inviteCode" label="选课码" width="160">
        <template #default="{ row }">
          <span style="font-family: monospace; margin-right: 8px">{{ row.inviteCode }}</span>
          <el-tooltip content="复制选课码">
            <el-button size="small" :icon="CopyDocument" circle @click="copyInviteCode(row.inviteCode)" />
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column prop="studentCount" label="学生数" width="100" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button size="small" @click="toggleStudents(row.id)">
            {{ expandedClass === row.id ? '收起' : '查看学生' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showCreateDialog" title="创建班级" width="480px">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="班级名称">
          <el-input v-model="createForm.name" placeholder="例如：2024级软件工程1班" maxlength="50" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- Add Student Dialog - search by username -->
    <el-dialog v-model="showAddStudentDialog" title="添加学生" width="480px">
      <el-form label-width="80px">
        <el-form-item label="搜索学生">
          <el-input v-model="searchUsername" placeholder="输入用户名或姓名搜索" @keyup.enter="searchStudent" />
        </el-form-item>
      </el-form>
      <div style="text-align:center;margin-bottom:12px">
        <el-button type="primary" :loading="searching" @click="searchStudent">搜索</el-button>
      </div>
      <el-table v-if="searchResults.length" :data="searchResults" size="small" max-height="300">
        <el-table-column prop="code" label="学号/工号" width="120" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="nickname" label="姓名" />
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleAddStudent(row)">添加</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else-if="!searching && searchUsername && !searchResults.length" description="未找到学生" :image-size="60" />
      <template #footer>
        <el-button @click="showAddStudentDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; }
</style>
