<template>
  <DefaultLayout>
    <div class="profile-page">
      <div class="profile-container">
        <h2>个人中心</h2>
        <el-tabs v-model="activeTab">
          <!-- 个人信息 -->
          <el-tab-pane label="个人信息" name="info">
            <el-form :model="infoForm" :rules="infoRules" ref="infoFormRef" label-width="100px" class="profile-form">
              <el-form-item label="学号">
                <el-input :value="authStore.user?.username" disabled />
              </el-form-item>
              <el-form-item label="姓名" prop="realName">
                <el-input v-model="infoForm.realName" />
              </el-form-item>
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="infoForm.email" />
              </el-form-item>
              <el-form-item label="手机号">
                <el-input v-model="infoForm.phone" placeholder="选填" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="saveProfile">保存修改</el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>

          <!-- 修改密码 -->
          <el-tab-pane label="修改密码" name="password">
            <el-form :model="pwdForm" :rules="pwdRules" ref="pwdFormRef" label-width="120px" class="profile-form">
              <el-form-item label="原密码" prop="oldPassword">
                <el-input v-model="pwdForm.oldPassword" type="password" show-password />
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input v-model="pwdForm.newPassword" type="password" show-password />
              </el-form-item>
              <el-form-item label="确认新密码" prop="confirmPassword">
                <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="changePassword">修改密码</el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { useAuthStore } from '@/stores/auth'
import request from '@/api/request'

async function updateProfile(data: Record<string, unknown>): Promise<void> {
  await request.put('/auth/profile', data)
}
async function changePwdApi(data: { oldPassword: string; newPassword: string }): Promise<void> {
  await request.put('/auth/password', data)
}

const authStore = useAuthStore()
const activeTab = ref('info')

// 个人信息表单
const infoForm = reactive({
  realName: authStore.user?.realName || '',
  email: '',
  phone: '',
})

onMounted(async () => {
  try {
    const res = await request.get('/auth/profile')
    const profile = res.data as any
    infoForm.realName = profile.realName || authStore.user?.realName || ''
    infoForm.email = profile.email || ''
    infoForm.phone = profile.phone || ''
  } catch { /* 加载失败使用默认值 */ }
})
const infoFormRef = ref<FormInstance>()
const infoRules: FormRules = {
  realName: [{ required: true, min: 2, max: 20, message: '姓名为2-20个字符', trigger: 'blur' }],
  email: [{ required: true, type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
}

async function saveProfile() {
  if (!infoFormRef.value) return
  await infoFormRef.value.validate(async (valid) => {
    if (!valid) return
    await updateProfile({ realName: infoForm.realName })
    ElMessage.success('保存成功')
  })
}

// 修改密码表单
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdFormRef = ref<FormInstance>()
const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, min: 6, message: '密码长度不少于6位', trigger: 'blur' },
    { pattern: /^(?=.*[a-zA-Z])(?=.*\d)/, message: '需包含字母和数字', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: (_r, v, cb) => v === pwdForm.newPassword ? cb() : cb(new Error('两次密码不一致')), trigger: 'blur' },
  ],
}

async function changePassword() {
  if (!pwdFormRef.value) return
  await pwdFormRef.value.validate(async (valid) => {
    if (!valid) return
    await changePwdApi({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码修改成功，请重新登录')
    authStore.logout()
  })
}
</script>

<style scoped lang="scss">
.profile-page {
  max-width: 640px;
  margin: 0 auto;
  padding: 32px 24px;
}

.profile-container {
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  h2 { margin-bottom: 24px; }
}

.profile-form {
  max-width: 480px;
  margin-top: 16px;
}
</style>
