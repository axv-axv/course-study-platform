import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getMe } from '@/api/auth'
import type { UserInfo } from '@/types'
import { tokenStore } from '@/api/request'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(tokenStore.accessToken)
  const userInfo = ref<UserInfo | null>(null)

  function setToken(t: string, refresh?: string) {
    token.value = t
    tokenStore.accessToken = t
    if (refresh) tokenStore.refreshToken = refresh
  }

  function setUserInfo(info: UserInfo | null) {
    userInfo.value = info
  }

  /** 拉取当前用户信息（自动带 token） */
  async function fetchUserInfo() {
    if (!token.value) return null
    try {
      const info = await getMe()
      userInfo.value = info
      return info
    } catch {
      return null
    }
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    tokenStore.clear()
  }

  return { token, userInfo, setToken, setUserInfo, fetchUserInfo, logout }
})
