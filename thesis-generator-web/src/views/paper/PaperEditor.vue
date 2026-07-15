<template>
  <EditorLayout>
    <!-- 顶部工具栏 -->
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
      <div class="editor-sidebar left-sidebar" :style="{ width: leftPanelWidth + 'px' }">
        <div class="sidebar-header">
          <span>章节导航</span>
          <el-button text size="small" :icon="Plus" @click="addSection()">添加</el-button>
        </div>
        <div class="section-tree">
          <el-tree
            :data="sectionTreeData"
            :props="{ children: 'children', label: 'title' }"
            node-key="id"
            draggable
            highlight-current
            :allow-drop="() => true"
            @node-click="handleSectionClick"
            @node-drag-end="handleDragEnd"
          >
            <template #default="{ node, data }">
              <div class="tree-node" @contextmenu.prevent="showContextMenu($event, data)">
                <span>{{ node.label }}</span>
              </div>
            </template>
          </el-tree>
        </div>
      </div>

      <!-- 面板拖拽分隔线 -->
      <div class="resize-handle left-handle" @mousedown="startResize('left', $event)" />

      <!-- 中心编辑区 -->
      <div class="editor-center">
        <!-- 格式工具栏 -->
        <div class="editor-toolbar">
          <el-button-group class="toolbar-group">
            <el-button size="small" text @click="editor?.chain().focus().toggleBold().run()" :class="{ active: editor?.isActive('bold') }"><b>B</b></el-button>
            <el-button size="small" text @click="editor?.chain().focus().toggleItalic().run()" :class="{ active: editor?.isActive('italic') }"><i>I</i></el-button>
            <el-button size="small" text @click="editor?.chain().focus().toggleUnderline().run()" :class="{ active: editor?.isActive('underline') }"><u>U</u></el-button>
            <el-button size="small" text @click="editor?.chain().focus().toggleStrike().run()" :class="{ active: editor?.isActive('strike') }"><s>S</s></el-button>
          </el-button-group>

          <el-select
            size="small"
            :model-value="currentHeading"
            class="heading-select"
            @change="setHeading"
          >
            <el-option label="正文" value="paragraph" />
            <el-option label="标题1" value="heading1" />
            <el-option label="标题2" value="heading2" />
            <el-option label="标题3" value="heading3" />
            <el-option label="标题4" value="heading4" />
          </el-select>

          <el-button-group class="toolbar-group">
            <el-button size="small" text @click="editor?.chain().focus().toggleBulletList().run()" :class="{ active: editor?.isActive('bulletList') }">• 列表</el-button>
            <el-button size="small" text @click="editor?.chain().focus().toggleOrderedList().run()" :class="{ active: editor?.isActive('orderedList') }">1. 列表</el-button>
          </el-button-group>

          <el-button-group class="toolbar-group">
            <el-button size="small" text @click="editor?.chain().focus().undo().run()">↶ 撤销</el-button>
            <el-button size="small" text @click="editor?.chain().focus().redo().run()">↷ 重做</el-button>
          </el-button-group>

          <el-button size="small" plain @click="insertReference">📎 插入引用</el-button>
        </div>

        <!-- Tiptap 编辑器 -->
        <div class="editor-content" ref="editorContainer">
          <div v-if="!currentSectionId" class="editor-placeholder">
            请在左侧选择一个章节开始编辑
          </div>
          <editor-content :editor="editor" class="tiptap-content" />
        </div>
      </div>

      <!-- 面板拖拽分隔线 -->
      <div class="resize-handle right-handle" @mousedown="startResize('right', $event)" />

      <!-- 右侧面板 -->
      <div class="editor-sidebar right-sidebar" :style="{ width: rightPanelWidth + 'px' }">
        <el-tabs v-model="rightTab" class="right-tabs">
          <el-tab-pane label="参考文献" name="references">
            <div class="ref-panel">
              <el-button size="small" type="primary" @click="showRefDialog = true">添加文献</el-button>
              <el-table :data="references" size="small" class="ref-table" max-height="400">
                <el-table-column prop="author" label="作者" width="80" />
                <el-table-column prop="title" label="标题" />
                <el-table-column label="操作" width="60">
                  <template #default="{ row }">
                    <el-button size="small" text type="primary" @click="insertRefAtCursor(row)">引用</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
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

    <!-- 章节右键菜单 -->
    <div
      v-if="contextMenu.visible"
      class="context-menu"
      :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }"
      @click.stop
    >
      <div class="menu-item" @click="addSection(contextMenu.node)">添加同级章节</div>
      <div class="menu-item" @click="addChildSection(contextMenu.node)">添加子章节</div>
      <div class="menu-item" @click="renameSection(contextMenu.node)">重命名</div>
      <div class="menu-item danger" @click="deleteSection(contextMenu.node)">删除章节</div>
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

    <!-- 添加文献对话框 -->
    <el-dialog v-model="showRefDialog" title="添加参考文献" width="500px">
      <el-form :model="refForm" label-position="top">
        <el-form-item label="文献类型">
          <el-select v-model="refForm.type">
            <el-option label="期刊论文 [J]" value="J" />
            <el-option label="会议论文 [C]" value="C" />
            <el-option label="书籍 [M]" value="M" />
            <el-option label="学位论文 [D]" value="D" />
            <el-option label="网络资源 [EB/OL]" value="EB_OL" />
          </el-select>
        </el-form-item>
        <el-form-item label="作者">
          <el-input v-model="refForm.author" placeholder="多个作者用分号分隔" />
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="refForm.title" />
        </el-form-item>
        <el-form-item label="期刊/出版社">
          <el-input v-model="refForm.journal" />
        </el-form-item>
        <el-form-item label="年份">
          <el-input v-model="refForm.year" placeholder="如：2026" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRefDialog = false">取消</el-button>
        <el-button type="primary" @click="addReference">确定</el-button>
      </template>
    </el-dialog>
  </EditorLayout>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, View, Download, Plus } from '@element-plus/icons-vue'
import EditorLayout from '@/layouts/EditorLayout.vue'
import { useEditorStore } from '@/stores/editor'
import { usePaperStore } from '@/stores/paper'
import type { ThesisSection, Reference, Template } from '@/types/api'

// --- Tiptap imports (动态导入在 onMounted 中处理) ---
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Underline from '@tiptap/extension-underline'

const router = useRouter()
const route = useRoute()
const editorStore = useEditorStore()
const paperStore = usePaperStore()

const paper = computed(() => paperStore.currentPaper)

// 编辑器实例
const editor = useEditor({
  content: '',
  extensions: [StarterKit, Underline],
})

// 当前状态
const editingTitle = ref(false)
const editTitleText = ref('')
const titleInputRef = ref()
const currentSectionId = ref<number | null>(null)
const showExportDialog = ref(false)
const showRefDialog = ref(false)
const exporting = ref(false)

// 右侧面板
const rightTab = ref('references')

// 面板宽度
const leftPanelWidth = ref(260)
const rightPanelWidth = ref(320)

// 上下文菜单
const contextMenu = reactive({ visible: false, x: 0, y: 0, node: null as ThesisSection | null })

// 导出配置
const exportFormat = ref('PDF')
const exportScope = ref('all')
const exportOptions = reactive({ cover: true, toc: true, references: true })

// 参考文献
const references = ref<Reference[]>([])
const refForm = reactive({ type: 'J', author: '', title: '', journal: '', year: '' })

// 模板信息
const templateInfo = ref<Template | null>(null)

// 章节树数据
const sectionTreeData = computed(() => paperStore.sections)

// 保存状态文本
const saveStatusText = computed(() => {
  const status = editorStore.saveStatus
  const time = editorStore.lastSaveTime
  if (status === 'saved') return time ? `已保存 ${time}` : '已保存'
  if (status === 'saving') return '保存中...'
  return '未保存'
})

// 当前标题级别
const currentHeading = computed(() => {
  if (!editor.value) return 'paragraph'
  if (editor.value.isActive('heading', { level: 1 })) return 'heading1'
  if (editor.value.isActive('heading', { level: 2 })) return 'heading2'
  if (editor.value.isActive('heading', { level: 3 })) return 'heading3'
  if (editor.value.isActive('heading', { level: 4 })) return 'heading4'
  return 'paragraph'
})

// --- 章节操作 ---
async function handleSectionClick(data: ThesisSection) {
  if (currentSectionId.value === data.id) return
  currentSectionId.value = data.id
  editorStore.setCurrentSection(data.id)
  // 加载章节内容
  editor.value?.commands.setContent(data.content || '')
}

async function addSection(parent?: ThesisSection | null) {
  const { value: title } = await ElMessageBox.prompt('请输入章节名称', '添加章节', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
  })
  if (!title) return
  // TODO: 调用 API 创建章节
  ElMessage.success(`章节"${title}"已添加`)
}

async function addChildSection(parent: ThesisSection) {
  await addSection(parent)
}

async function renameSection(node: ThesisSection) {
  const { value: title } = await ElMessageBox.prompt('请输入新名称', '重命名', {
    confirmButtonText: '确定',
    inputValue: node.title,
  })
  if (!title) return
  // TODO: 调用 API 重命名章节
}

async function deleteSection(node: ThesisSection) {
  const hasChildren = node.children && node.children.length > 0
  const msg = hasChildren ? `将同时删除${node.children!.length}个子章节，确定删除"${node.title}"吗？` : `确定删除"${node.title}"吗？`
  try {
    await ElMessageBox.confirm(msg, '确认删除', { type: 'warning' })
    // TODO: 调用 API 删除章节
    ElMessage.success('章节已删除')
  } catch {}
}

async function handleDragEnd() {
  // TODO: 获取拖拽后的节点顺序，调用 API 更新
}

// --- 右键菜单 ---
function showContextMenu(event: MouseEvent, data: ThesisSection) {
  contextMenu.visible = true
  contextMenu.x = event.clientX
  contextMenu.y = event.clientY
  contextMenu.node = data
  // 点击其他区域关闭
  setTimeout(() => document.addEventListener('click', () => { contextMenu.visible = false }, { once: true }), 0)
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

function addReference() {
  references.value.push({
    id: Date.now(),
    thesisId: 0,
    type: refForm.type as Reference['type'],
    author: refForm.author,
    title: refForm.title,
    journal: refForm.journal,
    year: refForm.year,
  })
  showRefDialog.value = false
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
    // TODO: 调用导出 API
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
    // 如果有章节，默认选中第一个
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
  border-right: 1px solid #e4e7ed;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;

  &.right-sidebar {
    border-right: none;
    border-left: 1px solid #e4e7ed;
  }
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
  font-weight: 600;
  font-size: 14px;
}

.section-tree {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.tree-node {
  padding: 4px 0;
  font-size: 14px;
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

.editor-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-bottom: 1px solid #ebeef5;
  background: #fff;
  flex-wrap: wrap;

  .toolbar-group {
    .el-button { font-size: 13px; }
  }

  .heading-select {
    width: 100px;
  }
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

.ref-panel, .template-info {
  .ref-table { margin-top: 12px; }
  .desc { font-size: 13px; color: #909399; margin-top: 8px; }
}

.context-menu {
  position: fixed;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
  z-index: 2000;
  min-width: 160px;
  .menu-item {
    padding: 10px 16px;
    cursor: pointer;
    font-size: 13px;
    &:hover { background: #f5f7fa; }
    &.danger { color: #F56C6C; }
  }
}
</style>
