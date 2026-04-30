<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElTable, ElTableColumn, ElButton, ElDialog, ElForm, ElFormItem, ElInput, ElTag, ElAlert, ElPopconfirm } from 'element-plus'
import { courseApi } from '@/api'

const router = useRouter()
const courses = ref<any[]>([])
const dialogVisible = ref(false)
const saving = ref(false)
const form = ref({ name: '', description: '', semester: '' })
const createdInviteCode = ref('')

const loadCourses = async () => {
  try {
    courses.value = (await courseApi.getMyCourses()).data || []
  } catch (e) { console.error(e) }
}

const handleCreate = async () => {
  if (!form.value.name.trim()) { ElMessage.warning('请输入课程名称'); return }
  saving.value = true
  try {
    const res: any = await courseApi.create(form.value)
    const course = res.data || res
    createdInviteCode.value = course.inviteCode || ''
    ElMessage.success('创建成功')
    form.value = { name: '', description: '', semester: '' }
    loadCourses()
  } catch (e: any) {
    ElMessage.error(e?.message || e?.response?.data?.msg || '创建失败')
  } finally { saving.value = false }
}

const copyCode = (code: string) => {
  navigator.clipboard.writeText(code)
  ElMessage.success('选课码已复制')
}

const handleDelete = async (id: number) => {
  try {
    await courseApi.delete(id)
    ElMessage.success('已删除')
    loadCourses()
  } catch {
    ElMessage.error('删除失败')
  }
}

onMounted(loadCourses)
</script>

<template>
  <div class="course-manage">
    <div class="page-header">
      <h2>课程管理</h2>
      <el-button type="primary" @click="dialogVisible = true; createdInviteCode = ''">创建课程</el-button>
    </div>

    <el-table :data="courses" stripe>
      <el-table-column prop="name" label="课程名称" min-width="160" />
      <el-table-column prop="semester" label="学期" width="120" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
            {{ row.status === 'ACTIVE' ? '活跃' : '已归档' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" type="primary" plain @click="router.push(`/teacher/classes/${row.id}`)">
            管理班级
          </el-button>
          <el-popconfirm title="确认删除该课程？删除后不可恢复" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button size="small" type="danger" plain>删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

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
.invite-code { font-family: var(--font-mono); font-size: var(--font-size-sm); margin-right: var(--space-2); }
</style>
