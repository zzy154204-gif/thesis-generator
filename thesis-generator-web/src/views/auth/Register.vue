<template>
  <AuthLayout>
    <div class="register-form-wrapper">
      <h2 class="form-title">创建账号</h2>
      <p class="form-subtitle">注册后即可开始使用</p>

      <el-form ref="formRef" :model="form" :rules="rules" size="large">
        <el-form-item prop="role">
          <el-select v-model="form.role" placeholder="选择身份" style="width: 100%">
            <el-option label="我是学生" value="STUDENT" />
            <el-option label="我是教师" value="TEACHER" />
          </el-select>
        </el-form-item>

        <el-form-item prop="username">
          <el-input v-model="form.username" :placeholder="form.role === 'TEACHER' ? '工号' : '学号'" />
        </el-form-item>

        <el-form-item prop="realName">
          <el-input v-model="form.realName" placeholder="真实姓名" />
        </el-form-item>

        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码（至少6位）" show-password />
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" show-password />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="loading" class="submit-btn" @click="handleRegister">
            {{ loading ? '注册中...' : '注 册' }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="form-footer">
        <span class="footer-text">已有账号？</span>
        <router-link to="/login" class="footer-link">立即登录</router-link>
      </div>
    </div>
  </AuthLayout>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AuthLayout from '@/layouts/AuthLayout.vue'
import { useAuthStore } from '@/stores/auth'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  username: '',
  realName: '',
  password: '',
  confirmPassword: '',
  role: 'STUDENT' as string,
})

const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
  if (value !== form.password) callback(new Error('两次密码不一致'))
  else callback()
}

const idLabel = computed(() => form.role === 'TEACHER' ? '工号' : '学号')

const rules = computed<FormRules>(() => ({
  username: [
    { required: true, message: `请输入${idLabel.value}`, trigger: 'blur' },
  ],
  realName: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不少于6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
  role: [{ required: true, message: '请选择身份', trigger: 'change' }],
}))

async function handleRegister() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await authStore.register({
        username: form.username,
        password: form.password,
        realName: form.realName,
        role: form.role,
      })
      ElMessage.success('注册成功，已自动登录')
      router.push('/papers')
    } catch {
      // 错误已在拦截器中处理
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped lang="scss">
.register-form-wrapper {
  width: 100%;
  max-width: 360px;
}

.form-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  margin-bottom: 6px;
}

.form-subtitle {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin-bottom: 28px;
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  letter-spacing: 2px;
}

.form-footer {
  margin-top: 24px;
  text-align: center;
  font-size: 14px;
  .footer-text { color: var(--el-text-color-secondary); margin-right: 4px; }
  .footer-link { color: var(--el-color-primary); font-weight: 500; }
}
</style>
