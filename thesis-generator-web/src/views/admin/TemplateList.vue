<template>
  <DefaultLayout>
    <div class="template-list">
      <div class="page-header">
        <h3>模板管理</h3>
        <el-button type="primary" @click="router.push('/admin/templates/new')">
          <el-icon><Plus /></el-icon> 新建模板
        </el-button>
      </div>

      <el-tabs v-model="activeType" @tab-change="fetchData">
        <el-tab-pane label="全部" name="" />
        <el-tab-pane label="毕业论文" name="GRADUATION" />
        <el-tab-pane label="课程论文" name="COURSE" />
        <el-tab-pane label="项目论文" name="PROJECT" />
      </el-tabs>

      <el-table :data="tableData" border v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column prop="name" label="模板名称" min-width="180" />
        <el-table-column prop="type" label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag>{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enabled !== false ? 'success' : 'info'" size="small">
              {{ row.enabled !== false ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.enabled !== false"
              size="small"
              @change="toggleStatus(row)"
              style="margin-right: 8px"
            />
            <el-button type="primary" link @click="router.push(`/admin/templates/${row.id}`)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { getAdminTemplates, toggleTemplateStatus, deleteTemplate } from '@/api/template'

const router = useRouter()
const loading = ref(false)
const activeType = ref('')
const tableData = ref<any[]>([])

function typeLabel(type: string) {
  const map: Record<string, string> = { GRADUATION: '毕业论文', COURSE: '课程论文', PROJECT: '项目论文' }
  return map[type] || type
}

async function fetchData() {
  loading.value = true
  try {
    const params: any = {}
    if (activeType.value) params.type = activeType.value
    const res = await getAdminTemplates(params)
    tableData.value = res.data || []
  } catch {
    ElMessage.error('加载模板列表失败')
  } finally {
    loading.value = false
  }
}

async function toggleStatus(item: any) {
  try {
    const newStatus = item.enabled !== false ? 'DISABLED' : 'ENABLED'
    await toggleTemplateStatus(item.id, newStatus as any)
    item.enabled = newStatus === 'ENABLED'
    ElMessage.success(`已${item.enabled ? '启用' : '停用'}`)
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确定要删除模板【${row.name}】吗？`, '警告', {
      confirmButtonText: '确定删除',
      type: 'warning',
    })
    await deleteTemplate(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch {
    // 用户取消
  }
}

onMounted(fetchData)
</script>

<style scoped lang="scss">
.template-list {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    h3 { margin: 0; }
  }
}
</style>
