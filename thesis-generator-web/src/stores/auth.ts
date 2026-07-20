import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, register as registerApi, logout as logoutApi, getProfile } from '@/api/auth'
import { setToken, removeToken, getToken } from '@/utils/token'
import type { UserInfo, LoginRequest, RegisterRequest } from '@/types/api'

export const useAuthStore = defineStore('auth', () => {
  // 从 localStorage 恢复用户信息
  const savedUser = localStorage.getItem('user')
  const user = ref<UserInfo | null>(savedUser ? JSON.parse(savedUser) : null)
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
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  async function login(data: LoginRequest) {
    const res = await loginApi(data)
    setAuthFromResponse(res.data)
  }

  async function register(data: RegisterRequest) {
    const res = await registerApi(data)
    setAuthFromResponse(res.data)
  }

  async function fetchProfile() {
    if (!token.value) return
    try {
      const res = await getProfile()
      if (res.data) {
        user.value = {
          userId: res.data.userId,
          username: res.data.username,
          realName: res.data.realName,
          role: res.data.role,
        }
        localStorage.setItem('user', JSON.stringify(user.value))
      }
    } catch {
      // 后端 profile 接口尚未实现时，保持当前用户信息不变
    }
  }

  function logout() {
    logoutApi().catch(() => {})
    token.value = null
    user.value = null
    removeToken()
    localStorage.removeItem('user')
  }

  return { user, token, isLoggedIn, isStudent, isTeacher, login, register, fetchProfile, logout }
})
