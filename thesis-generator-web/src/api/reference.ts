import request from './request'
import type { ApiResult, Reference } from '@/types/api'

// 对齐后端 ReferenceController: /api/references

/** 获取所有参考文献 */
export function getReferences(): Promise<ApiResult<Reference[]>> {
  return request.get('/references')
}

/** 获取单条参考文献 */
export function getReference(id: number): Promise<ApiResult<Reference>> {
  return request.get(`/references/${id}`)
}

/** 新增参考文献 */
export function createReference(data: Omit<Reference, 'id' | 'thesisId'>): Promise<ApiResult<Reference>> {
  return request.post('/references', data)
}

/** 更新参考文献 */
export function updateReference(id: number, data: Partial<Reference>): Promise<ApiResult<Reference>> {
  return request.put(`/references/${id}`, data)
}

/** 删除参考文献 */
export function deleteReference(id: number): Promise<ApiResult<void>> {
  return request.delete(`/references/${id}`)
}

/** 按作者搜索 */
export function searchByAuthor(keyword: string): Promise<ApiResult<Reference[]>> {
  return request.get('/references/search/author', { params: { keyword } })
}

/** 按标题搜索 */
export function searchByTitle(keyword: string): Promise<ApiResult<Reference[]>> {
  return request.get('/references/search/title', { params: { keyword } })
}

/** 获取单条文献的 GB/T 7714 格式化文本 */
export function formatReference(id: number): Promise<ApiResult<{ formatted: string }>> {
  return request.get(`/references/${id}/format`)
}

/** 获取所有文献的格式化列表 */
export function formatAllReferences(): Promise<ApiResult<string[]>> {
  return request.get('/references/format/all')
}
