<template>
  <el-container class="layout-root">
    <!-- 侧边栏 -->
    <el-aside :width="collapsed ? '72px' : '232px'" class="layout-aside">
      <div class="brand" @click="$router.push('/home')">
        <div class="brand-logo">
          <el-icon :size="22"><Reading /></el-icon>
        </div>
        <transition name="fade">
          <span v-if="!collapsed" class="brand-name">知学<span class="brand-dot">·</span>ZhiXue</span>
        </transition>
      </div>

      <el-scrollbar class="menu-scroll">
        <el-menu :default-active="activeMenu" :collapse="collapsed" router class="side-menu">
          <el-menu-item index="/home">
            <el-icon><HomeFilled /></el-icon>
            <template #title>首页</template>
          </el-menu-item>
          <el-menu-item index="/my-learning">
            <el-icon><DataBoard /></el-icon>
            <template #title>我的学习</template>
          </el-menu-item>
          <el-menu-item index="/courses">
            <el-icon><Collection /></el-icon>
            <template #title>全部课程</template>
          </el-menu-item>
          <el-menu-item index="/ai">
            <el-icon><MagicStick /></el-icon>
            <template #title>AI 课程问答</template>
          </el-menu-item>
          <el-menu-item index="/search">
            <el-icon><Search /></el-icon>
            <template #title>资源搜索</template>
          </el-menu-item>
          <el-menu-item index="/favorites">
            <el-icon><Star /></el-icon>
            <template #title>我的收藏</template>
          </el-menu-item>
          <el-menu-item index="/notes">
            <el-icon><Notebook /></el-icon>
            <template #title>我的笔记</template>
          </el-menu-item>

          <template v-if="isTeacher">
            <el-divider class="menu-divider" />
            <div v-if="!collapsed" class="menu-group-title">教学管理</div>
            <el-menu-item index="/teacher/courses">
              <el-icon><School /></el-icon>
              <template #title>我的课程</template>
            </el-menu-item>
            <el-menu-item index="/teacher/course/create">
              <el-icon><CirclePlus /></el-icon>
              <template #title>创建课程</template>
            </el-menu-item>
          </template>

          <template v-if="isAdmin">
            <el-divider class="menu-divider" />
            <div v-if="!collapsed" class="menu-group-title">系统管理</div>
            <el-menu-item index="/admin">
              <el-icon><Monitor /></el-icon>
              <template #title>平台管理</template>
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container class="layout-body">
      <!-- 顶栏 -->
      <el-header class="layout-header" height="60px">
        <div class="header-left">
          <el-button text class="collapse-btn" @click="collapsed = !collapsed">
            <el-icon :size="18"><Expand v-if="collapsed" /><Fold v-else /></el-icon>
          </el-button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/home' }">知学</el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <!-- 全局搜索 -->
          <div class="header-search" @click="$router.push('/search')">
            <el-icon><Search /></el-icon>
            <span>搜索课程 / 资料 / 标签</span>
            <el-tag size="small" effect="plain" round class="search-kbd">/</el-tag>
          </div>

          <el-switch
            v-model="themeStore.dark"
            inline-prompt
            :active-icon="Moon"
            :inactive-icon="Sunny"
            @change="themeStore.toggle"
          />

          <el-dropdown trigger="click" @command="handleCommand">
            <div class="user-entry">
              <el-avatar :size="32" :src="userAvatar(userStore.userInfo?.nickname || userStore.userInfo?.username, userStore.userInfo?.avatarUrl)">
                {{ (userStore.userInfo?.nickname || userStore.userInfo?.username || 'U').charAt(0).toUpperCase() }}
              </el-avatar>
              <span class="user-name">{{ userStore.userInfo?.nickname || userStore.userInfo?.username || '未登录' }}</span>
              <el-tag size="small" effect="light" round class="role-tag">{{ roleText }}</el-tag>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item command="my-learning">
                  <el-icon><DataBoard /></el-icon>我的学习
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="layout-main">
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { Moon, Sunny } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { userAvatar } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()

const collapsed = ref(false)

const isTeacher = computed(() => ['TEACHER', 'ADMIN'].includes(userStore.userInfo?.role || ''))
const isAdmin = computed(() => userStore.userInfo?.role === 'ADMIN')

const roleText = computed(() => {
  const map: Record<string, string> = { STUDENT: '学生', TEACHER: '教师', ADMIN: '管理员' }
  return map[userStore.userInfo?.role || ''] || ''
})

const activeMenu = computed(() => {
  // 高亮当前路由（父级路径）
  const path = route.path
  if (path.startsWith('/course/')) return '/courses'
  if (path.startsWith('/resource/')) return '/courses'
  if (path.startsWith('/ai/')) return '/ai'
  if (path.startsWith('/teacher/')) return '/teacher/courses'
  return path
})

function handleCommand(cmd: string) {
  if (cmd === 'logout') {
    ElMessageBox.confirm('确定退出登录吗？', '退出登录', { type: 'warning' })
      .then(() => {
        userStore.logout()
        router.push('/login')
      })
      .catch(() => {})
    return
  }
  router.push(`/${cmd}`)
}

// 快捷键 "/" 聚焦搜索
function onKeydown(e: KeyboardEvent) {
  if (e.key === '/' && !['INPUT', 'TEXTAREA'].includes((e.target as HTMLElement).tagName)) {
    e.preventDefault()
    router.push('/search')
  }
}

onMounted(() => window.addEventListener('keydown', onKeydown))
onUnmounted(() => window.removeEventListener('keydown', onKeydown))
</script>

<style scoped lang="scss">
.layout-root {
  height: 100vh;
}

// ---------- 侧边栏 ----------
.layout-aside {
  background: #fff;
  border-right: 1px solid $color-border;
  display: flex;
  flex-direction: column;
  transition: width 0.25s ease;
  overflow: hidden;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 18px 16px;
  cursor: pointer;
  white-space: nowrap;

  .brand-logo {
    width: 38px;
    height: 38px;
    border-radius: 12px;
    background: $gradient-primary;
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    box-shadow: 0 4px 12px rgba(79, 107, 255, 0.35);
  }

  .brand-name {
    font-size: 18px;
    font-weight: 800;
    letter-spacing: 0.5px;

    .brand-dot {
      color: $color-primary;
      margin: 0 2px;
    }
  }
}

.menu-scroll {
  flex: 1;
}

.side-menu {
  border-right: none;
  padding: 4px 10px;

  :deep(.el-menu-item) {
    height: 44px;
    line-height: 44px;
    border-radius: 10px;
    margin-bottom: 4px;
    color: $color-text-2;

    &:hover {
      background: rgba(79, 107, 255, 0.06);
      color: $color-primary;
    }

    &.is-active {
      background: $gradient-soft;
      color: $color-primary;
      font-weight: 600;
    }
  }
}

.menu-divider {
  margin: 10px 8px;
  border-color: $color-border;
}

.menu-group-title {
  font-size: 12px;
  color: $color-text-3;
  padding: 6px 14px;
}

// ---------- 顶栏 ----------
.layout-header {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid $color-border;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.collapse-btn {
  color: $color-text-2;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-search {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 12px;
  border-radius: 10px;
  background: $color-bg-page;
  border: 1px solid $color-border;
  color: $color-text-3;
  font-size: 13px;
  cursor: pointer;
  transition: $transition-base;

  &:hover {
    border-color: $color-primary;
    color: $color-primary;
  }

  .search-kbd {
    background: #fff;
  }
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 6px;
  border-radius: 10px;
  transition: $transition-base;

  &:hover {
    background: $color-bg-page;
  }

  .user-name {
    font-size: 14px;
    font-weight: 500;
    max-width: 96px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .role-tag {
    transform: scale(0.9);
  }
}

// ---------- 内容区 ----------
.layout-main {
  padding: 20px;
  overflow-y: auto;
  background: $color-bg-page;
}

.page-enter-active,
.page-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.page-enter-from {
  opacity: 0;
  transform: translateY(6px);
}

.page-leave-to {
  opacity: 0;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
