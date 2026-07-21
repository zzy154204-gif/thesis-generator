<template>
  <DefaultLayout>
    <div class="reference-manage">
      <div class="page-header">
        <h3>参考文献管理</h3>
        <el-button type="primary" @click="openDialog()">
          <el-icon><Plus /></el-icon> 新增文献
        </el-button>
      </div>

      <!-- 搜索栏 -->
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索标题或作者..."
          style="width: 240px"
          clearable
          @keyup.enter="search"
        />
        <el-button @click="search">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <el-table :data="tableData" border v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column prop="authors" label="作者" min-width="160" />
        <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="year" label="年份" width="80" align="center" />
        <el-table-column prop="journal" label="期刊/来源" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDialog(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog
        v-model="dialogVisible"
        :title="isEdit ? '编辑文献' : '新增文献'"
        width="700px"
        destroy-on-close
      >
        <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="作者" prop="authors">
                <el-input v-model="form.authors" placeholder="多作者用逗号分隔" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="文献类型" prop="type">
                <el-select v-model="form.type" style="width: 100%">
                  <el-option label="期刊文章 [J]" value="J" />
                  <el-option label="专著/书籍 [M]" value="M" />
                  <el-option label="会议论文 [C]" value="C" />
                  <el-option label="学位论文 [D]" value="D" />
                  <el-option label="网络资源 [EB]" value="EB" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="标题" prop="title">
            <el-input v-model="form.title" placeholder="文献标题" />
          </el-form-item>
          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item label="年份" prop="year">
                <el-input-number v-model="form.year" :min="1900" :max="2099" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="卷号">
                <el-input v-model="form.volume" placeholder="卷号" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="期号">
                <el-input v-model="form.issue" placeholder="期号" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="期刊/刊物" prop="journal">
            <el-input v-model="form.journal" placeholder="期刊或出版物名称" />
          </el-form-item>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="页码">
                <el-input v-model="form.pages" placeholder="如 120-135" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="DOI">
                <el-input v-model="form.doi" placeholder="DOI 编号" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="出版社" v-if="form.type === 'M'">
            <el-input v-model="form.publisher" placeholder="出版社名称" />
          </el-form-item>
          <el-form-item label="URL" v-if="form.type === 'EB'">
            <el-input v-model="form.url" placeholder="访问链接" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
        </template>
      </el-dialog>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { getReferences, createReference, updateReference, deleteReference, searchByAuthor, searchByTitle } from '@/api/reference'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<any[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const keyword = ref('')

const form = reactive({
  id: 0,
  authors: '',
  title: '',
  type: 'J',
  year: new Date().getFullYear(),
  journal: '',
  volume: '',
  issue: '',
  pages: '',
  doi: '',
  publisher: '',
  url: '',
})

const rules: FormRules = {
  authors: [{ required: true, message: '请输入作者', trigger: 'blur' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getReferences()
    tableData.value = res.data || []
  } catch {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

async function search() {
  if (!keyword.value) {
    fetchData()
    return
  }
  loading.value = true
  try {
    const [byAuthor, byTitle] = await Promise.all([
      searchByAuthor(keyword.value).catch(() => ({ data: [] })),
      searchByTitle(keyword.value).catch(() => ({ data: [] })),
    ])
    const merged = [...(byAuthor.data || []), ...(byTitle.data || [])]
    const seen = new Set<number>()
    tableData.value = merged.filter((item: any) => {
      if (seen.has(item.id)) return false
      seen.add(item.id)
      return true
    })
  } catch {
    ElMessage.error('搜索失败')
  } finally {
    loading.value = false
  }
}

function resetSearch() {
  keyword.value = ''
  fetchData()
}

function openDialog(row?: any) {
  isEdit.value = !!row
  if (row) {
    Object.assign(form, row)
  } else {
    form.id = 0
    form.authors = ''
    form.title = ''
    form.type = 'J'
    form.year = new Date().getFullYear()
    form.journal = ''
    form.volume = ''
    form.issue = ''
    form.pages = ''
    form.doi = ''
    form.publisher = ''
    form.url = ''
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      if (isEdit.value) {
        await updateReference(form.id, form as any)
        ElMessage.success('修改成功')
      } else {
        await createReference(form as any)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      fetchData()
    } catch {
      // 错误已在拦截器中处理
    } finally {
      submitLoading.value = false
    }
  })
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确定要删除该文献吗？`, '警告', {
      confirmButtonText: '确定删除',
      type: 'warning',
    })
    await deleteReference(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch {
    // 用户取消
  }
}

onMounted(fetchData)
</script>

<style scoped lang="scss">
.reference-manage {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    h3 { margin: 0; }
  }
  .toolbar {
    display: flex;
    gap: 8px;
    margin-bottom: 16px;
    align-items: center;
  }
}
</style>
