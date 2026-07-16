<!-- src/views/admin/TemplateList.vue -->
<template>
  <DefaultLayout>
    <div class="template-list">
      <!-- 顶栏 -->
      <div class="header">
        <h3>模板管理</h3>
        <el-button type="primary" @click="router.push('/admin/templates/new')">
          <el-icon><Plus /></el-icon> 新建模板
        </el-button>
      </div>

      <!-- Tab切换 -->
      <el-tabs v-model="activeType" @tab-change="fetchData">
        <el-tab-pane label="毕业论文" name="GRADUATION" />
        <el-tab-pane label="课程论文" name="COURSE" />
        <el-tab-pane label="项目论文" name="PROJECT" />
      </el-tabs>

      <!-- 卡片列表 -->
      <div class="card-grid" v-loading="loading">
        <div v-for="item in tableData" :key="item.id" class="template-card">
          <div class="card-header">
            <span class="name">{{ item.name }}</span>
            <el-tag :type="item.status === 'ENABLED' ? 'success' : 'info'" size="small">
              {{ item.status === 'ENABLED' ? '已启用' : '已停用' }}
            </el-tag>
          </div>
          <div class="card-body">
            <div><span class="label">版本：</span>v{{ item.version }}</div>
            <div><span class="label">所属学院：</span>{{ item.collegeName || '全局' }}</div>
            <div class="desc">{{ item.description || '' }}</div>
          </div>
          <div class="card-footer">
            <el-switch
              :model-value="item.status === 'ENABLED'"
              @change="toggleStatus(item)"
            />
            <div class="actions">
              <el-button type="primary" link @click="previewTemplate(item)">预览</el-button>
              <el-button type="primary" link @click="router.push(`/admin/templates/${item.id}`)">编辑</el-button>
            </div>
          </div>
        </div>
        <div v-if="!tableData.length && !loading" class="empty-state">
          <el-empty description="暂无模板，点击上方新建" />
        </div>
      </div>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import DefaultLayout from '@/layouts/DefaultLayout.vue'

const router = useRouter()
const loading = ref(false)
const activeType = ref('GRADUATION')
const tableData = ref<any[]>([])

async function fetchData() {
  loading.value = true
  try {
    // TODO: 调用真实 API
    // const res = await templateApi.getList({ type: activeType.value })
    // tableData.value = res.data
    tableData.value = [
      { id: 1, name: '计算机学院毕业论文模板', type: 'GRADUATION', version: '2.1', status: 'ENABLED', collegeName: '计算机学院' },
      { id: 2, name: '电子学院课程论文模板', type: 'COURSE', version: '1.0', status: 'DISABLED', collegeName: '电子学院' },
    ]
  } finally {
    loading.value = false
  }
}

async function toggleStatus(item: any) {
  const newStatus = item.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  // TODO: await templateApi.updateStatus(item.id, newStatus)
  item.status = newStatus
  ElMessage.success(`已${newStatus === 'ENABLED' ? '启用' : '停用'}`)
}

function previewTemplate(item: any) {
  // TODO: 打开预览弹窗
  ElMessage.info('预览功能开发中')
}

onMounted(fetchData)
</script>

<style scoped lang="scss">
.template-list {
  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    h3 { margin: 0; }
  }
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 20px;
  margin-top: 16px;
}

.template-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  transition: box-shadow 0.2s;
  &:hover { box-shadow: 0 4px 20px rgba(0,0,0,0.1); }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    .name { font-weight: 600; font-size: 15px; }
  }

  .card-body {
    margin: 12px 0;
    font-size: 14px;
    color: #606266;
    .label { color: #909399; }
    .desc { margin-top: 8px; color: #909399; font-size: 13px; }
  }

  .card-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    .actions { display: flex; gap: 8px; }
  }
}
</style>
