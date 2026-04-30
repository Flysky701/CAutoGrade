<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue'
import { ElMessage, ElSelect, ElOption, ElCard, ElRow, ElCol, ElStatistic, ElTable, ElTableColumn, ElProgress } from 'element-plus'
import { analyticsApi, courseApi, classApi, assignmentApi } from '@/api'
import * as echarts from 'echarts'

const courses = ref<any[]>([])
const classes = ref<any[]>([])
const assignments = ref<any[]>([])
const selectedCourseId = ref<number | null>(null)
const selectedClassId = ref<number | null>(null)
const selectedAssignmentId = ref<number | null>(null)

const analyticsData = ref<any>(null)
const loading = ref(false)

const scoreChartRef = ref<HTMLDivElement>()
const knowledgeChartRef = ref<HTMLDivElement>()
let scoreChart: echarts.ECharts | null = null
let knowledgeChart: echarts.ECharts | null = null

const loadMeta = async () => {
  try {
    courses.value = (await courseApi.getMyCourses() as any).data || []
  } catch { /* */ }
}

watch(selectedCourseId, async (cid) => {
  classes.value = []
  assignments.value = []
  if (!cid) return
  try {
    const [cr, ar] = await Promise.all([classApi.getByCourse(cid), assignmentApi.getByCourse(cid)])
    classes.value = (cr as any).data || []
    assignments.value = (ar as any).data || []
  } catch { /* */ }
})

const loadAnalytics = async () => {
  if (!selectedClassId.value) return
  loading.value = true
  try {
    const res = await analyticsApi.getClassAnalytics(selectedClassId.value)
    analyticsData.value = (res as any).data
    await nextTick()
    renderCharts()
  } catch {
    analyticsData.value = null
  } finally {
    loading.value = false
  }
}

const renderCharts = () => {
  if (!analyticsData.value) return

  // Score distribution histogram
  if (scoreChartRef.value) {
    if (!scoreChart) scoreChart = echarts.init(scoreChartRef.value)
    const dist = analyticsData.value.scoreDistribution || {}
    scoreChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: Object.keys(dist), name: '分数段' },
      yAxis: { type: 'value', name: '人数' },
      series: [{
        type: 'bar', data: Object.values(dist),
        itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#409eff' }, { offset: 1, color: '#79bbff' }
        ])},
      }],
      grid: { left: 50, right: 20, top: 30, bottom: 40 },
    })
  }

  // Knowledge point heatmap / radar
  if (knowledgeChartRef.value) {
    if (!knowledgeChart) knowledgeChart = echarts.init(knowledgeChartRef.value)
    const kp = analyticsData.value.knowledgePoints || []
    const indicators = kp.map((k: any) => ({ name: k.name, max: 100 }))
    knowledgeChart.setOption({
      tooltip: {},
      radar: {
        indicator: indicators.length > 0 ? indicators : [{ name: '暂无数据', max: 100 }],
      },
      series: [{
        type: 'radar',
        data: [{ value: kp.map((k: any) => k.avgScore || 0), name: '知识点掌握度' }],
      }],
    })
  }
}

const errorTop10 = ref<any[]>([])
watch(analyticsData, () => {
  errorTop10.value = (analyticsData.value?.errorTop10 || []).slice(0, 10)
})

onMounted(loadMeta)
</script>

<template>
  <div class="analytics">
    <div class="page-header">
      <h2>学情分析</h2>
      <div style="display:flex;gap:12px">
        <el-select v-model="selectedCourseId" placeholder="选择课程" clearable style="width:200px">
          <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-select v-model="selectedClassId" placeholder="选择班级" clearable style="width:200px" @change="loadAnalytics">
          <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </div>
    </div>

    <template v-if="analyticsData">
      <!-- Summary cards -->
      <el-row :gutter="20" style="margin-bottom:20px">
        <el-col :span="6">
          <el-card shadow="hover">
            <el-statistic title="平均分" :value="analyticsData.averageScore || 0" suffix="分" />
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <el-statistic title="提交率" :value="analyticsData.submitRate || 0" suffix="%" :precision="1" />
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <el-statistic title="及格率" :value="analyticsData.passRate || 0" suffix="%" :precision="1" />
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <el-statistic title="优秀率" :value="analyticsData.excellentRate || 0" suffix="%" :precision="1" />
          </el-card>
        </el-col>
      </el-row>

      <!-- Charts row -->
      <el-row :gutter="20" style="margin-bottom:20px">
        <el-col :span="14">
          <el-card header="成绩分布">
            <div ref="scoreChartRef" style="height:300px" />
          </el-card>
        </el-col>
        <el-col :span="10">
          <el-card header="知识点掌握度">
            <div ref="knowledgeChartRef" style="height:300px" />
          </el-card>
        </el-col>
      </el-row>

      <!-- Error Top 10 -->
      <el-card header="易错知识点 Top 10">
        <el-table :data="errorTop10" stripe>
          <el-table-column type="index" label="#" width="50" />
          <el-table-column prop="knowledgePoint" label="知识点" />
          <el-table-column prop="errorCount" label="错误次数" width="100" />
          <el-table-column prop="errorRate" label="错误率" width="180">
            <template #default="{ row }">
              <el-progress :percentage="Math.round(row.errorRate * 100)" :color="row.errorRate > 0.5 ? '#f56c6c' : '#e6a23c'" />
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>

    <el-empty v-else description="请选择班级查看学情数据" :image-size="120" />
  </div>
</template>
