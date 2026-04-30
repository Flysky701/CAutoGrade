<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '@/api/request'

const savingLLM = ref(false)
const savingSandbox = ref(false)
const savingScoring = ref(false)

const llmConfig = reactive({
  provider: 'deepseek',
  apiKey: '',
  model: 'deepseek-chat',
  temperature: 0.3,
  maxTokens: 2048,
  timeout: 30,
})

const sandboxConfig = reactive({
  compileTimeout: 5,
  runTimeout: 5,
  memoryLimitMB: 256,
  maxConcurrent: 4,
  enableNetwork: false,
})

const scoringConfig = reactive({
  correctnessWeight: 60,
  styleWeight: 20,
  efficiencyWeight: 20,
  deviationThreshold: 15,
  fallbackToRules: true,
})

const loadConfig = async () => {
  try {
    const res = await api.get('/admin/config')
    const data = (res as any).data
    if (data) {
      if (data.llm) Object.assign(llmConfig, data.llm)
      if (data.sandbox) Object.assign(sandboxConfig, data.sandbox)
      if (data.scoring) Object.assign(scoringConfig, data.scoring)
    }
  } catch { /* use defaults if config API not yet available */ }
}

const handleSaveLLM = async () => {
  savingLLM.value = true
  try {
    if (scoringConfig.correctnessWeight + scoringConfig.styleWeight + scoringConfig.efficiencyWeight !== 100) {
      ElMessage.warning('评分配置三项权重之和应为 100%')
      return
    }
    await api.put('/admin/config/llm', { ...llmConfig })
    ElMessage.success('LLM 配置保存成功')
  } catch (e: any) {
    ElMessage.error(e?.message || e?.response?.data?.msg || '保存失败')
  } finally { savingLLM.value = false }
}

const handleSaveSandbox = async () => {
  savingSandbox.value = true
  try {
    await api.put('/admin/config/sandbox', { ...sandboxConfig })
    ElMessage.success('沙箱配置保存成功')
  } catch (e: any) {
    ElMessage.error(e?.message || e?.response?.data?.msg || '保存失败')
  } finally { savingSandbox.value = false }
}

const handleSaveScoring = async () => {
  savingScoring.value = true
  try {
    if (scoringConfig.correctnessWeight + scoringConfig.styleWeight + scoringConfig.efficiencyWeight !== 100) {
      ElMessage.warning('评分配置三项权重之和应为 100%')
      return
    }
    await api.put('/admin/config/scoring', { ...scoringConfig })
    ElMessage.success('评分配置保存成功')
  } catch (e: any) {
    ElMessage.error(e?.message || e?.response?.data?.msg || '保存失败')
  } finally { savingScoring.value = false }
}

onMounted(loadConfig)
</script>

<template>
  <div class="system-config">
    <div class="page-header"><h2>系统配置</h2></div>

    <el-row :gutter="20">
      <el-col :span="24" class="config-section">
        <el-card header="LLM 参数配置">
          <el-form :model="llmConfig" label-width="120px">
            <el-row :gutter="16">
              <el-col :xs="24" :sm="12">
                <el-form-item label="LLM 提供商">
                  <el-select v-model="llmConfig.provider" class="form-full-width">
                    <el-option label="DeepSeek" value="deepseek" />
                    <el-option label="OpenAI" value="openai" />
                    <el-option label="智谱 GLM" value="zhipu" />
                    <el-option label="通义千问" value="qwen" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="模型">
                  <el-input v-model="llmConfig.model" placeholder="模型名称" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :xs="24" :sm="12">
                <el-form-item label="API Key">
                  <el-input v-model="llmConfig.apiKey" type="password" show-password placeholder="sk-..." />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="请求超时(秒)">
                  <el-input-number v-model="llmConfig.timeout" :min="5" :max="120" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :xs="24" :sm="12">
                <el-form-item label="Temperature">
                  <el-input-number v-model="llmConfig.temperature" :min="0" :max="2" :step="0.1" :precision="1" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="Max Tokens">
                  <el-input-number v-model="llmConfig.maxTokens" :min="256" :max="8192" :step="256" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item>
              <el-button type="primary" :loading="savingLLM" @click="handleSaveLLM">保存 LLM 配置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="12" class="config-section">
        <el-card header="代码沙箱策略">
          <el-form :model="sandboxConfig" label-width="130px">
            <el-form-item label="编译超时(秒)">
              <el-input-number v-model="sandboxConfig.compileTimeout" :min="1" :max="30" />
            </el-form-item>
            <el-form-item label="运行超时(秒)">
              <el-input-number v-model="sandboxConfig.runTimeout" :min="1" :max="30" />
            </el-form-item>
            <el-form-item label="内存限制(MB)">
              <el-input-number v-model="sandboxConfig.memoryLimitMB" :min="64" :max="1024" :step="64" />
            </el-form-item>
            <el-form-item label="最大并发数">
              <el-input-number v-model="sandboxConfig.maxConcurrent" :min="1" :max="16" />
            </el-form-item>
            <el-form-item label="允许网络访问">
              <el-switch v-model="sandboxConfig.enableNetwork" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingSandbox" @click="handleSaveSandbox">保存沙箱配置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="12" class="config-section">
        <el-card header="自动评分阈值">
          <el-form :model="scoringConfig" label-width="130px">
            <el-form-item label="正确性权重(%)">
              <el-input-number v-model="scoringConfig.correctnessWeight" :min="0" :max="100" />
            </el-form-item>
            <el-form-item label="规范性权重(%)">
              <el-input-number v-model="scoringConfig.styleWeight" :min="0" :max="100" />
            </el-form-item>
            <el-form-item label="效率权重(%)">
              <el-input-number v-model="scoringConfig.efficiencyWeight" :min="0" :max="100" />
            </el-form-item>
            <el-form-item label="偏差阈值(%)">
              <el-input-number v-model="scoringConfig.deviationThreshold" :min="5" :max="50" />
            </el-form-item>
            <el-form-item label="偏差时回退规则">
              <el-switch v-model="scoringConfig.fallbackToRules" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingScoring" @click="handleSaveScoring">保存评分配置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.config-section { margin-bottom: var(--space-5); }
.form-full-width { width: 100%; }
</style>
