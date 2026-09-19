import { get } from './request'
import type { Course, PageResult, Resource, Tag } from '@/types'

/** 统一搜索 */
export function unifiedSearch(params: { keyword: string; type?: 'course' | 'resource' | 'tag'; page?: number; size?: number }) {
  return get<{
    courses?: PageResult<Course>
    resources?: PageResult<Resource>
    tags?: PageResult<Tag>
  }>('/search', params)
}

/** 课程搜索 */
export function searchCourses(params: { keyword: string; page?: number; size?: number }) {
  return get<PageResult<Course>>('/search/courses', params)
}

/** 学习资料搜索（支持多条件筛选） */
export function searchResources(params: {
  keyword?: string
  courseId?: number
  chapterId?: number
  resourceType?: string
  tagId?: number
  creatorId?: number
  sort?: string
  page?: number
  size?: number
}) {
  return get<PageResult<Resource>>('/search/resources', params)
}
