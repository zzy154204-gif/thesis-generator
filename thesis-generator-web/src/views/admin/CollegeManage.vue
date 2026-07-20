<!-- src/views/admin/CollegeManage.vue -->
<template>
  <DefaultLayout>
    <div class="college-manage">
      <!-- 工具栏 -->
      <div class="toolbar">
        <el-button type="primary" @click="openDialog()">
          <el-icon><Plus /></el-icon> 新增学院
        </el-button>
        <el-input
          v-model="keyword"
          placeholder="搜索学院名称/代码"
          style="width: 240px; margin-left: 16px"
          clearable
          @keyup.enter="fetchData"
        />
        <el-button @click="fetchData">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <!-- 表格 -->
      <el-table :data="tableData" border v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="name" label="学院名称" min-width="150" />
        <el-table-column prop="code" label="学院代码" width="120" align="center" />
        <el-table-column prop="templateCount" label="关联模板" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.templateCount > 0 ? 'primary' : 'info'" size="small">
              {{ row.templateCount }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" align="center">
          <template #default="{ row }">
            {{ new Date(row.createdAt).toLocaleString('zh-CN') }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDialog(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="fetchData"
        @size-change="fetchData"
        style="margin-top: 20px; justify-content: flex-end"
      />

      <!-- 新增/编辑弹窗 -->
      <el-dialog
        v-model="dialogVisible"
        :title="dialogTitle"
        width="500px"
        destroy-on-close
      >
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="100px"
        >
          <el-form-item label="学院名称" prop="name">
            <el-input v-model="form.name" placeholder="请输入学院名称（2-50字符）" />
          </el-form-item>
          <el-form-item label="学院代码" prop="code">
            <el-input v-model="form.code" placeholder="请输入学院代码（2-10字符）" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
            确定
          </el-button>
        </template>
      </el-dialog>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { getColleges, createCollege, updateCollege, deleteCollege } from '@/api/college'

// --- 状态 ---
const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({ id: 0, name: '', code: '' })

// --- 表单校验 ---
const rules: FormRules = {
  name: [
    { required: true, message: '请输入学院名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入学院代码', trigger: 'blur' },
    { min: 2, max: 10, message: '长度在 2 到 10 个字符', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9]+$/, message: '仅允许字母和数字', trigger: 'blur' }
  ]
}

const dialogTitle = computed(() => isEdit.value ? '编辑学院' : '新增学院')

// --- 数据加载 ---
async function fetchData() {
  loading.value = true
  try {
    const res = await getColleges()
    const all = res.data || []
    // 关键字过滤
    const filtered = keyword.value
      ? all.filter((c: any) => c.name?.includes(keyword.value) || c.code?.includes(keyword.value))
      : all
    total.value = filtered.length
    // 简易分页
    const from = (page.value - 1) * pageSize.value
    tableData.value = filtered.slice(from, from + pageSize.value)
  } catch {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// --- 弹窗操作 ---
function openDialog(row?: any) {
  isEdit.value = !!row
  if (row) {
    form.id = row.id
    form.name = row.name
    form.code = row.code || ''
  } else {
    form.id = 0
    form.name = ''
    form.code = ''
  }
  dialogVisible.value = true
}

// --- 提交 ---
async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      if (isEdit.value) {
        await updateCollege(form.id, { name: form.name, code: form.code })
        ElMessage.success('修改成功')
      } else {
        await createCollege({ name: form.name, code: form.code })
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      fetchData()
    } catch {
      ElMessage.error('操作失败')
    } finally {
      submitLoading.value = false
    }
  })
}

// --- 删除 ---
async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(
      `确定要删除【${row.name}】吗？此操作不可恢复。`,
      '警告',
      { confirmButtonText: '确定删除', type: 'warning' }
    )
    await deleteCollege(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch {}
}

// --- 搜索重置 ---
function resetSearch() {
  keyword.value = ''
  page.value = 1
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped lang="scss">
.college-manage {
  .toolbar {
    display: flex;
    align-items: center;
    margin-bottom: 20px;
    flex-wrap: wrap;
    gap: 8px;
  }
}
</style>
