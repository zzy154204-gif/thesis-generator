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

// TODO: 待后端实现以下接口后启用
// GET /auth/profile  — 获取当前用户信息
// PUT /auth/profile  — 修改个人信息
// PUT /auth/password — 修改密码
