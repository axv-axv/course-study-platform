<template>
  <router-view />
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { tokenStore } from '@/api/request'

const userStore = useUserStore()
const themeStore = useThemeStore()

onMounted(() => {
  themeStore.apply()
  // 已登录则恢复用户信息
  if (tokenStore.accessToken && !userStore.userInfo) {
    userStore.fetchUserInfo()
  }
})
</script>
