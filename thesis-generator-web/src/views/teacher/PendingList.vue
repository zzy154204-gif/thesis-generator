<template>
  <DefaultLayout>
    <div class="pending-list">
      <div class="page-header">
        <h3>📝 待批阅论文</h3>
        <el-badge :value="pendingCount" :hidden="!pendingCount">
          <el-tag type="warning">待批阅</el-tag>
        </el-badge>
      </div>

      <!-- 搜索筛选 -->
      <div class="filter-bar">
        <el-input
          v-model="keyword"
          placeholder="搜索学生姓名 / 论文标题"
          style="width: 240px"
          clearable
          @keyup.enter="fetchData"
        />
        <el-select v-model="courseFilter" placeholder="选择课程" clearable style="width: 160px">
          <el-option label="软件工程" value="SE" />
          <el-option label="数据结构" value="DS" />
          <el-option label="操作系统" value="OS" />
        </el-select>
        <el-button type="primary" @click="fetchData">搜索</el-button>
        <el-button @click="resetFilter">重置</el-button>
      </div>

      <!-- 表格 -->
      <el-table :data="tableData" border v-loading="loading">
        <el-table-column prop="title" label="论文标题" min-width="180" />
        <el-table-column prop="studentName" label="学生" width="100" />
        <el-table-column prop="studentId" label="学号" width="120" />
        <el-table-column prop="course" label="课程" width="100" />
        <el-table-column prop="submittedAt" label="提交时间" width="170">
          <template #default="{ row }">
            {{ new Date(row.submittedAt).toLocaleString('zh-CN') }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUBMITTED' ? 'warning' : 'primary'">
              {{ row.status === 'SUBMITTED' ? '待批阅' : '批阅中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="enterReview(row.id)">批阅</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="fetchData"
        @size-change="fetchData"
        style="margin-top:20px;justify-content:flex-end"
      />
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { getPendingList } from '@/api/review'

const router = useRouter()
const loading = ref(false)
const keyword = ref('')
const courseFilter = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref<any[]>([])

// 待批阅数量（从数据中计算）
const pendingCount = computed(() => tableData.value.filter((item) => item.status === 'SUBMITTED').length)

function formatDate(date: string) {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}


// ===== 获取数据 =====
async function fetchData() {
  loading.value = true
  try {
    const res = await getPendingList({
      page: page.value,
      size: pageSize.value,
      keyword: keyword.value,
      course: courseFilter.value,
    })
    tableData.value = res.data.list || []
    total.value = res.data.total || 0
  } catch {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

function resetFilter() {
  keyword.value = ''
  courseFilter.value = ''
  page.value = 1
  fetchData()
}

function enterReview(paperId: number) {
  router.push(`/teacher/review/${paperId}`)
}

onMounted(fetchData)
</script>

<style scoped lang="scss">
.pending-list {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    h3 { margin: 0; }
  }

  .filter-bar {
    display: flex;
    gap: 12px;
    margin-bottom: 20px;
    flex-wrap: wrap;
    align-items: center;
  }
}
</style>
