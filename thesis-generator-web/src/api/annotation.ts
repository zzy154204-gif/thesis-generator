import request from './request'
import type { ApiResult, Annotation, AnnotationRequest, AnnotationUpdateRequest } from '@/types'

export function createAnnotation(data: AnnotationRequest) {
  return request.post<unknown, ApiResult<Annotation>>('/annotations', data)
}

export function updateAnnotation(id: number, data: AnnotationUpdateRequest) {
  return request.put<unknown, ApiResult<Annotation>>(`/annotations/${id}`, data)
}

export function deleteAnnotation(id: number) {
  return request.delete<unknown, ApiResult<null>>(`/annotations/${id}`)
}

export function getAnnotationsByThesis(thesisId: number) {
  return request.get<unknown, ApiResult<Annotation[]>>(`/annotations/thesis/${thesisId}`)
}

export function getAnnotationsBySection(thesisId: number, sectionId: number) {
  return request.get<unknown, ApiResult<Annotation[]>>(`/annotations/thesis/${thesisId}/section/${sectionId}`)
}
