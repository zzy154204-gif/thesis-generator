<template>
  <DefaultLayout>
    <div class="page">
      <div class="header">
        <h2>待批阅论文</h2>
        <span class="count" v-if="total">共 {{ total }} 篇</span>
      </div>

      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索论文标题..." :prefix-icon="Search" clearable style="width:300px" @input="onSearch" />
      </div>

      <el-table :data="list" v-loading="loading" stripe @row-click="(r: any) => router.push(`/teacher/review/${r.id}`)">
        <el-table-column prop="title" label="论文标题" min-width="260" show-overflow-tooltip>
          <template #default="{ row }"><span class="link">{{ row.title }}</span></template>
        </el-table-column>
        <el-table-column prop="studentName" label="学生" width="120" />
        <el-table-column prop="studentId" label="学号" width="120" />
        <el-table-column prop="submittedAt" label="提交时间" width="160">
          <template #default="{ row }">{{ relativeTime(row.submittedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click.stop="router.push(`/teacher/review/${row.id}`)">批阅</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !list.length" :description="keyword ? '没有匹配的论文' : '暂没有待批阅的论文'" :image-size="140" />

      <div v-if="total > size" class="pagination">
        <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="prev,pager,next" background small @change="fetch" />
      </div>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { relativeTime } from '@/utils/format'
import { getPendingList } from '@/api/review'

const router = useRouter()
const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')

let timer: any = null
function onSearch() {
  page.value = 1
  clearTimeout(timer)
  timer = setTimeout(fetch, 300)
}

async function fetch() {
  loading.value = true
  try {
    const res = await getPendingList({ page: page.value, size: size.value, keyword: keyword.value || undefined })
    list.value = res.data.list || []
    total.value = res.data.total || 0
  } catch { /* handled */ }
  finally { loading.value = false }
}

onMounted(fetch)
</script>

<style scoped lang="scss">
.header { display: flex; align-items: baseline; gap: 12px; margin-bottom: 20px;
  h2 { font-size: 22px; font-weight: 700; margin: 0; }
  .count { font-size: 14px; color: var(--el-text-color-secondary); }
}
.toolbar { margin-bottom: 20px; }
.link { font-weight: 500; color: var(--el-color-primary); cursor: pointer; }
.pagination { display: flex; justify-content: center; margin-top: 24px; }
</style>
