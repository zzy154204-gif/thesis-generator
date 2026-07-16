import request from './request'
import type { ApiResult } from '@/types/api'

// 对齐后端 DraftController: /api/v1/drafts

/** 保存章节草稿 */
export function saveDraft(thesisId: number, sectionId: number, content: string): Promise<ApiResult> {
  return request.post('/drafts', { thesisId, sectionId, content })
}

/** 读取章节草稿 */
export function getDraft(thesisId: number, sectionId: number): Promise<ApiResult<string>> {
  return request.get('/drafts', { params: { thesisId, sectionId } })
}

/** 删除章节草稿 */
export function deleteDraft(thesisId: number, sectionId: number): Promise<ApiResult> {
  return request.delete('/drafts', { params: { thesisId, sectionId } })
}
