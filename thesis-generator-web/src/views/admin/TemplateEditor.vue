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

          <!-- Tab2: 封面配置 -->
          <el-tab-pane label="封面配置" name="cover">
            <CoverConfig ref="coverConfigRef" @change="onCoverChange" />
          </el-tab-pane>

          <!-- Tab3: 样式配置 -->
          <el-tab-pane label="样式配置" name="styles">
            <div class="style-config" style="padding-top:20px;">
              <el-form label-width="120px">
                <el-form-item label="正文字体">
                  <el-select v-model="styles.font" style="width:200px;">
                    <el-option label="宋体" value="SimSun" />
                    <el-option label="黑体" value="SimHei" />
                    <el-option label="Times New Roman" value="TimesNewRoman" />
                    <el-option label="Calibri" value="Calibri" />
                  </el-select>
                </el-form-item>
                <el-form-item label="正文字号">
                  <el-select v-model="styles.fontSize" style="width:200px;">
                    <el-option label="五号 (10.5pt)" value="10.5" />
                    <el-option label="小四 (12pt)" value="12" />
                    <el-option label="四号 (14pt)" value="14" />
                  </el-select>
                </el-form-item>
                <el-form-item label="行距">
                  <el-select v-model="styles.lineHeight" style="width:200px;">
                    <el-option label="1.5 倍" value="1.5" />
                    <el-option label="2 倍" value="2" />
                    <el-option label="固定值 20pt" value="20" />
                  </el-select>
                </el-form-item>
              </el-form>
            </div>
          </el-tab-pane>

          <!-- Tab4: 章节结构 -->
          <el-tab-pane label="章节结构" name="structure">
            <div class="structure-config" style="padding-top:20px;">
              <el-form label-width="160px">
                <el-form-item label="包含原创声明">
                  <el-switch v-model="structure.hasDeclaration" />
                </el-form-item>
                <el-form-item label="包含中文摘要">
                  <el-switch v-model="structure.hasAbstract" />
                </el-form-item>
                <el-form-item label="包含英文摘要">
                  <el-switch v-model="structure.hasEnglishAbstract" />
                </el-form-item>
                <el-form-item label="最大标题层级">
                  <el-select v-model="structure.maxHeadingLevel" style="width:200px;">
                    <el-option label="2 级" :value="2" />
                    <el-option label="3 级" :value="3" />
                    <el-option label="4 级" :value="4" />
                  </el-select>
                </el-form-item>
                <el-form-item label="包含附录">
                  <el-switch v-model="structure.hasAppendix" />
                </el-form-item>
                <el-form-item label="包含参考文献">
                  <el-switch v-model="structure.hasReferences" />
                </el-form-item>
              </el-form>
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

// 如果组件还不存在，创建占位或临时注释
// import CoverConfig from '@/components/admin/TemplateEditor/CoverConfig.vue'

const router = useRouter()
const route = useRoute()
const saving = ref(false)
const activeTab = ref('basic')
const coverConfigRef = ref()
const isNew = ref(true)

const colleges = ref([
  { id: 1, name: '计算机学院' },
  { id: 2, name: '电子工程学院' },
  { id: 3, name: '数学学院' },
])

const form = reactive({
  id: 0,
  name: '',
  collegeId: '',
  type: 'GRADUATION' as 'GRADUATION' | 'COURSE' | 'PROJECT',
  description: '',
})

const styles = reactive({
  font: 'SimSun',
  fontSize: '12',
  lineHeight: '1.5',
})

const structure = reactive({
  hasDeclaration: true,
  hasAbstract: true,
  hasEnglishAbstract: false,
  maxHeadingLevel: 3,
  hasAppendix: false,
  hasReferences: true,
})

function onCoverChange(config: any[]) {
  // 封面配置变化
}

async function handleSave() {
  saving.value = true
  try {
    const coverConfig = coverConfigRef.value?.getConfig()
    // TODO: 调用保存 API
    // await templateApi.save({
    //   ...form,
    //   styles,
    //   structure,
    //   coverConfig,
    // })
    await new Promise((r) => setTimeout(r, 1000))
    ElMessage.success('保存成功')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

function handlePreview() {
  ElMessage.info('预览功能开发中')
}

onMounted(async () => {
  const id = route.params.id
  if (id && id !== 'new') {
    isNew.value = false
    // TODO: 加载模板数据
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

  .title {
    font-size: 16px;
    font-weight: 600;
    flex: 1;
  }
  .actions {
    display: flex;
    gap: 8px;
  }
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
