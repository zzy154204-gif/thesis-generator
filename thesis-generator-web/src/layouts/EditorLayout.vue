<template>
  <div class="editor-layout">
    <!-- ===== 1. 顶栏 ===== -->
    <header class="editor-header">
      <div class="header-left">
        <el-button text @click="goBack">
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
        <span class="paper-title">{{ paperTitle || '未命名论文' }}</span>
        <el-tag :type="statusTagType" size="small">{{ statusText }}</el-tag>
      </div>
      <div class="header-right">
        <el-button size="small" @click="emit('save')">保存草稿</el-button>
        <el-button size="small" type="primary" plain @click="emit('preview')">预览</el-button>
        <el-button size="small" type="success" @click="emit('submit')">提交批阅</el-button>
        <el-divider direction="vertical" />
        <el-dropdown @command="handleCommand">
          <el-avatar :size="28" icon="UserFilled" />
        </el-dropdown>
      </div>
    </header>

    <!-- ===== 2. 主体（三栏） ===== -->
    <div class="editor-body">
      <!-- 左栏：章节树 -->
      <aside class="editor-left" :style="{ width: leftWidth + 'px' }">
        <slot name="sidebar" />
      </aside>

      <!-- 中栏：编辑器核心 -->
      <main class="editor-center">
        <div class="editor-toolbar">
          <slot name="toolbar" />
        </div>
        <div class="editor-content">
          <slot name="content" />
        </div>
      </main>

      <!-- 右栏：面板 -->
      <aside class="editor-right" :style="{ width: rightWidth + 'px' }" v-show="!isRightCollapsed">
        <div class="panel-header">
          <span>{{ rightPanelTitle }}</span>
          <el-icon class="close-btn" @click="isRightCollapsed = true"><Close /></el-icon>
        </div>
        <div class="panel-body">
          <slot name="panel" />
        </div>
      </aside>

      <div class="right-toggle-btn" v-if="isRightCollapsed" @click="isRightCollapsed = false">
        <el-icon><DArrowLeft /></el-icon>
      </div>
    </div>

    <!-- ===== 3. 底栏 ===== -->
    <footer class="editor-footer">
      <span>最后保存：{{ lastSaveTime || '--:--' }}</span>
      <span class="divider">|</span>
      <span>字数：{{ wordCount || 0 }}</span>
      <span class="divider">|</span>
      <span v-if="isDirty" class="dirty-tag">● 未保存</span>
      <span v-else class="saved-tag">✓ 已同步</span>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, Close, DArrowLeft } from '@element-plus/icons-vue'

// Props
const props = defineProps<{
  paperTitle?: string
  status?: 'DRAFT' | 'SUBMITTED' | 'REVIEWING' | 'RETURNED' | 'APPROVED'
  lastSaveTime?: string
  wordCount?: number
  isDirty?: boolean
  leftWidth?: number
  rightWidth?: number
  rightPanelTitle?: string
}>()

// Emits
const emit = defineEmits<{
  (e: 'save'): void
  (e: 'preview'): void
  (e: 'submit'): void
  (e: 'goBack'): void
}>()

// 状态
const router = useRouter()
const isRightCollapsed = ref(false)

// 计算属性
const statusText = computed(() => ({
  DRAFT: '草稿',
  SUBMITTED: '已提交',
  REVIEWING: '批阅中',
  RETURNED: '已退回',
  APPROVED: '已通过'
})[props.status || 'DRAFT'] || '草稿')

const statusTagType = computed(() => ({
  DRAFT: 'info',
  SUBMITTED: 'warning',
  REVIEWING: 'primary',
  RETURNED: 'danger',
  APPROVED: 'success'
})[props.status || 'DRAFT'] || 'info')

const goBack = () => emit('goBack') || router.push('/papers')
const handleCommand = (cmd: string) => {
  if (cmd === 'logout') {
    // 退出逻辑
  }
}
</script>

<style scoped lang="scss">
.editor-layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

// ---- 顶栏 ----
.editor-header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;
    .paper-title { font-size: 16px; font-weight: 600; color: #303133; }
  }
  .header-right { display: flex; align-items: center; gap: 10px; }
}

// ---- 主体 ----
.editor-body {
  flex: 1;
  display: flex;
  overflow: hidden;
  position: relative;
}

.editor-left {
  background: #fff;
  border-right: 1px solid #e4e7ed;
  overflow-y: auto;
  flex-shrink: 0;
  padding: 12px 0;
  width: v-bind('leftWidth + "px"');
}

.editor-center {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  margin: 12px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
  overflow: hidden;
  .editor-toolbar {
    padding: 8px 16px;
    border-bottom: 1px solid #e4e7ed;
    flex-shrink: 0;
    background: #fafafa;
  }
  .editor-content {
    flex: 1;
    overflow-y: auto;
    padding: 24px 40px;
  }
}

.editor-right {
  background: #fff;
  border-left: 1px solid #e4e7ed;
  width: v-bind('rightWidth + "px"');
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  transition: width 0.3s;
  .panel-header {
    display: flex;
    justify-content: space-between;
    padding: 12px 16px;
    border-bottom: 1px solid #e4e7ed;
    font-weight: 600;
    .close-btn { cursor: pointer; color: #909399; &:hover { color: #303133; } }
  }
  .panel-body { flex: 1; overflow-y: auto; padding: 12px 16px; }
}

.right-toggle-btn {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  background: #fff;
  border: 1px solid #e4e7ed;
  border-right: none;
  padding: 12px 4px;
  border-radius: 6px 0 0 6px;
  cursor: pointer;
}

// ---- 底栏 ----
.editor-footer {
  height: 32px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 24px;
  background: #fff;
  border-top: 1px solid #e4e7ed;
  font-size: 13px;
  color: #909399;
  flex-shrink: 0;
  .dirty-tag { color: #e6a23c; }
  .saved-tag { color: #67c23a; }
}
</style>
