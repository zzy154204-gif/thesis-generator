import request from './request'
import type { ApiResult, Reference } from '@/types/api'

/** 获取参考文献列表 */
export function getReferences(paperId: number): Promise<ApiResult<Reference[]>> {
  return request.get(`/papers/${paperId}/references`)
}

/** 添加参考文献 */
export function createReference(paperId: number, data: Omit<Reference, 'id' | 'thesisId'>): Promise<ApiResult<Reference>> {
  return request.post(`/papers/${paperId}/references`, data)
}

/** 更新参考文献 */
export function updateReference(paperId: number, refId: number, data: Partial<Reference>): Promise<ApiResult> {
  return request.put(`/papers/${paperId}/references/${refId}`, data)
}

/** 删除参考文献 */
export function deleteReference(paperId: number, refId: number): Promise<ApiResult> {
  return request.delete(`/papers/${paperId}/references/${refId}`)
}

/** 更新参考文献排序 */
export function updateReferencesOrder(paperId: number, refIds: number[]): Promise<ApiResult> {
  return request.put(`/papers/${paperId}/references/order`, { refIds })
}
