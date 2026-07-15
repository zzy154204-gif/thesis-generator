import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, register as registerApi, logout as logoutApi, getProfile } from '@/api/auth'
import { setToken, removeToken, getToken } from '@/utils/token'
import type { UserInfo, LoginRequest, RegisterRequest } from '@/types/api'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserInfo | null>(null)
  const token = ref<string | null>(getToken())

  const isLoggedIn = computed(() => !!token.value)
  const isStudent = computed(() => user.value?.role === 'STUDENT')
  const isTeacher = computed(() => user.value?.role === 'TEACHER')

  async function login(data: LoginRequest) {
    const res = await loginApi(data)
    token.value = res.data.token
    user.value = res.data.user
    setToken(res.data.token)
  }

  async function register(data: RegisterRequest) {
    const res = await registerApi(data)
    token.value = res.data.token
    user.value = res.data.user
    setToken(res.data.token)
  }

  async function fetchProfile() {
    if (!token.value) return
    try {
      const res = await getProfile()
      user.value = res.data
    } catch {
      // token 可能过期，清除
      token.value = null
      user.value = null
      removeToken()
    }
  }

  function logout() {
    logoutApi().catch(() => {})
    token.value = null
    user.value = null
    removeToken()
  }

  return { user, token, isLoggedIn, isStudent, isTeacher, login, register, fetchProfile, logout }
})
