<template>
  <DefaultLayout>
    <div class="page">
      <div class="header">
        <h2>学院管理</h2>
        <el-button type="primary" :icon="Plus" @click="showDialog()">新增学院</el-button>
      </div>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="code" label="代码" width="120" />
        <el-table-column prop="name" label="学院名称" min-width="200" />
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template #default="{ row }">{{ row.createdAt?.split('T')[0] }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="showDialog(row)">编辑</el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !list.length" description="暂无学院" :image-size="120" />

      <!-- 编辑对话框 -->
      <el-dialog v-model="visible" :title="editing ? '编辑学院' : '新增学院'" width="450px">
        <el-form :model="form" label-width="80px" :rules="rules" ref="formRef">
          <el-form-item label="学院名称" prop="name">
            <el-input v-model="form.name" placeholder="如 计算机科学与技术学院" />
          </el-form-item>
          <el-form-item label="学院代码" prop="code">
            <el-input v-model="form.code" placeholder="如 CS" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="visible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
        </template>
      </el-dialog>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { getColleges, createCollege, updateCollege, deleteCollege } from '@/api/college'
import type { College } from '@/types'

const loading = ref(false)
const list = ref<College[]>([])
const visible = ref(false)
const editing = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive({ name: '', code: '' })
const rules = {
  name: [{ required: true, message: '请输入学院名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入学院代码', trigger: 'blur' }],
}

async function fetch() {
  loading.value = true
  try {
    const res = await getColleges()
    list.value = res.data || []
  } catch { /* handled */ }
  finally { loading.value = false }
}

function showDialog(col?: College) {
  if (col) {
    editing.value = true
    form.name = col.name
    form.code = col.code
  } else {
    editing.value = false
    form.name = ''
    form.code = ''
  }
  visible.value = true
}

async function handleSave() {
  if (!formRef.value) return
  await formRef.value.validate(async (ok: boolean) => {
    if (!ok) return
    saving.value = true
    try {
      if (editing.value) {
        const item = list.value.find(c => c.code === form.code || c.name === form.name)
        if (item) await updateCollege(item.id, { name: form.name, code: form.code })
        ElMessage.success('已更新')
      } else {
        await createCollege({ name: form.name, code: form.code })
        ElMessage.success('已添加')
      }
      visible.value = false
      await fetch()
    } catch { /* handled */ }
    finally { saving.value = false }
  })
}

async function handleDelete(col: College) {
  try {
    await ElMessageBox.confirm(`确定删除"${col.name}"？`, '确认', { type: 'warning' })
    await deleteCollege(col.id)
    ElMessage.success('已删除')
    await fetch()
  } catch { /* 取消 */ }
}

onMounted(fetch)
</script>

<style scoped lang="scss">
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;
  h2 { font-size: 22px; font-weight: 700; margin: 0; }
}
</style>
