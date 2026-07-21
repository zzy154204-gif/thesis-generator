import request from './request'
import type { ApiResult, ExportTask } from '@/types/api'

/** 提交导出任务 */
export function submitExport(paperId: number, data: {
  format: 'PDF' | 'DOCX'
  scope?: 'all' | 'custom'
  sectionIds?: number[]
  options?: { cover?: boolean; toc?: boolean; references?: boolean }
}): Promise<ApiResult<ExportTask>> {
  return request.post(`/papers/${paperId}/export`, data)
}

/** 查询导出进度 */
export function getExportStatus(taskId: string): Promise<ApiResult<ExportTask>> {
  return request.get(`/export/${taskId}`)
}

/** 获取导出历史 */
export function getExportHistory(paperId?: number): Promise<ApiResult<ExportTask[]>> {
  return request.get('/export/history', { params: paperId ? { paperId } : {} })
}

/** 下载导出文件 */
export function downloadExport(taskId: string): Promise<ApiResult<{ downloadUrl: string }>> {
  return request.get(`/export/${taskId}/download`)
}
