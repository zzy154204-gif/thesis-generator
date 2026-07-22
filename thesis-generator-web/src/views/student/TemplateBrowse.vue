<template>
  <DefaultLayout>
    <div class="page">
      <div class="header">
        <h2>模板库</h2>
        <span class="count">共 {{ templates.length }} 个模板</span>
      </div>

      <div class="toolbar">
        <el-select v-model="typeFilter" placeholder="模板类型" clearable style="width:140px" @change="fetch">
          <el-option label="毕业论文" value="GRADUATION" />
          <el-option label="课程论文" value="COURSE" />
          <el-option label="项目报告" value="PROJECT" />
        </el-select>
        <el-select v-model="collegeFilter" placeholder="所属学院" clearable filterable style="width:180px" @change="fetch">
          <el-option v-for="c in colleges" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </div>

      <el-empty v-if="!loading && !templates.length" description="暂无可用的模板" :image-size="140" />

      <div v-else class="grid">
        <div v-for="t in templates" :key="t.id" class="card" @click="selectTemplate(t)">
          <div class="card-badge">{{ typeLabel(t.type) }}</div>
          <h3 class="card-title">{{ t.name }}</h3>
          <p class="card-desc">{{ t.description || '暂无描述' }}</p>
          <div class="card-footer">
            <span class="college">{{ collegeName(t.collegeId) }}</span>
            <el-button type="primary" size="small" @click.stop="useTemplate(t)">使用此模板</el-button>
          </div>
        </div>
      </div>

      <!-- 选择模板确认对话框 -->
      <el-dialog v-model="dialogVisible" title="使用模板" width="400px">
        <p>确定使用模板 <strong>{{ selected?.name }}</strong> 创建论文吗？</p>
        <p style="font-size:13px;color:var(--el-text-color-secondary);margin-top:8px">
          系统将按模板结构自动生成章节框架
        </p>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="creating" @click="confirmUse">确定创建</el-button>
        </template>
      </el-dialog>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { getAvailableTemplates } from '@/api/template'
import { getColleges } from '@/api/college'
import { createPaper } from '@/api/paper'
import { getTemplateVersions } from '@/api/template'
import type { Template, College } from '@/types'

const router = useRouter()
const loading = ref(true)
const templates = ref<any[]>([])
const colleges = ref<College[]>([])
const typeFilter = ref('')
const collegeFilter = ref<number | undefined>()
const dialogVisible = ref(false)
const selected = ref<any>(null)
const creating = ref(false)

function typeLabel(type: string) {
  return type === 'GRADUATION' ? '毕业论文' : type === 'COURSE' ? '课程论文' : '项目报告'
}

function collegeName(id?: number) {
  if (!id) return '通用'
  return colleges.value.find(c => c.id === id)?.name || '通用'
}

async function fetch() {
  loading.value = true
  try {
    const params: any = {}
    if (typeFilter.value) params.type = typeFilter.value
    if (collegeFilter.value) params.collegeId = collegeFilter.value
    const res = await getAvailableTemplates(params)
    // 附加版本信息
    const all = await Promise.all((res.data || []).map(async (t) => {
      try {
        const verRes = await getTemplateVersions(t.id)
        const active = (verRes.data || []).find(v => v.isCurrent)
        return { ...t, versionId: active?.id }
      } catch { return { ...t, versionId: undefined } }
    }))
    templates.value = all
  } catch { /* handled */ }
  finally { loading.value = false }
}

async function selectTemplate(t: any) {
  selected.value = t
  dialogVisible.value = true
}

function useTemplate(t: any) {
  selected.value = t
  dialogVisible.value = true
}

async function confirmUse() {
  if (!selected.value) return
  creating.value = true
  try {
    const res = await createPaper({
      title: selected.value.name + '论文',
      templateVersionId: selected.value.versionId,
    })
    ElMessage.success('已创建并应用模板')
    dialogVisible.value = false
    router.push(`/editor/${res.data.id}`)
  } catch { /* handled */ }
  finally { creating.value = false }
}

onMounted(async () => {
  const colRes = await getColleges()
  colleges.value = colRes.data || []
  await fetch()
})
</script>

<style scoped lang="scss">
.header { display: flex; align-items: baseline; gap: 12px; margin-bottom: 20px;
  h2 { font-size: 22px; font-weight: 700; margin: 0; }
  .count { font-size: 14px; color: var(--el-text-color-secondary); }
}
.toolbar { display: flex; gap: 12px; margin-bottom: 20px; flex-wrap: wrap; }

.grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px;
  @media (max-width:900px) { grid-template-columns: repeat(2, 1fr); }
  @media (max-width:600px) { grid-template-columns: 1fr; }
}

.card {
  background: var(--el-fill-color-blank); border-radius: 10px; padding: 20px;
  border: 1px solid var(--el-border-color-light); cursor: pointer;
  display: flex; flex-direction: column; transition: all 0.25s;
  &:hover { box-shadow: 0 6px 20px rgba(62,46,31,0.10); transform: translateY(-2px); border-color: var(--el-color-primary-light-5); }
  .card-badge { font-size: 11px; color: var(--el-color-primary); background: var(--el-color-primary-light-9); display: inline-block; padding: 2px 8px; border-radius: 4px; margin-bottom: 10px; width: fit-content; }
  .card-title { font-size: 16px; font-weight: 600; margin-bottom: 8px; }
  .card-desc { font-size: 13px; color: var(--el-text-color-secondary); flex: 1; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; margin-bottom: 14px; }
  .card-footer { display: flex; justify-content: space-between; align-items: center; padding-top: 12px; border-top: 1px solid var(--el-border-color-extra-light); }
  .college { font-size: 12px; color: var(--el-text-color-secondary); }
}
</style>
