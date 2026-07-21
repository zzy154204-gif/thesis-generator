<template>
  <EditorLayout>
    <!-- 顶部栏 -->
    <div class="editor-topbar">
      <div class="topbar-left">
        <el-button text :icon="ArrowLeft" @click="handleBack">返回列表</el-button>
        <el-divider direction="vertical" />
        <span class="paper-title" @dblclick="editingTitle = true">
          <template v-if="!editingTitle">{{ paper?.title || '加载中...' }}</template>
          <el-input
            v-else
            v-model="editTitleText"
            size="small"
            class="title-input"
            @blur="saveTitle"
            @keyup.enter="saveTitle"
            ref="titleInputRef"
          />
        </span>
      </div>
      <div class="topbar-center">
        <span class="save-indicator" :class="editorStore.saveStatus">
          {{ saveStatusText }}
        </span>
      </div>
      <div class="topbar-right">
        <el-button plain :icon="View" @click="handlePreview">预览</el-button>
        <el-button type="primary" :icon="Download" @click="showExportDialog = true">导出</el-button>
      </div>
    </div>

    <div class="editor-main">
      <!-- 左侧章节树 -->
      <SectionTree
        :sections="sectionTreeData"
        :panel-width="leftPanelWidth"
        @section-click="handleSectionClick"
        @add-section="addSection"
        @add-child-section="addChildSection"
        @rename-section="renameSection"
        @delete-section="deleteSection"
        @drag-end="handleDragEnd"
      />

      <!-- 拖拽分隔线 -->
      <div class="resize-handle left-handle" @mousedown="startResize('left', $event)" />

      <!-- 中心编辑区 -->
      <div class="editor-center">
        <EditorToolbar
          :editor="editor"
          :current-heading="currentHeading"
          @set-heading="setHeading"
          @insert-reference="insertReference"
        />

        <div class="editor-content" ref="editorContainer">
          <div v-if="!currentSectionId" class="editor-placeholder">
            请在左侧选择一个章节开始编辑
          </div>
          <editor-content :editor="editor" class="tiptap-content" />
        </div>
      </div>

      <!-- 拖拽分隔线 -->
      <div class="resize-handle right-handle" @mousedown="startResize('right', $event)" />

      <!-- 右侧面板 -->
      <div class="editor-sidebar right-sidebar" :style="{ width: rightPanelWidth + 'px' }">
        <el-tabs v-model="rightTab" class="right-tabs">
          <el-tab-pane label="参考文献" name="references">
            <ReferencePanel
              :references="references"
              :show-dialog="showRefDialog"
              @add="addReference"
              @insert-at-cursor="insertRefAtCursor"
              @show-add-dialog="showRefDialog = true"
              @update:show-dialog="showRefDialog = $event"
            />
          </el-tab-pane>
          <el-tab-pane label="模板信息" name="template">
            <div class="template-info">
              <p>当前模板：{{ templateInfo?.name || '默认模板' }}</p>
              <p class="desc">{{ templateInfo?.description || '' }}</p>
              <el-button size="small" plain style="margin-top: 12px">切换模板</el-button>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

    <!-- 导出对话框 -->
    <el-dialog v-model="showExportDialog" title="导出论文" width="500px">
      <el-form label-position="top">
        <el-form-item label="导出格式">
          <el-radio-group v-model="exportFormat">
            <el-radio value="PDF">PDF</el-radio>
            <el-radio value="DOCX">DOCX</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="导出范围">
          <el-radio-group v-model="exportScope">
            <el-radio value="all">全部章节</el-radio>
            <el-radio value="custom">指定章节</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="选项">
          <el-checkbox v-model="exportOptions.cover">包含封面</el-checkbox>
          <el-checkbox v-model="exportOptions.toc">包含目录</el-checkbox>
          <el-checkbox v-model="exportOptions.references">包含参考文献</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showExportDialog = false">取消</el-button>
        <el-button type="primary" :loading="exporting" @click="handleExport">
          {{ exporting ? '生成中...' : '开始导出' }}
        </el-button>
      </template>
    </el-dialog>
  </EditorLayout>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, View, Download } from '@element-plus/icons-vue'
import EditorLayout from '@/layouts/EditorLayout.vue'
import EditorToolbar from '@/components/editor/EditorToolbar.vue'
import SectionTree from '@/components/editor/SectionTree.vue'
import ReferencePanel from '@/components/editor/ReferencePanel.vue'
import { useEditorStore } from '@/stores/editor'
import { usePaperStore } from '@/stores/paper'
import type { ThesisSection, Reference, Template } from '@/types/api'

import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Underline from '@tiptap/extension-underline'

const router = useRouter()
const route = useRoute()
const editorStore = useEditorStore()
const paperStore = usePaperStore()

const paper = computed(() => paperStore.currentPaper)

const editor = useEditor({
  content: '',
  extensions: [StarterKit, Underline],
})

const editingTitle = ref(false)
const editTitleText = ref('')
const titleInputRef = ref()
const currentSectionId = ref<number | null>(null)
const showExportDialog = ref(false)
const showRefDialog = ref(false)
const exporting = ref(false)

const rightTab = ref('references')
const leftPanelWidth = ref(260)
const rightPanelWidth = ref(320)

const exportFormat = ref('PDF')
const exportScope = ref('all')
const exportOptions = reactive({ cover: true, toc: true, references: true })

const references = ref<Reference[]>([])
const templateInfo = ref<Template | null>(null)
const sectionTreeData = computed(() => paperStore.sections)

const saveStatusText = computed(() => {
  const status = editorStore.saveStatus
  const time = editorStore.lastSaveTime
  if (status === 'saved') return time ? `已保存 ${time}` : '已保存'
  if (status === 'saving') return '保存中...'
  return '未保存'
})

const currentHeading = computed(() => {
  if (!editor.value) return 'paragraph'
  if (editor.value.isActive('heading', { level: 1 })) return 'heading1'
  if (editor.value.isActive('heading', { level: 2 })) return 'heading2'
  if (editor.value.isActive('heading', { level: 3 })) return 'heading3'
  if (editor.value.isActive('heading', { level: 4 })) return 'heading4'
  return 'paragraph'
})

// --- 章节操作 ---
function handleSectionClick(data: ThesisSection) {
  if (currentSectionId.value === data.id) return
  currentSectionId.value = data.id
  editorStore.setCurrentSection(data.id)
  editor.value?.commands.setContent(data.content || '')
}

async function addSection(parent?: ThesisSection | null) {
  const { value: title } = await ElMessageBox.prompt('请输入章节名称', '添加章节', {
    confirmButtonText: '确定', cancelButtonText: '取消',
  })
  if (!title) return
  ElMessage.success(`章节"${title}"已添加`)
}

async function addChildSection(parent: ThesisSection) {
  await addSection(parent)
}

async function renameSection(node: ThesisSection) {
  const { value: title } = await ElMessageBox.prompt('请输入新名称', '重命名', {
    confirmButtonText: '确定', inputValue: node.title,
  })
  if (!title) return
}

async function deleteSection(node: ThesisSection) {
  const hasChildren = node.children && node.children.length > 0
  const msg = hasChildren ? `将同时删除${node.children!.length}个子章节，确定删除"${node.title}"吗？` : `确定删除"${node.title}"吗？`
  try {
    await ElMessageBox.confirm(msg, '确认删除', { type: 'warning' })
    ElMessage.success('章节已删除')
  } catch {}
}

function handleDragEnd() {
  // TODO: 调用 API 更新排序
}

// --- 标题编辑 ---
function saveTitle() {
  editingTitle.value = false
  if (editTitleText.value && editTitleText.value !== paper.value?.title) {
    // TODO: 调用 API 更新标题
  }
}

// --- 工具栏 ---
function setHeading(level: string) {
  if (level === 'paragraph') {
    editor.value?.chain().focus().setParagraph().run()
  } else {
    const lv = parseInt(level.replace('heading', ''))
    editor.value?.chain().focus().toggleHeading({ level: lv as 1 | 2 | 3 | 4 }).run()
  }
}

function insertReference() {
  rightTab.value = 'references'
}

function insertRefAtCursor(ref: Reference) {
  editor.value?.chain().focus().insertContent(`[${ref.id}]`).run()
  ElMessage.success('引用标记已插入')
}

function addReference(refData: Omit<Reference, 'id' | 'thesisId'>) {
  references.value.push({
    id: Date.now(),
    thesisId: 0,
    ...refData,
  })
  ElMessage.success('文献已添加')
}

// --- 面板缩放 ---
function startResize(side: 'left' | 'right', event: MouseEvent) {
  const startX = event.clientX
  const startWidth = side === 'left' ? leftPanelWidth.value : rightPanelWidth.value

  function onMouseMove(e: MouseEvent) {
    const diff = side === 'left' ? e.clientX - startX : startX - e.clientX
    const newWidth = Math.max(180, Math.min(500, startWidth + diff))
    if (side === 'left') leftPanelWidth.value = newWidth
    else rightPanelWidth.value = newWidth
  }

  function onMouseUp() {
    document.removeEventListener('mousemove', onMouseMove)
    document.removeEventListener('mouseup', onMouseUp)
  }

  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
}

// --- 导航操作 ---
function handleBack() {
  if (editorStore.isDirty) {
    ElMessageBox.confirm('有未保存的修改，确定离开吗？', '提示', { type: 'warning' })
      .then(() => router.push('/papers'))
      .catch(() => {})
  } else {
    router.push('/papers')
  }
}

function handlePreview() {
  const paperId = route.params.paperId
  window.open(`/preview/${paperId}`, '_blank')
}

async function handleExport() {
  exporting.value = true
  try {
    await new Promise((r) => setTimeout(r, 2000))
    ElMessage.success('导出成功，文件下载中...')
    showExportDialog.value = false
  } catch {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

// --- 生命周期 ---
onMounted(async () => {
  const paperId = Number(route.params.paperId)
  if (paperId) {
    await paperStore.fetchPaper(paperId)
    await paperStore.fetchSections(paperId)
    if (paperStore.sections.length > 0) {
      handleSectionClick(paperStore.sections[0])
    }
  }
})

onBeforeUnmount(() => {
  editor.value?.destroy()
})
</script>

<style scoped lang="scss">
.editor-topbar {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;

  .topbar-left {
    display: flex;
    align-items: center;
    gap: 8px;
    .paper-title { font-size: 15px; cursor: pointer; max-width: 300px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .title-input { width: 260px; }
  }

  .save-indicator {
    font-size: 13px;
    padding: 4px 12px;
    border-radius: 12px;
    &.saved { color: #67C23A; background: #f0f9eb; }
    &.saving { color: #E6A23C; background: #fdf6ec; }
    &.unsaved { color: #F56C6C; background: #fef0f0; }
  }
}

.editor-main {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.editor-sidebar {
  background: #fff;
  border-left: 1px solid #e4e7ed;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}

.resize-handle {
  width: 4px;
  background: transparent;
  cursor: col-resize;
  flex-shrink: 0;
  transition: background 0.2s;
  &:hover { background: #409EFF; }
}

.editor-center {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 400px;
}

.editor-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: #fff;

  .editor-placeholder {
    text-align: center;
    color: #c0c4cc;
    font-size: 16px;
    margin-top: 120px;
  }

  .tiptap-content {
    max-width: 800px;
    margin: 0 auto;
    min-height: 500px;
    outline: none;

    :deep(h1) { font-size: 24px; margin: 16px 0; }
    :deep(h2) { font-size: 20px; margin: 14px 0; }
    :deep(h3) { font-size: 18px; margin: 12px 0; }
    :deep(h4) { font-size: 16px; margin: 10px 0; }
    :deep(p) { line-height: 1.8; margin: 8px 0; }
    :deep(ul), :deep(ol) { padding-left: 24px; margin: 8px 0; }
  }
}

.right-tabs {
  :deep(.el-tabs__header) { margin: 0 16px; }
  :deep(.el-tabs__content) { padding: 0 16px 16px; }
}

.template-info {
  .desc { font-size: 13px; color: #909399; margin-top: 8px; }
}
</style>
