<template>
  <DefaultLayout>
    <div class="college-manage">
      <div class="page-header">
        <h3>学院管理</h3>
        <el-button type="primary" @click="openDialog()">
          <el-icon><Plus /></el-icon> 新增学院
        </el-button>
      </div>

      <el-table :data="tableData" border v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="name" label="学院名称" min-width="200" />
        <el-table-column prop="code" label="学院代码" width="150" align="center" />
        <el-table-column prop="createdAt" label="创建时间" width="180" align="center">
          <template #default="{ row }">
            {{ row.createdAt ? new Date(row.createdAt).toLocaleString('zh-CN') : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDialog(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog
        v-model="dialogVisible"
        :title="isEdit ? '编辑学院' : '新增学院'"
        width="500px"
        destroy-on-close
      >
        <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
          <el-form-item label="学院名称" prop="name">
            <el-input v-model="form.name" placeholder="请输入学院名称" />
          </el-form-item>
          <el-form-item label="学院代码" prop="code">
            <el-input v-model="form.code" placeholder="请输入学院代码" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
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
import { getColleges, createCollege, updateCollege, deleteCollege } from '@/api/college'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<any[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({ id: 0, name: '', code: '' })

const rules: FormRules = {
  name: [{ required: true, message: '请输入学院名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入学院代码', trigger: 'blur' }],
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getColleges()
    tableData.value = res.data || []
  } catch {
    // 请求失败时静默处理
  } finally {
    loading.value = false
  }
}

function openDialog(row?: any) {
  isEdit.value = !!row
  if (row) {
    form.id = row.id
    form.name = row.name
    form.code = row.code
  } else {
    form.id = 0
    form.name = ''
    form.code = ''
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
        await updateCollege(form.id, { name: form.name, code: form.code })
        ElMessage.success('修改成功')
      } else {
        await createCollege({ name: form.name, code: form.code })
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
    await ElMessageBox.confirm(`确定要删除【${row.name}】吗？`, '警告', {
      confirmButtonText: '确定删除',
      type: 'warning',
    })
    await deleteCollege(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch {
    // 用户取消
  }
}

onMounted(fetchData)
</script>

<style scoped lang="scss">
.college-manage {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    h3 { margin: 0; }
  }
}
</style>
