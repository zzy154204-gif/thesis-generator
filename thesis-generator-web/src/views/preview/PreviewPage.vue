<template>
  <DefaultLayout>
    <div class="page" v-if="paper">
      <div class="toolbar">
        <el-button text :icon="ArrowLeft" @click="router.push(`/editor/${paperId}`)">返回编辑</el-button>
        <el-button text :icon="Edit" @click="router.push(`/editor/${paperId}`)">继续编辑</el-button>
        <el-button :icon="Download" @click="handleExport('DOCX')">导出 DOCX</el-button>
        <el-button :icon="Download" @click="handleExport('PDF')">导出 PDF</el-button>
        <el-button type="primary" :icon="Upload" @click="handleSubmit" v-if="paper.status !== 'SUBMITTED'">提交</el-button>
        <el-button :icon="Close" @click="handleWithdraw" v-if="paper.status === 'SUBMITTED'">撤回提交</el-button>
      </div>

      <el-card class="preview-card">
        <div class="paper-header">
          <h1 class="paper-title">{{ paper.title }}</h1>
          <div class="paper-meta">
            <span>状态：<el-tag :type="statusTagType(paper.status)" size="small">{{ statusLabel(paper.status) }}</el-tag></span>
            <span>更新：{{ relativeTime(paper.updatedAt) }}</span>
          </div>
        </div>

        <el-divider />

        <div v-if="!sections.length" class="empty"><el-empty description="暂无章节内容" :image-size="100" /></div>

        <div v-for="section in sections" :key="section.id" class="section-block">
          <h2 class="section-title">{{ section.title }}</h2>
          <div class="section-content" v-html="section.content || emptyHtml" />
        </div>
      </el-card>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Edit, Download, Upload, Close } from '@element-plus/icons-vue'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { usePaperStore } from '@/stores/paper'
import { statusLabel, statusTagType, relativeTime } from '@/utils/format'
import { downloadExport } from '@/api/paper'
import { submitThesis, withdrawSubmission } from '@/api/submission'
import type { ThesisSection } from '@/types'

const route = useRoute()
const router = useRouter()
const store = usePaperStore()
const paperId = Number(route.params.paperId)

const emptyHtml = ref('<p style="color:#ccc">（空）</p>')
const paper = ref(store.currentPaper)
const sections = ref<ThesisSection[]>([])

async function handleExport(format: 'DOCX' | 'PDF') {
  try {
    await downloadExport(paperId, format)
    ElMessage.success(`论文已导出为 ${format}`)
  } catch (e: any) {
    ElMessage.error(e.message || '导出失败')
  }
}

async function handleSubmit() {
  try {
    await ElMessageBox.confirm('确定提交论文吗？', '确认', { type: 'info', confirmButtonText: '提交', cancelButtonText: '取消' })
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

onMounted(async () => {
  await store.fetchPaper(paperId)
  paper.value = store.currentPaper
  await store.fetchSections(paperId)
  sections.value = store.sections
})
</script>

<style scoped lang="scss">
.page { max-width: 800px; margin: 0 auto; }

.toolbar { display: flex; gap: 8px; margin-bottom: 20px; flex-wrap: wrap; }

.preview-card { padding: 0; }
.paper-header { text-align: center; padding: 32px 40px 0; }
.paper-title { font-size: 22px; font-weight: 700; color: var(--el-text-color-primary); margin-bottom: 12px; }
.paper-meta { display: flex; justify-content: center; gap: 20px; font-size: 13px; color: var(--el-text-color-secondary); }

.empty { padding: 60px 0; }

.section-block { padding: 24px 40px; &:not(:last-child) { border-bottom: 1px solid var(--el-border-color-extra-light); } }
.section-title { font-size: 18px; font-weight: 600; color: var(--el-text-color-primary); margin-bottom: 16px; }
.section-content { font-size: 14px; line-height: 1.8; color: var(--el-text-color-regular);
  :deep(img) { max-width: 100%; }
  :deep(table) { width: 100%; border-collapse: collapse; margin: 1em 0;
    th, td { border: 1px solid var(--el-border-color); padding: 8px; }
    th { background: var(--el-fill-color); }
  }
}
</style>
