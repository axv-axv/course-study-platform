<template>
  <div class="page-container">
    <div class="page-head">
      <div>
        <h1 class="page-title">我的课程管理</h1>
        <p class="page-subtitle">创建和管理你的课程、章节与学习资料</p>
      </div>
      <el-button class="btn-gradient" size="large" @click="$router.push('/teacher/course/create')">
        <el-icon class="mr6"><CirclePlus /></el-icon>创建课程
      </el-button>
    </div>

    <div v-loading="loading" class="course-grid">
      <div v-for="c in courses" :key="c.id" class="manage-card app-card">
        <div class="manage-cover">
          <img :src="cover(c)" :alt="c.title" />
          <div class="cover-mask">
            <el-tag v-if="c.visibility === 'PRIVATE'" size="small" effect="dark" round>私密</el-tag>
            <el-tag v-else size="small" effect="dark" round type="success">公开</el-tag>
          </div>
        </div>
        <div class="manage-body">
          <h3 class="manage-title text-ellipsis">{{ c.title }}</h3>
          <div class="manage-meta">
            <span><el-icon><User /></el-icon>{{ c.memberCount ?? 0 }} 人</span>
            <span><el-icon><Document /></el-icon>{{ c.resourceCount ?? 0 }} 资料</span>
            <span><el-icon><FolderOpened /></el-icon>{{ c.chapterCount ?? 0 }} 章节</span>
          </div>
          <div class="manage-actions">
            <el-button size="small" type="primary" plain @click="$router.push(`/course/${c.id}`)">查看</el-button>
            <el-button size="small" @click="$router.push(`/teacher/course/${c.id}/edit`)">管理</el-button>
            <el-button size="small" @click="$router.push(`/teacher/course/${c.id}/members`)">成员</el-button>
            <el-button size="small" type="danger" plain @click="removeCourse(c.id)">删除</el-button>
          </div>
        </div>
      </div>
    </div>

    <EmptyState v-if="!loading && !courses.length" icon="School" text="还没有创建课程" sub-text="创建你的第一门课程，开始知识分享吧" padding="60">
      <el-button class="btn-gradient" @click="$router.push('/teacher/course/create')">创建课程</el-button>
    </EmptyState>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import EmptyState from '@/components/EmptyState.vue'
import { getMyCreatedCourses } from '@/api/user'
import { deleteCourse } from '@/api/course'
import { courseCover } from '@/utils/format'
import type { Course } from '@/types'

const router = useRouter()
const courses = ref<Course[]>([])
const loading = ref(false)

function cover(c: Course) {
  return courseCover(c.title, c.coverUrl)
}

async function load() {
  loading.value = true
  try {
    const res = await getMyCreatedCourses()
    courses.value = res.items || []
  } catch {
    courses.value = []
  } finally {
    loading.value = false
  }
}

function removeCourse(id: number) {
  ElMessageBox.confirm('删除课程将同时删除其章节与资料，且不可恢复。确定删除吗？', '删除课程', { type: 'error', confirmButtonText: '确认删除' })
    .then(async () => {
      await deleteCourse(id)
      ElMessage.success('课程已删除')
      await load()
    })
    .catch(() => {})
}

onMounted(load)
</script>

<style scoped lang="scss">
.page-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  min-height: 200px;

  @media (max-width: 1100px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 700px) {
    grid-template-columns: 1fr;
  }
}

.manage-card {
  overflow: hidden;
  display: flex;
  flex-direction: column;
  transition: $transition-base;

  &:hover {
    transform: translateY(-4px);
    box-shadow: $shadow-hover;
  }
}

.manage-cover {
  height: 130px;
  position: relative;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .cover-mask {
    position: absolute;
    top: 10px;
    left: 10px;
  }
}

.manage-body {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
}

.manage-title {
  font-size: 16px;
  font-weight: 700;
}

.manage-meta {
  display: flex;
  gap: 14px;
  font-size: 12px;
  color: $color-text-3;

  span {
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }
}

.manage-actions {
  margin-top: auto;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.mr6 {
  margin-right: 6px;
}
</style>
