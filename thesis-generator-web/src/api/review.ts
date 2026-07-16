// src/api/review.ts
import request from './request'



/** 获取待批阅列表 */
export function getPendingList(params: {
  page: number
  size: number
  keyword?: string
  course?: string
}) {
  return request.get('/teacher/reviews/pending', { params })
}

/** 获取批阅详情（论文内容 + 批注列表） */
export function getReviewDetail(paperId: number) {
  return request.get(`/teacher/reviews/${paperId}`)
}

/** 添加批注 */
export function addAnnotation(paperId: number, data: {
  startOffset: number
  endOffset: number
  selectedText: string
  content: string
}) {
  return request.post(`/teacher/reviews/${paperId}/annotations`, data)
}

/** 删除批注 */
export function deleteAnnotation(paperId: number, annotationId: number) {
  return request.delete(`/teacher/reviews/${paperId}/annotations/${annotationId}`)
}

/** 更新批注 */
export function updateAnnotation(paperId: number, annotationId: number, content: string) {
  return request.put(`/teacher/reviews/${paperId}/annotations/${annotationId}`, { content })
}

/** 暂存评语和评分 */
export function saveReviewDraft(paperId: number, data: {
  comment: string
  score: number
  grade: string
}) {
  return request.post(`/teacher/reviews/${paperId}/draft`, data)
}

/** 退回论文 */
export function returnPaper(paperId: number, data: { comment: string }) {
  return request.post(`/teacher/reviews/${paperId}/return`, data)
}

/** 通过论文 */
export function approvePaper(paperId: number, data: {
  comment: string
  score: number
  grade: string
}) {
  return request.post(`/teacher/reviews/${paperId}/approve`, data)
}

/** 获取批阅历史 */
export function getReviewHistory(paperId: number) {
  return request.get(`/teacher/reviews/${paperId}/history`)
}
