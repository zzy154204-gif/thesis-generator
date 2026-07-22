import request from './request'
import type { ApiResult, College } from '@/types'

export function getColleges() {
  return request.get<unknown, ApiResult<College[]>>('/colleges')
}

export function getCollege(id: number) {
  return request.get<unknown, ApiResult<College>>(`/colleges/${id}`)
}

export function createCollege(data: { name: string; code: string }) {
  return request.post<unknown, ApiResult<College>>('/colleges', data)
}

export function updateCollege(id: number, data: { name: string; code: string }) {
  return request.put<unknown, ApiResult<College>>(`/colleges/${id}`, data)
}

export function deleteCollege(id: number) {
  return request.delete<unknown, ApiResult<null>>(`/colleges/${id}`)
}
