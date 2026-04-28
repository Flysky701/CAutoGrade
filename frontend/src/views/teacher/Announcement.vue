<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox, ElTable, ElTableColumn, ElButton, ElDialog, ElForm, ElFormItem, ElInput, ElTag, ElSwitch, ElPopconfirm } from 'element-plus'
import { announcementApi, courseApi } from '@/api'

const announcements = ref<any[]>([])
const courses = ref<any[]>([])
const loading = ref(false)

const showDialog = ref(false)
const editingId = ref<number | null>(null)
const form = ref({ title: '', content: '', courseId: null as number | null, pinned: false })
const saving = ref(false)

const loadAnnouncements = async () => {
  loading.value = true
  try {
    const results = await Promise.all(
      courses.value.map((c: any) => announcementApi.getByCourse(c.id).then((r: any) => (r.data || []).map((a: any) => ({ ...a, courseName: c.name }))))
    )
    announcements.value = results.flat().sort((a: any, b: any) => {
      if (a.pinned !== b.pinned) return b.pinned ? 1 : -1
      return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    })
  } catch {
    ElMessage.error('加载公告失败')
  } finally {
    loading.value = false
  }
}

const loadCourses = async () => {
  try {
    courses.value = (await courseApi.getMyCourses() as any).data || []
  } catch { /* silently fail */ }
}

const openCreate = () => {
  editingId.value = null
  form.value = { title: '', content: '', courseId: courses.value[0]?.id || null, pinned: false }
  showDialog.value = true
}

const openEdit = (row: any) => {
  editingId.value = row.id
  form.value = { title: row.title, content: row.content, courseId: row.courseId, pinned: row.pinned }
  showDialog.value = true
}

const handleSave = async () => {
  if (!form.value.title.trim() || !form.value.content.trim() || !form.value.courseId) {
    ElMessage.warning('请填写完整信息')
    return
  }
  saving.value = true
  try {
    const data = { ...form.value }
    if (editingId.value) {
      await announcementApi.update(editingId.value, data)
      ElMessage.success('更新成功')
    } else {
      await announcementApi.create(data)
      ElMessage.success('发布成功')
    }
    showDialog.value = false
    loadAnnouncements()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.msg || '保存失败')
  } finally {
    saving.value = false
  }
}

const handleDelete = async (id: number) => {
  try {
    await announcementApi.delete(id)
    ElMessage.success('已删除')
    loadAnnouncements()
  } catch {
    ElMessage.error('删除失败')
  }
}

const handlePin = async (row: any) => {
  try {
    await announcementApi.update(row.id, { pinned: !row.pinned })
    row.pinned = !row.pinned
    ElMessage.success(row.pinned ? '已置顶' : '已取消置顶')
    loadAnnouncements()
  } catch {
    ElMessage.error('操作失败')
  }
}

onMounted(async () => {
  await loadCourses()
  loadAnnouncements()
})
</script>

<template>
  <div class="announcement-manage">
    <div class="page-header">
      <h2>公告管理</h2>
      <el-button type="primary" @click="openCreate">发布公告</el-button>
    </div>

    <el-table :data="announcements" stripe v-loading="loading">
      <el-table-column prop="title" label="标题" min-width="200">
        <template #default="{ row }">
          <div style="display:flex;align-items:center;gap:6px">
            <el-tag v-if="row.pinned" type="danger" size="small">置顶</el-tag>
            {{ row.title }}
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="courseName" label="所属课程" width="160" />
      <el-table-column prop="createdAt" label="发布时间" width="180">
        <template #default="{ row }">
          {{ new Date(row.createdAt).toLocaleString('zh-CN') }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" @click="handlePin(row)">
            {{ row.pinned ? '取消置顶' : '置顶' }}
          </el-button>
          <el-popconfirm title="确认删除该公告？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button size="small" type="danger" plain>删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showDialog" :title="editingId ? '编辑公告' : '发布公告'" width="640px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="所属课程">
          <el-select v-model="form.courseId" placeholder="选择课程" style="width:100%">
            <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="公告标题" maxlength="100" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="支持 Markdown 格式" />
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="form.pinned" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
