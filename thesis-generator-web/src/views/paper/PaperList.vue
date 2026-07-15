<template>
  <DefaultLayout>
    <div class="paper-list-page">
      <div class="page-header">
        <div class="header-actions">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索论文标题..."
            :prefix-icon="Search"
            clearable
            class="search-input"
            @input="onSearch"
          />
          <el-select v-model="sortBy" placeholder="排序方式" class="sort-select" @change="fetchPapers">
            <el-option label="最近修改" value="updatedAt" />
            <el-option label="创建时间" value="createdAt" />
            <el-option label="标题" value="title" />
          </el-select>
        </div>
        <el-button type="primary" :icon="Plus" @click="router.push('/papers/create')">
          新建论文
        </el-button>
      </div>

      <!-- 加载中骨架屏 -->
      <div v-if="loading" class="paper-grid">
        <el-skeleton v-for="i in 8" :key="i" animated>
          <template #template>
            <div class="skeleton-card">
              <el-skeleton-item variant="text" style="width: 80%; height: 22px" />
              <el-skeleton-item variant="text" style="width: 50%; height: 16px; margin-top: 12px" />
              <el-skeleton-item variant="text" style="width: 40%; height: 14px; margin-top: 8px" />
            </div>
          </template>
        </el-skeleton>
      </div>

      <!-- 空状态 -->
      <el-empty
        v-else-if="!loading && paperList.length === 0"
        description="暂无论文，快去创建第一篇吧"
        :image-size="160"
      >
        <el-button type="primary" @click="router.push('/papers/create')">创建论文</el-button>
      </el-empty>

      <!-- 论文卡片网格 -->
      <div v-else class="paper-grid">
        <div
          v-for="paper in paperList"
          :key="paper.id"
          class="paper-card"
          @click="router.push(`/editor/${paper.id}`)"
        >
          <div class="card-body">
            <h3 class="card-title">{{ paper.title }}</h3>
            <div class="card-meta">
              <el-tag size="small" type="info">DRAFT</el-tag>
              <span class="card-time">{{ formatTime(paper.updatedAt) }}</span>
            </div>
          </div>
          <div class="card-footer">
            <el-button text type="primary" @click.stop="router.push(`/editor/${paper.id}`)">
              编辑
            </el-button>
            <el-button text type="success" @click.stop="router.push(`/preview/${paper.id}`)">
              预览
            </el-button>
            <el-button text type="danger" @click.stop="handleDelete(paper)">
              删除
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-design/icons-vue'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { usePaperStore } from '@/stores/paper'
import { deletePaper } from '@/api/paper'
import type { Thesis } from '@/types/api'

const router = useRouter()
const paperStore = usePaperStore()
const { paperList } = paperStore

const loading = ref(true)
const searchKeyword = ref('')
const sortBy = ref('updatedAt')

let searchTimer: ReturnType<typeof setTimeout> | null = null

function onSearch() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => fetchPapers(), 300)
}

async function fetchPapers() {
  loading.value = true
  try {
    await paperStore.fetchPapers(searchKeyword.value || undefined, sortBy.value)
  } finally {
    loading.value = false
  }
}

function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60))

  if (diffHours < 1) return '刚刚'
  if (diffHours < 24) return `${diffHours}小时前`
  if (diffHours < 48) return '昨天'
  return date.toLocaleDateString('zh-CN')
}

async function handleDelete(paper: Thesis) {
  try {
    await ElMessageBox.confirm(
      `确定要删除"${paper.title}"吗？此操作不可恢复。`,
      '确认删除',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    await deletePaper(paper.id)
    ElMessage.success('删除成功')
    await fetchPapers()
  } catch {
    // 取消或不处理
  }
}

onMounted(() => {
  fetchPapers()
})
</script>

<style scoped lang="scss">
.paper-list-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  .header-actions {
    display: flex;
    gap: 12px;
  }
}

.search-input {
  width: 280px;
}

.sort-select {
  width: 140px;
}

.paper-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;

  @media (max-width: 1400px) { grid-template-columns: repeat(3, 1fr); }
  @media (max-width: 1024px) { grid-template-columns: repeat(2, 1fr); }
  @media (max-width: 640px) { grid-template-columns: 1fr; }
}

.paper-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 140px;

  &:hover {
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
    transform: translateY(-2px);
    border-color: #409EFF;
  }

  .card-title {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    margin-bottom: 12px;
    line-height: 1.4;
  }

  .card-meta {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 16px;
    .card-time { font-size: 12px; color: #909399; }
  }

  .card-footer {
    display: flex;
    gap: 4px;
    border-top: 1px solid #f0f0f0;
    padding-top: 12px;
  }
}

.skeleton-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  min-height: 140px;
}
</style>
