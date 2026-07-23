<template>
  <DefaultLayout>
    <!-- 全屏加载中 -->
    <div v-if="pageLoading" class="loading-container">
      <el-skeleton :rows="6" animated />
    </div>

    <div class="editor-layout" v-else-if="paper">
      <!-- 左侧：章节树 -->
      <aside class="section-panel">
        <div class="panel-header">
          <h3>章节结构</h3>
          <el-button text :icon="Plus" size="small" @click="showAddDialog()">新增</el-button>
        </div>
        <el-tree
          :data="treeData"
          node-key="id"
          :default-expanded-keys="expanded"
          :highlight-current="true"
          :current-node-key="currentNodeKey"
          @node-click="onNodeClick"
          draggable
          @node-drop="onDrop"
        >
          <template #default="{ node, data }">
            <span class="tree-node">
              <span class="node-label">{{ data.title }}</span>
              <span class="node-actions">
                <el-icon class="act" @click.stop="showAddDialog(data)"><Plus /></el-icon>
                <el-icon class="act" @click.stop="handleDeleteSection(data)"><Delete /></el-icon>
              </span>
            </span>
          </template>
        </el-tree>
      </aside>

      <!-- 中间：编辑器 -->
      <section class="editor-panel">
        <div class="editor-toolbar">
          <div class="toolbar-left">
            <span class="section-title">{{ currentSection?.title || '选择章节' }}</span>
          </div>
          <div class="toolbar-right">
            <span class="save-status">
              <template v-if="editorStore.saveStatus === 'saving'">保存中...</template>
              <template v-else-if="editorStore.saveStatus === 'unsaved'">未保存</template>
              <template v-else-if="editorStore.lastSaveTime">已保存 {{ editorStore.lastSaveTime }}</template>
            </span>
            <el-button text :icon="Upload" @click="handleSave">保存</el-button>
          </div>
        </div>

        <!-- 富文本格式工具栏 -->
        <div v-if="currentSection && editor" class="format-bar">
          <!-- 样式下拉 -->
          <el-select v-model="currentStyle" size="small" class="style-select" @change="applyStyle">
            <el-option label="正文" value="paragraph" />
            <el-option label="标题 1" value="h1" />
            <el-option label="标题 2" value="h2" />
            <el-option label="标题 3" value="h3" />
            <el-option label="图标题" value="figure" />
            <el-option label="表标题" value="table" />
          </el-select>
          <div class="sep" />
          <el-tooltip content="标题 1" placement="bottom">
            <el-button :class="{ active: editor.isActive('heading', { level: 1 }) }" size="small" @click="setStyle('h1')">H1</el-button>
          </el-tooltip>
          <el-tooltip content="标题 2" placement="bottom">
            <el-button :class="{ active: editor.isActive('heading', { level: 2 }) }" size="small" @click="setStyle('h2')">H2</el-button>
          </el-tooltip>
          <el-tooltip content="标题 3" placement="bottom">
            <el-button :class="{ active: editor.isActive('heading', { level: 3 }) }" size="small" @click="setStyle('h3')">H3</el-button>
          </el-tooltip>
          <div class="sep" />
          <el-tooltip content="加粗" placement="bottom">
            <el-button :class="{ active: editor.isActive('bold') }" size="small" @click="editor.chain().focus().toggleBold().run()"><b>B</b></el-button>
          </el-tooltip>
          <el-tooltip content="斜体" placement="bottom">
            <el-button :class="{ active: editor.isActive('italic') }" size="small" @click="editor.chain().focus().toggleItalic().run()"><i>I</i></el-button>
          </el-tooltip>
          <el-tooltip content="下划线" placement="bottom">
            <el-button :class="{ active: editor.isActive('underline') }" size="small" @click="editor.chain().focus().toggleUnderline().run()"><u>U</u></el-button>
          </el-tooltip>
          <el-tooltip content="删除线" placement="bottom">
            <el-button :class="{ active: editor.isActive('strike') }" size="small" @click="editor.chain().focus().toggleStrike().run()"><s>S</s></el-button>
          </el-tooltip>
          <el-tooltip content="上标" placement="bottom">
            <el-button :class="{ active: editor.isActive('superscript') }" size="small" @click="editor.chain().focus().toggleSuperscript().run()">x<sup>2</sup></el-button>
          </el-tooltip>
          <el-tooltip content="下标" placement="bottom">
            <el-button :class="{ active: editor.isActive('subscript') }" size="small" @click="editor.chain().focus().toggleSubscript().run()">H<sub>2</sub>O</el-button>
          </el-tooltip>
          <div class="sep" />
          <el-tooltip content="无序列表" placement="bottom">
            <el-button :class="{ active: editor.isActive('bulletList') }" size="small" @click="editor.chain().focus().toggleBulletList().run()"><el-icon><List /></el-icon></el-button>
          </el-tooltip>
          <el-tooltip content="有序列表" placement="bottom">
            <el-button :class="{ active: editor.isActive('orderedList') }" size="small" @click="editor.chain().focus().toggleOrderedList().run()">1.</el-button>
          </el-tooltip>
          <el-tooltip content="引用" placement="bottom">
            <el-button :class="{ active: editor.isActive('blockquote') }" size="small" @click="editor.chain().focus().toggleBlockquote().run()">❝</el-button>
          </el-tooltip>
          <div class="sep" />
          <el-tooltip content="左对齐" placement="bottom">
            <el-button :class="{ active: editor.isActive({ textAlign: 'left' }) }" size="small" @click="editor.chain().focus().setTextAlign('left').run()">左</el-button>
          </el-tooltip>
          <el-tooltip content="居中" placement="bottom">
            <el-button :class="{ active: editor.isActive({ textAlign: 'center' }) }" size="small" @click="editor.chain().focus().setTextAlign('center').run()">中</el-button>
          </el-tooltip>
          <el-tooltip content="右对齐" placement="bottom">
            <el-button :class="{ active: editor.isActive({ textAlign: 'right' }) }" size="small" @click="editor.chain().focus().setTextAlign('right').run()">右</el-button>
          </el-tooltip>
          <div class="sep" />
          <el-tooltip content="插入图片" placement="bottom">
            <el-button size="small" @click="triggerImageUpload"><el-icon><Picture /></el-icon></el-button>
          </el-tooltip>
          <el-tooltip content="插入图片链接" placement="bottom">
            <el-button size="small" @click="insertImageByUrl"><el-icon><Link /></el-icon></el-button>
          </el-tooltip>
          <el-tooltip content="插入表格" placement="bottom">
            <el-button size="small" @click="insertTable"><el-icon><Grid /></el-icon></el-button>
          </el-tooltip>
          <div class="sep" />
          <el-tooltip content="撤销" placement="bottom">
            <el-button size="small" @click="editor.chain().focus().undo().run()"><el-icon><RefreshLeft /></el-icon></el-button>
          </el-tooltip>
          <el-tooltip content="重做" placement="bottom">
            <el-button size="small" @click="editor.chain().focus().redo().run()"><el-icon><RefreshRight /></el-icon></el-button>
          </el-tooltip>
          <div class="sep" />
          <el-tooltip :content="footnoteCleanEnabled ? '脚注清理已开启' : '脚注清理已关闭'" placement="bottom">
            <el-button size="small" :class="{ active: footnoteCleanEnabled }" @click="toggleFootnoteClean">
              <span style="font-size:12px">¶</span>
            </el-button>
          </el-tooltip>
          <!-- 隐藏的文件上传 input -->
          <input ref="fileInputRef" type="file" accept="image/*" style="display:none" @change="onFileSelected" />
        </div>

        <div v-if="!currentSection || !editor" class="empty-editor">
          <el-empty :description="!currentSection ? '请在左侧选择或创建章节' : '编辑器初始化中...'" :image-size="120" />
        </div>

        <div v-else class="editor-wrap" :style="editorStyle">
          <editor-content :editor="editor" class="tiptap-editor" />
        </div>
      </section>

      <!-- 右侧：参考信息 -->
      <aside class="info-panel">
        <div class="panel-header"><h3>论文信息</h3></div>
        <div class="info-body">
          <div class="info-row"><label>标题</label><span>{{ paper.title }}</span></div>
          <div class="info-row"><label>状态</label><el-tag :type="statusTagType(paper.status)" size="small">{{ statusLabel(paper.status) }}</el-tag></div>
          <div class="info-row"><label>创建</label><span>{{ relativeTime(paper.createdAt) }}</span></div>
          <div class="info-row"><label>更新</label><span>{{ relativeTime(paper.updatedAt) }}</span></div>

          <!-- Word 导入封面元数据 -->
          <template v-if="parsedMetadata.length">
            <el-divider />
            <div class="meta-section">
              <h4 class="meta-title">导入封面信息</h4>
              <div v-for="m in parsedMetadata" :key="m.key" class="info-row">
                <label>{{ m.key }}</label><span>{{ m.value }}</span>
              </div>
            </div>
          </template>
        </div>
        <el-divider />

        <!-- 教师评语（退回/已批阅时显示） -->
        <div v-if="reviewFeedback?.latestReview" class="feedback-section">
          <h4 class="feedback-title">教师反馈</h4>
          <el-tag v-if="reviewFeedback.latestReview.action === 'RETURNED'" type="danger" size="small" effect="dark">退回</el-tag>
          <el-tag v-else-if="reviewFeedback.latestReview.action === 'REVIEWED'" type="success" size="small" effect="dark">已通过</el-tag>
          <div v-if="reviewFeedback.latestReview.returnReason" class="feedback-item">
            <label>退回原因：</label>
            <p>{{ reviewFeedback.latestReview.returnReason }}</p>
          </div>
          <div v-if="reviewFeedback.latestReview.commentHtml" class="feedback-item">
            <label>评语：</label>
            <div v-html="reviewFeedback.latestReview.commentHtml" />
          </div>
          <div v-if="reviewFeedback.latestReview.score != null" class="feedback-item">
            <label>评分：</label><span>{{ reviewFeedback.latestReview.score }} 分</span>
          </div>
          <div v-if="reviewFeedback.latestReview.grade" class="feedback-item">
            <label>等级：</label><span>{{ reviewFeedback.latestReview.grade }}</span>
          </div>
          <el-divider />
        </div>

        <!-- 批注列表 -->
        <div v-if="reviewFeedback?.annotations?.length" class="feedback-section">
          <h4 class="feedback-title">教师批注 ({{ reviewFeedback.annotations.length }})</h4>
          <div v-for="a in reviewFeedback.annotations" :key="a.id" class="ann-item">
            <div class="ann-text">{{ a.content }}</div>
            <div v-if="a.selectedText" class="ann-quote">引用：{{ a.selectedText }}</div>
            <div class="ann-meta">
              <span>{{ relativeTime(a.createdAt) }}</span>
            </div>
          </div>
          <el-divider />
        </div>

        <!-- 参考文献列表 -->
        <div class="feedback-section" v-if="thesisRefs.length">
          <h4 class="feedback-title">参考文献 ({{ thesisRefs.length }})</h4>
          <div v-for="ref in thesisRefs" :key="ref.id" class="ref-item">
            <span class="ref-index">[{{ ref.sortOrder }}]</span>
            <span class="ref-text" :title="ref.title">{{ ref.authors }}. {{ ref.title }}{{ ref.journal ? '. ' + ref.journal : '' }}</span>
            <div class="ref-actions">
              <el-button text type="primary" size="small" @click="insertCitation(ref)">引用</el-button>
              <el-button text type="danger" size="small" @click="handleRemoveRef(ref.id)">移除</el-button>
            </div>
          </div>
          <el-divider />
        </div>

        <div class="panel-body">
          <el-button type="primary" :icon="View" class="action-btn" @click="router.push(`/preview/${paperId}`)">预览全文</el-button>
          <el-button :icon="Download" class="action-btn" @click="handleExport('DOCX')">导出 DOCX</el-button>
          <el-button :icon="Download" class="action-btn" @click="handleExport('PDF')">导出 PDF</el-button>
          <el-button :icon="Upload" class="action-btn" @click="handleSubmit" :disabled="paper.status === 'SUBMITTED'">提交论文</el-button>
          <el-button v-if="paper.status === 'SUBMITTED'" :icon="Close" class="action-btn" @click="handleWithdraw">撤回提交</el-button>
          <el-button :icon="Reading" class="action-btn" @click="openRefDialog">管理参考文献</el-button>
        </div>
      </aside>
    </div>

    <el-empty v-else description="论文数据加载失败，请返回重试" :image-size="100" />

    <!-- 新增章节对话框 -->
    <el-dialog v-model="addVisible" :title="addParent ? '新增子章节' : '新增章节'" width="400px">
      <el-form :model="addForm">
        <el-form-item label="章节标题">
          <el-input v-model="addForm.title" placeholder="请输入章节标题" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="adding" @click="handleAddSection">确定</el-button>
      </template>
    </el-dialog>

    <!-- 参考文献管理对话框 -->
    <el-dialog v-model="refDialogVisible" title="管理参考文献" width="600px">
      <div class="ref-dialog-body">
        <!-- 已关联列表 -->
        <div v-if="thesisRefs.length" class="ref-section">
          <h4>已引用文献（点击「引用」插入到编辑器光标位置）</h4>
          <div v-for="ref in thesisRefs" :key="ref.id" class="ref-row">
            <span class="ref-badge">[{{ ref.sortOrder }}]</span>
            <span class="ref-info">{{ ref.authors }}. {{ ref.title }}<template v-if="ref.year"> ({{ ref.year }})</template></span>
            <div class="ref-row-actions">
              <el-button text type="primary" size="small" @click="insertCitation(ref); refDialogVisible = false">引用</el-button>
              <el-button text type="danger" size="small" @click="handleRemoveRef(ref.id)">移除</el-button>
            </div>
          </div>
        </div>
        <el-empty v-else description="还没有关联的参考文献" :image-size="80" />

        <el-divider />

        <!-- 从全局文献库添加 -->
        <h4>从文献库添加</h4>
        <el-input v-model="refSearchKeyword" placeholder="搜索标题或作者..." clearable size="small" style="margin:8px 0 12px" />
        <div v-if="refLoading" style="text-align:center;padding:20px"><el-icon class="is-loading"><Loading /></el-icon></div>
        <div v-else-if="!filteredReferences.length" style="font-size:13px;color:var(--el-text-color-secondary);text-align:center;padding:12px">没有可添加的文献</div>
        <div v-else class="ref-add-list">
          <div v-for="ref in filteredReferences" :key="ref.id" class="ref-add-row">
            <span class="ref-info">{{ ref.authors }}. {{ ref.title }} ({{ ref.year || '无年份' }})</span>
            <el-button text type="primary" size="small" @click="handleAddRef(ref.id)">添加</el-button>
          </div>
        </div>
      </div>
    </el-dialog>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Upload, View, Download, Picture, Grid, RefreshLeft, RefreshRight, Reading, Loading, Close, Link } from '@element-plus/icons-vue'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { usePaperStore } from '@/stores/paper'
import { useEditorStore } from '@/stores/editor'
import { statusLabel, statusTagType, relativeTime } from '@/utils/format'
import { getSection, createSection, updateSection, deleteSection, updateSectionsOrder, rebuildReferences } from '@/api/section'
import { downloadExport, getReviewFeedback } from '@/api/paper'
import { submitThesis, withdrawSubmission } from '@/api/submission'
import { saveDraft, getDraft } from '@/api/draft'
import { uploadImage } from '@/api/image'
import { getTemplateVersion } from '@/api/template'
import { getReferences } from '@/api/reference'
import { getThesisReferences, addThesisReference, removeThesisReference } from '@/api/reference'
import type { ThesisSection } from '@/types'
import { cleanFootnotesFromHtml, cleanFootnotesFromText } from '@/utils/cleanFootnotes'

// Tiptap
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Underline from '@tiptap/extension-underline'
import TextAlign from '@tiptap/extension-text-align'
import Table from '@tiptap/extension-table'
import TableRow from '@tiptap/extension-table-row'
import TableCell from '@tiptap/extension-table-cell'
import TableHeader from '@tiptap/extension-table-header'
import Superscript from '@tiptap/extension-superscript'
import Subscript from '@tiptap/extension-subscript'
import { ResizableImage } from '@/extensions/ResizableImage'
import { FigureTitle, TableTitle } from '@/extensions/TitleStyles'

const route = useRoute()
const router = useRouter()
const store = usePaperStore()
const editorStore = useEditorStore()

/** 页面是否正在加载初始数据 */
const pageLoading = ref(true)

const paper = computed(() => store.currentPaper)
const paperId = Number(route.params.paperId)

/** 解析 Word 导入封面元数据（学号、姓名、学院等） */
const parsedMetadata = computed(() => {
  if (!paper.value?.importMetadata) return []
  try {
    const obj = JSON.parse(paper.value.importMetadata)
    return Object.entries(obj).map(([key, value]) => ({ key, value }))
  } catch {
    return []
  }
})

/** 当前高亮章节 key（防止 editorStore.currentSectionId 在章节未加载时指向旧值） */
const currentNodeKey = computed(() => {
  if (!store.sections.length) return undefined
  const id = editorStore.currentSectionId
  return id !== null ? String(id) : undefined
})
const formatConfig = ref<Record<string, any>>({})   // 模板格式配置
const reviewFeedback = ref<{ latestReview: any; annotations: any[] } | null>(null)
const expanded = ref<string[]>([])
const addVisible = ref(false)
const adding = ref(false)
const addParent = ref<ThesisSection | null>(null)
const addForm = ref({ title: '' })
const fileInputRef = ref<HTMLInputElement>()

// ---- 脚注清理开关 ----
const footnoteCleanEnabled = ref(true)
const toggleFootnoteClean = () => {
  footnoteCleanEnabled.value = !footnoteCleanEnabled.value
}

// ---- 参考文献引用 ----
const thesisRefs = ref<any[]>([])
const refDialogVisible = ref(false)
const allReferences = ref<any[]>([])
const refSearchKeyword = ref('')
const refLoading = ref(false)

const filteredReferences = computed(() => {
  if (!refSearchKeyword.value) return allReferences.value
  const kw = refSearchKeyword.value.toLowerCase()
  return allReferences.value.filter((r: any) =>
    r.title?.toLowerCase().includes(kw) || r.authors?.toLowerCase().includes(kw)
  )
})

/** 加载论文已关联的参考文献 */
async function loadThesisReferences() {
  try {
    const res = await getThesisReferences(paperId)
    thesisRefs.value = res.data || []
  } catch { /* ignore */ }
}

/** 打开添加文献对话框 */
async function openRefDialog() {
  refLoading.value = true
  refDialogVisible.value = true
  refSearchKeyword.value = ''
  try {
    const res = await getReferences()
    // 过滤掉已添加的
    const addedIds = new Set(thesisRefs.value.map((r: any) => r.referenceId).filter(Boolean))
    allReferences.value = (res.data || []).filter((r: any) => !addedIds.has(r.id))
  } catch { /* ignore */ }
  finally { refLoading.value = false }
}

/** 为论文添加参考文献 */
async function handleAddRef(referenceId: number) {
  try {
    await addThesisReference(paperId, referenceId)
    // 从待选列表移除
    allReferences.value = allReferences.value.filter((r: any) => r.id !== referenceId)
    await loadThesisReferences()
    // 同步更新参考文献章节内容
    await syncReferenceSection()
  } catch { /* handled */ }
}

/** 移除参考文献 */
async function handleRemoveRef(refId: number) {
  try {
    await removeThesisReference(paperId, refId)
    await loadThesisReferences()
    // 同步更新参考文献章节内容
    await syncReferenceSection()
  } catch { /* handled */ }
}

/** 同步参考文献章节内容（调用后端重建接口 + 刷新本地章节数据） */
async function syncReferenceSection() {
  try {
    await rebuildReferences(paperId)
    // 刷新章节数据以获取更新后的参考文献章节 content
    await store.fetchSections(paperId)
    // 如果当前正在查看参考文献章节，刷新编辑器内容
    if (currentSection.value?.title?.includes('参考文献')) {
      const refSection = store.sections.find(s => s.title.includes('参考文献'))
      if (refSection && editor.value) {
        editor.value.commands.setContent(refSection.content || '')
      }
    }
  } catch { /* ignore - 可能没有参考文献章节 */ }
}

/** 插入引用标记到编辑器 */
function insertCitation(ref: any) {
  if (!editor.value) return
  const citation = `[${ref.sortOrder}]`
  editor.value.chain().focus().insertContent(citation).run()
}

// 构建树
const treeData = computed(() => {
  const items = store.sections
  const top = items.filter(s => !s.parentId)
  return top.map(s => buildTree(s, items))
})
function buildTree(node: ThesisSection, all: ThesisSection[]): any {
  const children = all.filter(s => s.parentId === node.id).sort((a, b) => a.sortOrder - b.sortOrder)
  return {
    id: String(node.id),
    title: node.title,
    section: node,
    children: children.map(c => buildTree(c, all)),
  }
}

const currentSection = computed(() =>
  store.sections.find(s => s.id === editorStore.currentSectionId) || null
)

// 当前选区样式
const currentStyle = ref('paragraph')

function setStyle(style: string) {
  if (!editor.value) return
  const chain = editor.value.chain().focus() as any
  if (style === 'paragraph') {
    chain.setParagraph().run()
    currentStyle.value = 'paragraph'
  } else if (style === 'h1') {
    chain.toggleHeading({ level: 1 }).run()
    currentStyle.value = editor.value.isActive('heading', { level: 1 }) ? 'h1' : 'paragraph'
  } else if (style === 'h2') {
    chain.toggleHeading({ level: 2 }).run()
    currentStyle.value = editor.value.isActive('heading', { level: 2 }) ? 'h2' : 'paragraph'
  } else if (style === 'h3') {
    chain.toggleHeading({ level: 3 }).run()
    currentStyle.value = editor.value.isActive('heading', { level: 3 }) ? 'h3' : 'paragraph'
  } else if (style === 'figure') {
    chain.toggleFigureTitle().run()
    currentStyle.value = editor.value.isActive('figureTitle') ? 'figure' : 'paragraph'
  } else if (style === 'table') {
    chain.toggleTableTitle().run()
    currentStyle.value = editor.value.isActive('tableTitle') ? 'table' : 'paragraph'
  } else {
    chain.setParagraph().run()
    currentStyle.value = 'paragraph'
  }
}

function applyStyle(style: string) {
  setStyle(style)
}

// 编辑器 selection 变化时同步下拉
function syncStyle() {
  if (!editor.value) return
  if (editor.value.isActive('figureTitle')) currentStyle.value = 'figure'
  else if (editor.value.isActive('tableTitle')) currentStyle.value = 'table'
  else if (editor.value.isActive('heading', { level: 1 })) currentStyle.value = 'h1'
  else if (editor.value.isActive('heading', { level: 2 })) currentStyle.value = 'h2'
  else if (editor.value.isActive('heading', { level: 3 })) currentStyle.value = 'h3'
  else currentStyle.value = 'paragraph'
}

// Tiptap 编辑器
const editor = useEditor({
  extensions: [
    StarterKit,
    Underline,
    TextAlign.configure({ types: ['heading', 'paragraph'] }),
    ResizableImage.configure({ inline: true }),
    Table.configure({ resizable: true }),
    TableRow, TableCell, TableHeader,
    Superscript, Subscript,
    FigureTitle, TableTitle,
  ],
  editorProps: {
    attributes: { class: 'prose-editor' },
    transformPastedHTML: (html: string) => {
      if (!footnoteCleanEnabled.value) return html
      return cleanFootnotesFromHtml(html)
    },
    transformPastedText: (text: string) => {
      if (!footnoteCleanEnabled.value) return text
      return cleanFootnotesFromText(text)
    },
    handleDrop: (view: any, event: DragEvent) => {
      const file = event.dataTransfer?.files?.[0]
      if (file?.type.startsWith('image/')) {
        uploadImageInEditor(file)
        return true
      }
      return false
    },
    handlePaste: (view: any, event: ClipboardEvent) => {
      const items = event.clipboardData?.items
      if (!items) return false
      for (let i = 0; i < items.length; i++) {
        if (items[i].type.startsWith('image/')) {
          const file = items[i].getAsFile()
          if (file) {
            uploadImageInEditor(file)
            return true
          }
        }
      }
      return false
    },
  },
  onUpdate: () => {
    editorStore.markDirty()
    autoSave()
  },
  onSelectionUpdate: () => {
    syncStyle()
  },
})

// 编辑器样式（从模板配置生成 CSS 变量）
const editorStyle = computed(() => {
  const cfg = formatConfig.value
  if (!cfg.body && !cfg.h1) return {}
  const vars: Record<string, string> = {}
  const body = cfg.body || {}
  if (body.fontFamily) vars['--font-family'] = body.fontFamily
  if (body.fontSize) vars['--font-size'] = body.fontSize
  if (body.lineHeight) vars['--line-height'] = body.lineHeight
  if (cfg.h1?.fontFamily) vars['--h1-font-family'] = cfg.h1.fontFamily
  if (cfg.h1?.fontSize) vars['--h1-font-size'] = cfg.h1.fontSize
  if (cfg.h2?.fontFamily) vars['--h2-font-family'] = cfg.h2.fontFamily
  if (cfg.h2?.fontSize) vars['--h2-font-size'] = cfg.h2.fontSize
  if (cfg.h3?.fontFamily) vars['--h3-font-family'] = cfg.h3.fontFamily
  if (cfg.h3?.fontSize) vars['--h3-font-size'] = cfg.h3.fontSize
  return vars as any
})

/** 加载模板格式配置 */
async function loadFormatConfig() {
  const tvid = store.currentPaper?.templateVersionId
  if (!tvid) return
  try {
    const res = await getTemplateVersion(tvid)
    if (res.data?.formatConfig) {
      formatConfig.value = JSON.parse(res.data.formatConfig)
    }
  } catch { /* 无模板配置时使用默认样式 */ }
}

// 上传图片
async function uploadImageInEditor(file: File) {
  try {
    const res = await uploadImage(file)
    const url = res.data.url
    editor.value?.chain().focus().setImage({ src: url }).run()
  } catch { /* handled */ }
}

function triggerImageUpload() {
  fileInputRef.value?.click()
}

function onFileSelected(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (file) uploadImageInEditor(file)
  // 重置 input 以便重复选择同一文件
  if (e.target) (e.target as HTMLInputElement).value = ''
}

/** 通过 URL 插入图片 */
async function insertImageByUrl() {
  if (!editor.value) return
  try {
    const { value } = await ElMessageBox.prompt('请输入图片 URL 地址', '插入图片链接', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: 'https://example.com/image.png 或 /uploads/xxx.jpg',
    })
    if (!value || !value.trim()) {
      ElMessage.warning('图片地址不能为空')
      return
    }
    const src = value.trim()
    editor.value.chain().focus().setImage({ src }).run()
  } catch {
    // 用户取消
  }
}

function insertTable() {
  editor.value?.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run()
}

// 自动保存防抖
let autoSaveTimer: any = null
function autoSave() {
  clearTimeout(autoSaveTimer)
  autoSaveTimer = setTimeout(async () => {
    if (!editorStore.currentSectionId || !editor.value) return
    editorStore.markSaving()
    try {
      await saveDraft(paperId, editorStore.currentSectionId, editor.value.getHTML())
      editorStore.markSaved()
    } catch { /* ignore */ }
  }, 3000)
}

// 加载章节
async function loadSections() {
  await store.fetchSections(paperId)
  if (store.sections.length && !editorStore.currentSectionId) {
    const first = store.sections[0]
    await loadSectionContent(first.id)
  }
}

// 加载章节内容到编辑器
async function loadSectionContent(sectionId: number) {
  editorStore.setCurrentSection(sectionId)
  try {
    const res = await getSection(paperId, sectionId)
    const section = res.data
    if (section.content && editor.value) {
      editor.value.commands.setContent(section.content)
    } else {
      // 尝试加载草稿
      try {
        const draftRes = await getDraft(paperId, sectionId)
        if (draftRes.data && editor.value) {
          editor.value.commands.setContent(draftRes.data)
        } else if (editor.value) {
          editor.value.commands.setContent('')
        }
      } catch {
        editor.value?.commands.setContent('')
      }
    }
    editorStore.markSaved()
  } catch { /* ignore */ }
}

// 点击节点
function onNodeClick(data: any) {
  const id = Number(data.id)
  if (id !== editorStore.currentSectionId) {
    // 先保存当前
    if (editorStore.isDirty) handleSave()
    loadSectionContent(id)
  }
}

// 拖拽排序
async function onDrop() {
  const ids = flattenTree(treeData.value)
  try {
    await updateSectionsOrder(paperId, ids.map(Number))
    await store.fetchSections(paperId)
  } catch { /* ignore */ }
}
function flattenTree(nodes: any[]): string[] {
  const ids: string[] = []
  for (const n of nodes) {
    ids.push(n.id)
    if (n.children?.length) ids.push(...flattenTree(n.children))
  }
  return ids
}

// 新增章节（自动插入到参考文献之前）
function showAddDialog(parent?: any) {
  addParent.value = parent?.section || null
  if (parent) {
    addForm.value.title = `子章节 ${(parent.children?.length || 0) + 1}`
  } else {
    // 顶级章节编号不计入 "参考文献"
    const topSections = store.sections.filter(s => !s.parentId && !s.title.includes('参考文献'))
    addForm.value.title = `第 ${topSections.length + 1} 章`
  }
  addVisible.value = true
}

async function handleAddSection() {
  if (!addForm.value.title) { ElMessage.warning('请输入标题'); return }
  adding.value = true
  try {
    await createSection(paperId, { title: addForm.value.title, parentId: addParent.value?.id })
    ElMessage.success('已添加')
    addVisible.value = false
    await loadSections()
  } catch { /* handled */ }
  finally { adding.value = false }
}

// 删除章节
async function handleDeleteSection(data: any) {
  try {
    await ElMessageBox.confirm(`确定删除"${data.title}"？`, '确认', { type: 'warning' })
    await deleteSection(paperId, Number(data.id))
    ElMessage.success('已删除')
    if (editorStore.currentSectionId === Number(data.id)) {
      editorStore.reset()
      editor.value?.commands.setContent('')
    }
    await loadSections()
  } catch { /* 取消 */ }
}

// 手动保存
async function handleSave() {
  if (!editorStore.currentSectionId || !editor.value) return
  editorStore.markSaving()
  try {
    await updateSection(paperId, editorStore.currentSectionId, {
      content: editor.value.getHTML(),
    })
    editorStore.markSaved()
    ElMessage.success('已保存')
  } catch { /* handled */ }
}

// 导出
async function handleExport(format: 'DOCX' | 'PDF') {
  try {
    await downloadExport(paperId, format)
    ElMessage.success(`论文已导出为 ${format}`)
  } catch (e: any) {
    ElMessage.error(e.message || '导出失败')
  }
}

// 提交
async function handleSubmit() {
  try {
    await ElMessageBox.confirm('确定提交论文吗？提交后将无法编辑，等待教师批阅。', '确认提交', {
      type: 'info', confirmButtonText: '提交', cancelButtonText: '取消',
    })
    await submitThesis(paperId)
    ElMessage.success('已提交')
    await store.fetchPaper(paperId)
  } catch { /* 取消 */ }
}

/** 撤回提交 */
async function handleWithdraw() {
  try {
    await ElMessageBox.confirm('确定撤回提交吗？撤回后论文将回到草稿状态，可继续编辑后重新提交。', '确认撤回', {
      type: 'warning', confirmButtonText: '撤回', cancelButtonText: '取消',
    })
    await withdrawSubmission(paperId)
    ElMessage.success('已撤回，论文回到草稿状态')
    await store.fetchPaper(paperId)
  } catch { /* 取消 */ }
}

/** 加载教师批阅反馈 */
async function loadReviewFeedback() {
  try {
    const res = await getReviewFeedback(paperId)
    reviewFeedback.value = res.data
  } catch { /* 无反馈时忽略 */ }
}

onMounted(async () => {
  // 每次挂载时重置编辑器状态，防止上个会话的残留
  editorStore.reset()
  pageLoading.value = true
  try {
    await store.fetchPaper(paperId)
    await loadFormatConfig()
    await loadSections()
    await loadReviewFeedback()
    await loadThesisReferences()
  } catch {
    /* handled */
  } finally {
    pageLoading.value = false
  }
})

/** 组件卸载时清理编辑器状态 */
onBeforeUnmount(() => {
  editorStore.reset()
})

watch(() => store.sections.length, () => {
  expanded.value = store.sections.map(s => String(s.id))
})
</script>

<style scoped lang="scss">
.loading-container {
  padding: 40px;
  max-width: 800px;
  margin: 0 auto;
}

.editor-layout {
  display: flex; gap: 0; height: calc(100vh - 56px - 48px); max-height: 900px;
  background: var(--el-fill-color-blank); border-radius: 10px; border: 1px solid var(--el-border-color-light); overflow: hidden;
}

.section-panel {
  width: 260px; border-right: 1px solid var(--el-border-color-light); overflow-y: auto; flex-shrink: 0;
  background: var(--el-fill-color);
  .panel-header { display: flex; justify-content: space-between; align-items: center; padding: 16px; border-bottom: 1px solid var(--el-border-color-light);
    h3 { font-size: 15px; font-weight: 600; margin: 0; }
  }
  :deep(.el-tree) { background: transparent; padding: 8px; }
  .tree-node { display: flex; align-items: center; justify-content: space-between; flex: 1; padding-right: 4px;
    .node-label { font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .node-actions { display: none; gap: 2px; }
    &:hover .node-actions { display: flex; }
    .act { font-size: 14px; cursor: pointer; color: var(--el-text-color-secondary); &:hover { color: var(--el-color-primary); } }
  }
}

.editor-panel {
  flex: 1; display: flex; flex-direction: column; min-width: 0;
}

.editor-toolbar {
  display: flex; justify-content: space-between; align-items: center; padding: 10px 16px;
  border-bottom: 1px solid var(--el-border-color-light);
  .section-title { font-weight: 600; font-size: 15px; }
  .toolbar-right { display: flex; align-items: center; gap: 12px; }
  .save-status { font-size: 12px; color: var(--el-text-color-secondary); }
}

/* 格式工具栏 */
.format-bar {
  display: flex; align-items: center; gap: 2px; padding: 6px 12px; flex-wrap: wrap;
  border-bottom: 1px solid var(--el-border-color-extra-light);
  background: var(--el-fill-color-light);

  .el-button {
    min-width: 32px; height: 30px; padding: 0 6px; font-size: 13px;
    border: 1px solid transparent; background: transparent;
    &.active {
      background: var(--el-color-primary-light-9) !important;
      border-color: var(--el-color-primary-light-5) !important;
      color: var(--el-color-primary) !important;
    }
    &:hover { background: var(--el-fill-color); }
  }
  .sep { width: 1px; height: 20px; background: var(--el-border-color-extra-light); margin: 0 4px; }

  .style-select {
    width: 110px;
    :deep(.el-input__wrapper) {
      background: transparent; box-shadow: none !important; height: 28px;
      &:hover { background: var(--el-fill-color); }
    }
    :deep(.el-input__inner) { font-size: 13px; }
  }
}

.editor-wrap { flex: 1; overflow-y: auto; padding: 20px 32px; }

.empty-editor { flex: 1; display: flex; align-items: center; justify-content: center; }

.info-panel {
  width: 220px; border-left: 1px solid var(--el-border-color-light); flex-shrink: 0; overflow-y: auto;
  .panel-header { padding: 16px; border-bottom: 1px solid var(--el-border-color-light);
    h3 { font-size: 15px; font-weight: 600; margin: 0; }
  }
  .info-body { padding: 16px; }
  .info-row { display: flex; justify-content: space-between; margin-bottom: 10px; font-size: 13px;
    label { color: var(--el-text-color-secondary); white-space: nowrap; margin-right: 8px; }
  }
  .meta-section { padding: 0; }
  .meta-title { font-size: 12px; font-weight: 600; margin: 0 0 8px; color: var(--el-text-color-secondary); }
  .panel-body { padding: 0 16px 16px; display: flex; flex-direction: column; gap: 8px; }
  .action-btn { width: 100%; }

  .feedback-section { padding: 0 16px; font-size: 13px;
    .feedback-title { font-size: 14px; font-weight: 600; margin: 0 0 10px; }
    .feedback-item { margin-bottom: 10px;
      label { color: var(--el-text-color-secondary); font-size: 12px; display: block; margin-bottom: 2px; }
      p { margin: 0; line-height: 1.6; }
    }
    .ann-item { padding: 8px 0; &:not(:last-child) { border-bottom: 1px solid var(--el-border-color-extra-light); } }
    .ann-text { font-size: 13px; margin-bottom: 4px; color: var(--el-text-color-primary); }
    .ann-quote { font-size: 12px; color: var(--el-color-primary); background: var(--el-color-primary-light-9); padding: 2px 6px; border-radius: 4px; margin-bottom: 4px; display: inline-block; }
    .ann-meta { font-size: 12px; color: var(--el-text-color-secondary); }
  }
}

:deep(.tiptap-editor) {
  min-height: 400px; outline: none;
  font-family: var(--font-family, '宋体');
  font-size: var(--font-size, 14px);
  line-height: var(--line-height, 1.8);
  p { margin-bottom: 0.5em; }
  h1, h2, h3, h4 {
    font-weight: 600; margin: 1em 0 0.5em;
  }
  h1 {
    font-family: var(--h1-font-family, '黑体');
    font-size: var(--h1-font-size, 16pt);
  }
  h2 {
    font-family: var(--h2-font-family, '黑体');
    font-size: var(--h2-font-size, 14pt);
  }
  h3 {
    font-family: var(--h3-font-family, '黑体');
    font-size: var(--h3-font-size, 12pt);
  }
  img { max-width: 100%; height: auto; }
  .figure-title, .table-title {
    text-align: center; font-weight: 600; margin: 0.8em 0 0.3em;
  }
  ul, ol { padding-left: 1.5em; }
  table { width: 100%; border-collapse: collapse; margin: 1em 0;
    th, td { border: 1px solid var(--el-border-color); padding: 8px 12px; text-align: left; }
    th { background: var(--el-fill-color); font-weight: 600; }
  }
}

/* ---- 参考文献样式 ---- */
.ref-item {
  font-size: 12px; padding: 6px 0; display: flex; align-items: flex-start; gap: 4px;
  &:not(:last-child) { border-bottom: 1px solid var(--el-border-color-extra-light); }
  .ref-index { font-weight: 600; color: var(--el-color-primary); flex-shrink: 0; min-width: 18px; }
  .ref-text { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; display: block; color: var(--el-text-color-regular); }
  .ref-actions { display: none; gap: 2px; flex-shrink: 0; }
  &:hover .ref-actions { display: flex; }
}

.ref-dialog-body {
  .ref-section { margin-bottom: 8px; }
  h4 { font-size: 14px; font-weight: 600; margin: 0 0 8px; }
  .ref-row { display: flex; align-items: flex-start; gap: 8px; padding: 8px 0; font-size: 13px; &:not(:last-child) { border-bottom: 1px solid var(--el-border-color-extra-light); } }
  .ref-badge { font-weight: 600; color: var(--el-color-primary); flex-shrink: 0; min-width: 24px; }
  .ref-info { flex: 1; color: var(--el-text-color-regular); line-height: 1.5; }
  .ref-row-actions { display: flex; gap: 4px; flex-shrink: 0; }
  .ref-add-list { max-height: 240px; overflow-y: auto; }
  .ref-add-row { display: flex; align-items: flex-start; gap: 8px; padding: 6px 0; font-size: 13px; &:not(:last-child) { border-bottom: 1px solid var(--el-border-color-extra-light); } }
}
</style>
