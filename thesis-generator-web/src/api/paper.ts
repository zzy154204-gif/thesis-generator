import request from './request'
import type { ApiResult, Thesis } from '@/types'
import { getToken } from '@/utils/token'

export interface PaperQuery {
  keyword?: string
  sortBy?: string
}

export function getPapers(params?: PaperQuery) {
  return request.get<unknown, ApiResult<Thesis[]>>('/papers', { params })
}

export function getPaper(id: number) {
  return request.get<unknown, ApiResult<Thesis>>(`/papers/${id}`)
}

/** 获取论文批阅反馈（评语 + 批注） */
export function getReviewFeedback(id: number) {
  return request.get<unknown, ApiResult<{ latestReview: any; annotations: any[] }>>(`/papers/${id}/review-feedback`)
}

export function createPaper(data: { title: string; collegeId?: number; templateVersionId?: number; teacherId?: number }) {
  return request.post<unknown, ApiResult<Thesis>>('/papers', data)
}

export function updatePaper(id: number, data: { title?: string; collegeId?: number; templateVersionId?: number }) {
  return request.put<unknown, ApiResult<Thesis>>(`/papers/${id}`, data)
}

export function deletePaper(id: number) {
  return request.delete<unknown, ApiResult<null>>(`/papers/${id}`)
}

/**
 * 导出论文并触发浏览器下载（使用 fetch 带 Authorization，绕过 axios 拦截器）
 */
export async function downloadExport(id: number, format: 'DOCX' | 'PDF' = 'DOCX') {
  const token = getToken()
  const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api/v1'
  const url = `${baseUrl}/papers/${id}/export?format=${format}`
  const ext = format === 'PDF' ? 'pdf' : 'docx'

  const response = await fetch(url, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  })

  // 错误处理：读取 JSON 错误信息
  if (!response.ok) {
    const contentType = response.headers.get('content-type') || ''
    if (contentType.includes('application/json')) {
      try {
        const err = await response.json()
        throw new Error(err.message || `导出失败 (${response.status})`)
      } catch (e: any) {
        throw e
      }
    }
    throw new Error(`导出失败 (${response.status})`)
  }

  // 从 Content-Disposition 提取文件名
  const disposition = response.headers.get('content-disposition') || ''
  const filenameMatch = disposition.match(/filename\*?=(?:UTF-8'')?(.+?)(?:;|$)/i)
  let filename = `论文.${ext}`
  if (filenameMatch) {
    try {
      filename = decodeURIComponent(filenameMatch[1].trim())
    } catch {
      filename = filenameMatch[1].trim()
    }
  }

  // 下载 Blob
  const blob = await response.blob()
  const blobUrl = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = blobUrl
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(blobUrl)
}

/** 导入 .docx 文件并自动解析章节 */
export function importDocx(file: File, title: string, templateVersionId?: number, teacherId?: number) {
  const form = new FormData()
  form.append('file', file)
  form.append('title', title)
  if (templateVersionId) form.append('templateVersionId', String(templateVersionId))
  if (teacherId) form.append('teacherId', String(teacherId))
  return request.post<unknown, ApiResult<Thesis>>('/papers/import', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000, // 大文件解析可能需要较长时间
  })
}
