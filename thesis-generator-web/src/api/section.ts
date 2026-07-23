import request from './request'
import type { ApiResult, ThesisSection } from '@/types'

export function getSections(thesisId: number) {
  return request.get<unknown, ApiResult<ThesisSection[]>>(`/papers/${thesisId}/sections`)
}

export function getSection(thesisId: number, sectionId: number) {
  return request.get<unknown, ApiResult<ThesisSection>>(`/papers/${thesisId}/sections/${sectionId}`)
}

export function createSection(thesisId: number, data: { title: string; parentId?: number }) {
  return request.post<unknown, ApiResult<ThesisSection>>(`/papers/${thesisId}/sections`, data)
}

export function updateSection(thesisId: number, sectionId: number, data: { content?: string; title?: string }) {
  return request.put<unknown, ApiResult<ThesisSection>>(`/papers/${thesisId}/sections/${sectionId}`, data)
}

export function updateSectionsOrder(thesisId: number, sectionIds: number[]) {
  return request.put<unknown, ApiResult<null>>(`/papers/${thesisId}/sections/order`, { sectionIds })
}

export function deleteSection(thesisId: number, sectionId: number) {
  return request.delete<unknown, ApiResult<null>>(`/papers/${thesisId}/sections/${sectionId}`)
}

/** 重建参考文献章节内容（读取已关联的引用数据，按 GB/T 7714 格式化写入参考文献章节） */
export function rebuildReferences(thesisId: number) {
  return request.post<unknown, ApiResult<ThesisSection>>(`/papers/${thesisId}/sections/rebuild-references`)
}
