// ============ 全局类型定义（对齐后端接口文档） ============

/** 统一响应包装 */
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

/** 分页返回结构 */
export interface PageResult<T = unknown> {
  items: T[]
  page: number
  size: number
  total: number
}

/** 用户角色 */
export type UserRole = 'STUDENT' | 'TEACHER' | 'ADMIN'

/** 当前用户 */
export interface UserInfo {
  id: number
  username: string
  nickname?: string
  email?: string
  avatarUrl?: string
  bio?: string
  role: UserRole
  createdAt?: string
}

/** 登录返回 */
export interface LoginResult {
  accessToken: string
  refreshToken: string
  expiresIn: number
}

/** 课程可见性 / 状态 */
export type CourseVisibility = 'PUBLIC' | 'PRIVATE'
export type CourseStatus = 'ACTIVE' | 'ARCHIVED'

/** 课程 */
export interface Course {
  id: number
  title: string
  description?: string
  coverUrl?: string
  creatorId?: number
  visibility: CourseVisibility
  status?: CourseStatus
  memberCount?: number
  resourceCount?: number
  chapterCount?: number
  creator?: { id: number; nickname?: string }
  joined?: boolean
  createdAt?: string
  updatedAt?: string
}

/** 章节 */
export interface Chapter {
  id: number
  courseId?: number
  title: string
  description?: string
  sortOrder?: number
  resourceCount?: number
  createdAt?: string
  updatedAt?: string
}

/** 资料类型 */
export type ResourceType =
  | 'PDF'
  | 'DOC'
  | 'PPT'
  | 'MARKDOWN'
  | 'TXT'
  | 'VIDEO'
  | 'LINK'
  | 'CODE'
  | 'OTHER'

/** AI 索引状态 */
export type AiIndexStatus = 'NOT_INDEXED' | 'PROCESSING' | 'INDEXED' | 'FAILED'

/** 学习状态 */
export type StudyStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED'

/** 标签 */
export interface Tag {
  id: number
  name: string
  createdAt?: string
}

/** 文件 */
export interface FileInfo {
  fileId: number
  fileName?: string
  name?: string
  contentType?: string
  size?: number
  url?: string
}

/** 学习资料 */
export interface Resource {
  id: number
  courseId?: number
  chapterId?: number
  title: string
  description?: string
  resourceType: ResourceType
  fileId?: number
  externalUrl?: string
  creatorId?: number
  viewCount?: number
  downloadCount?: number
  aiIndexStatus?: AiIndexStatus
  aiIndexError?: string
  indexedAt?: string
  createdAt?: string
  updatedAt?: string
  course?: { id: number; title: string }
  chapter?: { id: number; title: string }
  file?: FileInfo
  tags?: Tag[]
  favorite?: boolean
  progress?: { status: StudyStatus; progress: number; position?: unknown }
  creator?: { id: number; nickname?: string }
}

/** 学习进度 */
export interface CourseProgress {
  resourceCount: number
  completedCount: number
  inProgressCount: number
  notStartedCount: number
  progress: number
}

/** 最近学习记录 */
export interface RecentLearning {
  resourceId: number
  title: string
  progress: number
  courseId?: number
  courseTitle?: string
  resourceType?: ResourceType
  updatedAt?: string
}

/** 学习仪表盘 */
export interface LearningDashboard {
  courseCount: number
  completedResourceCount: number
  favoriteCount: number
  noteCount: number
  recentLearning: RecentLearning[]
  courses: { courseId: number; title: string; progress: number; coverUrl?: string }[]
}

/** 笔记 */
export interface Note {
  id: number
  userId?: number
  resourceId?: number
  content: string
  position?: { page?: number; seconds?: number }
  createdAt?: string
  updatedAt?: string
  resource?: { id: number; title: string; resourceType?: ResourceType; courseTitle?: string }
}

/** AI 回答来源 */
export interface AiSource {
  resourceId: number
  resourceTitle: string
  chapterId?: number
  chapterTitle?: string
  page?: number
  snippet?: string
}

/** AI 对话消息 */
export interface AiMessage {
  role: 'USER' | 'ASSISTANT'
  content: string
  sources?: AiSource[]
  createdAt?: string
}

/** AI 对话 */
export interface AiConversation {
  id: number
  courseId?: number
  title?: string
  messages?: AiMessage[]
  createdAt?: string
}

/** 管理端统计 */
export interface AdminStatistics {
  userCount: number
  courseCount: number
  resourceCount: number
  fileCount: number
  indexedResourceCount: number
  storageUsage: number
}

/** 课程成员 */
export interface CourseMember {
  userId: number
  username?: string
  nickname?: string
  avatarUrl?: string
  role?: UserRole
  joinedAt?: string
}
