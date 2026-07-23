<template>
  <DefaultLayout>
    <div class="page">
      <div class="header">
        <div class="header-left">
          <h2>我的论文</h2>
          <span class="count">共 {{ store.paperList.length }} 篇</span>
        </div>
        <el-button type="primary" :icon="Plus" @click="router.push('/papers/create')">新建论文</el-button>
        <el-button :icon="Upload" @click="router.push('/papers/import')">导入论文 (.docx)</el-button>
      </div>

      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索论文标题..." :prefix-icon="Search" clearable class="search" @input="onSearch" />
        <el-select v-model="sortBy" class="sort" @change="fetch">
          <el-option label="最近修改" value="updatedAt" />
          <el-option label="创建时间" value="createdAt" />
          <el-option label="标题 A-Z" value="title" />
        </el-select>
        <el-radio-group v-model="view" size="small">
          <el-radio-button value="grid"><el-icon><Grid /></el-icon></el-radio-button>
          <el-radio-button value="list"><el-icon><List /></el-icon></el-radio-button>
        </el-radio-group>
      </div>

      <!-- 加载骨架 -->
      <div v-if="loading" :class="view === 'grid' ? 'grid' : 'list'">
        <div v-for="i in 6" :key="i" class="skeleton">
          <el-skeleton animated>
            <template #template>
              <el-skeleton-item variant="text" style="width:60%;height:22px" />
              <el-skeleton-item variant="text" style="width:35%;height:16px;margin-top:12px" />
            </template>
          </el-skeleton>
        </div>
      </div>

      <!-- 空态 -->
      <el-empty v-else-if="!store.paperList.length" :description="keyword ? '没有匹配的论文' : '暂无论文，快去创建第一篇吧'" :image-size="140">
        <el-button v-if="!keyword" type="primary" @click="router.push('/papers/create')">创建论文</el-button>
        <el-button v-else @click="keyword = ''; fetch()">清除搜索</el-button>
      </el-empty>

      <!-- 网格视图 -->
      <div v-else-if="view === 'grid'" class="grid">
        <div v-for="p in list" :key="p.id" class="card" @click="router.push(`/editor/${p.id}`)">
          <div class="card-body">
            <h3 class="card-title">{{ p.title }}</h3>
            <div class="card-template" v-if="p.templateName">模板：{{ p.templateName }}</div>
            <div class="card-meta">
              <el-tag :type="statusTagType(p.status)" size="small">{{ statusLabel(p.status) }}</el-tag>
              <span class="time">{{ relativeTime(p.updatedAt) }}</span>
            </div>
          </div>
          <div class="card-actions">
            <el-button text type="primary" size="small" :icon="Edit" @click.stop="router.push(`/editor/${p.id}`)">编辑</el-button>
            <el-button text type="success" size="small" :icon="View" @click.stop="router.push(`/preview/${p.id}`)">预览</el-button>
            <el-button text type="danger" size="small" :icon="Delete" @click.stop="handleDelete(p)">删除</el-button>
          </div>
        </div>
      </div>

      <!-- 列表视图 -->
      <div v-else class="table-wrap">
        <el-table :data="list" @row-click="(r: any) => router.push(`/editor/${r.id}`)">
          <el-table-column prop="title" label="论文标题" min-width="280">
            <template #default="{ row }"><span class="link">{{ row.title }}</span></template>
          </el-table-column>
          <el-table-column label="论文模板" width="150">
            <template #default="{ row }">
              <el-tag v-if="row.templateName" size="small" effect="plain">{{ row.templateName }}</el-tag>
              <span v-else style="color:var(--el-text-color-secondary);font-size:13px">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="110">
            <template #default="{ row }"><el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="updatedAt" label="最后修改" width="140">
            <template #default="{ row }">{{ relativeTime(row.updatedAt) }}</template>
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

      <div v-if="store.paperList.length > 12" class="pagination">
        <el-pagination v-model:current-page="page" :page-size="12" :total="store.paperList.length" layout="prev,pager,next" background small />
      </div>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Edit, View, Delete, Grid, List, Upload } from '@element-plus/icons-vue'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { usePaperStore } from '@/stores/paper'
import { deletePaper } from '@/api/paper'
import { statusLabel, statusTagType, relativeTime } from '@/utils/format'
import type { Thesis } from '@/types'

const router = useRouter()
const store = usePaperStore()
const loading = ref(true)
const keyword = ref('')
const sortBy = ref('updatedAt')
const view = ref<'grid' | 'list'>('grid')
const page = ref(1)

const list = computed(() => {
  const s = (page.value - 1) * 12
  return store.paperList.slice(s, s + 12)
})

let timer: any = null
function onSearch() {
  page.value = 1
  clearTimeout(timer)
  timer = setTimeout(fetch, 300)
}

async function fetch() {
  loading.value = true
  try { await store.fetchPapers(keyword.value || undefined, sortBy.value) }
  finally { loading.value = false }
}

async function handleDelete(paper: Thesis) {
  try {
    await ElMessageBox.confirm(`确定删除"${paper.title}"？此操作不可恢复。`, '确认', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
    await deletePaper(paper.id)
    ElMessage.success('已删除')
    await fetch()
  } catch { /* 取消 */ }
}

onMounted(fetch)
</script>

<style scoped lang="scss">
.page { max-width: 1300px; margin: 0 auto; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;
  h2 { font-size: 22px; font-weight: 700; color: var(--el-text-color-primary); margin: 0; }
  .count { font-size: 14px; color: var(--el-text-color-secondary); margin-left: 12px; }
  .header-left { display: flex; align-items: baseline; }
}
.toolbar { display: flex; gap: 12px; align-items: center; margin-bottom: 20px; flex-wrap: wrap; }
.search { width: 260px; }
.sort { width: 140px; }

.grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px;
  @media (max-width:1200px) { grid-template-columns: repeat(3, 1fr); }
  @media (max-width:900px) { grid-template-columns: repeat(2, 1fr); }
  @media (max-width:600px) { grid-template-columns: 1fr; }
}

.card {
  background: var(--el-fill-color-blank); border-radius: 10px; padding: 20px; cursor: pointer;
  border: 1px solid var(--el-border-color-light); min-height: 140px;
  display: flex; flex-direction: column; justify-content: space-between;
  transition: all 0.25s;
  &:hover {
    box-shadow: 0 6px 20px rgba(62,46,31,0.10); transform: translateY(-2px);
    border-color: var(--el-color-primary-light-5);
  }
  .card-title { font-size: 16px; font-weight: 600; color: var(--el-text-color-primary); display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; margin-bottom: 4px; line-height: 1.4; }
  .card-template { font-size: 12px; color: var(--el-color-primary); margin-bottom: 10px; }
  .card-meta { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; }
  .time { font-size: 12px; color: var(--el-text-color-secondary); }
  .card-actions { display: flex; gap: 4px; border-top: 1px solid var(--el-border-color-extra-light); padding-top: 12px; }
}
.skeleton { background: var(--el-fill-color-blank); border-radius: 10px; padding: 20px; min-height: 120px; }

.table-wrap { background: var(--el-fill-color-blank); border-radius: 10px; border: 1px solid var(--el-border-color-light); overflow: hidden;
  .link { font-weight: 500; color: var(--el-text-color-primary); }
}
.pagination { display: flex; justify-content: center; margin-top: 24px; }
</style>
