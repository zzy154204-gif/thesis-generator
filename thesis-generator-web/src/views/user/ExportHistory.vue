<template>
  <DefaultLayout>
    <div class="page">
      <div class="header">
        <h2>导出历史</h2>
        <span class="count" v-if="list.length">共 {{ list.length }} 条记录</span>
      </div>

      <!-- 加载骨架 -->
      <div v-if="loading" class="skeleton-wrap">
        <el-skeleton :rows="5" animated />
      </div>

      <!-- 空态 -->
      <el-empty v-else-if="!list.length" description="还没有导出过论文，在编辑器中导出后会出现在这里" :image-size="140">
        <el-button type="primary" @click="router.push('/papers')">去我的论文</el-button>
      </el-empty>

      <!-- 导出记录列表 -->
      <el-card v-else class="list-card" :body-style="{ padding: 0 }">
        <el-table :data="list" stripe>
          <el-table-column prop="thesisTitle" label="论文标题" min-width="240" show-overflow-tooltip />
          <el-table-column prop="format" label="格式" width="90">
            <template #default="{ row }">
              <el-tag :type="row.format === 'PDF' ? 'danger' : 'primary'" size="small" effect="plain">
                {{ row.format }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" size="small">
                {{ row.status === 'SUCCESS' ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="文件大小" width="110">
            <template #default="{ row }">
              {{ row.fileSize ? formatSize(row.fileSize) : '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="导出时间" width="170">
            <template #default="{ row }">
              {{ formatTime(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button
                text type="primary" size="small"
                :disabled="row.status !== 'SUCCESS'"
                @click="handleReDownload(row)"
              >
                重新下载
              </el-button>
              <el-button
                v-if="row.errorMessage"
                text type="danger" size="small"
                @click="showError(row)"
              >
                错误详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 错误详情对话框 -->
      <el-dialog v-model="errorVisible" title="导出失败详情" width="480px">
        <div class="error-msg">{{ errorMessage }}</div>
        <template #footer>
          <el-button @click="errorVisible = false">关闭</el-button>
        </template>
      </el-dialog>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { getExportHistory } from '@/api/export'
import { downloadExport } from '@/api/paper'
import { ElMessage } from 'element-plus'
import type { ExportRecord } from '@/types'

const router = useRouter()
const loading = ref(true)
const list = ref<ExportRecord[]>([])
const errorVisible = ref(false)
const errorMessage = ref('')

function formatSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function formatTime(dateStr: string): string {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const h = String(date.getHours()).padStart(2, '0')
  const min = String(date.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${d} ${h}:${min}`
}

async function fetch() {
  loading.value = true
  try {
    const res = await getExportHistory()
    list.value = res.data || []
  } catch { /* handled */ }
  finally { loading.value = false }
}

async function handleReDownload(row: ExportRecord) {
  const format = row.format as 'DOCX' | 'PDF'
  try {
    await downloadExport(row.thesisId, format)
    ElMessage.success(`正在下载 ${row.thesisTitle}.${format.toLowerCase()}`)
  } catch (e: any) {
    ElMessage.error(e.message || '下载失败')
  }
}

function showError(row: ExportRecord) {
  errorMessage.value = row.errorMessage || '未知错误'
  errorVisible.value = true
}

onMounted(fetch)
</script>

<style scoped lang="scss">
.header { display: flex; align-items: baseline; gap: 12px; margin-bottom: 20px;
  h2 { font-size: 22px; font-weight: 700; margin: 0; }
  .count { font-size: 14px; color: var(--el-text-color-secondary); }
}
.list-card { border-radius: 10px; overflow: hidden; }
.skeleton-wrap { padding: 20px; background: var(--el-fill-color-blank); border-radius: 10px; }
.error-msg { font-size: 14px; line-height: 1.6; color: var(--el-color-danger); background: var(--el-color-danger-light-9); padding: 16px; border-radius: 8px; white-space: pre-wrap; }
</style>
