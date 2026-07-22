import request from './request'
import type { ApiResult, Reference } from '@/types'

export function getReferences() {
  return request.get<unknown, ApiResult<Reference[]>>('/references')
}

export function getReference(id: number) {
  return request.get<unknown, ApiResult<Reference>>(`/references/${id}`)
}

export function createReference(data: Omit<Reference, 'id'>) {
  return request.post<unknown, ApiResult<Reference>>('/references', data)
}

export function updateReference(id: number, data: Partial<Reference>) {
  return request.put<unknown, ApiResult<Reference>>(`/references/${id}`, data)
}

export function deleteReference(id: number) {
  return request.delete<unknown, ApiResult<null>>(`/references/${id}`)
}

export function searchReferenceByAuthor(keyword: string) {
  return request.get<unknown, ApiResult<Reference[]>>('/references/search/author', {
    params: { keyword },
  })
}

export function searchReferenceByTitle(keyword: string) {
  return request.get<unknown, ApiResult<Reference[]>>('/references/search/title', {
    params: { keyword },
  })
}

export function formatReference(id: number) {
  return request.get<unknown, ApiResult<{ formatted: string }>>(`/references/${id}/format`)
}

export function formatAllReferences() {
  return request.get<unknown, ApiResult<string[]>>('/references/format/all')
}

/* ========== 论文学位参考文献关联 ========== */

/** 获取论文已关联的参考文献 */
export function getThesisReferences(thesisId: number) {
  return request.get<unknown, ApiResult<any[]>>(`/papers/${thesisId}/references`)
}

/** 为论文添加一条参考文献 */
export function addThesisReference(thesisId: number, referenceId: number) {
  return request.post<unknown, ApiResult<any>>(`/papers/${thesisId}/references`, { referenceId })
}

/** 从论文中移除参考文献 */
export function removeThesisReference(thesisId: number, id: number) {
  return request.delete<unknown, ApiResult<null>>(`/papers/${thesisId}/references/${id}`)
}

/** 更新引用排序 */
export function updateThesisReferenceOrder(thesisId: number, ids: number[]) {
  return request.put<unknown, ApiResult<null>>(`/papers/${thesisId}/references/order`, { ids })
}
