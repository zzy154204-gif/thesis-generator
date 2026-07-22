<template>
  <AuthLayout>
    <div class="form-wrap">
      <h2 class="title">欢迎回来</h2>
      <p class="subtitle">请登录您的账号</p>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" class="btn" @click="handleLogin">
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="footer">
        <span class="text">还没有账号？</span>
        <router-link to="/register" class="link">立即注册</router-link>
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
const auth = useAuthStore()
const loading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({ username: '', password: '' })
const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  if (!formRef.value) return
  await formRef.value.validate(async (ok) => {
    if (!ok) return
    loading.value = true
    try {
      await auth.login({ username: form.username, password: form.password })
      ElMessage.success('登录成功')
      // 按角色跳转
      const redirect = route.query.redirect as string
      if (redirect) {
        router.push(redirect)
      } else if (auth.role === 'TEACHER') {
        router.push('/teacher/review')
      } else if (auth.role === 'ADMIN') {
        router.push('/admin/templates')
      } else {
        router.push('/papers')
      }
    } catch { /* 已在拦截器处理 */ }
    finally { loading.value = false }
  })
}
</script>

<style scoped lang="scss">
.form-wrap { width: 100%; max-width: 360px; }
.title { font-size: 24px; font-weight: 700; color: var(--el-text-color-primary); margin-bottom: 6px; }
.subtitle { font-size: 14px; color: var(--el-text-color-secondary); margin-bottom: 32px; }
.btn { width: 100%; height: 44px; font-size: 15px; letter-spacing: 2px; }
.footer { margin-top: 24px; text-align: center; font-size: 14px; }
.text { color: var(--el-text-color-secondary); margin-right: 4px; }
.link { color: var(--el-color-primary); font-weight: 500; }
</style>
