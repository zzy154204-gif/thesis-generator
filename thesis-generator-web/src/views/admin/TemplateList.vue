<template>
  <div class="template-list">
      <!-- 顶栏 -->
      <div class="page-header">
        <h3>📋 模板管理</h3>
        <el-button type="primary" @click="router.push('/admin/templates/new')">
          <el-icon><Plus /></el-icon> 新建模板
        </el-button>
      </div>

      <!-- Tab 切换 -->
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
            <div class="desc" v-if="item.description">{{ item.description }}</div>
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

        <!-- 空状态 -->
        <div v-if="!tableData.length && !loading" class="empty-state">
          <el-empty description="暂无模板，点击上方新建" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getAdminTemplates, toggleTemplateStatus } from '@/api/template'

const router = useRouter()
const loading = ref(false)
const activeType = ref('GRADUATION')
const tableData = ref<any[]>([])

async function fetchData() {
  loading.value = true
  try {
    const res = await getAdminTemplates({ type: activeType.value as 'GRADUATION' | 'COURSE' | 'PROJECT' })
    tableData.value = (res.data || []).map((t: any) => ({
      ...t,
      status: t.enabled ? 'ENABLED' : 'DISABLED',
    }))
  } catch {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

async function toggleStatus(item: any) {
  const newStatus = item.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  try {
    await toggleTemplateStatus(item.id, newStatus)
    item.status = newStatus
    ElMessage.success(`已${newStatus === 'ENABLED' ? '启用' : '停用'}`)
  } catch {
    ElMessage.error('操作失败')
  }
}

function previewTemplate(item: any) {
  ElMessage.info('预览功能开发中')
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

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  margin-top: 16px;
}

.template-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    .name {
      font-weight: 600;
      font-size: 15px;
      color: #303133;
    }
  }

  .card-body {
    margin: 12px 0;
    font-size: 14px;
    color: #606266;
    line-height: 1.8;
    .label {
      color: #909399;
    }
    .desc {
      color: #909399;
      font-size: 13px;
      margin-top: 4px;
    }
  }

  .card-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: 12px;
    border-top: 1px solid #ebeef5;
    .actions {
      display: flex;
      gap: 8px;
    }
  }
}

.empty-state {
  grid-column: 1 / -1;
  padding: 60px 0;
}
</style>
