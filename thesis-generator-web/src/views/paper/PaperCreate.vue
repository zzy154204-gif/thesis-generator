<template>
  <DefaultLayout>
    <div class="create-page">
      <div class="create-container">
        <h2>新建论文</h2>

        <el-steps :active="activeStep" align-center class="steps">
          <el-step title="基本信息" />
          <el-step title="选择模板" />
          <el-step title="确认创建" />
        </el-steps>

        <!-- 步骤一：基本信息 -->
        <div v-if="activeStep === 0" class="step-content">
          <el-form :model="form" :rules="baseRules" ref="baseFormRef" label-position="top" size="large">
            <el-form-item label="论文标题" prop="title">
              <el-input
                v-model="form.title"
                placeholder="请输入论文标题"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>
            <el-form-item label="所属学院" prop="collegeId">
              <el-select v-model="form.collegeId" placeholder="请选择学院" class="full-width">
                <el-option
                  v-for="college in colleges"
                  :key="college.id"
                  :label="college.name"
                  :value="college.id"
                />
              </el-select>
            </el-form-item>
          </el-form>
          <div class="step-actions">
            <el-button type="primary" @click="nextStep">下一步</el-button>
          </div>
        </div>

        <!-- 步骤二：选择模板 -->
        <div v-else-if="activeStep === 1" class="step-content">
          <div v-if="templates.length === 0" class="empty-templates">
            <el-empty description="该学院暂无可用模板" :image-size="120" />
          </div>
          <div v-else class="template-grid">
            <div
              v-for="tpl in templates"
              :key="tpl.id"
              class="template-card"
              :class="{ selected: selectedTemplateId === tpl.id }"
              @click="selectedTemplateId = tpl.id"
            >
              <div class="tpl-check" v-if="selectedTemplateId === tpl.id">✓</div>
              <h4>{{ tpl.name }}</h4>
              <p>{{ tpl.description || '标准论文模板' }}</p>
            </div>
          </div>
          <div class="step-actions">
            <el-button @click="activeStep = 0">上一步</el-button>
            <el-button type="primary" :disabled="!selectedTemplateId" @click="nextStep">
              下一步
            </el-button>
          </div>
        </div>

        <!-- 步骤三：确认创建 -->
        <div v-else class="step-content">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="论文标题">{{ form.title }}</el-descriptions-item>
            <el-descriptions-item label="学院">
              {{ colleges.find((c) => c.id === form.collegeId)?.name }}
            </el-descriptions-item>
            <el-descriptions-item label="模板">
              {{ templates.find((t) => t.id === selectedTemplateId)?.name }}
            </el-descriptions-item>
          </el-descriptions>
          <div class="step-actions">
            <el-button @click="activeStep = 1">上一步</el-button>
            <el-button type="primary" :loading="creating" @click="handleCreate">
              {{ creating ? '创建中...' : '确认创建' }}
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { createPaper } from '@/api/paper'
import type { FormInstance, FormRules } from 'element-plus'
import type { College, Template } from '@/types/api'

const router = useRouter()
const activeStep = ref(0)
const creating = ref(false)
const selectedTemplateId = ref<number | null>(null)
const baseFormRef = ref<FormInstance>()

const form = reactive({
  title: '',
  collegeId: null as number | null,
})

const baseRules: FormRules = {
  title: [
    { required: true, message: '请输入论文标题', trigger: 'blur' },
    { min: 1, max: 200, message: '标题为1-200个字符', trigger: 'blur' },
  ],
  collegeId: [
    { required: true, message: '请选择学院', trigger: 'change' },
  ],
}

// 模拟的学院和模板数据（后续从后端获取）
const colleges = ref<College[]>([
  { id: 1, name: '计算机科学与技术学院' },
  { id: 2, name: '电子信息工程学院' },
  { id: 3, name: '数学与统计学院' },
])

const templates = ref<Template[]>([
  { id: 1, name: '本科毕业论文标准模板', description: '适用于本科毕业论文，含封面、目录、章节格式', collegeId: 1 },
  { id: 2, name: '课程设计报告模板', description: '适用于课程设计报告，简洁清晰', collegeId: 1 },
  { id: 3, name: '硕士学位论文模板', description: '适用于硕士学位论文，格式规范完整', collegeId: 1 },
])

function nextStep() {
  if (activeStep.value === 0 && baseFormRef.value) {
    baseFormRef.value.validate((valid) => {
      if (valid) activeStep.value = 1
    })
  } else if (activeStep.value === 1) {
    activeStep.value = 2
  }
}

async function handleCreate() {
  creating.value = true
  try {
    const result = await createPaper({
      title: form.title,
      collegeId: form.collegeId!,
      templateVersionId: selectedTemplateId.value || undefined,
    })
    ElMessage.success('论文创建成功')
    router.push(`/editor/${result.data.id}`)
  } catch {
    // 错误已在拦截器中处理
  } finally {
    creating.value = false
  }
}

onMounted(() => {
  // TODO: 从后端获取学院列表和模板列表
})
</script>

<style scoped lang="scss">
.create-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 32px 24px;
}

.create-container {
  background: #fff;
  border-radius: 12px;
  padding: 32px;

  h2 {
    text-align: center;
    margin-bottom: 32px;
    font-size: 22px;
  }
}

.steps {
  margin-bottom: 40px;
}

.step-content {
  max-width: 500px;
  margin: 0 auto;
}

.step-actions {
  margin-top: 32px;
  text-align: center;
  display: flex;
  gap: 12px;
  justify-content: center;
}

.full-width {
  width: 100%;
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.template-card {
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;

  &:hover { border-color: #409EFF; }

  &.selected {
    border-color: #409EFF;
    background: #ecf5ff;
  }

  h4 { margin-bottom: 8px; font-size: 15px; }
  p { font-size: 13px; color: #909399; }

  .tpl-check {
    position: absolute;
    top: 8px;
    right: 8px;
    width: 24px;
    height: 24px;
    border-radius: 50%;
    background: #409EFF;
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
  }
}

.empty-templates {
  padding: 40px 0;
}
</style>
