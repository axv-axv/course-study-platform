<template>
  <div class="page-container course-detail" v-loading="loading">
    <template v-if="course">
      <!-- 课程头部 -->
      <section class="course-header app-card">
        <div class="header-cover">
          <img :src="cover" :alt="course.title" />
          <div class="cover-overlay"></div>
        </div>
        <div class="header-info">
          <div class="info-tags">
            <el-tag v-if="course.visibility === 'PUBLIC'" type="success" effect="light" round>公开课程</el-tag>
            <el-tag v-else type="warning" effect="light" round>私密课程</el-tag>
            <el-tag v-if="course.joined" type="primary" effect="plain" round>已加入</el-tag>
          </div>
          <h1 class="course-title">{{ course.title }}</h1>
          <p class="course-desc">{{ course.description || '暂无课程简介' }}</p>
          <div class="course-meta">
            <span class="meta-item">
              <el-icon><User /></el-icon>{{ course.memberCount ?? 0 }} 人在学
            </span>
            <span class="meta-item">
              <el-icon><FolderOpened /></el-icon>{{ course.chapterCount ?? 0 }} 个章节
            </span>
            <span class="meta-item">
              <el-icon><Document /></el-icon>{{ course.resourceCount ?? 0 }} 份资料
            </span>
            <span v-if="course.creator" class="meta-item creator">
              <el-avatar :size="22" :src="creatorAvatar">{{ (course.creator.nickname || '师').charAt(0) }}</el-avatar>
              {{ course.creator.nickname || '讲师' }}
            </span>
          </div>
          <div class="header-actions">
            <el-button v-if="!course.joined" class="btn-gradient" size="large" :loading="joining" @click="handleJoin">
              <el-icon class="mr6"><Plus /></el-icon>加入课程
            </el-button>
            <el-button v-else size="large" plain type="danger" @click="handleLeave">退出课程</el-button>
            <el-button size="large" plain type="primary" @click="$router.push(`/ai/${course.id}`)">
              <el-icon class="mr6"><MagicStick /></el-icon>AI 问答
            </el-button>
            <el-button v-if="canManage" size="large" type="warning" plain @click="$router.push(`/teacher/course/${course.id}/edit`)">
              <el-icon class="mr6"><Edit /></el-icon>管理课程
            </el-button>
          </div>
        </div>
      </section>

      <!-- 学习进度条 -->
      <section v-if="overview.progress" class="progress-bar app-card">
        <div class="pb-label">本课程学习进度</div>
        <el-progress
          :percentage="overview.progress?.progress || 0"
          :stroke-width="12"
          color="#4f6bff"
          class="pb-progress"
          :format="() => `${overview.progress?.completedCount ?? 0} / ${overview.progress?.resourceCount ?? 0} 已完成`"
        />
        <div class="pb-stats">
          <span>已完成 {{ overview.progress?.completedCount ?? 0 }}</span>
          <span>学习中 {{ overview.progress?.inProgressCount ?? 0 }}</span>
          <span>未开始 {{ overview.progress?.notStartedCount ?? 0 }}</span>
        </div>
      </section>

      <!-- 章节 + 资料 -->
      <el-row :gutter="20">
        <el-col :xs="24" :md="7">
          <div class="app-card chapter-panel">
            <div class="panel-head">
              <h3 class="panel-title">课程章节</h3>
              <span class="panel-count">{{ chapters.length }}</span>
            </div>
            <ChapterTree :chapters="chapters" :active-id="activeChapterId" @select="selectChapter" />
          </div>
        </el-col>

        <el-col :xs="24" :md="17">
          <div class="app-card resources-panel">
            <div class="panel-head">
              <div>
                <h3 class="panel-title">{{ activeChapter ? activeChapter.title : '全部资料' }}</h3>
                <span class="panel-sub">{{ activeChapter ? `该章节共 ${activeChapter.resourceCount ?? 0} 份资料` : `共 ${course.resourceCount ?? 0} 份资料` }}</span>
              </div>
              <div class="resource-tools">
                <el-select v-model="typeFilter" placeholder="资料类型" clearable size="small" class="type-select" @change="loadResources(1)">
                  <el-option v-for="(text, key) in typeOptions" :key="key" :label="text" :value="key" />
                </el-select>
              </div>
            </div>

            <div v-loading="resourcesLoading" class="resource-list">
              <ResourceCard
                v-for="r in resources"
                :key="r.id"
                :resource="r"
                @open="(res) => $router.push(`/resource/${res.id}`)"
              />
              <EmptyState v-if="!resourcesLoading && !resources.length" icon="Document" text="该分类下暂无资料" padding="48" />
            </div>

            <div v-if="resourcesTotal > resourcesSize" class="pagination-wrap">
              <el-pagination
                v-model:current-page="resourcesPage"
                :page-size="resourcesSize"
                :total="resourcesTotal"
                layout="prev, pager, next"
                background
                small
                @current-change="loadResources()"
              />
            </div>
          </div>
        </el-col>
      </el-row>
    </template>

    <EmptyState v-else-if="!loading" icon="Warning" text="课程不存在或已被删除" padding="80" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import ChapterTree from '@/components/ChapterTree.vue'
import ResourceCard from '@/components/ResourceCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import { getCourseOverview, joinCourse, leaveCourse, getCourseResources } from '@/api/course'
import { useUserStore } from '@/stores/user'
import type { Chapter, Course, Resource } from '@/types'
import { courseCover, resourceTypeText, userAvatar } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const courseId = computed(() => Number(route.params.courseId))

const loading = ref(false)
const course = ref<Course | null>(null)
const chapters = ref<Chapter[]>([])
const overview = ref<{ progress?: { completedCount?: number; inProgressCount?: number; notStartedCount?: number; resourceCount?: number; progress?: number } }>({})

const activeChapterId = ref<number | undefined>(undefined)
const activeChapter = computed(() => chapters.value.find((c) => c.id === activeChapterId.value))

const resources = ref<Resource[]>([])
const resourcesLoading = ref(false)
const resourcesPage = ref(1)
const resourcesSize = ref(10)
const resourcesTotal = ref(0)
const typeFilter = ref('')
const typeOptions = resourceTypeText

const joining = ref(false)

const cover = computed(() => courseCover(course.value?.title || '', course.value?.coverUrl))
const creatorAvatar = computed(() => userAvatar(course.value?.creator?.nickname))
const canManage = computed(() => {
  const role = userStore.userInfo?.role
  if (role === 'ADMIN') return true
  if (role === 'TEACHER' && course.value?.creatorId === userStore.userInfo?.id) return true
  return false
})

function selectChapter(chapter: Chapter) {
  activeChapterId.value = chapter.id
  loadResources(1)
}

async function loadResources(p?: number) {
  if (p) resourcesPage.value = p
  resourcesLoading.value = true
  try {
    const params: Record<string, unknown> = {
      page: resourcesPage.value,
      size: resourcesSize.value,
      type: typeFilter.value || undefined
    }
    if (activeChapterId.value) params.chapterId = activeChapterId.value
    const res = await getCourseResources(courseId.value, params)
    resources.value = res.items || []
    resourcesTotal.value = res.total || 0
  } catch {
    resources.value = []
  } finally {
    resourcesLoading.value = false
  }
}

async function loadOverview() {
  loading.value = true
  try {
    const res = await getCourseOverview(courseId.value)
    course.value = res.course
    chapters.value = res.chapters || []
    overview.value = res
    activeChapterId.value = chapters.value[0]?.id
    await loadResources(1)
  } catch {
    course.value = null
  } finally {
    loading.value = false
  }
}

async function handleJoin() {
  joining.value = true
  try {
    await joinCourse(courseId.value)
    ElMessage.success('加入课程成功，开始学习吧！')
    await loadOverview()
  } catch {
    // 已提示
  } finally {
    joining.value = false
  }
}

function handleLeave() {
  ElMessageBox.confirm('确定退出该课程吗？学习进度将被保留。', '退出课程', { type: 'warning' })
    .then(async () => {
      await leaveCourse(courseId.value)
      ElMessage.success('已退出课程')
      await loadOverview()
    })
    .catch(() => {})
}

onMounted(loadOverview)
</script>

<style scoped lang="scss">
.course-detail {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

// ---------- 课程头部 ----------
.course-header {
  overflow: hidden;
  display: flex;

  @media (max-width: 800px) {
    flex-direction: column;
  }
}

.header-cover {
  width: 360px;
  min-height: 240px;
  position: relative;
  flex-shrink: 0;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }

  .cover-overlay {
    position: absolute;
    inset: 0;
    background: linear-gradient(90deg, transparent 60%, rgba(15, 23, 42, 0.25));
  }

  @media (max-width: 800px) {
    width: 100%;
    height: 180px;
  }
}

.header-info {
  flex: 1;
  padding: 28px 32px;
  display: flex;
  flex-direction: column;
}

.info-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.course-title {
  font-size: 26px;
  font-weight: 800;
  margin-bottom: 10px;
}

.course-desc {
  font-size: 14px;
  color: $color-text-2;
  line-height: 1.7;
  margin-bottom: 16px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.course-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  font-size: 13px;
  color: $color-text-3;
  margin-bottom: 22px;

  .meta-item {
    display: inline-flex;
    align-items: center;
    gap: 5px;

    &.creator {
      color: $color-text-2;
    }
  }
}

.header-actions {
  margin-top: auto;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

// ---------- 进度条 ----------
.progress-bar {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 18px 24px;

  .pb-label {
    font-size: 14px;
    font-weight: 700;
    white-space: nowrap;
  }

  .pb-progress {
    flex: 1;
  }

  .pb-stats {
    display: flex;
    gap: 14px;
    font-size: 12px;
    color: $color-text-3;
    white-space: nowrap;
  }

  @media (max-width: 700px) {
    flex-wrap: wrap;

    .pb-stats {
      width: 100%;
    }
  }
}

// ---------- 章节面板 ----------
.chapter-panel {
  padding: 20px;
  position: sticky;
  top: 20px;

  @media (max-width: 992px) {
    position: static;
  }
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;

  .panel-title {
    font-size: 17px;
    font-weight: 800;
  }

  .panel-count {
    background: $gradient-soft;
    color: $color-primary;
    font-weight: 700;
    font-size: 13px;
    padding: 2px 10px;
    border-radius: 999px;
  }
}

// ---------- 资料面板 ----------
.resources-panel {
  padding: 20px;
  min-height: 420px;
}

.panel-sub {
  font-size: 12px;
  color: $color-text-3;
  margin-top: 3px;
  display: block;
}

.resource-tools {
  display: flex;
  gap: 8px;

  .type-select {
    width: 140px;
  }
}

.resource-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 200px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

.mr6 {
  margin-right: 6px;
}
</style>
