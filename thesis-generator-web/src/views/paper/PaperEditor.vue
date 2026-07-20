<template>
  <div class="editor-page">
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
          @upload-image="handleImageUpload"
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
              @update="updateReference"
              @delete-ref="deleteReference"
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, View, Download } from '@element-plus/icons-vue'
import EditorToolbar from '@/components/editor/EditorToolbar.vue'
import SectionTree from '@/components/editor/SectionTree.vue'
import ReferencePanel from '@/components/editor/ReferencePanel.vue'
import { useEditorStore } from '@/stores/editor'
import { usePaperStore } from '@/stores/paper'
import type { ThesisSection, Reference, Template } from '@/types/api'

import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Underline from '@tiptap/extension-underline'
import Image from '@tiptap/extension-image'
import { uploadImage } from '@/api/image'
import { saveDraft, getDraft } from '@/api/draft'
import { createSection, saveSection, deleteSection, updateSectionsOrder } from '@/api/section'
import { updatePaper } from '@/api/paper'
import { submitExport, getExportStatus, downloadExport } from '@/api/export'
import { getReferences, createReference, updateReference as updateRefApi, deleteReference as deleteRefApi } from '@/api/reference'

const router = useRouter()
const route = useRoute()
const editorStore = useEditorStore()
const paperStore = usePaperStore()

const paper = computed(() => paperStore.currentPaper)

const editor = useEditor({
  content: '',
  extensions: [
    StarterKit,
    Underline,
    Image.configure({ inline: true, allowBase64: false }),
  ],
})

// ===== 自动保存 =====
let autoSaveTimer: ReturnType<typeof setInterval> | null = null
const AUTO_SAVE_INTERVAL = 15000 // 15秒

function startAutoSave() {
  stopAutoSave()
  autoSaveTimer = setInterval(async () => {
    if (!editorStore.isDirty || !currentSectionId.value || !editor.value) return
    editorStore.markSaving()
    try {
      const paperId = Number(route.params.paperId)
      await saveDraft(paperId, currentSectionId.value, editor.value.getHTML())
      editorStore.markSaved()
    } catch {
      editorStore.markDirty()
    }
  }, AUTO_SAVE_INTERVAL)
}

function stopAutoSave() {
  if (autoSaveTimer) { clearInterval(autoSaveTimer); autoSaveTimer = null }
}

// ===== 图片上传 =====
const imageUploading = ref(false)
async function handleImageUpload(file: File) {
  if (!editor.value) return
  imageUploading.value = true
  try {
    const res = await uploadImage(file)
    const url = `/api/v1/images/${res.data.id}/file`
    editor.value.chain().focus().setImage({ src: url }).run()
    editorStore.markDirty()
  } catch {
    ElMessage.error('图片上传失败')
  } finally {
    imageUploading.value = false
  }
}

// 监听编辑器内容变化，标记为脏状态
watch(() => editor.value?.getHTML(), () => {
  if (editor.value) editorStore.markDirty()
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
  const paperId = Number(route.params.paperId)
  try {
    const res = await createSection(paperId, { title, parentId: parent?.id })
    // 乐观更新本地章节树
    const newSection: ThesisSection = {
      id: res.data?.id ?? Date.now(),
      title,
      content: '',
      children: [],
    }
    if (parent) {
      if (!parent.children) parent.children = []
      parent.children.push(newSection)
    } else {
      paperStore.sections.push(newSection)
    }
    ElMessage.success(`章节"${title}"已添加`)
  } catch {
    ElMessage.warning('后端暂未就绪，章节添加未持久化')
  }
}

async function addChildSection(parent: ThesisSection) {
  await addSection(parent)
}

async function renameSection(node: ThesisSection) {
  const { value: title } = await ElMessageBox.prompt('请输入新名称', '重命名', {
    confirmButtonText: '确定', inputValue: node.title,
  })
  if (!title) return
  const paperId = Number(route.params.paperId)
  try {
    await saveSection(paperId, node.id, { title })
    node.title = title
    ElMessage.success('重命名成功')
  } catch {
    ElMessage.warning('后端暂未就绪，重命名未持久化')
  }
}

async function deleteSection(node: ThesisSection) {
  const hasChildren = node.children && node.children.length > 0
  const msg = hasChildren ? `将同时删除${node.children!.length}个子章节，确定删除"${node.title}"吗？` : `确定删除"${node.title}"吗？`
  try {
    await ElMessageBox.confirm(msg, '确认删除', { type: 'warning' })
    const paperId = Number(route.params.paperId)
    try {
      await deleteSection(paperId, node.id)
    } catch {
      // 后端接口未就绪时继续删除本地数据
    }
    // 从本地章节树中移除
    removeSectionFromTree(paperStore.sections, node.id)
    // 如果删除的是当前编辑的章节，清空编辑器
    if (currentSectionId.value === node.id) {
      currentSectionId.value = null
      editor.value?.commands.setContent('')
    }
    ElMessage.success('章节已删除')
  } catch {
    // 用户取消删除
  }
}

/** 递归从章节树中移除指定节点 */
function removeSectionFromTree(nodes: ThesisSection[], targetId: number): boolean {
  const idx = nodes.findIndex((n) => n.id === targetId)
  if (idx !== -1) {
    nodes.splice(idx, 1)
    return true
  }
  for (const node of nodes) {
    if (node.children && removeSectionFromTree(node.children, targetId)) return true
  }
  return false
}

/** 递归展平章节树，收集 ID 顺序 */
function flattenSectionIds(nodes: ThesisSection[]): number[] {
  const ids: number[] = []
  function walk(items: ThesisSection[]) {
    for (const item of items) {
      ids.push(item.id)
      if (item.children && item.children.length > 0) {
        walk(item.children)
      }
    }
  }
  walk(nodes)
  return ids
}

async function handleDragEnd() {
  const paperId = Number(route.params.paperId)
  const orderedIds = flattenSectionIds(paperStore.sections)
  try {
    await updateSectionsOrder(paperId, orderedIds)
  } catch {
    // 后端章节排序 API 尚未就绪时静默失败
  }
}

// --- 标题编辑 ---
async function saveTitle() {
  editingTitle.value = false
  if (editTitleText.value && editTitleText.value !== paper.value?.title) {
    try {
      const paperId = Number(route.params.paperId)
      await updatePaper(paperId, { title: editTitleText.value })
      // 更新本地 store 中的标题
      if (paperStore.currentPaper) {
        paperStore.currentPaper.title = editTitleText.value
      }
      ElMessage.success('标题已更新')
    } catch {
      // 错误已在拦截器中处理
    }
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

/** 加载参考文献列表 */
async function loadReferences() {
  try {
    const res = await getReferences()
    references.value = res.data ?? []
  } catch {
    // 后端未就绪时静默
  }
}

async function addReference(refData: Omit<Reference, 'id' | 'thesisId'>) {
  try {
    const res = await createReference(refData)
    references.value.push(res.data)
    ElMessage.success('文献已添加')
  } catch {
    // 后端未就绪时本地添加
    references.value.push({ id: Date.now(), thesisId: 0, ...refData } as Reference)
    ElMessage.warning('文献已本地添加，后端未持久化')
  }
}

async function updateReference(ref: Reference) {
  try {
    await updateRefApi(ref.id, ref)
    const idx = references.value.findIndex((r) => r.id === ref.id)
    if (idx !== -1) references.value[idx] = ref
    ElMessage.success('文献已更新')
  } catch {
    const idx = references.value.findIndex((r) => r.id === ref.id)
    if (idx !== -1) references.value[idx] = ref
    ElMessage.warning('文献已本地更新，后端未持久化')
  }
}

async function deleteReference(ref: Reference) {
  try {
    await deleteRefApi(ref.id)
  } catch {
    // 后端未就绪时继续删除本地
  }
  references.value = references.value.filter((r) => r.id !== ref.id)
  ElMessage.success('文献已删除')
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
  const paperId = Number(route.params.paperId)
  try {
    const res = await submitExport(paperId, {
      format: exportFormat.value as 'PDF' | 'DOCX',
      scope: exportScope.value as 'all' | 'custom',
      options: {
        cover: exportOptions.cover,
        toc: exportOptions.toc,
        references: exportOptions.references,
      },
    })
    const taskId = res.data.taskId
    ElMessage.success('导出任务已提交，正在生成文件...')

    // 轮询直到完成
    let downloadUrl = ''
    for (let i = 0; i < 30; i++) {
      await new Promise((r) => setTimeout(r, 2000))
      const statusRes = await getExportStatus(taskId)
      const task = statusRes.data
      if (task.status === 'COMPLETED') {
        const dlRes = await downloadExport(taskId)
        downloadUrl = dlRes.data.downloadUrl
        break
      }
      if (task.status === 'FAILED') {
        throw new Error(task.errorMessage || '导出失败')
      }
    }
    if (!downloadUrl) throw new Error('导出超时，请稍后重试')

    const a = document.createElement('a')
    a.href = downloadUrl
    a.download = `${paper.value?.title || '论文'}.${exportFormat.value.toLowerCase()}`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)

    ElMessage.success('导出完成，文件开始下载')
    showExportDialog.value = false
  } catch (e: any) {
    ElMessage.error(e?.message || '导出失败')
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
    loadReferences()
    if (paperStore.sections.length > 0) {
      handleSectionClick(paperStore.sections[0])
    }
  }
  startAutoSave()
})

onBeforeUnmount(() => {
  stopAutoSave()
  editor.value?.destroy()
})
</script>

<style scoped lang="scss">
.editor-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}
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
