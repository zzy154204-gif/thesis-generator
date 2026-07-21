<template>
  <div class="ref-panel">
    <div class="ref-toolbar">
      <el-button size="small" type="primary" @click="$emit('showAddDialog')">添加文献</el-button>
      <el-button size="small" @click="handleFormatAll" :loading="formatting">
        批量格式化
      </el-button>
    </div>

    <div v-if="formattedList.length > 0" class="formatted-list">
      <div class="format-header">
        <span>参考文献列表 (GB/T 7714)</span>
        <el-button size="small" text @click="formattedList = []">收起</el-button>
      </div>
      <div v-for="(ref, i) in formattedList" :key="i" class="format-item">
        <span class="ref-index">[{{ i + 1 }}]</span>
        <span class="ref-text">{{ ref }}</span>
      </div>
    </div>

    <el-divider v-if="formattedList.length > 0" style="margin:12px 0;" />

    <el-table :data="references" size="small" class="ref-table" max-height="300">
      <el-table-column prop="authors" label="作者" width="80" show-overflow-tooltip />
      <el-table-column prop="title" label="标题" show-overflow-tooltip />
      <el-table-column label="格式化" width="50">
        <template #default="{ row }">
          <el-button
            size="small"
            text
            type="primary"
            @click="handleFormatOne(row)"
            :loading="formattingId === row.id"
          >
            GB
          </el-button>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="50">
        <template #default="{ row }">
          <el-button size="small" text type="primary" @click="$emit('insertAtCursor', row)">引用</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="singleFormatted" class="single-format-preview">
      <div class="single-header">
        <span>格式化预览</span>
        <el-button size="small" text @click="singleFormatted = ''">关闭</el-button>
      </div>
      <div class="single-text">{{ singleFormatted }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { Reference } from '@/types/api'
import { formatReference, formatAllReferences } from '@/api/reference'

const props = defineProps<{
  references: Reference[]
}>()

const emit = defineEmits<{
  add: [ref: Omit<Reference, 'id'>]
  insertAtCursor: [ref: Reference]
  showAddDialog: []
}>()

const formatting = ref(false)
const formattingId = ref<number | null>(null)
const formattedList = ref<string[]>([])
const singleFormatted = ref('')

async function handleFormatAll() {
  formatting.value = true
  try {
    const res = await formatAllReferences()
    formattedList.value = res.data || []
    if (formattedList.value.length === 0) {
      ElMessage.info('暂无参考文献')
    }
  } catch {
    ElMessage.error('格式化失败')
  } finally {
    formatting.value = false
  }
}

async function handleFormatOne(ref: Reference) {
  formattingId.value = ref.id
  try {
    const res = await formatReference(ref.id)
    singleFormatted.value = res.data?.formatted || ''
  } catch {
    ElMessage.error('格式化失败')
  } finally {
    formattingId.value = null
  }
}
</script>

<style scoped lang="scss">
.ref-panel {
  .ref-toolbar {
    display: flex;
    gap: 8px;
    margin-bottom: 12px;
  }

  .ref-table { margin-top: 8px; }
}

.formatted-list {
  background: #f9f9f9;
  border-radius: 6px;
  padding: 12px;

  .format-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: 600;
    font-size: 13px;
    margin-bottom: 8px;
  }

  .format-item {
    display: flex;
    gap: 8px;
    font-size: 12px;
    line-height: 1.6;
    margin-bottom: 6px;

    .ref-index { color: #409EFF; flex-shrink: 0; }
    .ref-text { color: #303133; }
  }
}

.single-format-preview {
  margin-top: 12px;
  background: #f0f9eb;
  border-radius: 6px;
  padding: 12px;

  .single-header {
    display: flex;
    justify-content: space-between;
    font-weight: 600;
    font-size: 13px;
    margin-bottom: 8px;
  }

  .single-text {
    font-size: 12px;
    line-height: 1.6;
    color: #303133;
  }
}
</style>
