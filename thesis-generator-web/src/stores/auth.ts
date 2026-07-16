import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, register as registerApi, logout as logoutApi } from '@/api/auth'
import { setToken, removeToken, getToken } from '@/utils/token'
import type { UserInfo, LoginRequest, RegisterRequest } from '@/types/api'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserInfo | null>(null)
  const token = ref<string | null>(getToken())

  const isLoggedIn = computed(() => !!token.value)
  const isStudent = computed(() => user.value?.role === 'STUDENT')
  const isTeacher = computed(() => user.value?.role === 'TEACHER')

  // 从后端扁平 AuthResponse 构建 UserInfo
  function setAuthFromResponse(data: { token: string; userId: number; username: string; realName: string; role: string }) {
    token.value = data.token
    user.value = {
      userId: data.userId,
      username: data.username,
      realName: data.realName,
      role: data.role as UserInfo['role'],
    }
    setToken(data.token)
  }

  async function login(data: LoginRequest) {
    const res = await loginApi(data)
    setAuthFromResponse(res.data)
  }

  async function register(data: RegisterRequest) {
    const res = await registerApi(data)
    setAuthFromResponse(res.data)
  }

  // TODO: 待后端实现 GET /auth/profile 后对接
  async function fetchProfile() {
    // 暂时从 token 解析用户信息，后续对接后端 profile 接口
    if (!token.value) return
  }

  function logout() {
    logoutApi().catch(() => {})
    token.value = null
    user.value = null
    removeToken()
  }

  return { user, token, isLoggedIn, isStudent, isTeacher, login, register, fetchProfile, logout }
})
