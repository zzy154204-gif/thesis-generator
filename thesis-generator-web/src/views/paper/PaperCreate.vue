<template>
  <DefaultLayout>
    <div class="page">
      <div class="header">
        <el-button text :icon="ArrowLeft" @click="router.push('/papers')">返回</el-button>
        <h2>创建新论文</h2>
      </div>

      <el-card class="form-card">
        <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
          <el-form-item label="论文标题" prop="title">
            <el-input v-model="form.title" placeholder="请输入论文标题" maxlength="200" />
          </el-form-item>

          <el-form-item label="所属学院" prop="collegeId">
            <el-select v-model="form.collegeId" placeholder="选择学院" clearable filterable style="width:300px">
              <el-option v-for="c in colleges" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
          </el-form-item>

          <el-form-item label="指导老师">
            <el-select v-model="form.teacherId" placeholder="选择指导老师" clearable filterable style="width:300px">
              <el-option
                v-for="t in teachers" :key="t.id"
                :label="`${t.realName} (${t.college || '学院未知'} - 工号: ${t.teacherNo || '无'})`"
                :value="t.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="选择模板" prop="templateVersionId">
            <el-select v-model="form.templateVersionId" placeholder="选择模板（可选）" clearable filterable style="width:400px">
              <el-option v-for="t in templates" :key="t.id" :label="t.name" :value="t.id">
                <span>{{ t.name }}</span>
                <span class="opt-desc">{{ templateDesc(t) }}</span>
              </el-option>
            </el-select>
            <div class="tip">选择模板后系统会按模板结构自动生成章节</div>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" size="large" :loading="submitting" @click="handleCreate">创建论文</el-button>
            <el-button size="large" @click="router.push('/papers')">取消</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { getColleges } from '@/api/college'
import { getAvailableTemplates } from '@/api/template'
import { getTemplateVersions } from '@/api/template'
import { getTeachers } from '@/api/review'
import { createPaper } from '@/api/paper'
import type { College, Template } from '@/types'

const router = useRouter()
const auth = useAuthStore()
const submitting = ref(false)
const formRef = ref()

const form = reactive({ title: '未命名论文', collegeId: undefined as number | undefined, teacherId: undefined as number | undefined, templateVersionId: undefined as number | undefined })
const rules = { title: [{ required: true, message: '请输入论文标题', trigger: 'blur' }] }
const colleges = ref<College[]>([])
const templates = ref<any[]>([])
const teachers = ref<{ id: number; realName: string; teacherNo?: string; college?: string }[]>([])

function templateDesc(t: Template) {
  return t.type === 'GRADUATION' ? '毕业论文' : t.type === 'COURSE' ? '课程论文' : '项目报告'
}

/** 监听学院变化，加载该学院的老师列表 */
watch(() => form.collegeId, async (collegeId) => {
  teachers.value = []
  try {
    const res = await getTeachers(collegeId || undefined)
    teachers.value = res.data || []
  } catch { /* ignore */ }
}, { immediate: true })

onMounted(async () => {
  try {
    const [colRes, tmplRes] = await Promise.all([getColleges(), getAvailableTemplates()])
    colleges.value = colRes.data || []
    const tmpls = tmplRes.data || []
    // 对每个模板获取最新版本
    const full = await Promise.all(tmpls.map(async (t) => {
      try {
        const verRes = await getTemplateVersions(t.id)
        const active = (verRes.data || []).find(v => v.isCurrent)
        return { ...t, templateVersionId: active?.id }
      } catch { return { ...t, templateVersionId: undefined } }
    }))
    templates.value = full.filter(t => t.templateVersionId)
  } catch { /* ignore */ }
})

async function handleCreate() {
  if (!formRef.value) return
  await formRef.value.validate(async (ok: boolean) => {
    if (!ok) return
    submitting.value = true
    try {
      const res = await createPaper({
        title: form.title,
        collegeId: form.collegeId,
        teacherId: form.teacherId,
        templateVersionId: form.templateVersionId,
      })
      ElMessage.success('创建成功')
      router.push(`/editor/${res.data.id}`)
    } catch { /* handled */ }
    finally { submitting.value = false }
  })
}
</script>

<style scoped lang="scss">
.header { display: flex; align-items: center; gap: 16px; margin-bottom: 24px;
  h2 { font-size: 20px; font-weight: 600; }
}
.form-card { max-width: 700px; }
.tip { font-size: 12px; color: var(--el-text-color-secondary); margin-top: 4px; }
.opt-desc { font-size: 12px; color: var(--el-text-color-secondary); margin-left: 8px; }
</style>
