<template>
  <AuthLayout>
    <div class="login-form-wrapper">
      <h2 class="form-title">欢迎回来</h2>
      <p class="form-subtitle">请登录您的账号</p>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        size="large"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            show-password
            :prefix-icon="Lock"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="loading" class="submit-btn" @click="handleLogin">
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="form-footer">
        <span class="footer-text">还没有账号？</span>
        <router-link to="/register" class="footer-link">立即注册</router-link>
      </div>
    </div>
  </AuthLayout>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import AuthLayout from '@/layouts/AuthLayout.vue'
import { useAuthStore } from '@/stores/auth'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const loading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({ username: '', password: '' })

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不少于6位', trigger: 'blur' },
  ],
}

async function handleLogin() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await authStore.login({ username: form.username, password: form.password })
      ElMessage.success('登录成功')
      const redirect = (route.query.redirect as string) || '/papers'
      router.push(redirect)
    } catch {
      // 错误已在拦截器中处理
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped lang="scss">
.login-form-wrapper {
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
  margin-bottom: 32px;
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

  .footer-text {
    color: var(--el-text-color-secondary);
    margin-right: 4px;
  }

  .footer-link {
    color: var(--el-color-primary);
    font-weight: 500;
    &:hover { text-decoration: underline; }
  }
}
</style>
