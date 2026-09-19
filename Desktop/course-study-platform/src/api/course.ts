import { del, get, patch, post, put } from './request'
import type { Chapter, Course, CourseMember, CourseProgress, PageResult, Resource } from '@/types'

/** 创建课程（TEACHER/ADMIN） */
export function createCourse(data: { title: string; description?: string; visibility?: 'PUBLIC' | 'PRIVATE' }) {
  return post<Course>('/courses', data)
}

/** 课程列表 */
export function getCourses(params: {
  page?: number
  size?: number
  keyword?: string
  creatorId?: number
  visibility?: string
  status?: string
}) {
  return get<PageResult<Course>>('/courses', params)
}

/** 课程详情 */
export function getCourseDetail(courseId: number) {
  return get<Course>(`/courses/${courseId}`)
}

/** 修改课程 */
export function updateCourse(courseId: number, data: Partial<Course>) {
  return patch<Course>(`/courses/${courseId}`, data)
}

/** 删除课程 */
export function deleteCourse(courseId: number) {
  return del<void>(`/courses/${courseId}`)
}

/** 课程主页聚合接口 */
export function getCourseOverview(courseId: number) {
  return get<{
    course: Course
    chapters: Chapter[]
    progress: CourseProgress
    recentResources: Resource[]
    aiEnabled: boolean
  }>(`/courses/${courseId}/overview`)
}

/** 加入课程 */
export function joinCourse(courseId: number) {
  return post<{ courseId: number; userId: number; joinedAt: string }>(`/courses/${courseId}/members`)
}

/** 退出课程 */
export function leaveCourse(courseId: number) {
  return del<void>(`/courses/${courseId}/members/me`)
}

/** 课程成员列表 */
export function getCourseMembers(courseId: number, params: { page?: number; size?: number; keyword?: string } = {}) {
  return get<PageResult<CourseMember>>(`/courses/${courseId}/members`, params)
}

/** 课程整体学习进度 */
export function getCourseProgress(courseId: number) {
  return get<CourseProgress>(`/courses/${courseId}/progress`)
}

/** 课程全部资料 */
export function getCourseResources(
  courseId: number,
  params: { page?: number; size?: number; chapterId?: number; type?: string; tagId?: number; keyword?: string; sort?: string } = {}
) {
  return get<PageResult<Resource>>(`/courses/${courseId}/resources`, params)
}

// ---------- 章节 ----------

/** 创建章节 */
export function createChapter(courseId: number, data: { title: string; description?: string }) {
  return post<Chapter>(`/courses/${courseId}/chapters`, data)
}

/** 课程章节列表 */
export function getChapters(courseId: number) {
  return get<Chapter[]>(`/courses/${courseId}/chapters`)
}

/** 章节详情 */
export function getChapterDetail(chapterId: number) {
  return get<Chapter>(`/chapters/${chapterId}`)
}

/** 修改章节 */
export function updateChapter(chapterId: number, data: { title?: string; description?: string }) {
  return patch<Chapter>(`/chapters/${chapterId}`, data)
}

/** 删除章节 */
export function deleteChapter(chapterId: number) {
  return del<void>(`/chapters/${chapterId}`)
}

/** 调整章节顺序 */
export function reorderChapters(courseId: number, chapterIds: number[]) {
  return put<void>(`/courses/${courseId}/chapters/order`, { chapterIds })
}
