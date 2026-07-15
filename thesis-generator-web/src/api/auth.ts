import request from './request'
import type { ApiResult, AuthResponse, LoginRequest, RegisterRequest, UserInfo } from '@/types/api'

/** 登录 */
export function login(data: LoginRequest): Promise<ApiResult<AuthResponse>> {
  return request.post('/auth/login', data)
}

/** 注册 */
export function register(data: RegisterRequest): Promise<ApiResult<AuthResponse>> {
  return request.post('/auth/register', data)
}

/** 退出登录 */
export function logout(): Promise<ApiResult> {
  return request.post('/auth/logout')
}

/** 获取当前用户信息 */
export function getProfile(): Promise<ApiResult<UserInfo>> {
  return request.get('/auth/profile')
}

/** 修改个人信息 */
export function updateProfile(data: Partial<UserInfo>): Promise<ApiResult> {
  return request.put('/auth/profile', data)
}

/** 修改密码 */
export function changePassword(oldPassword: string, newPassword: string): Promise<ApiResult> {
  return request.put('/auth/password', { oldPassword, newPassword })
}
