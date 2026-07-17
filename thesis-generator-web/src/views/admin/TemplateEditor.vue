<!-- src/views/admin/TemplateEditor.vue -->
<template>
  <DefaultLayout>
    <div class="template-editor">
      <!-- 顶栏 -->
      <div class="editor-header">
        <el-button text @click="router.back()">
          <el-icon><ArrowLeft /></el-icon> 返回列表
        </el-button>
        <span class="title">{{ isNew ? '新建模板' : '编辑模板' }}</span>
        <div class="actions">
          <el-button @click="handlePreview">预览</el-button>
          <el-button type="primary" :loading="saving" @click="handleSave">保存模板</el-button>
        </div>
      </div>

      <!-- 主体 -->
      <div class="editor-body">
        <el-tabs v-model="activeTab" class="editor-tabs">
          <!-- Tab1: 基础信息 -->
          <el-tab-pane label="基础信息" name="basic">
            <el-form :model="form" label-width="120px" style="max-width:600px;padding-top:20px;">
              <el-form-item label="模板名称" required>
                <el-input v-model="form.name" placeholder="请输入模板名称" />
              </el-form-item>
              <el-form-item label="所属学院" required>
                <el-select v-model="form.collegeId" placeholder="请选择学院" style="width:100%;">
                  <el-option label="全局" value="" />
                  <el-option v-for="c in colleges" :key="c.id" :label="c.name" :value="c.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="模板类型" required>
                <el-radio-group v-model="form.type">
                  <el-radio-button label="GRADUATION">毕业论文</el-radio-button>
                  <el-radio-button label="COURSE">课程论文</el-radio-button>
                  <el-radio-button label="PROJECT">项目论文</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="模板描述">
                <el-input v-model="form.description" type="textarea" :rows="3" placeholder="描述模板的适用场景" />
              </el-form-item>
              <el-form-item label="模板状态">
                <el-switch v-model="form.statusEnabled" active-text="启用" inactive-text="停用" />
              </el-form-item>
            </el-form>
          </el-tab-pane>

          <!-- Tab2: 封面配置 -->
          <el-tab-pane label="封面配置" name="cover">
            <CoverConfig ref="coverConfigRef" @change="onCoverChange" />
          </el-tab-pane>

          <!-- Tab3: 样式配置 -->
          <el-tab-pane label="样式配置" name="styles">
            <StyleConfig ref="styleConfigRef" v-model="styleConfig" />
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import CoverConfig from '@/components/admin/TemplateEditor/CoverConfig.vue'
import StyleConfig from '@/components/admin/TemplateEditor/StyleConfig.vue'
import { getAdminTemplate, createTemplate, updateTemplate } from '@/api/template'
import { getColleges } from '@/api/college'

const router = useRouter()
const route = useRoute()
const saving = ref(false)
const loading = ref(false)
const activeTab = ref('basic')
const coverConfigRef = ref()
const styleConfigRef = ref()
const isNew = ref(true)

// 学院列表
const colleges = ref<{ id: number; name: string }[]>([])

// 表单数据
const form = reactive({
  id: 0,
  name: '',
  collegeId: '',
  type: 'GRADUATION' as 'GRADUATION' | 'COURSE' | 'PROJECT',
  description: '',
  statusEnabled: true,
})

// 样式配置
const styleConfig = ref({})

function goBack() {
  router.push('/admin/templates')
}

// 封面配置变化
function onCoverChange(config: any[]) {
  // 封面配置变化时保存到本地
}

// 预览
function handlePreview() {
  // 跳转到预览页面，传递模板数据
  const data = {
    ...form,
    coverConfig: coverConfigRef.value?.getConfig(),
    styleConfig: styleConfigRef.value?.getConfig(),
  }
  // 存储到 sessionStorage 供预览页面使用
  sessionStorage.setItem('templatePreviewData', JSON.stringify(data))
  window.open('/admin/templates/preview', '_blank')
}

async function loadColleges() {
  try {
    const res = await getColleges()
    colleges.value = res.data || []
  } catch {
    // 学院列表加载失败，使用空列表
  }
}

// ===== 加载模板数据（编辑模式） =====
async function loadTemplate(id: number) {
  loading.value = true
  try {
    const res = await getAdminTemplate(id)
    const data = res.data

    form.id = data.id
    form.name = data.name || ''
    form.collegeId = data.collegeId?.toString() || ''
    form.type = data.type || 'GRADUATION'
    form.description = data.description || ''
    form.statusEnabled = data.status === 'ENABLED'

    // 设置封面配置
    if (data.coverConfig && coverConfigRef.value) {
      coverConfigRef.value.setConfig(data.coverConfig)
    }

    // 设置样式配置
    if (data.styleConfig && styleConfigRef.value) {
      styleConfig.value = data.styleConfig
    }
  } catch {
    ElMessage.error('加载模板数据失败')
  } finally {
    loading.value = false
  }
}

// 保存
// ===== 保存 =====
async function handleSave() {
  // 基础校验
  if (!form.name.trim()) {
    ElMessage.warning('请输入模板名称')
    return
  }
  if (!form.collegeId) {
    ElMessage.warning('请选择所属学院')
    return
  }

  saving.value = true
  try {
    const coverConfig = coverConfigRef.value?.getConfig() || []
    const styleConfigData = styleConfigRef.value?.getConfig() || {}

    const payload = {
      name: form.name,
      type: form.type,
      collegeId: Number(form.collegeId),
      description: form.description,
      status: form.statusEnabled ? 'ENABLED' : 'DISABLED',
      coverConfig,
      styles: styleConfigData,
    }

    if (isNew.value) {
      await createTemplate(payload)
      ElMessage.success('模板创建成功')
    } else {
      await updateTemplate(form.id, payload)
      ElMessage.success('模板更新成功')
    }

    // 保存成功后返回列表
    setTimeout(() => {
      router.push('/admin/templates')
    }, 500)
  } catch (error: any) {
    ElMessage.error(error?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ===== 生命周期 =====
onMounted(async () => {
  // 加载学院列表
  await loadColleges()

  // 判断是否为编辑模式
  const id = route.params.id
  if (id && id !== 'new') {
    isNew.value = false
    await loadTemplate(Number(id))
  }
})
</script>

<style scoped lang="scss">
.template-editor {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.editor-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 20px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;

  .title { font-size: 16px; font-weight: 600; flex: 1; }
  .actions { display: flex; gap: 8px; }
}

.editor-body {
  flex: 1;
  background: #fff;
  padding: 0 20px;
  overflow-y: auto;
}

.editor-tabs {
  height: 100%;
  :deep(.el-tabs__content) {
    height: calc(100% - 40px);
    overflow-y: auto;
  }
}
</style>
