import request from './request'
import type { ApiResult } from '@/types'

export function uploadImage(file: File) {
  const form = new FormData()
  form.append('file', file)
  return request.post<unknown, ApiResult<{ id: number; url: string; originalName: string }>>('/images/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function deleteImage(id: number) {
  return request.delete<unknown, ApiResult<null>>(`/images/${id}`)
}
