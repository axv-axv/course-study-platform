import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { tokenStore } from '@/api/request'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/',
    component: () => import('@/components/layout/MainLayout.vue'),
    redirect: '/home',
    children: [
      { path: 'home', name: 'home', component: () => import('@/views/Home.vue'), meta: { title: '首页' } },
      { path: 'my-learning', name: 'my-learning', component: () => import('@/views/MyLearning.vue'), meta: { title: '我的学习' } },
      { path: 'courses', name: 'courses', component: () => import('@/views/course/CourseList.vue'), meta: { title: '全部课程' } },
      { path: 'course/:courseId', name: 'course-detail', component: () => import('@/views/course/CourseDetail.vue'), meta: { title: '课程详情' } },
      { path: 'resource/:resourceId', name: 'resource-detail', component: () => import('@/views/resource/ResourceDetail.vue'), meta: { title: '学习资料' } },
      { path: 'ai/:courseId?', name: 'ai-chat', component: () => import('@/views/ai/AiChat.vue'), meta: { title: 'AI 课程问答' } },
      { path: 'search', name: 'search', component: () => import('@/views/Search.vue'), meta: { title: '搜索' } },
      { path: 'favorites', name: 'favorites', component: () => import('@/views/Favorites.vue'), meta: { title: '我的收藏' } },
      { path: 'notes', name: 'notes', component: () => import('@/views/MyNotes.vue'), meta: { title: '我的笔记' } },
      { path: 'profile', name: 'profile', component: () => import('@/views/Profile.vue'), meta: { title: '个人中心' } },
      // 教师端
      { path: 'teacher/courses', name: 'teacher-courses', component: () => import('@/views/teacher/TeacherCourses.vue'), meta: { title: '我的课程管理', roles: ['TEACHER', 'ADMIN'] } },
      { path: 'teacher/course/create', name: 'teacher-course-create', component: () => import('@/views/teacher/CourseEdit.vue'), meta: { title: '创建课程', roles: ['TEACHER', 'ADMIN'] } },
      { path: 'teacher/course/:courseId/edit', name: 'teacher-course-edit', component: () => import('@/views/teacher/CourseEdit.vue'), meta: { title: '编辑课程', roles: ['TEACHER', 'ADMIN'] } },
      { path: 'teacher/course/:courseId/members', name: 'teacher-members', component: () => import('@/views/teacher/MemberManage.vue'), meta: { title: '成员管理', roles: ['TEACHER', 'ADMIN'] } },
      // 管理端
      { path: 'admin', name: 'admin', component: () => import('@/views/admin/AdminDashboard.vue'), meta: { title: '平台管理', roles: ['ADMIN'] } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/home' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach(async (to) => {
  const userStore = useUserStore()
  document.title = to.meta.title ? `${String(to.meta.title)} · 知学` : '知学 · 课程资料与智能学习平台'

  if (to.meta.public) {
    if (tokenStore.accessToken && to.path === '/login') return { path: '/home' }
    return true
  }

  if (!tokenStore.accessToken) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  // 已登录但无用户信息时拉取一次
  if (!userStore.userInfo) {
    await userStore.fetchUserInfo()
  }

  // 角色路由守卫
  const roles = to.meta.roles as string[] | undefined
  if (roles && userStore.userInfo && !roles.includes(userStore.userInfo.role)) {
    return { path: '/home' }
  }

  return true
})

export default router
