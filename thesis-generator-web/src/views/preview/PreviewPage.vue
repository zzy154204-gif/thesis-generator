<template>
  <DefaultLayout>
    <div class="preview-page">
      <div class="preview-toolbar">
        <el-button text :icon="ArrowLeft" @click="handleClose">关闭预览</el-button>
        <span class="title">{{ paper?.title || '论文预览' }}</span>
        <el-button type="primary" :icon="Download" @click="handleExport">导出</el-button>
      </div>
      <div class="preview-content">
        <div class="paper-a4" v-if="previewHtml" v-html="previewHtml" />
        <el-empty v-else description="暂无预览内容" :image-size="160" />
      </div>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Download } from '@element-plus/icons-vue'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { usePaperStore } from '@/stores/paper'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const paperStore = usePaperStore()

const paper = computed(() => paperStore.currentPaper)
const previewHtml = ref('')

function handleExport() {
  // TODO: 打开导出对话框或直接触发导出
  ElMessage.info('导出功能开发中')
}

function handleClose() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/papers')
  }
}

onMounted(async () => {
  const paperId = Number(route.params.paperId)
  if (paperId) {
    await paperStore.fetchPaper(paperId)
    await paperStore.fetchSections(paperId)
    // 生成简单的 HTML 预览
    previewHtml.value = paperStore.sections
      .map((s) => `<h2>${s.title}</h2><div>${s.content || '（暂无内容）'}</div>`)
      .join('<br/>')
  }
})
</script>

<style scoped lang="scss">
.preview-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 56px);
}

.preview-toolbar {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  .title { font-size: 15px; font-weight: 600; }
}

.preview-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  justify-content: center;
  background: #e9ecef;
}

.paper-a4 {
  width: 210mm;
  min-height: 297mm;
  padding: 25mm 20mm;
  background: #fff;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
  font-size: 14px;
  line-height: 1.8;
}
</style>
