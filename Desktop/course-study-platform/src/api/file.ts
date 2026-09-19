import { del, get, request } from './request'
import type { FileInfo } from '@/types'

/** 上传文件（multipart/form-data） */
export function uploadFile(file: File, onProgress?: (percent: number) => void) {
  const formData = new FormData()
  formData.append('file', file)
  return request<FileInfo>({
    url: '/files',
    method: 'POST',
    data: formData,
    onUploadProgress: (event) => {
      if (onProgress && event.total) onProgress(Math.round((event.loaded * 100) / event.total))
    }
  })
}

/** 获取文件信息 */
export function getFileInfo(fileId: number) {
  return get<FileInfo>(`/files/${fileId}`)
}

/** 删除文件 */
export function deleteFile(fileId: number) {
  return del<void>(`/files/${fileId}`)
}

/** 文件预览地址（后端返回流，用于 <iframe>/<img>/<video> 直接访问） */
export function filePreviewUrl(fileId: number): string {
  return `${import.meta.env.VITE_API_BASE || '/api/v1'}/files/${fileId}/preview`
}

/** 文件下载地址（带 token 时走 Blob 下载） */
export function fileDownloadUrl(fileId: number): string {
  return `${import.meta.env.VITE_API_BASE || '/api/v1'}/files/${fileId}/download`
}
