// src/api/admin.ts
import request from './request'

/** 保存模板完整配置（含封面、样式、结构） */
export function saveTemplate(data: {
  id?: number
  name: string
  collegeId: number | string
  type: 'GRADUATION' | 'COURSE' | 'PROJECT'
  description?: string
  coverConfig: Array<{ key: string; label: string; required: boolean }>
  styles: { font: string; fontSize: string; lineHeight: string }
  structure: {
    hasDeclaration: boolean
    hasAbstract: boolean
    hasEnglishAbstract: boolean
    maxHeadingLevel: number
    hasAppendix: boolean
    hasReferences: boolean
  }
}) {
  if (data.id) {
    return request.put(`/admin/templates/${data.id}`, data)
  }
  return request.post('/admin/templates', data)
}

/** 获取模板详情（含完整配置） */
export function getTemplateDetail(id: number) {
  return request.get(`/admin/templates/${id}`)
}

/** 预览模板 */
export function previewTemplate(id: number) {
  return request.get(`/admin/templates/${id}/preview`)
}

// ============ 系统管理（扩展） ============

/** 获取系统统计信息 */
export function getSystemStats() {
  return request.get('/admin/stats')
}

/** 获取所有用户列表（管理员专用） */
export function getUserList(params: { page: number; size: number; role?: string; keyword?: string }) {
  return request.get('/admin/users', { params })
}
