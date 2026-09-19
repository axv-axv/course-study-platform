import { get, patch, post } from './request'
import type { LoginResult, UserInfo } from '@/types'

/** 用户注册 */
export function register(data: { username: string; password: string; email?: string }) {
  return post<{ id: number; username: string; role: string }>('/auth/register', data)
}

/** 用户登录 */
export function login(data: { username: string; password: string }) {
  return post<LoginResult>('/auth/login', data)
}

/** 刷新 Token */
export function refresh(data: { refreshToken: string }) {
  return post<{ accessToken: string; refreshToken: string }>('/auth/refresh', data)
}

/** 获取当前用户信息 */
export function getMe() {
  return get<UserInfo>('/users/me')
}

/** 修改个人信息 */
export function updateMe(data: { nickname?: string; bio?: string; email?: string }) {
  return patch<UserInfo>('/users/me', data)
}

/** 修改头像 */
export function updateAvatar(formData: FormData) {
  return post<{ avatarUrl: string }>('/users/me/avatar', formData)
}
