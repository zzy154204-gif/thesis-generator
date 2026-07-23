<template>
  <DefaultLayout>
    <div class="review-records">
      <div class="page-header">
        <h2>批阅记录</h2>
        <el-tag type="info" effect="plain">共 {{ total }} 条记录</el-tag>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-wrap">
        <el-skeleton :rows="6" animated />
      </div>

      <!-- 空状态 -->
      <el-empty v-else-if="!records.length" description="暂无批阅记录" />

      <!-- 数据表格 -->
      <el-table v-else :data="records" stripe @row-click="viewDetail" style="cursor: pointer">
        <el-table-column label="论文题目" prop="thesisTitle" min-width="200" show-overflow-tooltip />
        <el-table-column label="学生姓名" prop="studentName" width="120" />
        <el-table-column label="学生学号" prop="studentUsername" width="120" />
      <el-table-column label="批阅结果" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="row.action === 'REVIEWED' ? 'success' : 'warning'" effect="dark" size="small">
            {{ row.action === 'REVIEWED' ? '已通过' : '已退回' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="评分" width="80" align="center">
        <template #default="{ row }">
          <span v-if="row.score != null">{{ row.score }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="批阅时间" width="180" align="center">
        <template #default="{ row }">
          {{ formatTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" align="center">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click.stop="viewDetail(row)">查看批注</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrap" v-if="total > size">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="size"
        :total="total"
        layout="prev, pager, next"
        @current-change="loadRecords"
      />
    </div>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="'批阅详情 - ' + (selectedRecord?.thesisTitle || '')"
      width="700px"
      top="5vh"
      destroy-on-close
    >
      <template v-if="selectedRecord">
        <div class="detail-section">
          <h4>批阅信息</h4>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="论文题目" :span="2">{{ selectedRecord.thesisTitle }}</el-descriptions-item>
            <el-descriptions-item label="学生姓名">{{ selectedRecord.studentName }}</el-descriptions-item>
            <el-descriptions-item label="学生学号">{{ selectedRecord.studentUsername }}</el-descriptions-item>
            <el-descriptions-item label="批阅结果">
              <el-tag :type="selectedRecord.action === 'REVIEWED' ? 'success' : 'warning'" effect="dark" size="small">
                {{ selectedRecord.action === 'REVIEWED' ? '已通过' : '已退回' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="评分/等级">
              {{ selectedRecord.score ?? '-' }} / {{ selectedRecord.grade || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="批阅时间" :span="2">{{ formatTime(selectedRecord.createdAt) }}</el-descriptions-item>
            <el-descriptions-item v-if="selectedRecord.commentHtml" label="评语" :span="2">
              <div class="comment-html" v-html="selectedRecord.commentHtml" />
            </el-descriptions-item>
            <el-descriptions-item v-if="selectedRecord.returnReason" label="退回原因" :span="2">
              <div class="return-reason">{{ selectedRecord.returnReason }}</div>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 批注列表 -->
        <div class="detail-section">
          <h4>论文批注
            <el-button type="primary" size="small" @click="viewAnnotations" style="margin-left: 12px">
              查看完整批注
            </el-button>
          </h4>
          <div v-if="annotationsLoading" class="loading-mini">
            <el-skeleton :rows="2" animated />
          </div>
          <div v-else-if="!annotations.length" class="no-annotations">暂无批注内容</div>
          <div v-else class="annotations-list">
            <div v-for="ann in annotations.slice(0, 5)" :key="ann.id" class="annotation-item">
              <div class="ann-text">
                <el-tag size="small" type="info" effect="plain">选中: "{{ ann.selectedText }}"</el-tag>
              </div>
              <div class="ann-comment">{{ ann.content }}</div>
              <div class="ann-time">{{ formatTime(ann.createdAt) }}</div>
            </div>
            <div v-if="annotations.length > 5" class="more-hint">...还有 {{ annotations.length - 5 }} 条批注</div>
          </div>
        </div>
      </template>
      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="goToReview">查看完整论文</el-button>
      </template>
    </el-dialog>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { getTeacherReviewRecords, getReviewDetail } from '@/api/review'
import type { ReviewRecordItem, ReviewAnnotation } from '@/types'

const router = useRouter()
const records = ref<ReviewRecordItem[]>([])
const total = ref(0)
const currentPage = ref(1)
const size = ref(10)
const loading = ref(false)

const dialogVisible = ref(false)
const selectedRecord = ref<ReviewRecordItem | null>(null)
const annotations = ref<ReviewAnnotation[]>([])
const annotationsLoading = ref(false)

function formatTime(t?: string) {
  if (!t) return '-'
  const d = new Date(t)
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

async function loadRecords() {
  loading.value = true
  try {
    const res = await getTeacherReviewRecords({ page: currentPage.value, size: size.value })
    records.value = res.data.list
    total.value = res.data.total
  } catch {
    records.value = []
  } finally {
    loading.value = false
  }
}

async function viewDetail(row: ReviewRecordItem) {
  selectedRecord.value = row
  dialogVisible.value = true
  // 异步加载批注
  annotationsLoading.value = true
  annotations.value = []
  try {
    const res = await getReviewDetail(row.thesisId)
    annotations.value = res.data.annotations || []
  } catch {
    annotations.value = []
  } finally {
    annotationsLoading.value = false
  }
}

function viewAnnotations() {
  // 在dialog中展开所有批注，已经显示了
}

function goToReview() {
  if (!selectedRecord.value) return
  dialogVisible.value = false
  router.push(`/teacher/review/${selectedRecord.value.thesisId}`)
}

onMounted(loadRecords)
</script>

<style scoped lang="scss">
.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  h2 { margin: 0; font-size: 20px; }
}

.loading-wrap { padding: 40px 20px; }
.pagination-wrap { margin-top: 20px; display: flex; justify-content: center; }

.detail-section {
  margin-bottom: 20px;
  h4 {
    margin: 0 0 12px;
    font-size: 15px;
    color: var(--el-text-color-primary);
    border-left: 3px solid var(--el-color-primary);
    padding-left: 10px;
  }
}

.comment-html {
  padding: 8px 12px;
  background: var(--el-fill-color-lighter);
  border-radius: 6px;
  line-height: 1.7;
}

.return-reason {
  padding: 8px 12px;
  background: #fef0f0;
  border-radius: 6px;
  color: #f56c6c;
}

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

.no-annotations {
  color: var(--el-text-color-secondary);
  font-size: 14px;
  padding: 16px 0;
}

.more-hint {
  text-align: center;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  padding: 8px;
}

.loading-mini { padding: 12px 0; }
</style>
