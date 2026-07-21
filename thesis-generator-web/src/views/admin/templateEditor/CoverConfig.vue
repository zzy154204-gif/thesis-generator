<!-- src/components/admin/TemplateEditor/CoverConfig.vue -->
<template>
  <div class="cover-config">
    <div class="config-hint">
      <el-alert
        title="从左侧字段池点击添加字段到封面，拖拽右侧列表调整显示顺序"
        type="info"
        :closable="false"
      />
    </div>

    <div class="config-body">
      <!-- 左侧：字段池 -->
      <div class="field-pool">
        <h4>📦 可选字段池</h4>
        <div class="pool-list">
          <div
            v-for="field in fieldPool"
            :key="field.key"
            class="pool-item"
            :class="{ disabled: isFieldSelected(field.key) }"
            @click="addField(field)"
          >
            <el-icon><Plus /></el-icon>
            <span>{{ field.label }}</span>
            <el-tag v-if="isFieldSelected(field.key)" size="small" type="info">已添加</el-tag>
          </div>
        </div>
      </div>

      <!-- 右侧：已选字段 -->
      <div class="selected-fields">
        <h4>📋 封面字段（拖拽排序）</h4>
        <div class="selected-list" v-if="selectedFields.length">
          <draggable
            v-model="selectedFields"
            item-key="key"
            handle=".drag-handle"
            @end="onDragEnd"
          >
            <template #item="{ element, index }">
              <div class="selected-item">
                <el-icon class="drag-handle"><Rank /></el-icon>
                <span class="field-label">{{ element.label }}</span>
                <span class="field-key">{{ element.key }}</span>
                <div class="actions">
                  <el-switch
                    v-model="element.required"
                    size="small"
                    active-text="必填"
                    @change="onChange"
                  />
                  <el-button
                    type="danger"
                    link
                    :disabled="selectedFields.length <= 1"
                    @click="removeField(index)"
                  >
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
              </div>
            </template>
          </draggable>
        </div>
        <div v-else class="empty-state">
          <el-empty description="请从左侧添加字段" :image-size="80" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import draggable from 'vuedraggable'
import { Plus, Rank, Delete } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

// ===== 字段池（固定） =====
const fieldPool = [
  { key: 'title', label: '论文标题' },
  { key: 'studentName', label: '学生姓名' },
  { key: 'studentId', label: '学号' },
  { key: 'major', label: '专业' },
  { key: 'college', label: '学院' },
  { key: 'advisor', label: '指导教师' },
  { key: 'date', label: '提交日期' },
  { key: 'abstract', label: '摘要' },
  { key: 'keywords', label: '关键词' },
  { key: 'declaration', label: '原创声明' },
]

// ===== 已选字段（默认选中几个） =====
const selectedFields = ref([
  { key: 'title', label: '论文标题', required: true },
  { key: 'studentName', label: '学生姓名', required: true },
  { key: 'studentId', label: '学号', required: false },
  { key: 'major', label: '专业', required: false },
  { key: 'college', label: '学院', required: false },
  { key: 'advisor', label: '指导教师', required: false },
  { key: 'date', label: '提交日期', required: false },
])

// ===== 方法 =====
function isFieldSelected(key: string): boolean {
  return selectedFields.value.some(f => f.key === key)
}

function addField(field: typeof fieldPool[0]) {
  if (isFieldSelected(field.key)) {
    ElMessage.warning('该字段已添加')
    return
  }
  selectedFields.value.push({ ...field, required: false })
  onChange()
}

function removeField(index: number) {
  if (selectedFields.value.length <= 1) {
    ElMessage.warning('至少保留一个字段')
    return
  }
  selectedFields.value.splice(index, 1)
  onChange()
}

function onDragEnd() {
  onChange()
}

function onChange() {
  // 通知父组件配置已变更
  emit('change', getConfig())
}

// ===== 对外接口 =====
function getConfig() {
  return selectedFields.value.map(f => ({
    key: f.key,
    label: f.label,
    required: f.required,
  }))
}

function setConfig(config: any[]) {
  if (config && config.length) {
    selectedFields.value = config.map(c => ({
      key: c.key,
      label: c.label || fieldPool.find(f => f.key === c.key)?.label || c.key,
      required: c.required || false,
    }))
  }
}

// ===== 暴露给父组件 =====
defineExpose({ getConfig, setConfig })

const emit = defineEmits<{
  (e: 'change', config: any[]): void
}>()
</script>

<style scoped lang="scss">
.cover-config {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px 0;
}

.config-hint {
  margin-bottom: 16px;
}

.config-body {
  flex: 1;
  display: flex;
  gap: 40px;
  min-height: 400px;
}

// ---- 左栏 ----
.field-pool {
  flex: 1;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 16px;
  background: #fafafa;

  h4 { margin: 0 0 12px; font-size: 14px; }
}

.pool-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.pool-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;

  &:hover:not(.disabled) {
    border-color: #409EFF;
    background: #ecf5ff;
  }

  &.disabled {
    cursor: not-allowed;
    opacity: 0.6;
    background: #f5f7fa;
  }
}

// ---- 右栏 ----
.selected-fields {
  flex: 1.5;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 16px;
  background: #fff;

  h4 { margin: 0 0 12px; font-size: 14px; }
}

.selected-list {
  min-height: 200px;
}

.selected-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  margin-bottom: 8px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  transition: border-color 0.2s;

  &:hover { border-color: #409EFF; }
}

.drag-handle {
  cursor: grab;
  color: #909399;
  font-size: 16px;
  &:active { cursor: grabbing; }
}

.field-label { font-weight: 500; flex: 1; }
.field-key { color: #909399; font-size: 12px; background: #f5f7fa; padding: 2px 8px; border-radius: 4px; }

.actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.empty-state {
  padding: 40px 0;
}
</style>
