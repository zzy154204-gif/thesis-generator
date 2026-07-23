<template>
  <DefaultLayout>
    <div class="page">
      <div class="header">
        <el-button text :icon="ArrowLeft" @click="router.push('/admin/templates')">返回</el-button>
        <h2>{{ isNew ? '新建模板' : '编辑模板' }}</h2>
      </div>

      <el-card class="form-card">
        <el-form :model="form" label-width="100px" size="large">
          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="模板名称" required>
                <el-input v-model="form.name" placeholder="如 毕业论文模板" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="模板类型" required>
                <el-select v-model="form.type" style="width:100%">
                  <el-option label="毕业论文" value="GRADUATION" />
                  <el-option label="课程论文" value="COURSE" />
                  <el-option label="项目报告" value="PROJECT" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="所属学院">
            <el-select v-model="form.collegeId" placeholder="通用（不限定学院）" clearable filterable style="width:300px">
              <el-option v-for="c in colleges" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
          </el-form-item>

          <el-form-item label="描述">
            <el-input v-model="form.description" type="textarea" :rows="3" placeholder="模板描述" />
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 章节结构配置 -->
      <el-card class="form-card">
        <template #header>
          <div class="card-header">
            <span>章节结构配置</span>
            <el-button size="small" :icon="Plus" @click="addChapter">添加章节</el-button>
          </div>
        </template>
        <p class="tip">定义论文的默认章节结构，学生使用此模板时将自动生成以下章节</p>
        <div v-for="(ch, i) in form.structure" :key="i" class="chapter-item">
          <div class="chapter-header">
            <el-icon class="drag"><Rank /></el-icon>
            <el-input v-model="ch.title" placeholder="章节标题" style="width:300px" />
            <el-button text size="small" :icon="Plus" @click="addSubChapter(i)">添加子节</el-button>
            <el-button text type="danger" size="small" :icon="Delete" @click="form.structure.splice(i, 1)" />
          </div>
          <div v-for="(sub, j) in ch.children" :key="j" class="sub-item">
            <el-icon class="drag"><Minus /></el-icon>
            <el-input v-model="sub.title" placeholder="子节标题" style="width:260px" />
            <el-button text type="danger" size="small" :icon="Delete" @click="ch.children.splice(j, 1)" />
          </div>
        </div>
      </el-card>

      <!-- 封面字段配置 -->
      <el-card class="form-card">
        <template #header>
          <div class="card-header">
            <span>封面字段配置</span>
            <el-button size="small" :icon="Plus" @click="addCoverField">添加字段</el-button>
          </div>
        </template>
        <p class="tip">定义论文封面需要填写的字段</p>
        <div v-for="(f, i) in form.coverConfig" :key="i" class="field-item">
          <el-input v-model="f.key" placeholder="字段键名" style="width:140px" />
          <el-input v-model="f.label" placeholder="显示名称" style="width:200px" />
          <el-select v-model="f.type" style="width:100px">
            <el-option label="文本" value="text" />
            <el-option label="选择" value="select" />
          </el-select>
          <el-switch v-model="f.required" active-text="必填" />
          <el-button text type="danger" size="small" :icon="Delete" @click="form.coverConfig.splice(i, 1)" />
        </div>
      </el-card>

      <!-- 格式配置 - 按样式分层 -->
      <el-card class="form-card">
        <template #header><span>格式配置（按样式分层设置）</span></template>
        <p class="tip">按论文规范分层设置字体格式，学生在编辑器中通过样式下拉选择，自动应用对应格式</p>

        <el-tabs v-model="styleTab" type="border-card" class="style-tabs">
          <!-- 正文 -->
          <el-tab-pane label="正文" name="body">
            <el-form label-width="100px" size="small">
              <el-row :gutter="16">
                <el-col :span="8">
                  <el-form-item label="字体">
                    <el-select v-model="form.styles.body.fontFamily">
                      <el-option label="宋体" value="宋体" /><el-option label="黑体" value="黑体" />
                      <el-option label="楷体" value="楷体" /><el-option label="仿宋" value="仿宋" />
                      <el-option label="Times New Roman" value="Times New Roman" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="字号">
                    <el-select v-model="form.styles.body.fontSize">
                      <el-option label="三号 (16pt)" value="16pt" /><el-option label="小三 (15pt)" value="15pt" />
                      <el-option label="四号 (14pt)" value="14pt" /><el-option label="小四 (12pt)" value="12pt" />
                      <el-option label="五号 (10.5pt)" value="10.5pt" /><el-option label="小五 (9pt)" value="9pt" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="行距">
                    <el-select v-model="form.styles.body.lineHeight">
                      <el-option label="1.0" value="1.0" /><el-option label="1.25" value="1.25" />
                      <el-option label="1.5" value="1.5" /><el-option label="1.75" value="1.75" />
                      <el-option label="2.0" value="2.0" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="16">
                <el-col :span="8">
                  <el-form-item label="首行缩进">
                    <el-switch v-model="form.styles.body.indent" active-text="缩进2字符" />
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </el-tab-pane>

          <!-- 一级标题 -->
          <el-tab-pane label="一级标题" name="h1">
            <el-form label-width="100px" size="small">
              <el-row :gutter="16">
                <el-col :span="8">
                  <el-form-item label="字体">
                    <el-select v-model="form.styles.h1.fontFamily">
                      <el-option label="黑体" value="黑体" /><el-option label="宋体" value="宋体" />
                      <el-option label="楷体" value="楷体" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="字号">
                    <el-select v-model="form.styles.h1.fontSize">
                      <el-option label="二号 (22pt)" value="22pt" /><el-option label="小二 (18pt)" value="18pt" />
                      <el-option label="三号 (16pt)" value="16pt" /><el-option label="小三 (15pt)" value="15pt" />
                      <el-option label="四号 (14pt)" value="14pt" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="加粗">
                    <el-switch v-model="form.styles.h1.bold" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="16">
                <el-col :span="8">
                  <el-form-item label="段前距">
                    <el-select v-model="form.styles.h1.before">
                      <el-option label="0pt" value="0pt" /><el-option label="3pt" value="3pt" />
                      <el-option label="6pt" value="6pt" /><el-option label="12pt" value="12pt" />
                      <el-option label="18pt" value="18pt" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="段后距">
                    <el-select v-model="form.styles.h1.after">
                      <el-option label="0pt" value="0pt" /><el-option label="3pt" value="3pt" />
                      <el-option label="6pt" value="6pt" /><el-option label="12pt" value="12pt" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </el-tab-pane>

          <!-- 二级标题 -->
          <el-tab-pane label="二级标题" name="h2">
            <el-form label-width="100px" size="small">
              <el-row :gutter="16">
                <el-col :span="8">
                  <el-form-item label="字体">
                    <el-select v-model="form.styles.h2.fontFamily">
                      <el-option label="黑体" value="黑体" /><el-option label="宋体" value="宋体" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="字号">
                    <el-select v-model="form.styles.h2.fontSize">
                      <el-option label="三号 (16pt)" value="16pt" /><el-option label="小三 (15pt)" value="15pt" />
                      <el-option label="四号 (14pt)" value="14pt" /><el-option label="小四 (12pt)" value="12pt" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="加粗">
                    <el-switch v-model="form.styles.h2.bold" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="16">
                <el-col :span="8">
                  <el-form-item label="段前距">
                    <el-select v-model="form.styles.h2.before">
                      <el-option label="0pt" value="0pt" /><el-option label="3pt" value="3pt" />
                      <el-option label="6pt" value="6pt" /><el-option label="12pt" value="12pt" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="段后距">
                    <el-select v-model="form.styles.h2.after">
                      <el-option label="0pt" value="0pt" /><el-option label="3pt" value="3pt" />
                      <el-option label="6pt" value="6pt" /><el-option label="12pt" value="12pt" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </el-tab-pane>

          <!-- 三级标题 -->
          <el-tab-pane label="三级标题" name="h3">
            <el-form label-width="100px" size="small">
              <el-row :gutter="16">
                <el-col :span="8">
                  <el-form-item label="字体">
                    <el-select v-model="form.styles.h3.fontFamily">
                      <el-option label="黑体" value="黑体" /><el-option label="宋体" value="宋体" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="字号">
                    <el-select v-model="form.styles.h3.fontSize">
                      <el-option label="四号 (14pt)" value="14pt" /><el-option label="小四 (12pt)" value="12pt" />
                      <el-option label="五号 (10.5pt)" value="10.5pt" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="加粗">
                    <el-switch v-model="form.styles.h3.bold" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="16">
                <el-col :span="8">
                  <el-form-item label="段前距">
                    <el-select v-model="form.styles.h3.before">
                      <el-option label="0pt" value="0pt" /><el-option label="3pt" value="3pt" />
                      <el-option label="6pt" value="6pt" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="段后距">
                    <el-select v-model="form.styles.h3.after">
                      <el-option label="0pt" value="0pt" /><el-option label="3pt" value="3pt" />
                      <el-option label="6pt" value="6pt" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </el-tab-pane>

          <!-- 图标题 -->
          <el-tab-pane label="图标题" name="figure">
            <el-form label-width="100px" size="small">
              <el-row :gutter="16">
                <el-col :span="8">
                  <el-form-item label="字体">
                    <el-select v-model="form.styles.figure.fontFamily">
                      <el-option label="宋体" value="宋体" /><el-option label="黑体" value="黑体" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="字号">
                    <el-select v-model="form.styles.figure.fontSize">
                      <el-option label="五号 (10.5pt)" value="10.5pt" /><el-option label="小五 (9pt)" value="9pt" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="对齐">
                    <el-select v-model="form.styles.figure.align">
                      <el-option label="居中" value="center" /><el-option label="左对齐" value="left" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </el-tab-pane>

          <!-- 表标题 -->
          <el-tab-pane label="表标题" name="table">
            <el-form label-width="100px" size="small">
              <el-row :gutter="16">
                <el-col :span="8">
                  <el-form-item label="字体">
                    <el-select v-model="form.styles.tableCaption.fontFamily">
                      <el-option label="黑体" value="黑体" /><el-option label="宋体" value="宋体" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="字号">
                    <el-select v-model="form.styles.tableCaption.fontSize">
                      <el-option label="五号 (10.5pt)" value="10.5pt" /><el-option label="小五 (9pt)" value="9pt" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="对齐">
                    <el-select v-model="form.styles.tableCaption.align">
                      <el-option label="居中" value="center" /><el-option label="左对齐" value="left" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </el-tab-pane>

          <!-- 参考文献 -->
          <el-tab-pane label="参考文献" name="ref">
            <el-form label-width="100px" size="small">
              <el-row :gutter="16">
                <el-col :span="8">
                  <el-form-item label="字体">
                    <el-select v-model="form.styles.reference.fontFamily">
                      <el-option label="宋体" value="宋体" /><el-option label="黑体" value="黑体" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="字号">
                    <el-select v-model="form.styles.reference.fontSize">
                      <el-option label="五号 (10.5pt)" value="10.5pt" /><el-option label="小五 (9pt)" value="9pt" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </el-tab-pane>

          <!-- 页面设置 -->
          <el-tab-pane label="页面设置" name="page">
            <el-form label-width="100px" size="small">
              <el-row :gutter="16">
                <el-col :span="8">
                  <el-form-item label="页面大小">
                    <el-select v-model="form.styles.page.pageSize">
                      <el-option label="A4" value="A4" /><el-option label="A3" value="A3" />
                      <el-option label="B5" value="B5" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="16">
                  <el-form-item label="页边距">
                    <el-input v-model="form.styles.page.margins" placeholder="上 下 左 右，如 2.5 2.5 3 2.5 cm" />
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </el-card>

      <div class="form-actions">
        <el-button size="large" @click="router.push('/admin/templates')">取消</el-button>
        <el-button type="primary" size="large" :loading="saving" @click="handleSave">保存模板</el-button>
      </div>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Plus, Delete, Rank, Minus } from '@element-plus/icons-vue'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { getColleges } from '@/api/college'
import { getAdminTemplate, createAdminTemplate, updateAdminTemplate } from '@/api/admin'
import type { College } from '@/types'

const route = useRoute()
const router = useRouter()
const templateId = route.params.id ? Number(route.params.id) : null
const isNew = !templateId
const saving = ref(false)
const colleges = ref<College[]>([])

const styleTab = ref('body')

const form = reactive({
  name: '',
  type: 'GRADUATION',
  collegeId: undefined as number | undefined,
  description: '',
  structure: [] as any[],
  coverConfig: [] as any[],
  styles: {
    body: { fontFamily: '宋体', fontSize: '12pt', lineHeight: '1.5', indent: true },
    h1: { fontFamily: '黑体', fontSize: '16pt', bold: true, before: '0pt', after: '6pt' },
    h2: { fontFamily: '黑体', fontSize: '14pt', bold: true, before: '0pt', after: '6pt' },
    h3: { fontFamily: '黑体', fontSize: '12pt', bold: true, before: '0pt', after: '3pt' },
    figure: { fontFamily: '宋体', fontSize: '9pt', align: 'center' },
    tableCaption: { fontFamily: '黑体', fontSize: '9pt', align: 'center' },
    reference: { fontFamily: '宋体', fontSize: '9pt' },
    page: { pageSize: 'A4', margins: '' },
  },
  versionId: undefined as number | undefined,
})

function addChapter() {
  form.structure.push({ title: `第 ${form.structure.length + 1} 章`, children: [] })
}

function addSubChapter(i: number) {
  form.structure[i].children.push({ title: `${i + 1}.${form.structure[i].children.length + 1}` })
}

function addCoverField() {
  form.coverConfig.push({ key: '', label: '', type: 'text', required: false })
}

async function loadTemplate() {
  if (!templateId) return
  try {
    const res = await getAdminTemplate(templateId)
    const data = res.data
    form.name = data.name
    form.type = data.type
    form.collegeId = data.collegeId
    form.description = data.description || ''
    form.coverConfig = data.coverConfig || []
    form.structure = data.structure || []
    // 深度合并 styles，保留默认值
    const loaded = data.styles || {} as any
    for (const key of Object.keys(form.styles)) {
      const k = key as keyof typeof form.styles
      if (typeof loaded[k] === 'object' && loaded[k] !== null) {
        form.styles[k] = { ...form.styles[k], ...loaded[k] }
      } else if (loaded[k] !== undefined) {
        form.styles[k] = loaded[k]
      }
    }
    form.versionId = data.versionId
  } catch { /* handled */ }
}

async function handleSave() {
  if (!form.name) { ElMessage.warning('请填写模板名称'); return }
  saving.value = true
  try {
    const payload = {
      name: form.name,
      type: form.type,
      collegeId: form.collegeId,
      description: form.description,
      coverConfig: form.coverConfig,
      structure: form.structure,
      styles: form.styles,
    }

    if (isNew) {
      await createAdminTemplate(payload)
      ElMessage.success('模板创建成功')
    } else {
      await updateAdminTemplate(templateId!, { ...payload, versionId: form.versionId })
      ElMessage.success('模板已更新')
    }
    router.push('/admin/templates')
  } catch { /* handled */ }
  finally { saving.value = false }
}

onMounted(async () => {
  const colRes = await getColleges()
  colleges.value = colRes.data || []
  if (templateId) await loadTemplate()
})
</script>

<style scoped lang="scss">
.header { display: flex; align-items: center; gap: 16px; margin-bottom: 24px;
  h2 { font-size: 20px; font-weight: 600; }
}
.form-card { margin-bottom: 20px; }
.tip { font-size: 13px; color: var(--el-text-color-secondary); margin-bottom: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }

.chapter-item { margin-bottom: 12px; padding: 12px; background: var(--el-fill-color); border-radius: 8px; }
.chapter-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.sub-item { display: flex; align-items: center; gap: 8px; margin-left: 40px; margin-bottom: 6px; }
.field-item { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; flex-wrap: wrap; }
.drag { cursor: grab; color: var(--el-text-color-secondary); }

.style-tabs {
  :deep(.el-tabs__content) { padding: 16px; }
  :deep(.el-tab-pane) { min-height: 100px; }
}

.form-actions { display: flex; justify-content: center; gap: 16px; margin-top: 24px; padding-bottom: 40px; }
</style>
