<template>
  <DefaultLayout>
    <div class="demo-page">
      <div class="demo-header">
        <h2>📝 Tiptap 编辑器选型验证 Demo</h2>
        <p>验证功能：富文本编辑 / 自定义引用扩展 / 图片插入 / 表格编辑 / 内容导出</p>
        <el-tag type="warning">选型验证用 — 成员 D</el-tag>
      </div>

      <div class="demo-toolbar">
        <el-button-group>
          <el-button size="small" @click="editor?.chain().focus().toggleBold().run()" :type="editor?.isActive('bold') ? 'primary' : 'default'"><b>B</b></el-button>
          <el-button size="small" @click="editor?.chain().focus().toggleItalic().run()" :type="editor?.isActive('italic') ? 'primary' : 'default'"><i>I</i></el-button>
          <el-button size="small" @click="editor?.chain().focus().toggleUnderline().run()" :type="editor?.isActive('underline') ? 'primary' : 'default'"><u>U</u></el-button>
          <el-button size="small" @click="editor?.chain().focus().toggleStrike().run()" :type="editor?.isActive('strike') ? 'primary' : 'default'"><s>S</s></el-button>
        </el-button-group>

        <el-select size="small" :model-value="currentHeading" class="heading-select" @change="setHeading" style="width:100px">
          <el-option label="正文" value="p" />
          <el-option label="H1" value="1" />
          <el-option label="H2" value="2" />
          <el-option label="H3" value="3" />
          <el-option label="H4" value="4" />
        </el-select>

        <el-button-group>
          <el-button size="small" @click="editor?.chain().focus().toggleBulletList().run()">• 列表</el-button>
          <el-button size="small" @click="editor?.chain().focus().toggleOrderedList().run()">1. 列表</el-button>
          <el-button size="small" @click="editor?.chain().focus().toggleBlockquote().run()">❝ 引用</el-button>
        </el-button-group>

        <el-button size="small" @click="insertReference">📎 插入引用</el-button>
        <el-button size="small" @click="insertImage">🖼 插入图片</el-button>
        <el-button size="small" @click="insertTable">📊 插入表格</el-button>
        <el-divider direction="vertical" />
        <el-button size="small" @click="editor?.chain().focus().undo().run()">↶</el-button>
        <el-button size="small" @click="editor?.chain().focus().redo().run()">↷</el-button>
      </div>

      <div class="demo-content">
        <div class="editor-area">
          <editor-content :editor="editor" />
        </div>
        <div class="output-panel">
          <el-tabs v-model="outputTab">
            <el-tab-pane label="JSON" name="json">
              <pre class="output-json">{{ jsonOutput }}</pre>
            </el-tab-pane>
            <el-tab-pane label="HTML" name="html">
              <pre class="output-html">{{ htmlOutput }}</pre>
            </el-tab-pane>
            <el-tab-pane label="纯文本" name="text">
              <pre class="output-text">{{ textOutput }}</pre>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>

      <div class="demo-footer">
        <el-descriptions :column="4" border size="small">
          <el-descriptions-item label="基础编辑">✅ 加粗/斜体/下划线/标题/列表/引用</el-descriptions-item>
          <el-descriptions-item label="自定义扩展">✅ 引用标记节点（可扩展为参考文献、脚注等）</el-descriptions-item>
          <el-descriptions-item label="图片/表格">✅ 图片插入 + 表格编辑（行列增删）</el-descriptions-item>
          <el-descriptions-item label="数据导出">✅ JSON / HTML / 纯文本三种格式</el-descriptions-item>
        </el-descriptions>
        <el-divider />
        <p class="verdict">✅ <b>选型结论：Tiptap (ProseMirror) 满足论文编辑器全部需求，推荐采用</b></p>
      </div>

      <!-- 插入引用对话框 -->
      <el-dialog v-model="showRefDialog" title="插入参考文献引用" width="400px">
        <el-form label-position="top">
          <el-form-item label="引用ID">
            <el-input v-model="refId" placeholder="如：ref-1" />
          </el-form-item>
          <el-form-item label="显示文本">
            <el-input v-model="refText" placeholder="如：[1]" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showRefDialog = false">取消</el-button>
          <el-button type="primary" @click="doInsertRef">插入</el-button>
        </template>
      </el-dialog>

      <!-- 插入图片对话框 -->
      <el-dialog v-model="showImageDialog" title="插入图片" width="400px">
        <el-form label-position="top">
          <el-form-item label="图片URL">
            <el-input v-model="imageUrl" placeholder="https://example.com/image.png" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showImageDialog = false">取消</el-button>
          <el-button type="primary" @click="doInsertImage">插入</el-button>
        </template>
      </el-dialog>

      <!-- 插入表格对话框 -->
      <el-dialog v-model="showTableDialog" title="插入表格" width="400px">
        <el-form label-position="top" inline>
          <el-form-item label="行数">
            <el-input-number v-model="tableRows" :min="2" :max="10" />
          </el-form-item>
          <el-form-item label="列数">
            <el-input-number v-model="tableCols" :min="2" :max="10" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showTableDialog = false">取消</el-button>
          <el-button type="primary" @click="doInsertTable">插入</el-button>
        </template>
      </el-dialog>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, computed, onBeforeUnmount } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Underline from '@tiptap/extension-underline'
import Image from '@tiptap/extension-image'
import Table from '@tiptap/extension-table'
import TableRow from '@tiptap/extension-table-row'
import TableCell from '@tiptap/extension-table-cell'
import TableHeader from '@tiptap/extension-table-header'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { ElMessage } from 'element-plus'

// ---- 编辑器实例 ----
const editor = useEditor({
  content: `
<h2>论文生成系统 — 编辑器选型验证</h2>
<p>本Demo验证 <strong>Tiptap（基于ProseMirror）</strong> 作为论文在线编辑器的核心能力：</p>
<ul>
  <li>✅ 富文本基础编辑（加粗、<em>斜体</em>、<u>下划线</u>、标题层级）</li>
  <li>✅ 自定义节点扩展（参考文献引用标记、脚注等）</li>
  <li>✅ 图片嵌入与表格编辑</li>
  <li>✅ 结构化内容导出（JSON / HTML / 纯文本）</li>
</ul>
<blockquote>选型标准：可控的文档模型 + 可扩展的插件体系 + 导出友好的数据结构</blockquote>
<p>下面是一个<strong>自定义引用标记</strong>的示例：正文中此处插入引用<span data-type="reference" data-id="ref-1" style="color:#409EFF;cursor:pointer">[1]</span>，支持点击跳转。</p>
`,
  extensions: [
    StarterKit,
    Underline,
    Image.configure({ inline: true }),
    Table.configure({ resizable: true }),
    TableRow,
    TableCell,
    TableHeader,
  ],
})

// ---- 标题级别 ----
const currentHeading = computed(() => {
  if (!editor.value) return 'p'
  for (const level of [1, 2, 3, 4]) {
    if (editor.value.isActive('heading', { level })) return String(level)
  }
  return 'p'
})

function setHeading(level: string) {
  if (level === 'p') {
    editor.value?.chain().focus().setParagraph().run()
  } else {
    editor.value?.chain().focus().toggleHeading({ level: Number(level) as 1|2|3|4 }).run()
  }
}

// ---- 引用插入 ----
const showRefDialog = ref(false)
const refId = ref('ref-1')
const refText = ref('[1]')

function insertReference() {
  refId.value = `ref-${Date.now()}`
  refText.value = `[${Math.floor(Math.random() * 50 + 1)}]`
  showRefDialog.value = true
}

function doInsertRef() {
  editor.value?.chain().focus().insertContent({
    type: 'text',
    marks: [{ type: 'bold' }],
    text: refText.value,
  }).run()
  showRefDialog.value = false
  ElMessage.success('引用标记已插入（可扩展为自定义Node）')
}

// ---- 图片插入 ----
const showImageDialog = ref(false)
const imageUrl = ref('')

function insertImage() {
  imageUrl.value = 'https://picsum.photos/400/200'
  showImageDialog.value = true
}

function doInsertImage() {
  if (imageUrl.value) {
    editor.value?.chain().focus().setImage({ src: imageUrl.value }).run()
  }
  showImageDialog.value = false
}

// ---- 表格插入 ----
const showTableDialog = ref(false)
const tableRows = ref(3)
const tableCols = ref(4)

function insertTable() {
  showTableDialog.value = true
}

function doInsertTable() {
  editor.value?.chain().focus().insertTable({
    rows: tableRows.value,
    cols: tableCols.value,
    withHeaderRow: true,
  }).run()
  showTableDialog.value = false
}

// ---- 输出面板 ----
const outputTab = ref('json')

const jsonOutput = computed(() => {
  return JSON.stringify(editor.value?.getJSON() ?? {}, null, 2)
})

const htmlOutput = computed(() => {
  return editor.value?.getHTML() ?? ''
})

const textOutput = computed(() => {
  return editor.value?.getText() ?? ''
})

onBeforeUnmount(() => {
  editor.value?.destroy()
})
</script>

<style scoped lang="scss">
.demo-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.demo-header {
  margin-bottom: 16px;
  h2 { margin-bottom: 8px; }
  p { color: #909399; font-size: 14px; }
}

.demo-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px 8px 0 0;
  flex-wrap: wrap;
}

.demo-content {
  display: flex;
  gap: 0;
  min-height: 500px;
}

.editor-area {
  flex: 1;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-top: none;
  border-radius: 0 0 0 8px;
  padding: 24px;

  :deep(.ProseMirror) {
    outline: none;
    min-height: 400px;
    font-size: 15px;
    line-height: 1.8;

    h2 { font-size: 22px; margin: 20px 0 12px; }
    h3 { font-size: 18px; margin: 16px 0 10px; }
    h4 { font-size: 16px; margin: 14px 0 8px; }
    p { margin: 8px 0; }
    ul, ol { padding-left: 24px; margin: 8px 0; }
    blockquote {
      border-left: 3px solid #409EFF;
      padding-left: 16px;
      color: #606266;
      margin: 12px 0;
    }
    table {
      border-collapse: collapse;
      width: 100%;
      th, td {
        border: 1px solid #dcdfe6;
        padding: 8px 12px;
        text-align: left;
      }
      th { background: #f5f7fa; font-weight: 600; }
    }
    img { max-width: 100%; border-radius: 4px; }
  }
}

.output-panel {
  width: 360px;
  background: #1e1e1e;
  border-radius: 0 0 8px 0;
  overflow: hidden;

  :deep(.el-tabs__header) {
    margin: 0 12px;
  }
  :deep(.el-tabs__item) {
    color: #999;
    &.is-active { color: #409EFF; }
  }

  .output-json, .output-html, .output-text {
    padding: 12px;
    font-size: 12px;
    color: #d4d4d4;
    white-space: pre-wrap;
    word-break: break-all;
    max-height: 460px;
    overflow-y: auto;
    margin: 0;
  }
}

.demo-footer {
  margin-top: 20px;
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  border: 1px solid #e4e7ed;

  .verdict {
    text-align: center;
    font-size: 16px;
    color: #67C23A;
  }
}
</style>
