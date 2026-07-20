// src/api/review.ts
import request from './request'
import type { ApiResult } from '@/types/api'

// ============================================================
// 批阅记录类型（与后端 ReviewRecord 实体对齐）
// ============================================================

export interface ReviewRecord {
  id: number
  thesisId: number
  teacherId: number
  commentHtml: string
  score?: number | null
  grade?: string | null
  action: 'REVIEWED' | 'RETURNED'
  returnReason?: string | null
  createdAt: string
}

// ============================================================
// 批注类型（与后端 Annotation 实体对齐）
// ============================================================

export interface Annotation {
  id: number
  thesisId: number
  sectionId: number
  teacherId: number
  startOffset: number
  textLength: number
  selectedText: string
  content: string
  createdAt: string
}

// ============================================================
// 提交批阅请求参数
// ============================================================

export interface SubmitReviewParams {
  commentHtml: string
  score?: number
  grade?: string
  action: 'REVIEWED' | 'RETURNED'
  returnReason?: string
}

// ============================================================
// 批阅 API（匹配 ReviewRecordController）
// ============================================================

/**
 * 提交批阅（统一接口）
 * POST /api/v1/reviews/{thesisId}
 */
export function submitReview(
  thesisId: number,
  data: SubmitReviewParams
): Promise<ApiResult<ReviewRecord>> {
  return request.post(`/reviews/${thesisId}`, data) as Promise<ApiResult<ReviewRecord>>
}

/**
 * 获取批阅历史
 * GET /api/v1/reviews/thesis/{thesisId}
 */
export function getReviewHistory(thesisId: number): Promise<ApiResult<ReviewRecord[]>> {
  return request.get(`/reviews/thesis/${thesisId}`) as Promise<ApiResult<ReviewRecord[]>>
}

/**
 * 退回论文（内部调用 submitReview，action=RETURNED）
 */
export function returnPaper(
  paperId: number,
  data: { comment: string }
): Promise<ApiResult<ReviewRecord>> {
  return submitReview(paperId, {
    commentHtml: data.comment,
    action: 'RETURNED',
    returnReason: data.comment,
  })
}

/**
 * 通过论文（内部调用 submitReview，action=REVIEWED）
 */
export function approvePaper(
  paperId: number,
  data: {
    comment: string
    score: number
    grade: string
  }
): Promise<ApiResult<ReviewRecord>> {
  return submitReview(paperId, {
    commentHtml: data.comment,
    score: data.score,
    grade: data.grade,
    action: 'REVIEWED',
  })
}

// ============================================================
// 批注 API（匹配 AnnotationController）
// ============================================================

/**
 * 获取论文所有批注
 * GET /api/v1/annotations/thesis/{thesisId}
 */
export function getAnnotations(thesisId: number): Promise<ApiResult<Annotation[]>> {
  return request.get(`/annotations/thesis/${thesisId}`) as Promise<ApiResult<Annotation[]>>
}

/**
 * 获取论文某章节批注
 * GET /api/v1/annotations/thesis/{thesisId}/section/{sectionId}
 */
export function getAnnotationsBySection(
  thesisId: number,
  sectionId: number
): Promise<ApiResult<Annotation[]>> {
  return request.get(`/annotations/thesis/${thesisId}/section/${sectionId}`) as Promise<
    ApiResult<Annotation[]>
  >
}

/**
 * 创建批注
 * POST /api/v1/annotations
 * 需要 teacherId，由后端从 JWT 中获取
 */
export function addAnnotation(data: {
  thesisId: number
  sectionId: number
  startOffset: number
  textLength: number
  selectedText: string
  content: string
}): Promise<ApiResult<Annotation>> {
  return request.post('/annotations', data) as Promise<ApiResult<Annotation>>
}

/**
 * 更新批注
 * PUT /api/v1/annotations/{id}
 */
export function updateAnnotation(
  annotationId: number,
  content: string
): Promise<ApiResult<Annotation>> {
  return request.put(`/annotations/${annotationId}`, { content }) as Promise<ApiResult<Annotation>>
}

/**
 * 删除批注
 * DELETE /api/v1/annotations/{id}
 */
export function deleteAnnotation(annotationId: number): Promise<ApiResult<void>> {
  return request.delete(`/annotations/${annotationId}`) as Promise<ApiResult<void>>
}

// ============================================================
// 暂存功能（⚠️ 后端暂未提供 /draft 接口）
// ============================================================

/**
 * 暂存评语和评分
 * ⚠️ ReviewRecordController 中没有 /draft 接口，暂不可用
 */
export function saveReviewDraft(
  paperId: number,
  data: {
    comment: string
    score: number
    grade: string
  }
): Promise<ApiResult<ReviewRecord>> {
  console.warn('暂存功能需要后端提供 /reviews/{id}/draft 接口，当前未实现')
  return Promise.reject(new Error('暂存功能开发中'))
}
