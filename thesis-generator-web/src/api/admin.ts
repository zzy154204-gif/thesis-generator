import request from './request'
import type { ApiResult, Template, TemplateDetail } from '@/types'

export interface AdminTemplateQuery {
  type?: string
  collegeId?: number
  status?: string
  keyword?: string
}

export function getAdminTemplates(params?: AdminTemplateQuery) {
  return request.get<unknown, ApiResult<Template[]>>('/admin/templates', { params })
}

export function getAdminTemplate(id: number) {
  return request.get<unknown, ApiResult<TemplateDetail>>(`/admin/templates/${id}`)
}

export function createAdminTemplate(data: Record<string, any>) {
  return request.post<unknown, ApiResult<Template>>('/admin/templates', data)
}

export function updateAdminTemplate(id: number, data: Record<string, any>) {
  return request.put<unknown, ApiResult<TemplateDetail>>(`/admin/templates/${id}`, data)
}

export function toggleTemplateStatus(id: number, status: string) {
  return request.patch<unknown, ApiResult<Template>>(`/admin/templates/${id}/status`, { status })
}

export function deleteAdminTemplate(id: number) {
  return request.delete<unknown, ApiResult<null>>(`/admin/templates/${id}`)
}

export function duplicateTemplate(id: number) {
  return request.post<unknown, ApiResult<Template>>(`/admin/templates/${id}/duplicate`)
}
