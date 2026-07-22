import request from './request'
import type { ApiResult, Template, TemplateVersion } from '@/types'

/** 学生端：获取已启用的模板 */
export function getAvailableTemplates(params?: { type?: string; collegeId?: number }) {
  return request.get<unknown, ApiResult<Template[]>>('/templates/available', { params })
}

/** 学生端：获取模板列表 */
export function getTemplates(params?: { type?: string; collegeId?: number }) {
  return request.get<unknown, ApiResult<Template[]>>('/templates', { params })
}

export function getTemplate(id: number) {
  return request.get<unknown, ApiResult<Template>>(`/templates/${id}`)
}

export function getTemplateVersions(id: number) {
  return request.get<unknown, ApiResult<TemplateVersion[]>>(`/templates/${id}/versions`)
}

/** 根据版本 ID 获取版本详情（含 formatConfig） */
export function getTemplateVersion(id: number) {
  return request.get<unknown, ApiResult<TemplateVersion>>(`/templates/versions/${id}`)
}
