// 统一响应包装
export interface ApiResult<T = any> {
  code: number
  message: string
  data: T
}

// 用户信息类型（与后端 LoginResponse 对齐）
export interface UserInfo {
  userId: number
  username: string
  realName: string
  role: 'STUDENT' | 'TEACHER' | 'ADMIN'
  collegeId?: number
}

// 登录请求
export interface LoginRequest {
  username: string
  password: string
}

// 注册请求
export interface RegisterRequest {
  username: string
  password: string
  realName: string
  role: string
  collegeId?: number
}

// 登录/注册响应（与后端 LoginResponse 对齐：扁平结构）
export interface AuthResponse {
  token: string
  userId: number
  username: string
  realName: string
  role: string
}

// 论文
export interface Thesis {
  id: number
  studentId: number
  templateVersionId?: number
  title: string
  status: 'DRAFT' | 'COMPLETED' | 'SUBMITTED' | 'REVIEWED' | 'RETURNED' | 'GENERATING'
  isLocked: boolean
  collegeId?: number
  createdAt: string
  updatedAt: string
}

// 章节
export interface ThesisSection {
  id: number
  thesisId: number
  parentId?: number
  title: string
  content: string
  sortOrder: number
  children?: ThesisSection[]
}

// 模板
export interface Template {
  id: number
  name: string
  description: string
  collegeId?: number
}

// 模板版本
export interface TemplateVersion {
  id: number
  templateId: number
  version: string
}

// 参考文献（与后端 Reference 实体对齐）
export interface Reference {
  id: number
  authors: string
  title: string
  type: 'J' | 'M' | 'C' | 'D' | 'EB'
  year?: number
  journal?: string
  volume?: string
  issue?: string
  pages?: string
  publisher?: string
  address?: string
  conference?: string
  institution?: string
  url?: string
  accessDate?: string
}

// 学院
export interface College {
  id: number
  name: string
  code?: string
  templateCount?: number
  createdAt?: string
}

// 导出任务
export interface ExportTask {
  taskId: string
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
  progress: number
  downloadUrl?: string
  errorMessage?: string
}
