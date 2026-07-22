import request from './request'
import type { ApiResult, ExportRecord } from '@/types'

/** 获取当前用户的导出历史 */
export function getExportHistory() {
  return request.get<unknown, ApiResult<ExportRecord[]>>('/exports')
}
