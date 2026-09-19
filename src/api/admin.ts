import { del, get, patch } from './request'
import type { AdminStatistics, Course, PageResult, Resource, UserInfo } from '@/types'

/** 用户列表（管理端） */
export function adminGetUsers(params: { page?: number; size?: number; keyword?: string; role?: string }) {
  return get<PageResult<UserInfo>>('/admin/users', params)
}

/** 用户详情（管理端） */
export function adminGetUser(userId: number) {
  return get<UserInfo>(`/admin/users/${userId}`)
}

/** 修改用户状态（管理端） */
export function adminUpdateUserStatus(userId: number, status: string) {
  return patch<void>(`/admin/users/${userId}/status`, { status })
}

/** 修改用户角色（管理端） */
export function adminUpdateUserRole(userId: number, role: string) {
  return patch<void>(`/admin/users/${userId}/role`, { role })
}

/** 课程管理列表（管理端） */
export function adminGetCourses(params: { page?: number; size?: number; keyword?: string }) {
  return get<PageResult<Course>>('/admin/courses', params)
}

/** 删除课程（管理端） */
export function adminDeleteCourse(courseId: number) {
  return del<void>(`/admin/courses/${courseId}`)
}

/** 学习资料管理列表（管理端） */
export function adminGetResources(params: { page?: number; size?: number; keyword?: string; courseId?: number }) {
  return get<PageResult<Resource>>('/admin/resources', params)
}

/** 删除学习资料（管理端） */
export function adminDeleteResource(resourceId: number) {
  return del<void>(`/admin/resources/${resourceId}`)
}

/** 平台统计（管理端） */
export function adminStatistics() {
  return get<AdminStatistics>('/admin/statistics')
}
