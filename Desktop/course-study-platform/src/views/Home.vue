<template>
  <div class="page-container">
    <!-- 顶部 Hero -->
    <section class="hero app-card">
      <div class="hero-left">
        <div class="hero-greeting">
          <span class="wave">👋</span> {{ greeting }}，{{ userStore.userInfo?.nickname || userStore.userInfo?.username || '同学' }}
        </div>
        <h2 class="hero-title">
          让每一份课程资料<br />
          <span class="gradient-text">都成为你的知识力量</span>
        </h2>
        <p class="hero-desc">集中管理课程资料，追踪学习进度，用 AI 随时解答课程问题</p>
        <div class="hero-actions">
          <el-button class="btn-gradient" size="large" @click="$router.push('/courses')">
            <el-icon class="mr8"><Collection /></el-icon>浏览课程
          </el-button>
          <el-button size="large" plain type="primary" @click="$router.push('/ai')">
            <el-icon class="mr8"><MagicStick /></el-icon>AI 提问
          </el-button>
        </div>
        <div class="hero-stats">
          <div v-for="s in stats" :key="s.label" class="stat-item">
            <span class="stat-num">{{ s.value }}</span>
            <span class="stat-label">{{ s.label }}</span>
          </div>
        </div>
      </div>
      <div class="hero-right">
        <div class="hero-illustration">
          <div class="il-card il-card-1">
            <el-icon :size="20" color="#4f6bff"><Collection /></el-icon>
            <div>
              <div class="il-title">机器学习基础</div>
              <div class="il-sub">12 / 20 已完成 · 60%</div>
            </div>
            <el-progress type="circle" :percentage="60" :width="44" :stroke-width="5" color="#4f6bff" />
          </div>
          <div class="il-card il-card-2">
            <div class="il-ai">
              <el-icon :size="16" color="#fff"><MagicStick /></el-icon>
            </div>
            <div class="il-ai-content">
              <div class="il-title">AI 解答</div>
              <div class="il-sub">梯度下降是一种优化算法…</div>
            </div>
          </div>
          <div class="il-card il-card-3">
            <div class="il-note">
              <el-icon :size="16" color="#f59e0b"><Notebook /></el-icon>
            </div>
            <div class="il-title">学习笔记</div>
            <div class="il-sub">已记录 28 条</div>
          </div>
        </div>
      </div>
    </section>

    <!-- 快捷入口 -->
    <section class="quick-grid">
      <div v-for="q in quickEntries" :key="q.path" class="quick-item app-card app-card-hover" @click="$router.push(q.path)">
        <div class="quick-icon" :style="{ background: q.bg, color: q.color }">
          <el-icon :size="22"><component :is="q.icon" /></el-icon>
        </div>
        <div>
          <div class="quick-title">{{ q.title }}</div>
          <div class="quick-desc">{{ q.desc }}</div>
        </div>
        <el-icon class="quick-arrow"><ArrowRight /></el-icon>
      </div>
    </section>

    <!-- 推荐课程 -->
    <section v-if="courses.length" class="section">
      <div class="section-head">
        <h3 class="section-title">推荐课程</h3>
        <el-button text type="primary" @click="$router.push('/courses')">查看全部<el-icon><ArrowRight /></el-icon></el-button>
      </div>
      <div class="course-grid">
        <CourseCard v-for="c in courses.slice(0, 4)" :key="c.id" :course="c" />
      </div>
    </section>

    <!-- 继续学习 -->
    <section v-if="recentLearning.length" class="section">
      <div class="section-head">
        <h3 class="section-title">继续学习</h3>
        <el-button text type="primary" @click="$router.push('/my-learning')">学习中心<el-icon><ArrowRight /></el-icon></el-button>
      </div>
      <div class="recent-list app-card">
        <div v-for="r in recentLearning.slice(0, 4)" :key="r.resourceId" class="recent-item" @click="$router.push(`/resource/${r.resourceId}`)">
          <div class="recent-icon">
            <el-icon :size="17" :color="resourceTypeColor(r.resourceType || 'OTHER')">
              <component :is="resourceTypeIcon(r.resourceType || 'OTHER')" />
            </el-icon>
          </div>
          <div class="recent-info">
            <span class="recent-title text-ellipsis">{{ r.title }}</span>
            <span class="recent-course text-ellipsis">{{ r.courseTitle || '' }}</span>
          </div>
          <el-progress :percentage="r.progress" :stroke-width="8" :color="progressColor(r.progress)" class="recent-progress" />
          <span class="recent-percent">{{ r.progress }}%</span>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import CourseCard from '@/components/CourseCard.vue'
import { getCourses } from '@/api/course'
import { getRecentLearning } from '@/api/user'
import type { Course, RecentLearning } from '@/types'
import { resourceTypeColor, resourceTypeIcon } from '@/utils/format'

const router = useRouter()
const userStore = useUserStore()

const courses = ref<Course[]>([])
const recentLearning = ref<RecentLearning[]>([])

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const stats = computed(() => [
  { value: courses.value.length, label: '推荐课程' },
  { value: recentLearning.value.length, label: '最近学习' },
  { value: 'AI', label: '智能问答' }
])

const quickEntries = [
  { path: '/courses', title: '全部课程', desc: '发现感兴趣的学习内容', icon: 'Collection', bg: 'rgba(79,107,255,0.1)', color: '#4f6bff' },
  { path: '/my-learning', title: '我的学习', desc: '查看进度与学习数据', icon: 'DataBoard', bg: 'rgba(16,185,129,0.12)', color: '#10b981' },
  { path: '/ai', title: 'AI 课程问答', desc: '基于资料智能回答', icon: 'MagicStick', bg: 'rgba(139,92,246,0.12)', color: '#8b5cf6' },
  { path: '/notes', title: '学习笔记', desc: '沉淀你的知识碎片', icon: 'Notebook', bg: 'rgba(245,158,11,0.12)', color: '#f59e0b' }
]

function progressColor(p: number) {
  if (p >= 100) return '#22c55e'
  if (p >= 50) return '#4f6bff'
  return '#f59e0b'
}

async function load() {
  try {
    const [courseRes, recentRes] = await Promise.all([
      getCourses({ page: 1, size: 8, status: 'ACTIVE' }),
      getRecentLearning()
    ])
    courses.value = courseRes.items || []
    recentLearning.value = recentRes || []
  } catch {
    ElMessage.warning('部分数据加载失败')
  }
}

onMounted(load)
</script>

<style scoped lang="scss">
.page-container {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

// ---------- Hero ----------
.hero {
  display: flex;
  justify-content: space-between;
  gap: 32px;
  padding: 44px 48px;
  overflow: hidden;
  position: relative;
  background:
    radial-gradient(600px 300px at 85% 20%, rgba(139, 92, 246, 0.08), transparent 60%),
    radial-gradient(500px 260px at 20% 100%, rgba(79, 107, 255, 0.08), transparent 60%),
    var(--app-card-bg, #fff);
}

.hero-left {
  flex: 1;
  min-width: 0;
}

.hero-greeting {
  font-size: 15px;
  color: $color-text-2;
  margin-bottom: 12px;

  .wave {
    display: inline-block;
  }
}

.hero-title {
  font-size: 34px;
  font-weight: 800;
  line-height: 1.35;
  margin-bottom: 14px;
}

.hero-desc {
  font-size: 15px;
  color: $color-text-3;
  margin-bottom: 26px;
}

.hero-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 34px;
}

.hero-stats {
  display: flex;
  gap: 40px;

  .stat-item {
    display: flex;
    flex-direction: column;

    .stat-num {
      font-size: 24px;
      font-weight: 800;
      color: $color-primary;
    }

    .stat-label {
      font-size: 12px;
      color: $color-text-3;
      margin-top: 2px;
    }
  }
}

// ---------- Hero 插画 ----------
.hero-right {
  width: 340px;
  position: relative;
  flex-shrink: 0;
}

.hero-illustration {
  position: relative;
  height: 240px;
}

.il-card {
  position: absolute;
  background: var(--app-card-bg, #fff);
  border: 1px solid $color-border;
  border-radius: 16px;
  box-shadow: $shadow-card;
  padding: 14px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  animation: float 5s ease-in-out infinite;

  .il-title {
    font-size: 13px;
    font-weight: 700;
  }

  .il-sub {
    font-size: 12px;
    color: $color-text-3;
    margin-top: 3px;
  }
}

.il-card-1 {
  top: 8px;
  left: 0;
  right: 0;
  animation-delay: 0s;
}

.il-card-2 {
  bottom: 26px;
  left: 0;
  animation-delay: 1.2s;
}

.il-card-3 {
  bottom: 0;
  right: 0;
  animation-delay: 2.4s;
}

.il-ai {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  background: $gradient-primary;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.35);
}

.il-note {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  background: rgba(245, 158, 11, 0.14);
  display: flex;
  align-items: center;
  justify-content: center;
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0);
  }

  50% {
    transform: translateY(-6px);
  }
}

// ---------- 快捷入口 ----------
.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;

  @media (max-width: 1100px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.quick-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
  cursor: pointer;
  position: relative;

  .quick-icon {
    width: 46px;
    height: 46px;
    border-radius: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .quick-title {
    font-size: 15px;
    font-weight: 700;
  }

  .quick-desc {
    font-size: 12px;
    color: $color-text-3;
    margin-top: 3px;
  }

  .quick-arrow {
    margin-left: auto;
    color: $color-text-3;
    transition: $transition-base;
  }

  &:hover .quick-arrow {
    color: $color-primary;
    transform: translateX(4px);
  }
}

// ---------- 课程网格 ----------
.section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .section-title {
    font-size: 20px;
    font-weight: 800;
  }
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;

  @media (max-width: 1200px) {
    grid-template-columns: repeat(3, 1fr);
  }

  @media (max-width: 900px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

// ---------- 继续学习 ----------
.recent-list {
  display: flex;
  flex-direction: column;
  padding: 6px 20px;
}

.recent-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 0;
  border-bottom: 1px solid $color-border;
  cursor: pointer;
  transition: $transition-base;

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    padding-left: 6px;
  }
}

.recent-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: var(--app-hover, rgba(79, 107, 255, 0.06));
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.recent-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.recent-title {
  font-size: 14px;
  font-weight: 600;
}

.recent-course {
  font-size: 12px;
  color: $color-text-3;
  max-width: 260px;
}

.recent-progress {
  width: 180px;
  flex-shrink: 0;

  @media (max-width: 700px) {
    display: none;
  }
}

.recent-percent {
  width: 44px;
  text-align: right;
  font-size: 13px;
  font-weight: 700;
  color: $color-primary;
  flex-shrink: 0;
}

.mr8 {
  margin-right: 6px;
}
</style>
