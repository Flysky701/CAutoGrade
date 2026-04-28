<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox, ElTable, ElTableColumn, ElButton, ElDialog, ElForm, ElFormItem, ElInput, ElInputNumber, ElSwitch, ElTag, ElTooltip, ElPopconfirm } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import { problemApi, testCaseApi } from '@/api'
import { difficultyStars } from '@/utils'

const problems = ref<any[]>([])
const loading = ref(false)

const showProblemDialog = ref(false)
const editingProblemId = ref<number | null>(null)
const problemForm = ref({ title: '', description: '', difficulty: 3, knowledgeTags: '', isPublic: false })
const savingProblem = ref(false)

const testCaseDialogVisible = ref(false)
const currentProblemId = ref(0)
const currentProblemTitle = ref('')
const testCases = ref<any[]>([])
const loadingTestCases = ref(false)
const showTestCaseForm = ref(false)
const editingTestCaseId = ref<number | null>(null)
const testCaseForm = ref({ inputData: '', expectedOutput: '', isHidden: false, weight: 1 })
const savingTestCase = ref(false)

const loadProblems = async () => {
  loading.value = true
  try {
    problems.value = (await problemApi.getMyProblems() as any).data || []
  } catch {
    ElMessage.error('加载题目列表失败')
  } finally {
    loading.value = false
  }
}

const openCreateProblem = () => {
  editingProblemId.value = null
  problemForm.value = { title: '', description: '', difficulty: 3, knowledgeTags: '', isPublic: false }
  showProblemDialog.value = true
}

const openEditProblem = (row: any) => {
  editingProblemId.value = row.id
  problemForm.value = {
    title: row.title,
    description: row.description,
    difficulty: row.difficulty,
    knowledgeTags: (row.knowledgeTags || []).join(', '),
    isPublic: row.isPublic,
  }
  showProblemDialog.value = true
}

const handleSaveProblem = async () => {
  if (!problemForm.value.title.trim() || !problemForm.value.description.trim()) {
    ElMessage.warning('请填写标题和描述')
    return
  }
  savingProblem.value = true
  try {
    const data = {
      ...problemForm.value,
      knowledgeTags: problemForm.value.knowledgeTags.split(/[,，]/).map((t: string) => t.trim()).filter(Boolean),
    }
    if (editingProblemId.value) {
      await problemApi.update(editingProblemId.value, data)
      ElMessage.success('更新成功')
    } else {
      await problemApi.create(data)
      ElMessage.success('创建成功')
    }
    showProblemDialog.value = false
    loadProblems()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.msg || '保存失败')
  } finally {
    savingProblem.value = false
  }
}

const handleDeleteProblem = async (id: number) => {
  try {
    await problemApi.delete(id)
    ElMessage.success('已删除')
    loadProblems()
  } catch {
    ElMessage.error('删除失败')
  }
}

const openTestCaseDialog = async (problemId: number, title: string) => {
  currentProblemId.value = problemId
  currentProblemTitle.value = title
  testCaseDialogVisible.value = true
  showTestCaseForm.value = false
  loadingTestCases.value = true
  try {
    testCases.value = (await testCaseApi.getByProblem(problemId) as any).data || []
  } catch {
    testCases.value = []
  } finally {
    loadingTestCases.value = false
  }
}

const openAddTestCase = () => {
  editingTestCaseId.value = null
  testCaseForm.value = { inputData: '', expectedOutput: '', isHidden: false, weight: 1 }
  showTestCaseForm.value = true
}

const openEditTestCase = (row: any) => {
  editingTestCaseId.value = row.id
  testCaseForm.value = {
    inputData: row.inputData,
    expectedOutput: row.expectedOutput,
    isHidden: row.isHidden,
    weight: row.weight,
  }
  showTestCaseForm.value = true
}

const handleSaveTestCase = async () => {
  savingTestCase.value = true
  try {
    const data = { ...testCaseForm.value, problemId: currentProblemId.value }
    if (editingTestCaseId.value) {
      await testCaseApi.update(editingTestCaseId.value, data)
      ElMessage.success('更新成功')
    } else {
      await testCaseApi.create(data)
      ElMessage.success('创建成功')
    }
    showTestCaseForm.value = false
    openTestCaseDialog(currentProblemId.value, currentProblemTitle.value)
  } catch (e: any) {
    ElMessage.error(e.response?.data?.msg || '保存失败')
  } finally {
    savingTestCase.value = false
  }
}

const handleDeleteTestCase = async (id: number) => {
  try {
    await testCaseApi.delete(id)
    ElMessage.success('已删除')
    openTestCaseDialog(currentProblemId.value, currentProblemTitle.value)
  } catch {
    ElMessage.error('删除失败')
  }
}

onMounted(loadProblems)
</script>

<template>
  <div class="problem-bank">
    <div class="page-header">
      <h2>题库管理</h2>
      <el-button type="primary" @click="openCreateProblem">创建题目</el-button>
    </div>

    <el-table :data="problems" stripe v-loading="loading">
      <el-table-column prop="title" label="标题" min-width="200" />
      <el-table-column prop="difficulty" label="难度" width="140">
        <template #default="{ row }">
          <el-tooltip :content="`难度 ${row.difficulty}/5`">
            <span style="color:#e6a23c;letter-spacing:2px">{{ difficultyStars(row.difficulty) }}</span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column prop="knowledgeTags" label="知识点" min-width="180">
        <template #default="{ row }">
          <el-tag v-for="tag in row.knowledgeTags" :key="tag" size="small" style="margin:2px">{{ tag }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="isPublic" label="公开" width="80">
        <template #default="{ row }">
          <el-tag :type="row.isPublic ? 'success' : 'info'" size="small">
            {{ row.isPublic ? '公开' : '私有' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280">
        <template #default="{ row }">
          <el-button size="small" @click="openEditProblem(row)">编辑</el-button>
          <el-button size="small" type="warning" plain @click="openTestCaseDialog(row.id, row.title)">测试用例</el-button>
          <el-popconfirm title="确认删除该题目？" @confirm="handleDeleteProblem(row.id)">
            <template #reference>
              <el-button size="small" type="danger" plain>删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 题目编辑弹窗 -->
    <el-dialog v-model="showProblemDialog" :title="editingProblemId ? '编辑题目' : '创建题目'" width="640px">
      <el-form :model="problemForm" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="problemForm.title" placeholder="题目标题" maxlength="100" />
        </el-form-item>
        <el-form-item label="题目描述">
          <el-input v-model="problemForm.description" type="textarea" :rows="5" placeholder="题目描述（支持 Markdown）" />
        </el-form-item>
        <el-form-item label="难度">
          <el-input-number v-model="problemForm.difficulty" :min="1" :max="5" />
          <span style="margin-left:8px;color:#e6a23c">{{ difficultyStars(problemForm.difficulty) }}</span>
        </el-form-item>
        <el-form-item label="知识点">
          <el-input v-model="problemForm.knowledgeTags" placeholder="多个标签用逗号分隔，如：循环,数组,指针" />
        </el-form-item>
        <el-form-item label="公开">
          <el-switch v-model="problemForm.isPublic" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showProblemDialog = false">取消</el-button>
        <el-button type="primary" :loading="savingProblem" @click="handleSaveProblem">保存</el-button>
      </template>
    </el-dialog>

    <!-- 测试用例弹窗 -->
    <el-dialog v-model="testCaseDialogVisible" :title="`测试用例 — ${currentProblemTitle}`" width="720px" @closed="showTestCaseForm = false">
      <template v-if="!showTestCaseForm">
        <div class="page-header" style="margin-bottom:12px">
          <span style="font-size:14px;color:#909399">共 {{ testCases.length }} 个测试用例</span>
          <el-button size="small" type="primary" :icon="Plus" @click="openAddTestCase">新增用例</el-button>
        </div>
        <el-table :data="testCases" size="small" stripe v-loading="loadingTestCases">
          <el-table-column prop="inputData" label="输入数据" min-width="150">
            <template #default="{ row }">
              <code style="white-space:pre-wrap;font-size:12px">{{ row.inputData }}</code>
            </template>
          </el-table-column>
          <el-table-column prop="expectedOutput" label="期望输出" min-width="150">
            <template #default="{ row }">
              <code style="white-space:pre-wrap;font-size:12px">{{ row.expectedOutput }}</code>
            </template>
          </el-table-column>
          <el-table-column prop="isHidden" label="隐藏" width="70">
            <template #default="{ row }">
              <el-tag :type="row.isHidden ? 'warning' : 'success'" size="small">
                {{ row.isHidden ? '是' : '否' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="weight" label="权重" width="70" />
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <el-button size="small" @click="openEditTestCase(row)">编辑</el-button>
              <el-popconfirm title="确认删除？" @confirm="handleDeleteTestCase(row.id)">
                <template #reference>
                  <el-button size="small" type="danger" :icon="Delete" circle />
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!loadingTestCases && testCases.length === 0" description="暂无测试用例，请添加" :image-size="60" />
      </template>

      <template v-else>
        <el-form :model="testCaseForm" label-width="80px">
          <el-form-item label="输入数据">
            <el-input v-model="testCaseForm.inputData" type="textarea" :rows="3" placeholder="标准输入数据" />
          </el-form-item>
          <el-form-item label="期望输出">
            <el-input v-model="testCaseForm.expectedOutput" type="textarea" :rows="3" placeholder="期望的标准输出" />
          </el-form-item>
          <el-form-item label="隐藏用例">
            <el-switch v-model="testCaseForm.isHidden" />
            <span style="margin-left:8px;color:#909399;font-size:12px">隐藏后学生不可见</span>
          </el-form-item>
          <el-form-item label="权重">
            <el-input-number v-model="testCaseForm.weight" :min="0" :max="100" :step="1" />
            <span style="margin-left:8px;color:#909399;font-size:12px">占总分的比重</span>
          </el-form-item>
          <el-form-item>
            <el-button @click="showTestCaseForm = false">返回列表</el-button>
            <el-button type="primary" :loading="savingTestCase" @click="handleSaveTestCase">
              {{ editingTestCaseId ? '更新' : '添加' }}
            </el-button>
          </el-form-item>
        </el-form>
      </template>
    </el-dialog>
  </div>
</template>
