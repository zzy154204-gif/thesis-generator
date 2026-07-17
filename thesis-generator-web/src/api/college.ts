import request from './request'
import type { ApiResult, College } from '@/types/api'

/** 获取学院列表 */
export function getColleges(): Promise<ApiResult<College[]>> {
  return request.get('/colleges')
}

/** 获取学院详情 */
export function getCollege(id: number): Promise<ApiResult<College>> {
  return request.get(`/colleges/${id}`)
}

/** 创建学院 */
export function createCollege(data: { name: string; code: string }): Promise<ApiResult<College>> {
  return request.post('/colleges', data)
}

/** 更新学院 */
export function updateCollege(id: number, data: { name: string; code: string }): Promise<ApiResult<College>> {
  return request.put(`/colleges/${id}`, data)
}

/** 删除学院 */
export function deleteCollege(id: number): Promise<ApiResult<void>> {
  return request.delete(`/colleges/${id}`)
}
