<template>
  <div class="page-container">
    <!-- 搜索 + 筛选栏 -->
    <div class="toolbar app-card">
      <el-input
        v-model="keyword"
        placeholder="搜索课程名称、简介…"
        :prefix-icon="Search"
        size="large"
        clearable
        class="search-input"
        @keyup.enter="load(1)"
        @clear="load(1)"
      />
      <el-select v-model="visibility" placeholder="可见性" clearable size="large" class="filter-select" @change="load(1)">
        <el-option label="公开" value="PUBLIC" />
        <el-option label="私密" value="PRIVATE" />
      </el-select>
      <el-button class="btn-gradient" size="large" @click="load(1)">搜索</el-button>
    </div>

    <!-- 课程网格 -->
    <div v-loading="loading" class="course-grid">
      <CourseCard v-for="c in courses" :key="c.id" :course="c" />
    </div>

    <EmptyState
      v-if="!loading && !courses.length"
      icon="Collection"
      text="没有找到相关课程"
      sub-text="换个关键词试试，或稍后再来"
      padding="60"
    />

    <div v-if="total > size" class="pagination-wrap">
      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next, jumper"
        background
        @current-change="load()"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import CourseCard from '@/components/CourseCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import { getCourses } from '@/api/course'
import type { Course } from '@/types'

const keyword = ref('')
const visibility = ref('')
const courses = ref<Course[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(12)
const total = ref(0)

async function load(p?: number) {
  if (p) page.value = p
  loading.value = true
  try {
    const res = await getCourses({
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined,
      visibility: visibility.value || undefined,
      status: 'ACTIVE'
    })
    courses.value = res.items || []
    total.value = res.total || 0
  } catch {
    ElMessage.warning('课程列表加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => load())
</script>

<style scoped lang="scss">
.page-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.toolbar {
  display: flex;
  gap: 12px;
  padding: 16px;

  .search-input {
    flex: 1;
    max-width: 520px;
  }

  .filter-select {
    width: 140px;
  }
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  min-height: 200px;

  @media (max-width: 1200px) {
    grid-template-columns: repeat(3, 1fr);
  }

  @media (max-width: 900px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  padding: 12px 0;
}
</style>
