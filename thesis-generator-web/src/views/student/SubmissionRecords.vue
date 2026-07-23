<template>
  <DefaultLayout>
    <div class="submission-records">
      <div class="page-header">
        <h2>提交记录</h2>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-wrap">
        <el-skeleton :rows="6" animated />
      </div>

      <!-- 空状态 -->
      <el-empty v-else-if="!records.length" description="暂无提交记录" />

      <!-- 数据表格 -->
      <el-table v-else :data="records" stripe @row-click="viewDetail" style="cursor: pointer">
        <el-table-column label="论文题目" prop="thesisTitle" min-width="220" show-overflow-tooltip />
        <el-table-column label="提交版本" width="90" align="center">
          <template #default="{ row }">第 {{ row.versionNumber }} 版</template>
      </el-table-column>
      <el-table-column label="提交时间" width="180" align="center">
        <template #default="{ row }">{{ formatTime(row.submittedAt) }}</template>
      </el-table-column>
      <el-table-column label="当前状态" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.thesisStatus)" effect="dark" size="small">
            {{ statusLabel(row.thesisStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" align="center">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click.stop="viewDetail(row)">查看详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="'提交详情 - ' + (selectedRecord?.thesisTitle || '')"
      width="700px"
      top="5vh"
      destroy-on-close
    >
      <template v-if="selectedRecord">
        <div class="detail-section">
          <h4>提交信息</h4>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="论文题目" :span="2">{{ selectedRecord.thesisTitle }}</el-descriptions-item>
            <el-descriptions-item label="提交版本">第 {{ selectedRecord.versionNumber }} 版</el-descriptions-item>
            <el-descriptions-item label="提交时间">{{ formatTime(selectedRecord.submittedAt) }}</el-descriptions-item>
            <el-descriptions-item label="当前状态" :span="2">
              <el-tag :type="statusTagType(selectedRecord.thesisStatus)" effect="dark">
                {{ statusLabel(selectedRecord.thesisStatus) }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 审批历史 -->
        <div class="detail-section">
          <h4>审批历史</h4>
          <div v-if="reviewLoading" class="loading-mini">
            <el-skeleton :rows="2" animated />
          </div>
          <div v-else-if="!reviewHistory.length" class="no-data">暂无审批记录</div>
          <div v-else class="review-timeline">
            <div v-for="review in reviewHistory" :key="review.id" class="review-item">
              <div class="review-dot" :class="review.action === 'REVIEWED' ? 'dot-pass' : 'dot-return'" />
              <div class="review-body">
                <div class="review-header">
                  <el-tag :type="review.action === 'REVIEWED' ? 'success' : 'warning'" size="small" effect="dark">
                    {{ review.action === 'REVIEWED' ? '已通过' : '已退回' }}
                  </el-tag>
                  <span v-if="review.score != null" class="review-score">评分：{{ review.score }}</span>
                  <span class="review-time">{{ formatTime(review.createdAt) }}</span>
                </div>
                <div v-if="review.commentHtml" class="review-comment" v-html="review.commentHtml" />
                <div v-if="review.returnReason" class="review-reason">
                  <el-alert :title="'退回原因：' + review.returnReason" type="warning" :closable="false" show-icon />
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 批注列表 -->
        <div class="detail-section">
          <h4>论文批注</h4>
          <div v-if="annotationsLoading" class="loading-mini">
            <el-skeleton :rows="2" animated />
          </div>
          <div v-else-if="!annotations.length" class="no-data">暂无批注</div>
          <div v-else class="annotations-list">
            <div v-for="ann in annotations" :key="ann.id" class="annotation-item">
              <div class="ann-text">
                <el-tag size="small" type="info" effect="plain">选中: "{{ ann.selectedText }}"</el-tag>
              </div>
              <div class="ann-comment">{{ ann.content }}</div>
              <div class="ann-time">{{ formatTime(ann.createdAt) }}</div>
            </div>
          </div>
        </div>
      </template>
      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="goToEditor">前往编辑</el-button>
      </template>
    </el-dialog>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { getStudentSubmissionRecords } from '@/api/submission'
import { getReviewHistory } from '@/api/review'
import { getReviewFeedback } from '@/api/paper'
import type { SubmissionRecordItem, ReviewRecord, Annotation } from '@/types'

const router = useRouter()
const records = ref<SubmissionRecordItem[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const selectedRecord = ref<SubmissionRecordItem | null>(null)
const reviewHistory = ref<ReviewRecord[]>([])
const reviewLoading = ref(false)
const annotations = ref<Annotation[]>([])
const annotationsLoading = ref(false)

function formatTime(t?: string) {
  if (!t) return '-'
  const d = new Date(t)
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    DRAFT: '草稿', COMPLETED: '已完成', SUBMITTED: '已提交',
    REVIEWED: '已通过', RETURNED: '已退回', GENERATING: '生成中',
  }
  return map[status] || status
}

function statusTagType(status: string) {
  const map: Record<string, string> = {
    DRAFT: 'info', COMPLETED: 'success', SUBMITTED: 'warning',
    REVIEWED: 'success', RETURNED: 'danger', GENERATING: '',
  }
  return map[status] || 'info'
}

async function loadRecords() {
  loading.value = true
  try {
    const res = await getStudentSubmissionRecords()
    records.value = res.data
  } catch {
    records.value = []
  } finally {
    loading.value = false
  }
}

async function viewDetail(row: SubmissionRecordItem) {
  selectedRecord.value = row
  dialogVisible.value = true
  reviewHistory.value = []
  annotations.value = []

  // 加载审批历史
  reviewLoading.value = true
  try {
    const res = await getReviewHistory(row.thesisId)
    reviewHistory.value = res.data
  } catch {
    reviewHistory.value = []
  } finally {
    reviewLoading.value = false
  }

  // 加载批注
  annotationsLoading.value = true
  try {
    const res = await getReviewFeedback(row.thesisId)
    if (res.data?.annotations) {
      annotations.value = res.data.annotations
    }
  } catch {
    annotations.value = []
  } finally {
    annotationsLoading.value = false
  }
}

function goToEditor() {
  if (!selectedRecord.value) return
  dialogVisible.value = false
  router.push(`/editor/${selectedRecord.value.thesisId}`)
}

onMounted(loadRecords)
</script>

<style scoped lang="scss">
.page-header {
  margin-bottom: 20px;
  h2 { margin: 0; font-size: 20px; }
}

.loading-wrap { padding: 40px 20px; }

.detail-section {
  margin-bottom: 24px;
  h4 {
    margin: 0 0 12px;
    font-size: 15px;
    color: var(--el-text-color-primary);
    border-left: 3px solid var(--el-color-primary);
    padding-left: 10px;
  }
}

.no-data {
  color: var(--el-text-color-secondary);
  font-size: 14px;
  padding: 16px 0;
}

// 审批时间线
.review-timeline {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.review-item {
  display: flex;
  gap: 12px;
  padding-bottom: 16px;
  position: relative;

  &:not(:last-child)::before {
    content: '';
    position: absolute;
    left: 7px;
    top: 18px;
    bottom: 0;
    width: 2px;
    background: var(--el-border-color-light);
  }
}

.review-dot {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 2px;

  &.dot-pass { background: var(--el-color-success); }
  &.dot-return { background: var(--el-color-warning); }
}

.review-body {
  flex: 1;
  min-width: 0;
}

.review-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;

  .review-score {
    font-size: 13px;
    color: var(--el-color-primary);
    font-weight: 500;
  }
  .review-time {
    font-size: 12px;
    color: var(--el-text-color-secondary);
    margin-left: auto;
  }
}

.review-comment {
  padding: 8px 12px;
  background: var(--el-fill-color-lighter);
  border-radius: 6px;
  margin-top: 6px;
  line-height: 1.7;
  font-size: 14px;
}

.review-reason {
  margin-top: 8px;
}

// 批注列表
.annotations-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.annotation-item {
  padding: 10px 12px;
  background: var(--el-fill-color-lighter);
  border-radius: 6px;
  border-left: 3px solid var(--el-color-warning);

  .ann-text { margin-bottom: 6px; }
  .ann-comment { font-size: 14px; line-height: 1.6; }
  .ann-time { font-size: 12px; color: var(--el-text-color-secondary); margin-top: 4px; }
}

.loading-mini { padding: 12px 0; }
</style>
