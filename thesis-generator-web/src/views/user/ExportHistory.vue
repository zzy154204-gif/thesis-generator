<template>
  <DefaultLayout>
    <div class="export-history-page">
      <div class="history-container">
        <h2>导出历史</h2>
        <el-table :data="exports" stripe class="history-table" v-loading="loading" empty-text="暂无导出记录">
          <el-table-column prop="fileName" label="文件名" min-width="200" />
          <el-table-column prop="format" label="格式" width="80" />
          <el-table-column prop="createdAt" label="导出时间" width="180" />
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag v-if="row.status === 'COMPLETED'" type="success" size="small">已完成</el-tag>
              <el-tag v-else-if="row.status === 'FAILED'" type="danger" size="small">失败</el-tag>
              <el-tag v-else type="warning" size="small">进行中</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 'COMPLETED'"
                text
                type="primary"
                size="small"
              >
                下载
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import DefaultLayout from '@/layouts/DefaultLayout.vue'

const loading = ref(false)

// 模拟数据，后续从后端获取
const exports = ref([
  { id: 1, fileName: '毕业论文-张三-2026.pdf', format: 'PDF', createdAt: '2026-07-20 15:30', status: 'COMPLETED' },
  { id: 2, fileName: '毕业论文-张三-2026.docx', format: 'DOCX', createdAt: '2026-07-20 14:12', status: 'COMPLETED' },
  { id: 3, fileName: '毕业论文-张三-2026-v2.pdf', format: 'PDF', createdAt: '2026-07-21 09:00', status: 'FAILED' },
])
</script>

<style scoped lang="scss">
.export-history-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 32px 24px;
}

.history-container {
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  h2 { margin-bottom: 24px; }
}
</style>
