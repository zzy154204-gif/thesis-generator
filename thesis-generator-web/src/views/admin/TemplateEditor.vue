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
                <el-select v-model="form.collegeId" placeholder="请选择学院">
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
            </el-form>
          </el-tab-pane>

          <!-- Tab2: 封面配置（核心功能，第3周重点） -->
          <el-tab-pane label="封面配置" name="cover">
            <CoverConfig ref="coverConfigRef" />
          </el-tab-pane>

          <!-- Tab3: 样式配置 -->
          <el-tab-pane label="样式配置" name="styles">
            <div class="style-config" style="padding-top:20px;">
              <!-- 字体/字号/行距等配置 -->
              <el-alert title="样式配置开发中" type="info" :closable="false" />
            </div>
          </el-tab-pane>

          <!-- Tab4: 章节结构 -->
          <el-tab-pane label="章节结构" name="structure">
            <div class="structure-config" style="padding-top:20px;">
              <el-alert title="章节结构配置开发中" type="info" :closable="false" />
            </div>
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

const router = useRouter()
const route = useRoute()
const saving = ref(false)
const activeTab = ref('basic')
const coverConfigRef = ref()
const coverData = ref<any[]>([])

const isNew = ref(true)
const colleges = ref<any[]>([])

const form = reactive({
  id: 0,
  name: '',
  collegeId: '',
  type: 'GRADUATION' as 'GRADUATION' | 'COURSE' | 'PROJECT',
  description: '',
})

function onCoverChange(config: any[]) {
  coverData.value = config
}

async function handleSave() {
  saving.value = true
  try {
    // 收集各Tab配置
    const coverConfig = coverConfigRef.value?.getConfig()
    // TODO: 调用保存API
    await new Promise(r => setTimeout(r, 1000))
    ElMessage.success('保存成功')
  } finally {
    saving.value = false
  }
}

function handlePreview() {
  ElMessage.info('预览功能开发中')
}

onMounted(async () => {
  // 判断是否为编辑模式
  const id = route.params.id
  if (id && id !== 'new') {
    isNew.value = false
    // TODO: 加载模板数据
  }
  // TODO: 加载学院列表
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
  :deep(.el-tabs__content) { height: calc(100% - 40px); overflow-y: auto; }
}
</style>
