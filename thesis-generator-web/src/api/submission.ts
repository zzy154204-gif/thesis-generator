import request from './request'
import type { ApiResult, Submission, Thesis } from '@/types'

export function submitThesis(thesisId: number) {
  return request.post<unknown, ApiResult<Submission>>(`/submissions/${thesisId}`)
}

export function getSubmissionHistory(thesisId: number) {
  return request.get<unknown, ApiResult<Submission[]>>(`/submissions/thesis/${thesisId}`)
}

/** 撤回提交 */
export function withdrawSubmission(thesisId: number) {
  return request.post<unknown, ApiResult<Thesis>>(`/submissions/${thesisId}/withdraw`)
}
