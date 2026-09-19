<template>
  <div class="page-container my-learning">
    <!-- 统计卡片 -->
    <div class="stat-grid">
      <div v-for="s in statCards" :key="s.label" class="stat-card app-card app-card-hover" @click="$router.push(s.path)">
        <div class="stat-icon" :style="{ background: s.bg, color: s.color }">
          <el-icon :size="24"><component :is="s.icon" /></el-icon>
        </div>
        <div class="stat-body">
          <div class="stat-value">{{ s.value }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </div>
        <el-icon class="stat-arrow" :size="14"><ArrowRight /></el-icon>
      </div>
    </div>

    <el-row :gutter="20">
      <!-- 左侧：我的课程进度 -->
      <el-col :xs="24" :lg="14">
        <div class="app-card panel">
          <div class="panel-head">
            <h3 class="panel-title">我的课程</h3>
            <el-button text type="primary" @click="$router.push('/courses')">去选课<el-icon><ArrowRight /></el-icon></el-button>
          </div>

          <template v-if="dashboard?.courses.length">
            <div v-for="c in dashboard.courses" :key="c.courseId" class="course-progress-item" @click="$router.push(`/course/${c.courseId}`)">
              <img :src="courseCover(c.title, c.coverUrl)" class="cp-cover" alt="" />
              <div class="cp-info">
                <span class="cp-title text-ellipsis">{{ c.title }}</span>
                <el-progress
                  :percentage="c.progress"
                  :stroke-width="9"
                  :color="progressColor(c.progress)"
                  class="cp-bar"
                  :format="() => `${c.progress}%`"
                />
              </div>
              <span class="cp-percent">{{ c.progress }}%</span>
            </div>
            <EmptyState v-if="!dashboard.courses.length" icon="Collection" text="还没有加入课程" sub-text="去课程广场选择感兴趣的课程开始学习吧" padding="40">
              <el-button class="btn-gradient" @click="$router.push('/courses')">浏览课程</el-button>
            </EmptyState>
          </template>
          <el-skeleton v-else :rows="3" animated />
        </div>
      </el-col>

      <!-- 右侧：最近学习 -->
      <el-col :xs="24" :lg="10">
        <div class="app-card panel">
          <div class="panel-head">
            <h3 class="panel-title">最近学习</h3>
            <el-button text type="primary" @click="$router.push('/notes')">我的笔记<el-icon><ArrowRight /></el-icon></el-button>
          </div>

          <div v-if="dashboard?.recentLearning.length" class="recent-list">
            <div v-for="r in dashboard.recentLearning.slice(0, 6)" :key="r.resourceId" class="recent-item" @click="$router.push(`/resource/${r.resourceId}`)">
              <div class="ri-icon">
                <el-icon :size="16" :color="resourceTypeColor(r.resourceType || 'OTHER')">
                  <component :is="resourceTypeIcon(r.resourceType || 'OTHER')" />
                </el-icon>
              </div>
              <div class="ri-info">
                <span class="ri-title text-ellipsis">{{ r.title }}</span>
                <span class="ri-course text-ellipsis">{{ r.courseTitle || '—' }}</span>
              </div>
              <div class="ri-progress">
                <ProgressRing :value="r.progress" :size="40" :stroke-width="5" :font-size="9" :display-text="`${r.progress}%`" />
              </div>
            </div>
          </div>
          <EmptyState v-else icon="Clock" text="暂无学习记录" sub-text="开始学习后，这里会展示你的最近动态" padding="40" />
        </div>
      </el-col>
    </el-row>

    <!-- 学习概览 -->
    <div class="app-card panel overview-panel">
      <div class="panel-head">
        <h3 class="panel-title">学习概览</h3>
      </div>
      <el-row :gutter="32" class="overview-row">
        <el-col :xs="24" :md="8" class="overview-ring-col">
          <ProgressRing :value="overallPercent" :size="150" :stroke-width="12" :font-size="26" :display-text="`${overallPercent}%`" color="#4f6bff" />
          <div class="ring-label">整体完成率</div>
        </el-col>
        <el-col :xs="24" :md="16">
          <div class="legend-grid">
            <div v-for="item in legendItems" :key="item.label" class="legend-item">
              <span class="legend-dot" :style="{ background: item.color }"></span>
              <span class="legend-label">{{ item.label }}</span>
              <span class="legend-value">{{ item.value }}</span>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ProgressRing from '@/components/ProgressRing.vue'
import EmptyState from '@/components/EmptyState.vue'
import { getLearningDashboard } from '@/api/user'
import type { LearningDashboard } from '@/types'
import { courseCover, resourceTypeColor, resourceTypeIcon } from '@/utils/format'

const router = useRouter()
const dashboard = ref<LearningDashboard | null>(null)
const loading = ref(true)

const statCards = computed(() => [
  {
    label: '在学课程',
    value: dashboard.value?.courseCount ?? 0,
    icon: 'Collection',
    bg: 'rgba(79,107,255,0.1)',
    color: '#4f6bff',
    path: '/courses'
  },
  {
    label: '已完成资料',
    value: dashboard.value?.completedResourceCount ?? 0,
    icon: 'CircleCheck',
    bg: 'rgba(34,197,94,0.12)',
    color: '#22c55e',
    path: '/my-learning'
  },
  {
    label: '我的收藏',
    value: dashboard.value?.favoriteCount ?? 0,
    icon: 'Star',
    bg: 'rgba(245,158,11,0.12)',
    color: '#f59e0b',
    path: '/favorites'
  },
  {
    label: '学习笔记',
    value: dashboard.value?.noteCount ?? 0,
    icon: 'Notebook',
    bg: 'rgba(139,92,246,0.12)',
    color: '#8b5cf6',
    path: '/notes'
  }
])

const overallPercent = computed(() => {
  const c = dashboard.value?.courses || []
  if (!c.length) return 0
  const sum = c.reduce((acc, x) => acc + (x.progress || 0), 0)
  return Math.round(sum / c.length)
})

const legendItems = computed(() => {
  const courses = dashboard.value?.courses || []
  const completed = courses.filter((c) => c.progress >= 100).length
  const inProgress = courses.filter((c) => c.progress > 0 && c.progress < 100).length
  const notStarted = courses.filter((c) => c.progress <= 0).length
  return [
    { label: '已学完课程', value: completed, color: '#22c55e' },
    { label: '学习中课程', value: inProgress, color: '#4f6bff' },
    { label: '未开始课程', value: notStarted, color: '#94a3b8' }
  ]
})

function progressColor(p: number) {
  if (p >= 100) return '#22c55e'
  if (p >= 50) return '#4f6bff'
  return '#f59e0b'
}

async function load() {
  loading.value = true
  try {
    dashboard.value = await getLearningDashboard()
  } catch {
    ElMessage.warning('学习数据加载失败，请检查后端服务')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped lang="scss">
.my-learning {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

// ---------- 统计卡片 ----------
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;

  @media (max-width: 1000px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px;
  cursor: pointer;
  position: relative;

  .stat-icon {
    width: 50px;
    height: 50px;
    border-radius: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .stat-value {
    font-size: 26px;
    font-weight: 800;
    line-height: 1.1;
  }

  .stat-label {
    font-size: 13px;
    color: $color-text-3;
  }

  .stat-arrow {
    margin-left: auto;
    color: $color-text-3;
    transition: $transition-base;
  }

  &:hover .stat-arrow {
    color: $color-primary;
    transform: translateX(3px);
  }
}

// ---------- 面板 ----------
.panel {
  padding: 20px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;

  .panel-title {
    font-size: 17px;
    font-weight: 800;
  }
}

// ---------- 课程进度列表 ----------
.course-progress-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px;
  border-radius: 12px;
  cursor: pointer;
  transition: $transition-base;

  &:hover {
    background: var(--app-hover, rgba(79, 107, 255, 0.05));
  }

  .cp-cover {
    width: 64px;
    height: 44px;
    border-radius: 8px;
    object-fit: cover;
    flex-shrink: 0;
  }

  .cp-info {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 6px;

    .cp-title {
      font-size: 14px;
      font-weight: 600;
    }
  }

  .cp-percent {
    font-size: 15px;
    font-weight: 800;
    color: $color-primary;
    width: 48px;
    text-align: right;
    flex-shrink: 0;
  }
}

// ---------- 最近学习 ----------
.recent-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.recent-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  border-radius: 12px;
  cursor: pointer;
  transition: $transition-base;

  &:hover {
    background: var(--app-hover, rgba(79, 107, 255, 0.05));
  }

  .ri-icon {
    width: 38px;
    height: 38px;
    border-radius: 10px;
    background: var(--app-hover, rgba(79, 107, 255, 0.06));
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .ri-info {
    flex: 1;
    min-width: 0;

    .ri-title {
      display: block;
      font-size: 14px;
      font-weight: 600;
    }

    .ri-course {
      display: block;
      font-size: 12px;
      color: $color-text-3;
      margin-top: 2px;
    }
  }

  .ri-progress {
    flex-shrink: 0;
  }
}

// ---------- 学习概览 ----------
.overview-panel {
  margin-top: 0;
}

.overview-row {
  align-items: center;
}

.overview-ring-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-bottom: 12px;

  .ring-label {
    margin-top: 10px;
    font-size: 14px;
    color: $color-text-3;
    font-weight: 600;
  }
}

.legend-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 16px;
  padding: 8px 0;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px;
  border-radius: 12px;
  background: var(--app-hover, rgba(79, 107, 255, 0.04));

  .legend-dot {
    width: 12px;
    height: 12px;
    border-radius: 50%;
    flex-shrink: 0;
  }

  .legend-label {
    font-size: 13px;
    color: $color-text-2;
    flex: 1;
  }

  .legend-value {
    font-size: 18px;
    font-weight: 800;
  }
}
</style>
