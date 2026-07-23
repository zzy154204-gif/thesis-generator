<template>
  <DefaultLayout>
    <div class="page" v-if="detail">
      <!-- 工具栏 -->
      <div class="toolbar">
        <el-button text :icon="ArrowLeft" @click="router.push('/teacher/review')">返回列表</el-button>
        <el-button type="primary" @click="showApproveDialog">通过</el-button>
        <el-button type="danger" @click="showReturnDialog">退回</el-button>
        <el-button @click="saveDraft">暂存评语</el-button>
      </div>

      <!-- 论文信息摘要 -->
      <el-card class="summary-card">
        <div class="summary">
          <div><label>标题：</label><strong>{{ detail.paper.title }}</strong></div>
          <div><label>学生：</label>{{ detail.paper.studentName }}（{{ detail.paper.studentId }}）</div>
          <div v-if="detail.paper.templateName"><label>模板：</label>{{ detail.paper.templateName }}</div>
          <div><label>状态：</label><el-tag :type="statusTagType(detail.paper.status)" size="small">{{ statusLabel(detail.paper.status) }}</el-tag></div>
          <div><label>提交时间：</label>{{ relativeTime(detail.paper.submittedAt) }}</div>
        </div>
      </el-card>

      <div class="review-layout">
        <!-- 论文内容 -->
        <div class="content-panel">
          <el-card v-if="sections.length">
            <div v-for="sec in sections" :key="sec.id" :id="'sec-' + sec.id" class="section">
              <h3 class="sec-title">{{ sec.title }}</h3>
              <div class="sec-content" v-html="sec.content || emptyHtml" />
            </div>
          </el-card>
          <el-empty v-else description="暂无章节内容" />
        </div>

        <!-- 批注面板 -->
        <div class="annotation-panel">
          <el-card>
            <template #header>
              <div class="ann-header">
                <span>批注列表 ({{ annotations.length }})</span>
                <el-button type="primary" size="small" @click="showAddAnnotation = true" :disabled="!sections.length">+ 新增批注</el-button>
              </div>
            </template>

            <!-- 新增批注表单 -->
            <div v-if="showAddAnnotation" class="ann-form">
              <el-form :model="annForm" size="small" label-width="0">
                <el-form-item>
                  <el-select v-model="annForm.sectionId" placeholder="选择章节" style="width:100%">
                    <el-option v-for="s in sections" :key="s.id" :label="s.title" :value="s.id" />
                  </el-select>
                </el-form-item>
                <el-form-item>
                  <el-input v-model="annForm.selectedText" placeholder="被批注的文本（选填）" />
                </el-form-item>
                <el-form-item>
                  <el-input v-model="annForm.content" type="textarea" :rows="3" placeholder="输入批注内容..." />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" size="small" :loading="annSubmitting" @click="handleAddAnnotation">提交批注</el-button>
                  <el-button size="small" @click="showAddAnnotation = false">取消</el-button>
                </el-form-item>
              </el-form>
            </div>

            <div v-if="!annotations.length" style="font-size:13px;color:var(--el-text-color-secondary);text-align:center;padding:20px">暂无批注</div>
            <div v-for="a in annotations" :key="a.id" class="ann-item">
              <div class="ann-text">{{ a.content }}</div>
              <div v-if="a.selectedText" class="ann-quote">引用：{{ a.selectedText }}</div>
              <div class="ann-meta">
                <span>{{ a.author }}</span>
                <span>{{ relativeTime(a.createdAt) }}</span>
                <el-button text type="danger" size="small" @click="handleDeleteAnnotation(a.id)">删除</el-button>
              </div>
            </div>
          </el-card>
        </div>
      </div>

      <!-- 通过对话框 -->
      <el-dialog v-model="approveVisible" title="通过论文" width="450px">
        <el-form :model="reviewForm" label-width="80px">
          <el-form-item label="评分">
            <el-input-number v-model="reviewForm.score" :min="0" :max="100" />
          </el-form-item>
          <el-form-item label="等级">
            <el-input v-model="reviewForm.grade" placeholder="如 A, B+, 优秀" />
          </el-form-item>
          <el-form-item label="评语">
            <el-input v-model="reviewForm.comment" type="textarea" :rows="4" placeholder="评语（可选）" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="approveVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleApprove">确认通过</el-button>
        </template>
      </el-dialog>

      <!-- 退回对话框 -->
      <el-dialog v-model="returnVisible" title="退回论文" width="450px">
        <el-form :model="reviewForm" label-width="80px">
          <el-form-item label="退回原因" required>
            <el-input v-model="reviewForm.returnReason" type="textarea" :rows="4" placeholder="请说明退回原因" />
          </el-form-item>
          <el-form-item label="评语">
            <el-input v-model="reviewForm.comment" type="textarea" :rows="3" placeholder="评语（可选）" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="returnVisible = false">取消</el-button>
          <el-button type="danger" :loading="submitting" @click="handleReturn">确认退回</el-button>
        </template>
      </el-dialog>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { statusLabel, statusTagType, relativeTime } from '@/utils/format'
import { getReviewDetail, saveReviewDraft, returnPaper, approvePaper } from '@/api/review'
import { getSections } from '@/api/section'
import { deleteAnnotation, createAnnotation } from '@/api/annotation'
import type { ThesisSection } from '@/types'

const route = useRoute()
const router = useRouter()
const paperId = Number(route.params.id)

const emptyHtml = '<p style="color:#ccc">（空）</p>'
const detail = ref<any>(null)
const sections = ref<ThesisSection[]>([])
const annotations = ref<any[]>([])
const approveVisible = ref(false)
const returnVisible = ref(false)
const submitting = ref(false)

const reviewForm = reactive({
  score: 80, grade: '', comment: '', returnReason: '',
})

// ---- 新增批注 ----
const showAddAnnotation = ref(false)
const annSubmitting = ref(false)
const annForm = reactive({
  sectionId: null as number | null,
  selectedText: '',
  content: '',
})

async function handleAddAnnotation() {
  const sectionId = annForm.sectionId || sections.value[0]?.id
  if (!sectionId) { ElMessage.warning('请先选择章节'); return }
  if (!annForm.content.trim()) { ElMessage.warning('请输入批注内容'); return }
  annSubmitting.value = true
  try {
    await createAnnotation({
      thesisId: paperId,
      sectionId,
      startOffset: 0,
      textLength: 1,
      selectedText: annForm.selectedText.trim() || undefined,
      content: annForm.content.trim(),
    })
    ElMessage.success('批注已添加')
    showAddAnnotation.value = false
    annForm.sectionId = null
    annForm.selectedText = ''
    annForm.content = ''
    // 刷新批注列表
    const detailRes = await getReviewDetail(paperId)
    annotations.value = detailRes.data.annotations || []
  } catch { /* handled */ }
  finally { annSubmitting.value = false }
}

async function fetchData() {
  try {
    const [detailRes, secRes] = await Promise.all([
      getReviewDetail(paperId),
      getSections(paperId),
    ])
    detail.value = detailRes.data
    annotations.value = detailRes.data.annotations || []
    sections.value = secRes.data || []
  } catch { /* handled */ }
}

async function saveDraft() {
  try {
    await saveReviewDraft(paperId, {
      comment: reviewForm.comment,
      score: reviewForm.score,
      grade: reviewForm.grade,
    })
    ElMessage.success('已暂存')
  } catch { /* handled */ }
}

function showApproveDialog() { approveVisible.value = true }
function showReturnDialog() {
  reviewForm.returnReason = ''
  returnVisible.value = true
}

async function handleApprove() {
  submitting.value = true
  try {
    await approvePaper(paperId, {
      comment: reviewForm.comment,
      score: reviewForm.score,
      grade: reviewForm.grade,
    })
    ElMessage.success('已通过')
    approveVisible.value = false
    router.push('/teacher/review')
  } catch { /* handled */ }
  finally { submitting.value = false }
}

async function handleReturn() {
  if (!reviewForm.returnReason) { ElMessage.warning('请填写退回原因'); return }
  submitting.value = true
  try {
    await returnPaper(paperId, {
      comment: reviewForm.comment,
      reason: reviewForm.returnReason,
    })
    ElMessage.success('已退回')
    returnVisible.value = false
    router.push('/teacher/review')
  } catch { /* handled */ }
  finally { submitting.value = false }
}

async function handleDeleteAnnotation(id: number) {
  try {
    await deleteAnnotation(id)
    ElMessage.success('已删除')
    annotations.value = annotations.value.filter(a => a.id !== id)
  } catch { /* handled */ }
}

onMounted(fetchData)
</script>

<style scoped lang="scss">
.toolbar { display: flex; gap: 8px; margin-bottom: 16px; flex-wrap: wrap; }

.summary-card { margin-bottom: 16px; }
.summary { display: flex; flex-wrap: wrap; gap: 16px; font-size: 14px;
  label { color: var(--el-text-color-secondary); }
}

.review-layout { display: flex; gap: 16px;
  @media (max-width: 900px) { flex-direction: column; }
}

.content-panel { flex: 1; min-width: 0; }
.section { margin-bottom: 24px; &:not(:last-child) { border-bottom: 1px solid var(--el-border-color-light); padding-bottom: 24px; } }
.sec-title { font-size: 17px; font-weight: 600; margin-bottom: 12px; }
.sec-content { font-size: 14px; line-height: 1.8;
  :deep(img) { max-width: 100%; }
  :deep(table) { width: 100%; border-collapse: collapse; th, td { border: 1px solid var(--el-border-color); padding: 6px 10px; } }
}

.annotation-panel { width: 320px; flex-shrink: 0;
  @media (max-width: 900px) { width: 100%; }
}
.ann-header { display: flex; align-items: center; justify-content: space-between; }
.ann-form { padding: 12px 0; border-bottom: 1px solid var(--el-border-color-extra-light); margin-bottom: 8px; }
.ann-item { padding: 10px 0; &:not(:last-child) { border-bottom: 1px solid var(--el-border-color-extra-light); } }
.ann-text { font-size: 13px; margin-bottom: 4px; color: var(--el-text-color-primary); }
.ann-quote { font-size: 12px; color: var(--el-color-primary); background: var(--el-color-primary-light-9); padding: 2px 6px; border-radius: 4px; margin-bottom: 4px; display: inline-block; }
.ann-meta { font-size: 12px; color: var(--el-text-color-secondary); display: flex; gap: 8px; align-items: center; }
</style>
