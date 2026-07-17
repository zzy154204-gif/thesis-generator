<!-- src/views/teacher/ReviewHistory.vue -->
<template>
  <DefaultLayout>
    <div class="review-history">
      <div class="page-header">
        <h3>📋 批阅历史</h3>
        <el-button @click="router.back()">返回</el-button>
      </div>

      <!-- 统计 -->
      <div class="stats-row">
        <el-statistic title="已批阅总数" :value="stats.total" />
        <el-statistic title="已通过" :value="stats.approved" />
        <el-statistic title="已退回" :value="stats.returned" />
        <el-statistic title="平均分" :value="stats.avgScore" />
      </div>

      <!-- 列表 -->
      <el-table :data="tableData" border v-loading="loading">
        <el-table-column prop="paperTitle" label="论文标题" min-width="180" />
        <el-table-column prop="studentName" label="学生" width="100" />
        <el-table-column prop="studentId" label="学号" width="120" />
        <el-table-column label="评分" width="100">
          <template #default="{ row }">
            <span v-if="row.score">{{ row.score }}分</span>
            <span v-else style="color:#909399;">-</span>
          </template>
        </el-table-column>
        <el-table-column label="等级" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.grade" :type="getGradeType(row.grade)">
              {{ row.grade }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'APPROVED' ? 'success' : 'warning'">
              {{ row.status === 'APPROVED' ? '已通过' : '已退回' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reviewedAt" label="批阅时间" width="170">
          <template #default="{ row }">
            {{ formatDate(row.reviewedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewDetail(row.paperId)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="fetchData"
        @size-change="fetchData"
        style="margin-top:20px;justify-content:flex-end"
      />
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { getReviewHistory } from '@/api/review'
import type { Annotation } from '@/api/review'

// ===== 类型定义 =====
interface HistoryItem {
  paperId: number
  paperTitle: string
  studentName: string
  studentId: string
  score: number | null
  grade: string | null
  status: 'APPROVED' | 'RETURNED'
  reviewedAt: string
}

const router = useRouter()
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref<HistoryItem[]>([])

const stats = reactive({
  total: 0,
  approved: 0,
  returned: 0,
  avgScore: 0,
})

// ===== 格式化日期（支持 undefined/null） =====
function formatDate(date: string | undefined | null) {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

// ===== 等级标签颜色 =====
function getGradeType(grade: string) {
  const map: Record<string, string> = {
    '优': 'success',
    '良': 'primary',
    '中': 'warning',
    '及格': 'info',
    '不及格': 'danger'
  }
  return map[grade] || 'info'
}

// ===== 获取数据 =====
async function fetchData() {
  loading.value = true
  try {
    // TODO: 调用真实 API
    // const res = await getReviewHistory()
    // tableData.value = res.data
    // 计算统计信息...

    // 模拟数据（替换上面的真实 API 调用）
    await new Promise((r) => setTimeout(r, 500))
    tableData.value = [
      {
        paperId: 1,
        paperTitle: '基于深度学习的图像分割研究',
        studentName: '张三',
        studentId: '2024001',
        score: 85,
        grade: '良',
        status: 'APPROVED',
        reviewedAt: '2026-07-15T16:00:00',
      },
      {
        paperId: 2,
        paperTitle: '区块链技术在供应链中的应用',
        studentName: '李四',
        studentId: '2024002',
        score: 62,
        grade: '及格',
        status: 'APPROVED',
        reviewedAt: '2026-07-14T11:00:00',
      },
      {
        paperId: 3,
        paperTitle: '基于Spring Boot的在线考试系统',
        studentName: '王五',
        studentId: '2024003',
        score: null,
        grade: null,
        status: 'RETURNED',
        reviewedAt: '2026-07-13T17:00:00',
      },
    ]
    total.value = tableData.value.length

    // 计算统计
    stats.total = tableData.value.length
    stats.approved = tableData.value.filter((d) => d.status === 'APPROVED').length
    stats.returned = tableData.value.filter((d) => d.status === 'RETURNED').length
    const scored = tableData.value.filter((d) => d.score !== null)
    stats.avgScore =
      scored.length > 0
        ? Math.round((scored.reduce((s, d) => s + (d.score || 0), 0) / scored.length) * 10) / 10
        : 0
  } catch {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// ===== 查看详情 =====
function viewDetail(paperId: number) {
  router.push(`/teacher/review/${paperId}`)
}

// ===== 生命周期 =====
onMounted(fetchData)
</script>

<style scoped lang="scss">
.review-history {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    h3 {
      margin: 0;
    }
  }

  .stats-row {
    display: flex;
    gap: 60px;
    padding: 20px 24px;
    background: #fff;
    border-radius: 8px;
    margin-bottom: 20px;
    border: 1px solid #ebeef5;
  }
}
</style>
