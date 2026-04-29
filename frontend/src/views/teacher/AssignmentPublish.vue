<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElAlert, ElTable, ElTableColumn, ElButton, ElDialog, ElForm, ElFormItem, ElInput, ElInputNumber, ElSelect, ElOption, ElDatePicker, ElTag, ElSteps, ElStep, ElPopconfirm } from 'element-plus'
import { assignmentApi, problemApi, courseApi, classApi } from '@/api'

const router = useRouter()
const assignments = ref<any[]>([])
const courses = ref<any[]>([])
const problems = ref<any[]>([])
const classes = ref<any[]>([])
const loading = ref(false)

const showDialog = ref(false)
const dialogStep = ref(0)
const editingId = ref<number | null>(null)
const form = ref({
  title: '',
  description: '',
  courseId: null as number | null,
  type: 'PRACTICE' as 'EXAM' | 'LAB' | 'PRACTICE',
  startTime: '',
  endTime: '',
  maxScore: 100,
  problemIds: [] as number[],
  classIds: [] as number[],
})
const saving = ref(false)

const loadAssignments = async () => {
  loading.value = true
  try {
    assignments.value = (await assignmentApi.getMyAssignments() as any).data || []
  } catch {
    ElMessage.error('加载作业列表失败')
  } finally {
    loading.value = false
  }
}

const loadMeta = async () => {
  try {
    const [cr, pr, clr] = await Promise.all([
      courseApi.getMyCourses(),
      problemApi.getMyProblems(),
      classApi.getMyClasses(),
    ])
    courses.value = (cr as any).data || []
    problems.value = (pr as any).data || []
    classes.value = (clr as any).data || []
  } catch { /* ignore */ }
}

const openCreate = () => {
  editingId.value = null
  dialogStep.value = 0
  form.value = {
    title: '', description: '', courseId: courses.value[0]?.id || null,
    type: 'PRACTICE', startTime: '', endTime: '', maxScore: 100,
    problemIds: [], classIds: [],
  }
  showDialog.value = true
}

const openEdit = (row: any) => {
  editingId.value = row.id
  dialogStep.value = 0
  form.value = {
    title: row.title, description: row.description,
    courseId: row.courseId, type: row.type,
    startTime: row.startTime, endTime: row.endTime,
    maxScore: row.maxScore,
    problemIds: row.problemIds || [],
    classIds: row.classIds || [],
  }
  showDialog.value = true
}

const canSave = computed(() => {
  const f = form.value
  return !!(f.title.trim() && f.courseId && f.startTime && f.endTime && f.problemIds.length > 0)
})

const handleSave = async () => {
  if (!canSave.value) {
    ElMessage.warning('请完善作业信息（标题、课程、时间、题目为必填）')
    return
  }
  saving.value = true
  try {
    const d = new Date(form.value.endTime).getTime()
    const nd = new Date()
    nd.setHours(0, 0, 0, 0)
    const draft = d > Date.now()
    if (editingId.value) {
      await assignmentApi.update(editingId.value, { ...form.value, status: draft ? 'DRAFT' : 'PUBLISHED' })
      ElMessage.success('更新成功')
    } else {
      await assignmentApi.create({ ...form.value, status: draft ? 'DRAFT' : 'PUBLISHED' })
      ElMessage.success('创建成功')
    }
    showDialog.value = false
    loadAssignments()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.msg || '保存失败')
  } finally {
    saving.value = false
  }
}

const handlePublish = async (id: number) => {
  try {
    await assignmentApi.publish(id)
    ElMessage.success('发布成功')
    loadAssignments()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.msg || '发布失败')
  }
}

const handleDelete = async (id: number) => {
  try {
    await assignmentApi.delete(id)
    ElMessage.success('已删除')
    loadAssignments()
  } catch {
    ElMessage.error('删除失败')
  }
}

const statusTag = (status: string) => {
  const m: Record<string, string> = { DRAFT: 'info', PUBLISHED: 'success', EXPIRED: 'warning', ARCHIVED: 'info' }
  return m[status] || 'info'
}

const statusLabel = (status: string) => {
  const m: Record<string, string> = { DRAFT: '草稿', PUBLISHED: '已发布', EXPIRED: '已截止', ARCHIVED: '已归档' }
  return m[status] || status
}

const typeLabel = (type: string) => {
  const m: Record<string, string> = { EXAM: '考试', LAB: '实验', PRACTICE: '练习' }
  return m[type] || type
}

const toggleProblem = (pid: number) => {
  const idx = form.value.problemIds.indexOf(pid)
  if (idx >= 0) form.value.problemIds.splice(idx, 1)
  else form.value.problemIds.push(pid)
}

const toggleClass = (cid: number) => {
  const idx = form.value.classIds.indexOf(cid)
  if (idx >= 0) form.value.classIds.splice(idx, 1)
  else form.value.classIds.push(cid)
}

onMounted(() => { loadAssignments(); loadMeta() })
</script>

<template>
  <div class="assignment-publish">
    <div class="page-header">
      <h2>作业管理</h2>
      <el-button type="primary" @click="openCreate">发布作业</el-button>
    </div>

    <el-table :data="assignments" stripe v-loading="loading">
      <el-table-column prop="title" label="标题" min-width="160" />
      <el-table-column label="类型" width="80">
        <template #default="{ row }">{{ typeLabel(row.type) }}</template>
      </el-table-column>
      <el-table-column prop="maxScore" label="满分" width="70" />
      <el-table-column prop="endTime" label="截止时间" width="180">
        <template #default="{ row }">
          {{ new Date(row.endTime).toLocaleString('zh-CN') }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="row.status === 'DRAFT'" size="small" type="success" @click="handlePublish(row.id)">发布</el-button>
          <el-popconfirm title="确认删除该作业？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button size="small" type="danger" plain>删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新建/编辑作业弹窗（向导式） -->
    <el-dialog v-model="showDialog" :title="editingId ? '编辑作业' : '新建作业'" width="720px" top="5vh">
      <el-steps :active="dialogStep" finish-status="success" align-center style="margin-bottom:24px">
        <el-step title="基本信息" />
        <el-step title="选择题目" />
        <el-step title="指定班级" />
      </el-steps>

      <!-- Step 1: 基本信息 -->
      <el-form v-show="dialogStep === 0" :model="form" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="作业标题" maxlength="100" />
        </el-form-item>
        <el-form-item label="课程" required>
          <el-select v-model="form.courseId" placeholder="选择课程" style="width:100%">
            <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width:100%">
            <el-option label="练习" value="PRACTICE" />
            <el-option label="实验" value="LAB" />
            <el-option label="考试" value="EXAM" />
          </el-select>
        </el-form-item>
        <el-form-item label="满分">
          <el-input-number v-model="form.maxScore" :min="1" :max="200" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="开始时间" required>
              <el-date-picker v-model="form.startTime" type="datetime" placeholder="开始时间" style="width:100%"
                value-format="YYYY-MM-DDTHH:mm:ss" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="截止时间" required>
              <el-date-picker v-model="form.endTime" type="datetime" placeholder="截止时间" style="width:100%"
                value-format="YYYY-MM-DDTHH:mm:ss" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="作业说明（支持 Markdown）" />
        </el-form-item>
      </el-form>

      <!-- Step 2: 选择题目 -->
      <div v-show="dialogStep === 1">
        <p style="margin-bottom:12px;color:#909399">已选 {{ form.problemIds.length }} 道题目</p>
        <el-table :data="problems" stripe max-height="360" @selection-change="(rows:any[]) => form.problemIds = rows.map((r:any) => r.id)"
          ref="problemTable">
          <el-table-column type="selection" :selectable="() => true" width="50" />
          <el-table-column prop="title" label="标题" />
          <el-table-column prop="difficulty" label="难度" width="140">
            <template #default="{ row }">
              <span style="color:#e6a23c">{{ '★'.repeat(row.difficulty) + '☆'.repeat(5 - row.difficulty) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- Step 3: 指定班级 -->
      <div v-show="dialogStep === 2">
        <el-alert v-if="classes.length === 0" type="warning" show-icon :closable="false" style="margin-bottom:12px">
          <template #title>
            暂无班级，请先在<a href="#" @click.prevent="router.push('/teacher/courses')" style="color:var(--primary)">课程管理</a>中为课程创建班级
          </template>
        </el-alert>
        <p v-else style="margin-bottom:12px;color:#909399">已选 {{ form.classIds.length }} 个班级</p>
        <el-table :data="classes" stripe max-height="360">
          <el-table-column type="selection" width="50" />
          <el-table-column prop="name" label="班级名称" />
          <el-table-column prop="courseName" label="所属课程" />
        </el-table>
      </div>

      <template #footer>
        <div style="display:flex;justify-content:space-between">
          <el-button @click="showDialog = false">取消</el-button>
          <div>
            <el-button v-if="dialogStep > 0" @click="dialogStep--">上一步</el-button>
            <el-button v-if="dialogStep < 2" type="primary" @click="dialogStep++">下一步</el-button>
            <el-button v-if="dialogStep === 2" type="primary" :loading="saving" @click="handleSave">保存</el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
