<template>
  <DefaultLayout>
    <div class="paper-list-page">
      <div class="page-header">
        <div class="header-left">
          <h2 class="page-title">我的论文</h2>
          <span class="paper-count">共 {{ paperList.length }} 篇</span>
        </div>
        <el-button type="primary" :icon="Plus" @click="router.push('/papers/create')">
          新建论文
        </el-button>
      </div>

      <div class="toolbar">
        <div class="toolbar-left">
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
            <el-option label="标题 A-Z" value="title" />
          </el-select>
        </div>
        <div class="toolbar-right">
          <el-radio-group v-model="viewMode" size="small">
            <el-radio-button value="grid">
              <el-icon><Grid /></el-icon>
            </el-radio-button>
            <el-radio-button value="list">
              <el-icon><List /></el-icon>
            </el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <!-- 加载中骨架屏 -->
      <div v-if="loading" :class="viewMode === 'grid' ? 'paper-grid' : 'paper-list'">
        <div v-for="i in 8" :key="i" :class="viewMode === 'grid' ? 'skeleton-card' : 'skeleton-row'">
          <el-skeleton animated>
            <template #template>
              <el-skeleton-item variant="text" style="width: 60%; height: 22px" />
              <el-skeleton-item variant="text" style="width: 35%; height: 16px; margin-top: 12px" />
              <el-skeleton-item variant="text" style="width: 25%; height: 14px; margin-top: 8px" />
            </template>
          </el-skeleton>
        </div>
      </div>

      <!-- 空状态 -->
      <el-empty
        v-else-if="!loading && paperList.length === 0"
        :description="searchKeyword ? '没有匹配的论文' : '暂无论文，快去创建第一篇吧'"
        :image-size="160"
      >
        <el-button v-if="!searchKeyword" type="primary" @click="router.push('/papers/create')">创建论文</el-button>
        <el-button v-else @click="searchKeyword = ''; fetchPapers()">清除搜索</el-button>
      </el-empty>

      <!-- 论文网格视图 -->
      <div v-else-if="viewMode === 'grid'" class="paper-grid">
        <div
          v-for="paper in paginatedList"
          :key="paper.id"
          class="paper-card"
          @click="router.push(`/editor/${paper.id}`)"
        >
          <div class="card-body">
            <h3 class="card-title">{{ paper.title }}</h3>
            <div class="card-meta">
              <el-tag :type="statusTagType(paper.status)" size="small">{{ statusLabel(paper.status) }}</el-tag>
              <span class="card-time">{{ formatTime(paper.updatedAt) }}</span>
            </div>
          </div>
          <div class="card-footer">
            <el-button text type="primary" @click.stop="router.push(`/editor/${paper.id}`)">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-button text type="success" @click.stop="router.push(`/preview/${paper.id}`)">
              <el-icon><View /></el-icon> 预览
            </el-button>
            <el-button text type="danger" @click.stop="handleDelete(paper)">
              <el-icon><Delete /></el-icon> 删除
            </el-button>
          </div>
        </div>
      </div>

      <!-- 论文列表视图 -->
      <div v-else class="paper-list-view">
        <el-table :data="paginatedList" style="width: 100%" @row-click="(row: Thesis) => router.push(`/editor/${row.id}`)">
          <el-table-column prop="title" label="论文标题" min-width="280">
            <template #default="{ row }">
              <span class="list-title">{{ row.title }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="updatedAt" label="最后修改" width="140">
            <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" size="small" @click.stop="router.push(`/editor/${row.id}`)">编辑</el-button>
              <el-button text type="success" size="small" @click.stop="router.push(`/preview/${row.id}`)">预览</el-button>
              <el-button text type="danger" size="small" @click.stop="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 分页 -->
      <div v-if="!loading && paperList.length > pageSize" class="pagination-wrap">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="paperList.length"
          layout="prev, pager, next"
          background
          small
        />
      </div>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Edit, View, Delete, Grid, List } from '@element-plus/icons-vue'
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
const viewMode = ref<'grid' | 'list'>('grid')
const currentPage = ref(1)
const pageSize = 12

const paginatedList = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return paperList.slice(start, start + pageSize)
})

let searchTimer: ReturnType<typeof setTimeout> | null = null

function onSearch() {
  currentPage.value = 1
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

function statusLabel(status: string): string {
  const map: Record<string, string> = {
    DRAFT: '草稿', COMPLETED: '已完成', SUBMITTED: '已提交',
    REVIEWED: '已批阅', RETURNED: '已退回', GENERATING: '生成中',
  }
  return map[status] || status
}

function statusTagType(status: string): 'info' | 'success' | 'warning' | 'danger' | '' {
  const map: Record<string, 'info' | 'success' | 'warning' | 'danger' | ''> = {
    DRAFT: 'info', COMPLETED: 'success', SUBMITTED: 'warning',
    REVIEWED: 'success', RETURNED: 'danger', GENERATING: 'warning',
  }
  return map[status] || 'info'
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
    currentPage.value = 1
    await fetchPapers()
  } catch {
    // 取消
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
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  .header-left {
    display: flex;
    align-items: baseline;
    gap: 12px;
    .page-title { font-size: 22px; font-weight: 700; color: var(--el-text-color-primary); margin: 0; }
    .paper-count { font-size: 14px; color: var(--el-text-color-secondary); }
  }
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  .toolbar-left { display: flex; gap: 12px; }
}

.search-input { width: 280px; }
.sort-select { width: 150px; }

.paper-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;

  @media (max-width: 1400px) { grid-template-columns: repeat(3, 1fr); }
  @media (max-width: 1024px) { grid-template-columns: repeat(2, 1fr); }
  @media (max-width: 640px) { grid-template-columns: 1fr; }
}

.paper-card {
  background: var(--el-fill-color-blank);
  border-radius: 10px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.25s ease;
  border: 1px solid var(--el-border-color-light);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 140px;

  &:hover {
    box-shadow: 0 6px 20px rgba(62, 46, 31, 0.1);
    transform: translateY(-2px);
    border-color: var(--el-color-primary-light-5);
  }

  .card-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--el-text-color-primary);
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
    .card-time { font-size: 12px; color: var(--el-text-color-secondary); }
  }

  .card-footer {
    display: flex;
    gap: 4px;
    border-top: 1px solid var(--el-border-color-extra-light);
    padding-top: 12px;
  }
}

.skeleton-card, .skeleton-row {
  background: var(--el-fill-color-blank);
  border-radius: 10px;
  padding: 20px;
  min-height: 140px;
}

.paper-list-view {
  background: var(--el-fill-color-blank);
  border-radius: 10px;
  border: 1px solid var(--el-border-color-light);
  overflow: hidden;
  .list-title { font-weight: 500; color: var(--el-text-color-primary); cursor: pointer; }
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
