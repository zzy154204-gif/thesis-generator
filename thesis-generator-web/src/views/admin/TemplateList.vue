<template>
  <DefaultLayout>
    <div class="page">
      <div class="header">
        <h2>模板管理</h2>
        <el-button type="primary" :icon="Plus" @click="router.push('/admin/templates/new')">新建模板</el-button>
      </div>

      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索模板..." :prefix-icon="Search" clearable style="width:260px" @input="onSearch" />
        <el-select v-model="typeFilter" placeholder="类型" clearable style="width:120px" @change="fetch">
          <el-option label="毕业论文" value="GRADUATION" />
          <el-option label="课程论文" value="COURSE" />
          <el-option label="项目报告" value="PROJECT" />
        </el-select>
      </div>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="name" label="模板名称" min-width="200" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">{{ typeLabel(row.type) }}</template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="enabled" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">{{ row.enabled ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新" width="140">
          <template #default="{ row }">{{ row.updatedAt?.split('T')[0] }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="router.push(`/admin/templates/${row.id}`)">编辑</el-button>
            <el-button text type="primary" size="small" @click="handleDuplicate(row)">复制</el-button>
            <el-button text :type="row.enabled ? 'warning' : 'success'" size="small" @click="handleToggle(row)">
              {{ row.enabled ? '停用' : '启用' }}
            </el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !list.length" description="暂无模板" :image-size="120" />
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { getAdminTemplates, toggleTemplateStatus, deleteAdminTemplate, duplicateTemplate } from '@/api/admin'
import type { Template } from '@/types'

const router = useRouter()
const loading = ref(false)
const list = ref<Template[]>([])
const keyword = ref('')
const typeFilter = ref('')

function typeLabel(t: string) { return t === 'GRADUATION' ? '毕业论文' : t === 'COURSE' ? '课程论文' : '项目报告' }

let timer: any = null
function onSearch() { clearTimeout(timer); timer = setTimeout(fetch, 300) }

async function fetch() {
  loading.value = true
  try {
    const res = await getAdminTemplates({
      keyword: keyword.value || undefined,
      type: typeFilter.value || undefined,
    })
    list.value = res.data || []
  } catch { /* handled */ }
  finally { loading.value = false }
}

async function handleToggle(t: Template) {
  try {
    await toggleTemplateStatus(t.id, t.enabled ? 'DISABLED' : 'ENABLED')
    ElMessage.success(t.enabled ? '已停用' : '已启用')
    await fetch()
  } catch { /* handled */ }
}

async function handleDelete(t: Template) {
  try {
    await ElMessageBox.confirm(`确定删除模板"${t.name}"？`, '确认', { type: 'warning' })
    await deleteAdminTemplate(t.id)
    ElMessage.success('已删除')
    await fetch()
  } catch { /* 取消 */ }
}

async function handleDuplicate(t: Template) {
  try {
    await duplicateTemplate(t.id)
    ElMessage.success('已复制')
    await fetch()
  } catch { /* handled */ }
}

onMounted(fetch)
</script>

<style scoped lang="scss">
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;
  h2 { font-size: 22px; font-weight: 700; margin: 0; }
}
.toolbar { display: flex; gap: 12px; margin-bottom: 20px; flex-wrap: wrap; }
</style>
