import type { AiIndexStatus, ResourceType } from '@/types'

/** 文件大小格式化 */
export function formatSize(bytes?: number): string {
  if (bytes === undefined || bytes === null || isNaN(bytes)) return '-'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)} MB`
  return `${(bytes / 1024 / 1024 / 1024).toFixed(2)} GB`
}

/** 时间格式化：相对时间（刚刚 / N分钟前 / N小时前 / N天前）+ 具体时间兜底 */
export function formatRelativeTime(input?: string | number): string {
  if (!input) return '-'
  const t = new Date(input).getTime()
  if (isNaN(t)) return '-'
  const diff = Date.now() - t
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour
  if (diff < minute) return '刚刚'
  if (diff < hour) return `${Math.floor(diff / minute)} 分钟前`
  if (diff < day) return `${Math.floor(diff / hour)} 小时前`
  if (diff < 7 * day) return `${Math.floor(diff / day)} 天前`
  const d = new Date(t)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/** 时间格式化：yyyy-MM-dd HH:mm */
export function formatDateTime(input?: string | number): string {
  if (!input) return '-'
  const d = new Date(input)
  if (isNaN(d.getTime())) return '-'
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 资源类型 → 中文名 */
export const resourceTypeText: Record<ResourceType, string> = {
  PDF: 'PDF 文档',
  DOC: 'Word 文档',
  PPT: 'PPT 演示',
  MARKDOWN: 'Markdown',
  TXT: '文本',
  VIDEO: '视频',
  LINK: '网页链接',
  CODE: '代码',
  OTHER: '其他'
}

/** 资源类型 → 图标名（Element Plus icon 组件名，由页面自行映射） */
export function resourceTypeIcon(type: ResourceType): string {
  const map: Record<ResourceType, string> = {
    PDF: 'Document',
    DOC: 'Document',
    PPT: 'Presentation',
    MARKDOWN: 'Memo',
    TXT: 'Tickets',
    VIDEO: 'VideoPlay',
    LINK: 'Link',
    CODE: 'Code',
    OTHER: 'Folder'
  }
  return map[type] || 'Folder'
}

/** 资源类型徽章颜色 */
export function resourceTypeColor(type: ResourceType): string {
  const map: Record<ResourceType, string> = {
    PDF: '#ef4444',
    DOC: '#3b82f6',
    PPT: '#f97316',
    MARKDOWN: '#0d9488',
    TXT: '#64748b',
    VIDEO: '#8b5cf6',
    LINK: '#06b6d4',
    CODE: '#22c55e',
    OTHER: '#94a3b8'
  }
  return map[type] || '#94a3b8'
}

/** AI 索引状态 → 中文 */
export const aiIndexStatusText: Record<AiIndexStatus, string> = {
  NOT_INDEXED: '未建索引',
  PROCESSING: '索引中',
  INDEXED: '已索引',
  FAILED: '索引失败'
}

/** 学习状态 → 中文 */
export const studyStatusText = {
  NOT_STARTED: '未开始',
  IN_PROGRESS: '学习中',
  COMPLETED: '已完成'
} as const

/**
 * 课程封面：无 coverUrl 时生成确定性渐变 SVG 封面
 * 基于课程标题 hash 选择渐变组合，保证同一课程封面稳定
 */
const coverGradients: Array<[string, string]> = [
  ['#4f6bff', '#8b5cf6'],
  ['#06b6d4', '#3b82f6'],
  ['#f59e0b', '#ef4444'],
  ['#10b981', '#06b6d4'],
  ['#8b5cf6', '#ec4899'],
  ['#3b82f6', '#6366f1'],
  ['#f97316', '#f43f5e'],
  ['#14b8a6', '#84cc16']
]

function hashCode(str: string): number {
  let h = 0
  for (let i = 0; i < str.length; i++) {
    h = (h << 5) - h + str.charCodeAt(i)
    h |= 0
  }
  return Math.abs(h)
}

/** 生成课程封面 data-uri（纯 SVG，无外部依赖） */
export function courseCover(title: string, coverUrl?: string): string {
  if (coverUrl) return coverUrl
  const [c1, c2] = coverGradients[hashCode(title) % coverGradients.length]
  const initial = (title || '课').trim().charAt(0).toUpperCase()
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="480" height="300" viewBox="0 0 480 300">
  <defs>
    <linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="${c1}"/>
      <stop offset="1" stop-color="${c2}"/>
    </linearGradient>
    <linearGradient id="g2" x1="0" y1="1" x2="1" y2="0">
      <stop offset="0" stop-color="rgba(255,255,255,0.16)"/>
      <stop offset="1" stop-color="rgba(255,255,255,0)"/>
    </linearGradient>
  </defs>
  <rect width="480" height="300" fill="url(#g)"/>
  <circle cx="420" cy="40" r="140" fill="url(#g2)"/>
  <circle cx="60" cy="290" r="120" fill="url(#g2)"/>
  <text x="40" y="168" font-family="PingFang SC, Microsoft YaHei, sans-serif" font-size="96" font-weight="700" fill="rgba(255,255,255,0.92)">${initial}</text>
  <text x="40" y="248" font-family="PingFang SC, Microsoft YaHei, sans-serif" font-size="30" font-weight="600" fill="rgba(255,255,255,0.85)">知学 · Course</text>
</svg>`
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`
}

/** 用户头像：无头像时按用户名生成渐变首字母头像 */
const avatarGradients: Array<[string, string]> = [
  ['#4f6bff', '#8b5cf6'],
  ['#0ea5e9', '#6366f1'],
  ['#f59e0b', '#f43f5e'],
  ['#10b981', '#06b6d4'],
  ['#ec4899', '#8b5cf6']
]

export function userAvatar(name?: string, avatarUrl?: string): string {
  if (avatarUrl) return avatarUrl
  const n = name || 'U'
  const [c1, c2] = avatarGradients[hashCode(n) % avatarGradients.length]
  const ch = n.trim().charAt(0).toUpperCase()
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="120" height="120" viewBox="0 0 120 120">
  <defs>
    <linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="${c1}"/>
      <stop offset="1" stop-color="${c2}"/>
    </linearGradient>
  </defs>
  <rect width="120" height="120" rx="60" fill="url(#g)"/>
  <text x="60" y="78" font-family="PingFang SC, Microsoft YaHei, sans-serif" font-size="48" font-weight="700" fill="#fff" text-anchor="middle">${ch}</text>
</svg>`
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`
}
