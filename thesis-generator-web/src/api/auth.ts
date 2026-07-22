import request from './request'
import type { ApiResult, LoginRequest, RegisterRequest, LoginResponse } from '@/types'

export function login(data: LoginRequest) {
  return request.post<unknown, ApiResult<LoginResponse>>('/auth/login', data)
}

export function register(data: RegisterRequest) {
  return request.post<unknown, ApiResult<LoginResponse>>('/auth/register', data)
}

export function logout() {
  return request.post<unknown, ApiResult<null>>('/auth/logout')
}

export function updateProfile(realName: string) {
  return request.put<unknown, ApiResult<null>>('/auth/profile', { realName })
}

export function changePassword(oldPassword: string, newPassword: string) {
  return request.put<unknown, ApiResult<null>>('/auth/password', { oldPassword, newPassword })
}
