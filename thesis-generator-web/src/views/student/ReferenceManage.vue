<template>
  <DefaultLayout>
    <div class="page">
      <div class="header">
        <h2>参考文献</h2>
        <el-button type="primary" :icon="Plus" @click="showDialog()">新增文献</el-button>
      </div>

      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索标题或作者..." :prefix-icon="Search" clearable style="width:300px" @input="onSearch" />
        <el-select v-model="typeFilter" placeholder="文献类型" clearable style="width:140px" @change="fetch">
          <el-option v-for="(l, k) in REF_TYPE_LABEL" :key="k" :label="l" :value="k" />
        </el-select>
      </div>

      <el-table :data="filtered" stripe v-loading="loading">
        <el-table-column prop="authors" label="作者" min-width="160" />
        <el-table-column prop="title" label="标题" min-width="260" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="120">
          <template #default="{ row }">{{ REF_TYPE_LABEL[row.type] || row.type }}</template>
        </el-table-column>
        <el-table-column prop="year" label="年份" width="80" />
        <el-table-column prop="journal" label="期刊/来源" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="showDialog(row)">编辑</el-button>
            <el-button text type="primary" size="small" @click="handleFormat(row)">格式化</el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !filtered.length" description="暂无文献" :image-size="120" />

      <!-- 格式化结果 -->
      <el-dialog v-model="formatVisible" title="GB/T 7714 格式" width="600px">
        <div class="format-result">{{ formatted }}</div>
        <template #footer>
          <el-button @click="copyFormat">{{ formatted ? '复制' : '关闭' }}</el-button>
        </template>
      </el-dialog>

      <!-- 编辑对话框 -->
      <el-dialog v-model="dialogVisible" :title="editing ? '编辑文献' : '新增文献'" width="650px">
        <el-form :model="form" label-width="80px" size="small">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="文献类型" required>
                <el-select v-model="form.type" placeholder="选择类型" style="width:100%">
                  <el-option v-for="(l, k) in REF_TYPE_LABEL" :key="k" :label="l" :value="k" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="年份">
                <el-input-number v-model="form.year" :min="1900" :max="2099" :step="1" style="width:100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="作者" required>
            <el-input v-model="form.authors" placeholder="多作者用逗号分隔，如：张三, 李四" />
          </el-form-item>
          <el-form-item label="标题" required>
            <el-input v-model="form.title" placeholder="文献标题" />
          </el-form-item>

          <el-row :gutter="16">
            <el-col :span="12" v-if="form.type === 'J'">
              <el-form-item label="期刊">
                <el-input v-model="form.journal" placeholder="期刊名称" />
              </el-form-item>
            </el-col>
            <el-col :span="12" v-if="form.type === 'J'">
              <el-form-item label="卷号">
                <el-input v-model="form.volume" placeholder="卷号" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16" v-if="form.type === 'J'">
            <el-col :span="12">
              <el-form-item label="期号">
                <el-input v-model="form.issue" placeholder="期号" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="页码">
                <el-input v-model="form.pages" placeholder="如 120-135" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16" v-if="form.type === 'M'">
            <el-col :span="12">
              <el-form-item label="出版社">
                <el-input v-model="form.publisher" placeholder="出版社" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="出版地">
                <el-input v-model="form.address" placeholder="出版地" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item v-if="form.type === 'C'" label="会议">
            <el-input v-model="form.conference" placeholder="会议名称" />
          </el-form-item>

          <el-form-item v-if="form.type === 'D'" label="学位授予单位">
            <el-input v-model="form.institution" placeholder="学位授予单位" />
          </el-form-item>

          <template v-if="form.type === 'EB'">
            <el-form-item label="链接">
              <el-input v-model="form.url" placeholder="URL" />
            </el-form-item>
            <el-form-item label="引用日期">
              <el-input v-model="form.accessDate" placeholder="如 2024-01-01" />
            </el-form-item>
          </template>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
        </template>
      </el-dialog>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { REF_TYPE_LABEL } from '@/utils/format'
import { getReferences, createReference, updateReference, deleteReference, formatReference } from '@/api/reference'
import type { Reference } from '@/types'

const loading = ref(false)
const allRefs = ref<Reference[]>([])
const keyword = ref('')
const typeFilter = ref('')
const dialogVisible = ref(false)
const formatVisible = ref(false)
const formatted = ref('')
const editing = ref(false)
const saving = ref(false)
const form = ref<any>({ authors: '', title: '', type: 'J' })

const filtered = computed(() => {
  let list = allRefs.value
  if (keyword.value) {
    const kw = keyword.value.toLowerCase()
    list = list.filter(r => r.title.toLowerCase().includes(kw) || r.authors.toLowerCase().includes(kw))
  }
  if (typeFilter.value) list = list.filter(r => r.type === typeFilter.value)
  return list
})

let timer: any = null
function onSearch() { clearTimeout(timer); timer = setTimeout(fetch, 300) }

async function fetch() {
  loading.value = true
  try {
    const res = await getReferences()
    allRefs.value = res.data || []
  } catch { /* handled */ }
  finally { loading.value = false }
}

function showDialog(refItem?: Reference) {
  if (refItem) {
    editing.value = true
    form.value = { ...refItem }
  } else {
    editing.value = false
    form.value = { authors: '', title: '', type: 'J', year: undefined, journal: '', volume: '', issue: '', pages: '', publisher: '', address: '', conference: '', institution: '', url: '', accessDate: '' }
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.value.authors || !form.value.title) {
    ElMessage.warning('请填写作者和标题')
    return
  }
  saving.value = true
  try {
    if (editing.value) {
      await updateReference(form.value.id, form.value)
      ElMessage.success('已更新')
    } else {
      await createReference(form.value)
      ElMessage.success('已添加')
    }
    dialogVisible.value = false
    await fetch()
  } catch { /* handled */ }
  finally { saving.value = false }
}

async function handleDelete(refItem: Reference) {
  try {
    await ElMessageBox.confirm(`确定删除"${refItem.title}"？`, '确认', { type: 'warning' })
    await deleteReference(refItem.id)
    ElMessage.success('已删除')
    await fetch()
  } catch { /* 取消 */ }
}

async function handleFormat(refItem: Reference) {
  try {
    const res = await formatReference(refItem.id)
    formatted.value = res.data.formatted
    formatVisible.value = true
  } catch { /* handled */ }
}

async function copyFormat() {
  if (!formatted.value) return
  try {
    await navigator.clipboard.writeText(formatted.value)
    ElMessage.success('已复制')
  } catch {
    ElMessage.warning('复制失败，请手动复制')
  }
}

onMounted(fetch)
</script>

<style scoped lang="scss">
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;
  h2 { font-size: 22px; font-weight: 700; margin: 0; }
}
.toolbar { display: flex; gap: 12px; margin-bottom: 20px; flex-wrap: wrap; }
.format-result { background: var(--el-fill-color); padding: 16px; border-radius: 8px; font-size: 14px; line-height: 1.8; white-space: pre-wrap; }
</style>
