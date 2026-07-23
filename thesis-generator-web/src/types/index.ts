/* ========== 通用响应 ========== */
export interface ApiResult<T = any> {
  code: number
  message: string
  data: T
}

/* ========== 认证 ========== */
export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  realName: string
  role: 'STUDENT' | 'TEACHER' | 'ADMIN'
  collegeId?: number
  teacherNo?: string // 教师工号（教师必填）
}

export interface LoginResponse {
  token: string
  userId: number
  username: string
  realName: string
  role: string
}

export interface UserInfo {
  userId: number
  username: string
  realName: string
  role: 'STUDENT' | 'TEACHER' | 'ADMIN'
  collegeId?: number
  teacherNo?: string
}

/* ========== 学院 ========== */
export interface College {
  id: number
  name: string
  code: string
  createdAt?: string
  updatedAt?: string
}

/* ========== 模板 ========== */
export interface Template {
  id: number
  name: string
  type: 'GRADUATION' | 'COURSE' | 'PROJECT'
  collegeId?: number
  enabled: boolean
  description?: string
  createdAt: string
  updatedAt: string
}

export interface TemplateVersion {
  id: number
  templateId: number
  versionNumber: string
  isCurrent: boolean
  coverFields: string
  chapterStructure: string
  formatConfig: string
  createdAt: string
}

/** 管理员获取的模板详情（含版本配置） */
export interface TemplateDetail {
  id: number
  name: string
  type: string
  collegeId?: number
  description?: string
  enabled: boolean
  coverConfig: CoverField[]
  structure: ChapterDef[]
  styles: FormatConfig
  version: string
  versionId: number
}

export interface CoverField {
  key: string
  label: string
  required: boolean
  type: 'text' | 'select'
  options?: string[]
}

export interface ChapterDef {
  key: string
  title: string
  children?: ChapterDef[]
}

export interface FormatConfig {
  fontFamily?: string
  fontSize?: string
  lineHeight?: string
  pageSize?: string
  margins?: string
  [key: string]: any
}

/* ========== 论文 ========== */
export type PaperStatus = 'DRAFT' | 'COMPLETED' | 'SUBMITTED' | 'REVIEWED' | 'RETURNED' | 'GENERATING'

export interface Thesis {
  id: number
  studentId: number
  templateVersionId?: number
  templateName?: string
  title: string
  status: PaperStatus
  isLocked: boolean
  teacherId?: number
  collegeId?: number
  importMetadata?: string // JSON: Word 导入时提取的封面元数据（学号、姓名、学院等）
  createdAt: string
  updatedAt: string
}

/* ========== 论文章节 ========== */
export interface ThesisSection {
  id: number
  thesisId: number
  title: string
  sectionKey: string
  content: string
  draftContent: string
  sectionType: string
  parentId?: number
  sortOrder: number
  createdAt: string
  updatedAt: string
}

/* ========== 批注 ========== */
export interface Annotation {
  id: number
  thesisId: number
  sectionId: number
  teacherId: number
  startOffset: number
  textLength: number
  selectedText: string
  content: string
  createdAt: string
}

export interface AnnotationRequest {
  thesisId: number
  sectionId: number
  startOffset: number
  textLength: number
  selectedText?: string
  content: string
}

export interface AnnotationUpdateRequest {
  content: string
}

/* ========== 参考文献 ========== */
export type ReferenceType = 'J' | 'M' | 'C' | 'D' | 'EB'

export interface Reference {
  id: number
  authors: string
  title: string
  type: ReferenceType
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
  createdAt?: string
  updatedAt?: string
}

/* ========== 批阅/审核 ========== */
export interface ReviewRecord {
  id: number
  thesisId: number
  teacherId: number
  commentHtml?: string
  score?: number
  grade?: string
  action: 'REVIEWED' | 'RETURNED' | 'DRAFT'
  returnReason?: string
  createdAt: string
}

export interface ReviewRequest {
  commentHtml?: string
  score: number
  grade?: string
  action: 'REVIEWED' | 'RETURNED'
  returnReason?: string
}

/** 教师待批阅列表项 */
export interface PendingItem {
  id: number
  title: string
  status: string
  submittedAt: string
  studentName: string
  studentId: string
}

/** 教师批阅详情 */
export interface ReviewDetail {
  paper: {
    id: number
    title: string
    status: string
    studentName: string
    studentId: string
    submittedAt: string
  }
  annotations: ReviewAnnotation[]
}

export interface ReviewAnnotation {
  id: number
  content: string
  selectedText: string
  startOffset: number
  textLength: number
  sectionId: number
  author: string
  createdAt: string
}

/* ========== 提交记录 ========== */
export interface Submission {
  id: number
  thesisId: number
  studentId: number
  versionNumber: number
  submittedAt: string
}

/** 学生提交记录列表项（含论文信息） */
export interface SubmissionRecordItem {
  id: number
  thesisId: number
  versionNumber: number
  submittedAt: string
  thesisTitle: string
  thesisStatus: string
}

/** 教师批阅记录列表项 */
export interface ReviewRecordItem {
  id: number
  thesisId: number
  action: 'REVIEWED' | 'RETURNED'
  score?: number
  grade?: string
  commentHtml?: string
  returnReason?: string
  createdAt: string
  thesisTitle: string
  thesisStatus: string
  studentName: string
  studentUsername: string
}

/* ========== 图片 ========== */
export interface ImageInfo {
  id: number
  originalName: string
  fileSize: number
  contentType: string
  url: string
}

/* ========== 导出 ========== */
export interface ExportTask {
  taskId: string
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
  progress: number
  downloadUrl?: string
  errorMessage?: string
}

/** 导出记录 */
export interface ExportRecord {
  id: number
  thesisId: number
  userId: number
  thesisTitle: string
  format: 'DOCX' | 'PDF'
  status: 'SUCCESS' | 'FAILED'
  errorMessage?: string
  fileSize?: number
  createdAt: string
}
