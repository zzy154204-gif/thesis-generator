import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, removeToken } from '@/utils/token'
import router from '@/router'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 15000,
})

// ---- 公开接口白名单（不要求登录，401/403 不弹过期提示）----
// 注意：config.url 是 Axios 实例接收的相对路径（不含 baseURL），
// 而 baseURL 默认为 /api/v1，所以这里写相对路径即可。
const PUBLIC_URL_PREFIXES = [
  '/auth/login',
  '/auth/register',
  '/colleges',
  '/teacher/reviews/teachers',
]

function isPublicUrl(url: string | undefined): boolean {
  if (!url) return false
  return PUBLIC_URL_PREFIXES.some(prefix => url.startsWith(prefix))
}

/** 当前是否在登录/注册页面 */
function isOnAuthPage(): boolean {
  try {
    return ['/login', '/register'].includes(router.currentRoute.value.path)
  } catch {
    return false
  }
}

// ---- 错误提示防抖 ----
let lastAuthErrorMessage = ''
let lastAuthErrorTime = 0
const AUTH_ERROR_DEBOUNCE_MS = 3000

function showAuthError(message: string) {
  const now = Date.now()
  if (message === lastAuthErrorMessage && now - lastAuthErrorTime < AUTH_ERROR_DEBOUNCE_MS) {
    return // 防抖：3 秒内同一错误不重复弹
  }
  lastAuthErrorMessage = message
  lastAuthErrorTime = now
  ElMessage.error(message)
}

function clearAuthDebounce() {
  lastAuthErrorMessage = ''
  lastAuthErrorTime = 0
}

request.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (e) => Promise.reject(e),
)

request.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body.code !== 200) {
      // 白名单接口的业务错误不抛“登录过期”
      if (!isPublicUrl(res.config?.url) && !isOnAuthPage()) {
        ElMessage.error(body.message || '请求失败')
      }
      return Promise.reject(new Error(body.message))
    }
    return body
  },
  (err) => {
    const status = err.response?.status
    const requestUrl: string | undefined = err.response?.config?.url || err.config?.url

    // ---- 白名单接口 或 已在登录/注册页 → 静默处理 ----
    if (isPublicUrl(requestUrl) || isOnAuthPage()) {
      // 仍移除 token 但绝不弹提示、不跳转
      if (status === 401) {
        removeToken()
        clearAuthDebounce()
      }
      return Promise.reject(err)
    }

    if (status === 401) {
      removeToken()
      clearAuthDebounce()
      showAuthError('登录已过期，请重新登录')
      router.push('/login')
    } else if (status === 403) {
      showAuthError('权限不足')
    } else {
      const msg = err.response?.data?.message || '网络请求失败'
      showAuthError(msg)
    }
    return Promise.reject(err)
  },
)

export default request
