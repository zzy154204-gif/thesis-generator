// src/api/template.ts
import request from './request'
import type { ApiResult, Template, TemplateVersion } from '@/types/api'

// ============================================================
// 学生端接口（只读）
// ============================================================

/** 获取模板列表（学生端：仅返回已启用的模板） */
export function getTemplates(params?: {
  type?: 'GRADUATION' | 'COURSE' | 'PROJECT'
  collegeId?: number
}): Promise<ApiResult<Template[]>> {
  return request.get('/templates', { params })
}

/** 获取模板详情（学生端） */
export function getTemplate(id: number): Promise<ApiResult<Template>> {
  return request.get(`/templates/${id}`)
}

/** 获取模板版本列表 */
export function getTemplateVersions(templateId: number): Promise<ApiResult<TemplateVersion[]>> {
  return request.get(`/templates/${templateId}/versions`)
}

/** 获取可用模板列表（学生创建论文时可选） */
export function getAvailableTemplates(params?: {
  type?: 'GRADUATION' | 'COURSE' | 'PROJECT'
  collegeId?: number
}): Promise<ApiResult<Template[]>> {
  return request.get('/templates/available', { params })
}

// ============================================================
// 管理员端接口（读写）
// ============================================================

/** 获取模板列表（管理员端：含所有状态） */
export function getAdminTemplates(params?: {
  type?: 'GRADUATION' | 'COURSE' | 'PROJECT'
  collegeId?: number
}): Promise<ApiResult<Template[]>> {
  return request.get('/templates', { params })
}

/** 获取模板详情（管理员端：含完整配置） */
export function getAdminTemplate(id: number): Promise<ApiResult<Template>> {
  return request.get(`/templates/${id}`)
}

/** 创建模板 */
export function createTemplate(data: {
  name: string
  type: 'GRADUATION' | 'COURSE' | 'PROJECT'
  collegeId: number
  description?: string
  coverConfig?: Array<{ key: string; label: string; required: boolean }>
  styles?: { font: string; fontSize: string; lineHeight: string }
  structure?: {
    hasDeclaration: boolean
    hasAbstract: boolean
    hasEnglishAbstract: boolean
    maxHeadingLevel: number
    hasAppendix: boolean
    hasReferences: boolean
  }
}): Promise<ApiResult<Template>> {
  return request.post('/templates', data)
}

/** 更新模板 */
export function updateTemplate(
  id: number,
  data: {
    name?: string
    type?: 'GRADUATION' | 'COURSE' | 'PROJECT'
    collegeId?: number
    description?: string
    coverConfig?: Array<{ key: string; label: string; required: boolean }>
    styles?: { font: string; fontSize: string; lineHeight: string }
    structure?: {
      hasDeclaration: boolean
      hasAbstract: boolean
      hasEnglishAbstract: boolean
      maxHeadingLevel: number
      hasAppendix: boolean
      hasReferences: boolean
    }
  }
): Promise<ApiResult<Template>> {
  return request.put(`/templates/${id}`, data)
}

/** 切换模板启用/停用 */
export function toggleTemplateStatus(
  id: number,
  status: 'ENABLED' | 'DISABLED'
): Promise<ApiResult<Template>> {
  return request.patch(`/templates/${id}/status`, { status })
}

/** 删除模板 */
export function deleteTemplate(id: number): Promise<ApiResult<void>> {
  return request.delete(`/templates/${id}`)
}

/** 预览模板（返回 HTML 预览内容） */
export function previewTemplate(id: number): Promise<ApiResult<{ html: string }>> {
  return request.get(`/templates/${id}/preview`)
}

/** 复制模板 */
export function duplicateTemplate(id: number): Promise<ApiResult<Template>> {
  return request.post(`/templates/${id}/duplicate`)
}
