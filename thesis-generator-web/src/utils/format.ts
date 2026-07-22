/** 论文状态中文映射 */
export const STATUS_LABEL: Record<string, string> = {
  DRAFT: '草稿',
  COMPLETED: '已完成',
  SUBMITTED: '已提交',
  REVIEWED: '已批阅',
  RETURNED: '已退回',
  GENERATING: '生成中',
}

export function statusLabel(status: string): string {
  return STATUS_LABEL[status] || status
}

export function statusTagType(status: string): 'info' | 'success' | 'warning' | 'danger' | '' {
  const map: Record<string, 'info' | 'success' | 'warning' | 'danger' | ''> = {
    DRAFT: 'info',
    COMPLETED: 'success',
    SUBMITTED: 'warning',
    REVIEWED: 'success',
    RETURNED: 'danger',
    GENERATING: 'warning',
  }
  return map[status] || 'info'
}

/** GB/T 7714 文献类型标识 */
export const REF_TYPE_LABEL: Record<string, string> = {
  J: '期刊文章[J]',
  M: '专著/书籍[M]',
  C: '会议论文[C]',
  D: '学位论文[D]',
  EB: '网络资源[EB]',
}

/** 相对时间 */
export function relativeTime(dateStr: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const hours = Math.floor(diff / (1000 * 60 * 60))
  if (hours < 1) return '刚刚'
  if (hours < 24) return `${hours}小时前`
  if (hours < 48) return '昨天'
  return date.toLocaleDateString('zh-CN')
}
