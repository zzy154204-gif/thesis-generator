import request from './request'
import type { ApiResult } from '@/types'

export function saveDraft(thesisId: number, sectionId: number, content: string) {
  return request.post<unknown, ApiResult<null>>('/drafts', { thesisId, sectionId, content })
}

export function getDraft(thesisId: number, sectionId: number) {
  return request.get<unknown, ApiResult<string>>('/drafts', {
    params: { thesisId, sectionId },
  })
}

export function deleteDraft(thesisId: number, sectionId: number) {
  return request.delete<unknown, ApiResult<null>>('/drafts', {
    params: { thesisId, sectionId },
  })
}
