import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, register as registerApi, logout as logoutApi } from '@/api/auth'
import { setToken, removeToken, getToken } from '@/utils/token'
import type { LoginRequest, RegisterRequest, UserInfo } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserInfo | null>(null)
  const token = ref<string | null>(getToken())

  const isLoggedIn = computed(() => !!token.value)
  const role = computed(() => user.value?.role || null)
  const isStudent = computed(() => role.value === 'STUDENT')
  const isTeacher = computed(() => role.value === 'TEACHER')
  const isAdmin = computed(() => role.value === 'ADMIN')

  function parseUserFromToken(tokenStr: string): UserInfo | null {
    try {
      // base64url → base64，并解码 UTF-8
      let base64 = tokenStr.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
      while (base64.length % 4) base64 += '='
      const json = decodeURIComponent(escape(atob(base64)))
      const payload = JSON.parse(json)
      return {
        userId: payload.userId,
        username: payload.username || '',
        realName: payload.realName || payload.username || '',
        role: payload.role || '',
        collegeId: payload.collegeId,
      }
    } catch {
      return null
    }
  }

  function setAuth(data: { token: string; userId: number; username: string; realName: string; role: string }) {
    token.value = data.token
    user.value = {
      userId: data.userId,
      username: data.username,
      realName: data.realName,
      role: data.role as UserInfo['role'],
    }
    setToken(data.token)
    localStorage.setItem('thesis_role', data.role)
  }

  // 初始化：从已有 token 解析用户信息
  if (token.value && !user.value) {
    const parsed = parseUserFromToken(token.value)
    if (parsed) {
      user.value = parsed
      localStorage.setItem('thesis_role', parsed.role)
    }
  }

  async function login(data: LoginRequest) {
    const res = await loginApi(data)
    setAuth(res.data)
  }

  async function register(data: RegisterRequest) {
    const res = await registerApi(data)
    setAuth(res.data)
  }

  async function logout() {
    try { await logoutApi() } catch { /* ignore */ }
    token.value = null
    user.value = null
    removeToken()
    localStorage.removeItem('thesis_role')
  }

  function refreshUser(realName: string) {
    if (user.value) user.value.realName = realName
  }

  return { user, token, isLoggedIn, role, isStudent, isTeacher, isAdmin, login, register, logout, refreshUser }
})
