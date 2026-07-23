import request from './request'
import type { ApiResult, ReviewRecord, ReviewRecordItem } from '@/types'

/** 获取待批阅列表 */
export function getPendingList(params: { page?: number; size?: number; keyword?: string }) {
  return request.get<unknown, ApiResult<{ list: any[]; total: number; page: number; size: number }>>(
    '/teacher/reviews/pending',
    { params },
  )
}

/** 获取批阅详情 */
export function getReviewDetail(paperId: number) {
  return request.get<unknown, ApiResult<any>>(`/teacher/reviews/${paperId}`)
}

/** 暂存评语 */
export function saveReviewDraft(paperId: number, data: { comment?: string; score?: number; grade?: string }) {
  return request.post<unknown, ApiResult<null>>(`/teacher/reviews/${paperId}/draft`, data)
}

/** 退回论文 */
export function returnPaper(paperId: number, data: { comment?: string; reason: string }) {
  return request.post<unknown, ApiResult<ReviewRecord>>(`/teacher/reviews/${paperId}/return`, data)
}

/** 通过论文 */
export function approvePaper(paperId: number, data: { comment?: string; score: number; grade?: string }) {
  return request.post<unknown, ApiResult<ReviewRecord>>(`/teacher/reviews/${paperId}/approve`, data)
}

/** 获取批阅历史 */
export function getReviewHistory(paperId: number) {
  return request.get<unknown, ApiResult<ReviewRecord[]>>(`/teacher/reviews/${paperId}/history`)
}

/** 获取教师列表（供学生选择指导老师） */
export function getTeachers(collegeId?: number) {
  return request.get<unknown, ApiResult<{ id: number; realName: string; collegeId?: number }[]>>(
    '/teacher/reviews/teachers',
    { params: { collegeId } },
  )
}

/** 获取教师所有批阅记录（分页） */
export function getTeacherReviewRecords(params: { page?: number; size?: number }) {
  return request.get<unknown, ApiResult<{ list: ReviewRecordItem[]; total: number; page: number; size: number }>>(
    '/teacher/reviews/records',
    { params },
  )
}
