import { del, get, patch, post, put } from './request'
import type { AiIndexStatus, Note, PageResult, Resource, StudyStatus, Tag } from '@/types'

// ---------- 学习资料 ----------

/** 创建学习资料 */
export function createResource(data: {
  courseId: number
  chapterId?: number
  title: string
  description?: string
  resourceType: string
  fileId?: number
  externalUrl?: string
}) {
  return post<Resource>('/resources', data)
}

/** 资料详情 */
export function getResourceDetail(resourceId: number) {
  return get<Resource>(`/resources/${resourceId}`)
}

/** 某章节下的资料 */
export function getChapterResources(
  chapterId: number,
  params: { page?: number; size?: number; type?: string; tagId?: number; keyword?: string } = {}
) {
  return get<PageResult<Resource>>(`/chapters/${chapterId}/resources`, params)
}

/** 修改学习资料 */
export function updateResource(resourceId: number, data: Partial<Resource>) {
  return patch<Resource>(`/resources/${resourceId}`, data)
}

/** 删除学习资料 */
export function deleteResource(resourceId: number) {
  return del<void>(`/resources/${resourceId}`)
}

// ---------- 标签 ----------

/** 标签列表 */
export function getTags(params: { keyword?: string } = {}) {
  return get<Tag[]>('/tags', params)
}

/** 创建标签 */
export function createTag(data: { name: string }) {
  return post<Tag>('/tags', data)
}

/** 设置资源标签集合（整体覆盖） */
export function setResourceTags(resourceId: number, tagIds: number[]) {
  return put<void>(`/resources/${resourceId}/tags`, { tagIds })
}

/** 删除单个标签关系 */
export function removeResourceTag(resourceId: number, tagId: number) {
  return del<void>(`/resources/${resourceId}/tags/${tagId}`)
}

// ---------- 收藏 ----------

/** 收藏资源 */
export function favoriteResource(resourceId: number) {
  return post<void>(`/resources/${resourceId}/favorite`)
}

/** 取消收藏 */
export function unfavoriteResource(resourceId: number) {
  return del<void>(`/resources/${resourceId}/favorite`)
}

/** 查询是否收藏 */
export function getFavoriteStatus(resourceId: number) {
  return get<{ favorite: boolean }>(`/resources/${resourceId}/favorite`)
}

// ---------- 学习进度 ----------

/** 更新资料学习进度 */
export function updateProgress(
  resourceId: number,
  data: { status: StudyStatus; progress: number; position?: { page?: number; seconds?: number } }
) {
  return put<void>(`/resources/${resourceId}/progress`, data)
}

/** 获取资料个人学习进度 */
export function getResourceProgress(resourceId: number) {
  return get<{ status: StudyStatus; progress: number; position?: { page?: number; seconds?: number }; lastStudyAt?: string }>(
    `/resources/${resourceId}/progress`
  )
}

// ---------- 笔记 ----------

/** 创建笔记 */
export function createNote(resourceId: number, data: { content: string; position?: { page?: number; seconds?: number } }) {
  return post<Note>(`/resources/${resourceId}/notes`, data)
}

/** 资料下的个人笔记 */
export function getResourceNotes(resourceId: number) {
  return get<Note[]>(`/resources/${resourceId}/notes`)
}

/** 修改笔记 */
export function updateNote(noteId: number, data: { content: string }) {
  return patch<Note>(`/notes/${noteId}`, data)
}

/** 删除笔记 */
export function deleteNote(noteId: number) {
  return del<void>(`/notes/${noteId}`)
}

// ---------- RAG 索引 ----------

/** 获取资源 AI 索引状态 */
export function getAiStatus(resourceId: number) {
  return get<{ resourceId: number; status: AiIndexStatus; indexedAt?: string; error?: string | null }>(
    `/resources/${resourceId}/ai-status`
  )
}

/** 手动重新建立索引 */
export function reindexResource(resourceId: number) {
  return post<{ resourceId: number; status: AiIndexStatus }>(`/resources/${resourceId}/reindex`)
}

/** 删除资源 AI 索引 */
export function deleteAiIndex(resourceId: number) {
  return del<void>(`/resources/${resourceId}/ai-index`)
}
