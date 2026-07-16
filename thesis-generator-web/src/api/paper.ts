import request from './request'
import type { ApiResult, Thesis } from '@/types/api'

/** 获取论文列表 */
export function getPapers(params?: { keyword?: string; sortBy?: string }): Promise<ApiResult<Thesis[]>> {
  return request.get('/papers', { params })
}

/** 新建论文 */
export function createPaper(data: { title: string; collegeId: number; templateVersionId?: number }): Promise<ApiResult<Thesis>> {
  return request.post('/papers', data)
}

/** 获取论文详情 */
export function getPaper(id: number): Promise<ApiResult<Thesis>> {
  return request.get(`/papers/${id}`)
}

/** 更新论文信息 */
export function updatePaper(id: number, data: Partial<Thesis>): Promise<ApiResult> {
  return request.put(`/papers/${id}`, data)
}

/** 删除论文 */
export function deletePaper(id: number): Promise<ApiResult> {
  return request.delete(`/papers/${id}`)
}

/** 导出论文（下载文件） */
export async function exportPaper(paperId: number, format: 'DOCX' | 'PDF'): Promise<void> {
  const response = await request.get(`/papers/${paperId}/export`, {
    params: { format },
    responseType: 'blob',
  })

  // 创建下载链接
  const blob = new Blob([response.data])
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `论文_${paperId}.${format.toLowerCase()}`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}
