import { get } from './request'
import type { Course, LearningDashboard, Note, PageResult, RecentLearning } from '@/types'

/** 我加入的课程 */
export function getMyCourses() {
  return get<PageResult<Course>>('/users/me/courses')
}

/** 我创建的课程 */
export function getMyCreatedCourses() {
  return get<PageResult<Course>>('/users/me/created-courses')
}

/** 我的收藏 */
export function getMyFavorites(params: { page?: number; size?: number; courseId?: number; type?: string; keyword?: string }) {
  return get<
    PageResult<{
      id: number
      title: string
      description?: string
      resourceType?: string
      course?: { id: number; title: string }
      courseTitle?: string
      progress?: number
    }>
  >('/users/me/favorites', params)
}

/** 我的全部笔记 */
export function getMyNotes(params: { page?: number; size?: number; courseId?: number; resourceId?: number; keyword?: string }) {
  return get<PageResult<Note>>('/users/me/notes', params)
}

/** 最近学习记录 */
export function getRecentLearning() {
  return get<RecentLearning[]>('/users/me/recent-learning')
}

/** 学习仪表盘聚合接口 */
export function getLearningDashboard() {
  return get<LearningDashboard>('/users/me/learning-dashboard')
}
