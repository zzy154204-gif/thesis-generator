<template>
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
      @change="(val: string) => $emit('setHeading', val)"
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

    <el-button size="small" plain @click="$emit('insertReference')">📎 插入引用</el-button>
  </div>
</template>

<script setup lang="ts">
import type { Editor } from '@tiptap/vue-3'

defineProps<{
  editor: Editor | undefined
  currentHeading: string
}>()

defineEmits<{
  setHeading: [level: string]
  insertReference: []
}>()
</script>

<style scoped lang="scss">
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
</style>
