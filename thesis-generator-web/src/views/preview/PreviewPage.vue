<template>
  <DefaultLayout>
    <div class="preview-page">
      <div class="preview-toolbar">
        <el-button text :icon="ArrowLeft" @click="handleClose">关闭预览</el-button>
        <span class="title">{{ paper?.title || '论文预览' }}</span>
        <el-button type="primary" :icon="Download" @click="showExportDialog = true">导出</el-button>
      </div>
      <div class="preview-content">
        <div class="paper-a4" v-if="previewHtml" v-html="previewHtml" />
        <el-empty v-else description="暂无预览内容" :image-size="160" />
      </div>

      <!-- 导出对话框 -->
      <el-dialog v-model="showExportDialog" title="导出论文" width="480px">
        <el-form label-position="top">
          <el-form-item label="导出格式">
            <el-radio-group v-model="exportFormat">
              <el-radio value="PDF">PDF</el-radio>
              <el-radio value="DOCX">DOCX</el-radio>
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
            {{ exporting ? '提交中...' : '开始导出' }}
          </el-button>
        </template>
      </el-dialog>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Download } from '@element-plus/icons-vue'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { usePaperStore } from '@/stores/paper'
import { ElMessage } from 'element-plus'
import { submitExport, getExportStatus, downloadExport } from '@/api/export'

const route = useRoute()
const router = useRouter()
const paperStore = usePaperStore()

const paper = computed(() => paperStore.currentPaper)
const previewHtml = ref('')
const showExportDialog = ref(false)
const exporting = ref(false)
const exportFormat = ref<'PDF' | 'DOCX'>('PDF')
const exportOptions = reactive({ cover: true, toc: true, references: true })

/** 轮询导出状态直到完成 */
async function pollExportTask(taskId: string, maxRetries = 30, intervalMs = 2000): Promise<string> {
  for (let i = 0; i < maxRetries; i++) {
    await new Promise((r) => setTimeout(r, intervalMs))
    const statusRes = await getExportStatus(taskId)
    const task = statusRes.data
    if (task.status === 'COMPLETED') {
      const dlRes = await downloadExport(taskId)
      return dlRes.data.downloadUrl
    }
    if (task.status === 'FAILED') {
      throw new Error(task.errorMessage || '导出失败')
    }
  }
  throw new Error('导出超时，请稍后重试')
}

/** 触发下载 */
function triggerDownload(url: string, filename: string) {
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
}

async function handleExport() {
  exporting.value = true
  try {
    const paperId = Number(route.params.paperId)
    const res = await submitExport(paperId, {
      format: exportFormat.value,
      scope: 'all',
      options: {
        cover: exportOptions.cover,
        toc: exportOptions.toc,
        references: exportOptions.references,
      },
    })
    const taskId = res.data.taskId
    ElMessage.success('导出任务已提交，正在生成文件...')

    const downloadUrl = await pollExportTask(taskId)
    const ext = exportFormat.value === 'PDF' ? 'pdf' : 'docx'
    const filename = `${paper.value?.title || '论文'}.${ext}`
    triggerDownload(downloadUrl, filename)
    ElMessage.success('导出完成，文件开始下载')
    showExportDialog.value = false
  } catch (e: any) {
    ElMessage.error(e?.message || '导出失败，请稍后重试')
  } finally {
    exporting.value = false
  }
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
