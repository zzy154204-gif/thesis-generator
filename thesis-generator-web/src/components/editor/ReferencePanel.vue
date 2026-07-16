<template>
  <div class="ref-panel">
    <el-button size="small" type="primary" @click="$emit('showAddDialog')">添加文献</el-button>
    <el-table :data="references" size="small" class="ref-table" max-height="400">
      <el-table-column prop="author" label="作者" width="80" />
      <el-table-column prop="title" label="标题" />
      <el-table-column label="操作" width="60">
        <template #default="{ row }">
          <el-button size="small" text type="primary" @click="$emit('insertAtCursor', row)">引用</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加文献对话框 -->
    <el-dialog v-model="dialogVisible" title="添加参考文献" width="500px" @closed="$emit('dialogClosed')">
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
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { Reference } from '@/types/api'

const props = defineProps<{
  references: Reference[]
  showDialog: boolean
}>()

const emit = defineEmits<{
  add: [ref: Omit<Reference, 'id' | 'thesisId'>]
  insertAtCursor: [ref: Reference]
  'update:showDialog': [val: boolean]
  showAddDialog: []
  dialogClosed: []
}>()

const dialogVisible = ref(false)

// 监听外部 showDialog 变化
import { watch } from 'vue'
watch(() => props.showDialog, (val) => {
  dialogVisible.value = val
})
watch(dialogVisible, (val) => {
  emit('update:showDialog', val)
})

const form = ref({ type: 'J', author: '', title: '', journal: '', year: '' })

function handleConfirm() {
  emit('add', { ...form.value, type: form.value.type as Reference['type'] })
  form.value = { type: 'J', author: '', title: '', journal: '', year: '' }
  dialogVisible.value = false
}
</script>

<style scoped lang="scss">
.ref-panel {
  .ref-table { margin-top: 12px; }
}
</style>
