<template>
  <DefaultLayout>
    <div class="page">
      <h2 class="page-title">个人中心</h2>

      <el-row :gutter="24">
        <el-col :span="12">
          <el-card>
            <template #header><span>基本信息</span></template>
            <el-form label-width="80px">
              <el-form-item label="用户名">
                <el-input :model-value="auth.user?.username" disabled />
              </el-form-item>
              <el-form-item label="姓名">
                <el-input v-model="realName" />
              </el-form-item>
              <el-form-item label="角色">
                <el-input :model-value="roleLabel" disabled />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="updating" @click="handleUpdateProfile">保存修改</el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </el-col>

        <el-col :span="12">
          <el-card>
            <template #header><span>修改密码</span></template>
            <el-form :model="pwdForm" :rules="pwdRules" ref="pwdRef" label-width="100px">
              <el-form-item label="当前密码" prop="oldPassword">
                <el-input v-model="pwdForm.oldPassword" type="password" show-password />
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input v-model="pwdForm.newPassword" type="password" show-password />
              </el-form-item>
              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="changingPwd" @click="handleChangePassword">修改密码</el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { updateProfile, changePassword } from '@/api/auth'

const auth = useAuthStore()
const realName = ref(auth.user?.realName || '')
const updating = ref(false)
const changingPwd = ref(false)
const pwdRef = ref()

const roleLabel = computed(() => {
  const map: Record<string, string> = { STUDENT: '学生', TEACHER: '教师', ADMIN: '管理员' }
  return map[auth.user?.role || ''] || ''
})

const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: (_r: any, v: string, cb: any) => v === pwdForm.value.newPassword ? cb() : cb(new Error('两次密码不一致')), trigger: 'blur' },
  ],
}

async function handleUpdateProfile() {
  updating.value = true
  try {
    await updateProfile(realName.value)
    auth.refreshUser(realName.value)
    ElMessage.success('已更新')
  } catch { /* handled */ }
  finally { updating.value = false }
}

async function handleChangePassword() {
  if (!pwdRef.value) return
  await pwdRef.value.validate(async (ok: boolean) => {
    if (!ok) return
    changingPwd.value = true
    try {
      await changePassword(pwdForm.value.oldPassword, pwdForm.value.newPassword)
      ElMessage.success('密码已修改，请重新登录')
      auth.logout()
    } catch { /* handled */ }
    finally { changingPwd.value = false }
  })
}
</script>

<style scoped lang="scss">
.page-title { font-size: 22px; font-weight: 700; margin-bottom: 24px; }
</style>
