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
