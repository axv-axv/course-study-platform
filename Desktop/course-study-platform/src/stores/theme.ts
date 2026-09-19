import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

const THEME_KEY = 'app-theme'

export const useThemeStore = defineStore('theme', () => {
  const dark = ref<boolean>(localStorage.getItem(THEME_KEY) === 'dark')

  function apply() {
    document.documentElement.classList.toggle('dark', dark.value)
  }

  function toggle() {
    dark.value = !dark.value
  }

  watch(
    dark,
    (v) => {
      localStorage.setItem(THEME_KEY, v ? 'dark' : 'light')
      apply()
    },
    { immediate: true }
  )

  return { dark, apply, toggle }
})
