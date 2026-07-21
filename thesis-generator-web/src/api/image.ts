import request from './request'
import type { ApiResult } from '@/types/api'

// 对齐后端 ImageController: /api/images

export interface ImageUploadResult {
  id: number
  originalName: string
  fileSize: number
  contentType: string
  url: string
}

export interface ImageMetadata {
  id: number
  originalName: string
  fileSize: number
  contentType: string
  createdAt: string
  url: string
}

/** 上传图片 */
export function uploadImage(file: File): Promise<ApiResult<ImageUploadResult>> {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/images/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** 获取图片元信息 */
export function getImageMetadata(id: number): Promise<ApiResult<ImageMetadata>> {
  return request.get(`/images/${id}`)
}

/** 获取图片文件 URL */
export function getImageUrl(id: number): string {
  return `/api/images/${id}/file`
}

/** 删除图片 */
export function deleteImage(id: number): Promise<ApiResult<void>> {
  return request.delete(`/images/${id}`)
}
