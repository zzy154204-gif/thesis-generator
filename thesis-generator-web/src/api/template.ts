import request from './request'
import type { ApiResult, Template, TemplateVersion } from '@/types/api'

/** 获取模板列表 */
export function getTemplates(params?: { collegeId?: number }): Promise<ApiResult<Template[]>> {
  return request.get('/templates', { params })
}

/** 获取模板详情 */
export function getTemplate(id: number): Promise<ApiResult<Template>> {
  return request.get(`/templates/${id}`)
}

/** 获取模板版本列表 */
export function getTemplateVersions(templateId: number): Promise<ApiResult<TemplateVersion[]>> {
  return request.get(`/templates/${templateId}/versions`)
}
