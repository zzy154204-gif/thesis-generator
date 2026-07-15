import request from './request'
import type { ApiResult, ThesisSection } from '@/types/api'

/** 获取章节树 */
export function getSections(paperId: number): Promise<ApiResult<ThesisSection[]>> {
  return request.get(`/papers/${paperId}/sections`)
}

/** 新增章节 */
export function createSection(paperId: number, data: { title: string; parentId?: number }): Promise<ApiResult<ThesisSection>> {
  return request.post(`/papers/${paperId}/sections`, data)
}

/** 获取章节内容 */
export function getSection(paperId: number, sectionId: number): Promise<ApiResult<ThesisSection>> {
  return request.get(`/papers/${paperId}/sections/${sectionId}`)
}

/** 保存章节内容 */
export function saveSection(paperId: number, sectionId: number, data: { content: string; title?: string }): Promise<ApiResult> {
  return request.put(`/papers/${paperId}/sections/${sectionId}`, data)
}

/** 更新章节排序 */
export function updateSectionsOrder(paperId: number, sectionIds: number[]): Promise<ApiResult> {
  return request.put(`/papers/${paperId}/sections/order`, { sectionIds })
}

/** 删除章节 */
export function deleteSection(paperId: number, sectionId: number): Promise<ApiResult> {
  return request.delete(`/papers/${paperId}/sections/${sectionId}`)
}
