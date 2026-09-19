import { del, get, post } from './request'
import type { AiConversation, AiSource, PageResult } from '@/types'

/** 创建 AI 对话 */
export function createConversation(data: { courseId: number; title?: string }) {
  return post<{ conversationId: number; courseId: number; title?: string }>('/ai/conversations', data)
}

/** 我的 AI 对话列表 */
export function getConversations(params: { courseId?: number; page?: number; size?: number } = {}) {
  return get<PageResult<AiConversation>>('/ai/conversations', params)
}

/** 获取某个 AI 对话（含消息历史） */
export function getConversationDetail(conversationId: number) {
  return get<AiConversation>(`/ai/conversations/${conversationId}`)
}

/** 删除 AI 对话 */
export function deleteConversation(conversationId: number) {
  return del<void>(`/ai/conversations/${conversationId}`)
}

/** 发送 AI 问题（支持课程 / 章节 / 资料三种范围） */
export function chat(payload: {
  conversationId?: number
  courseId: number
  chapterId?: number
  resourceId?: number
  message: string
}) {
  return post<{ answer: string; sources: AiSource[]; conversationId?: number }>('/ai/chat', payload)
}
