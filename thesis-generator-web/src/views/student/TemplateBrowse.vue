<template>
  <DefaultLayout>
    <div class="template-browse">
      <div class="page-header">
        <h3>模板库</h3>
      </div>

      <el-alert
        title="选择模板后即可创建论文，系统将按模板预设的章节结构自动生成论文大纲。"
        type="info"
        show-icon
        :closable="false"
        style="margin-bottom: 20px"
      />

      <el-tabs v-model="activeType" @tab-change="fetchData">
        <el-tab-pane label="全部" name="" />
        <el-tab-pane label="毕业论文" name="GRADUATION" />
        <el-tab-pane label="课程论文" name="COURSE" />
        <el-tab-pane label="项目论文" name="PROJECT" />
      </el-tabs>

      <div class="card-grid" v-loading="loading">
        <div v-for="item in tableData" :key="item.id" class="template-card">
          <div class="card-header">
            <span class="name">{{ item.name }}</span>
            <el-tag size="small">{{ typeLabel(item.type) }}</el-tag>
          </div>
          <div class="card-body">
            <p v-if="item.description" class="desc">{{ item.description }}</p>
            <p v-else class="desc muted">暂无描述</p>
          </div>
          <div class="card-footer">
            <el-button type="primary" @click="useTemplate(item)">使用此模板</el-button>
          </div>
        </div>

        <div v-if="!tableData.length && !loading" class="empty-state">
          <el-empty description="暂无可用的模板" />
        </div>
      </div>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { getAvailableTemplates } from '@/api/template'
import { createPaper } from '@/api/paper'

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
    const res = await getAvailableTemplates(params)
    tableData.value = res.data || []
  } catch {
    ElMessage.error('加载模板失败')
  } finally {
    loading.value = false
  }
}

async function useTemplate(template: any) {
  try {
    const res = await createPaper({
      title: '未命名论文',
      templateVersionId: template.id,
      collegeId: template.collegeId,
    })
    ElMessage.success('论文创建成功')
    router.push(`/editor/${res.data.id}`)
  } catch {
    ElMessage.error('创建论文失败')
  }
}

onMounted(fetchData)
</script>

<style scoped lang="scss">
.template-browse {
  .page-header {
    margin-bottom: 16px;
    h3 { margin: 0; }
  }
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.template-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
    .name { font-weight: 600; font-size: 15px; }
  }

  .card-body {
    .desc { font-size: 14px; color: #606266; margin: 0; line-height: 1.6; }
    .muted { color: #c0c4cc; }
  }

  .card-footer {
    margin-top: 16px;
    padding-top: 12px;
    border-top: 1px solid #ebeef5;
  }
}

.empty-state {
  grid-column: 1 / -1;
  padding: 60px 0;
}
</style>
