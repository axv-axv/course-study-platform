import axios, { type AxiosRequestConfig, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/types'

const BASE_URL = import.meta.env.VITE_API_BASE || '/api/v1'

const service = axios.create({
  baseURL: BASE_URL,
  timeout: 30000
})

/** token 存取 */
export const tokenStore = {
  get accessToken() {
    return localStorage.getItem('accessToken') || ''
  },
  set accessToken(v: string) {
    localStorage.setItem('accessToken', v)
  },
  get refreshToken() {
    return localStorage.getItem('refreshToken') || ''
  },
  set refreshToken(v: string) {
    localStorage.setItem('refreshToken', v)
  },
  clear() {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
  }
}

// ---------- 请求拦截：携带 token ----------
service.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = tokenStore.accessToken
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// ---------- 响应拦截 ----------
let refreshing = false
let waitQueue: Array<(token: string) => void> = []

async function refreshToken(): Promise<string> {
  const refresh = tokenStore.refreshToken
  if (!refresh) throw new Error('no refresh token')
  const resp = await axios.post(`${BASE_URL}/auth/refresh`, { refreshToken: refresh })
  const data = resp.data as ApiResponse<{ accessToken: string; refreshToken: string }>
  if (data.code !== 0 || !data.data) throw new Error('refresh failed')
  tokenStore.accessToken = data.data.accessToken
  tokenStore.refreshToken = data.data.refreshToken || refresh
  return data.data.accessToken
}

function toLogin() {
  tokenStore.clear()
  if (!location.pathname.startsWith('/login')) {
    location.href = '/login'
  }
}

service.interceptors.response.use(
  (response: AxiosResponse) => {
    const body = response.data as ApiResponse
    // 后端可能直接返回 data（无包装），兼容两种
    if (body && typeof body === 'object' && 'code' in body && body.code !== undefined) {
      if (body.code !== 0) {
        ElMessage.error(body.message || '请求失败')
        return Promise.reject(new Error(body.message || '请求失败'))
      }
      return body.data as unknown as AxiosResponse
    }
    return response.data
  },
  async (error) => {
    const { response, config } = error
    if (!response) {
      ElMessage.error('网络异常，请检查网络连接')
      return Promise.reject(error)
    }
    // 401：尝试刷新 token 后重放
    if (response.status === 401 && !config._retry && tokenStore.refreshToken) {
      config._retry = true
      try {
        if (!refreshing) {
          refreshing = true
          const token = await refreshToken()
          refreshing = false
          waitQueue.forEach((cb) => cb(token))
          waitQueue = []
        } else {
          const token = await new Promise<string>((resolve) => waitQueue.push(resolve))
          config.headers.Authorization = `Bearer ${token}`
        }
        return service(config)
      } catch (e) {
        refreshing = false
        waitQueue = []
        toLogin()
        ElMessage.error('登录已过期，请重新登录')
        return Promise.reject(e)
      }
    }
    const msg = response.data?.message || `请求失败（${response.status}）`
    if (response.status === 403) {
      ElMessage.error('没有权限执行该操作')
    } else if (response.status === 404) {
      ElMessage.error('请求的资源不存在')
    } else if (response.status !== 401) {
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

/** 类型化请求方法 */
export function request<T = unknown>(config: AxiosRequestConfig): Promise<T> {
  return service.request<unknown, T>(config) as Promise<T>
}

export function get<T = unknown>(url: string, params?: Record<string, unknown>): Promise<T> {
  return request<T>({ url, method: 'GET', params })
}

export function post<T = unknown>(url: string, data?: unknown): Promise<T> {
  return request<T>({ url, method: 'POST', data })
}

export function patch<T = unknown>(url: string, data?: unknown): Promise<T> {
  return request<T>({ url, method: 'PATCH', data })
}

export function put<T = unknown>(url: string, data?: unknown): Promise<T> {
  return request<T>({ url, method: 'PUT', data })
}

export function del<T = unknown>(url: string, params?: Record<string, unknown>): Promise<T> {
  return request<T>({ url, method: 'DELETE', params })
}

export default service
