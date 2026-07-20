<template>
  <div class="ref-panel">
    <el-button size="small" type="primary" @click="$emit('showAddDialog')">添加文献</el-button>
    <el-table :data="references" size="small" class="ref-table" max-height="400">
      <el-table-column prop="author" label="作者" width="80" />
      <el-table-column prop="title" label="标题" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button size="small" text type="primary" @click="$emit('insertAtCursor', row)">引用</el-button>
          <el-button size="small" text @click="startEdit(row)">编辑</el-button>
          <el-button size="small" text type="danger" @click="$emit('deleteRef', row)">删</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加/编辑文献对话框 -->
    <el-dialog v-model="dialogVisible" :title="editingRef ? '编辑参考文献' : '添加参考文献'" width="500px" @closed="resetForm">
      <el-form :model="form" label-position="top">
        <el-form-item label="文献类型">
          <el-select v-model="form.type">
            <el-option label="期刊论文 [J]" value="J" />
            <el-option label="会议论文 [C]" value="C" />
            <el-option label="书籍 [M]" value="M" />
            <el-option label="学位论文 [D]" value="D" />
            <el-option label="网络资源 [EB/OL]" value="EB_OL" />
          </el-select>
        </el-form-item>
        <el-form-item label="作者">
          <el-input v-model="form.author" placeholder="多个作者用分号分隔" />
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="期刊/出版社">
          <el-input v-model="form.journal" />
        </el-form-item>
        <el-form-item label="年份">
          <el-input v-model="form.year" placeholder="如：2026" />
        </el-form-item>
        <el-form-item label="卷号/期号/页码">
          <el-input v-model="form.pages" placeholder="如：Vol.10 No.2 pp.12-34" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirm">{{ editingRef ? '保存' : '确定' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { Reference } from '@/types/api'

const props = defineProps<{
  references: Reference[]
  showDialog: boolean
}>()

const emit = defineEmits<{
  add: [ref: Omit<Reference, 'id' | 'thesisId'>]
  update: [ref: Reference]
  deleteRef: [ref: Reference]
  insertAtCursor: [ref: Reference]
  'update:showDialog': [val: boolean]
  showAddDialog: []
  dialogClosed: []
}>()

const dialogVisible = ref(false)
const editingRef = ref<Reference | null>(null)

// 监听外部 showDialog 变化
watch(() => props.showDialog, (val) => {
  dialogVisible.value = val
})
watch(dialogVisible, (val) => {
  emit('update:showDialog', val)
})

const form = ref<{ type: string; author: string; title: string; journal: string; year: string; pages: string }>({ type: 'J', author: '', title: '', journal: '', year: '', pages: '' })

function startEdit(ref: Reference) {
  editingRef.value = ref
  form.value = {
    type: ref.type || 'J',
    author: ref.author || '',
    title: ref.title || '',
    journal: ref.journal || '',
    year: ref.year || '',
    pages: ref.pages || '',
  }
  dialogVisible.value = true
}

function resetForm() {
  editingRef.value = null
  form.value = { type: 'J', author: '', title: '', journal: '', year: '', pages: '' }
}

function handleConfirm() {
  if (editingRef.value) {
    emit('update', { ...editingRef.value, ...form.value, type: form.value.type as Reference['type'] })
  } else {
    emit('add', { ...form.value, type: form.value.type as Reference['type'] })
  }
  resetForm()
  dialogVisible.value = false
}
</script>

<style scoped lang="scss">
.ref-panel {
  .ref-table { margin-top: 12px; }
}
</style>
