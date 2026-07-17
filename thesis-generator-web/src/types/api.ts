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
  collegeName?: string          // 用于列表展示
  type: 'GRADUATION' | 'COURSE' | 'PROJECT'
  status: 'ENABLED' | 'DISABLED'
  version: string

  // 管理员端扩展字段（完整配置）
  coverConfig?: CoverFieldConfig[]
  styleConfig?: StyleConfig
  structure?: StructureConfig
}

// 封面字段配置
export interface CoverFieldConfig {
  key: string
  label: string
  required: boolean
}

// 样式配置
export interface StyleConfig {
  font?: string
  fontSize?: string
  headingFont?: string
  lineHeight?: string
  indent?: string
  paragraphSpacing?: number
  marginTop?: number
  marginBottom?: number
  marginLeft?: number
  marginRight?: number
  headerText?: string
  headerFontSize?: string
  pageNumberPosition?: 'bottomCenter' | 'bottomRight' | 'topCenter'
}

// 结构配置
export interface StructureConfig {
  hasDeclaration?: boolean
  hasAbstract?: boolean
  hasEnglishAbstract?: boolean
  maxHeadingLevel?: number
  hasAppendix?: boolean
  hasReferences?: boolean
}

// 模板版本
export interface TemplateVersion {
  id: number
  templateId: number
  version: string
}

// 参考文献
export interface Reference {
  id: number
  thesisId: number
  type: 'J' | 'C' | 'M' | 'D' | 'EB_OL'
  author: string
  title: string
  journal?: string
  year: string
  volume?: string
  issue?: string
  pages?: string
  publisher?: string
  doi?: string
  url?: string
}

// 学院
export interface College {
  id: number
  name: string
  code: string           // 学院代码
  templateCount?: number // 关联模板数量
  createdAt?: string
  updatedAt?: string
}

// 导出任务
export interface ExportTask {
  taskId: string
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
  progress: number
  downloadUrl?: string
  errorMessage?: string
}
