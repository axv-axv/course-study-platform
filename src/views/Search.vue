<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <div class="search-hero app-card">
      <h2 class="search-title">
        <span class="gradient-text">搜索</span>课程 · 资料 · 标签
      </h2>
      <div class="search-bar">
        <el-input
          v-model="keyword"
          size="large"
          placeholder="输入关键词，如：线性回归、机器学习、梯度下降…"
          :prefix-icon="Search"
          clearable
          @keyup.enter="doSearch(1)"
          @clear="doSearch(1)"
        />
        <el-button class="btn-gradient" size="large" :loading="loading" @click="doSearch(1)">搜索</el-button>
      </div>
      <div class="hot-words" v-if="!keyword">
        <span class="hot-label">热门：</span>
        <a v-for="w in hotWords" :key="w" class="hot-word" @click="keyword = w; doSearch(1)">{{ w }}</a>
      </div>
    </div>

    <!-- 结果 -->
    <template v-if="searched">
      <!-- 课程结果 -->
      <section v-if="courseResults?.length" class="result-section">
        <div class="result-head">
          <h3 class="result-title">课程（{{ courseTotal }}）</h3>
          <el-button text type="primary" @click="expandType = expandType === 'course' ? '' : 'course'">
            {{ expandType === 'course' ? '收起' : '展开全部' }}
          </el-button>
        </div>
        <div v-if="expandType === 'course'" class="course-grid">
          <CourseCard v-for="c in courseResults" :key="c.id" :course="c" />
        </div>
        <div v-else class="compact-list">
          <div v-for="c in courseResults.slice(0, 3)" :key="c.id" class="compact-item" @click="$router.push(`/course/${c.id}`)">
            <img :src="courseCover(c.title, c.coverUrl)" class="ci-cover" alt="" />
            <div class="ci-info">
              <span class="ci-title">{{ c.title }}</span>
              <span class="ci-desc text-ellipsis">{{ c.description }}</span>
            </div>
            <el-tag size="small" type="primary" effect="light" round>{{ c.resourceCount ?? 0 }} 资料</el-tag>
          </div>
        </div>
      </section>

      <!-- 资料结果 -->
      <section v-if="resourceResults?.length" class="result-section">
        <div class="result-head">
          <h3 class="result-title">学习资料（{{ resourceTotal }}）</h3>
          <el-button text type="primary" @click="expandType = expandType === 'resource' ? '' : 'resource'">
            {{ expandType === 'resource' ? '收起' : '展开全部' }}
          </el-button>
        </div>
        <div v-if="expandType === 'resource'" class="resource-grid">
          <ResourceCard v-for="r in resourceResults" :key="r.id" :resource="r" @open="(res) => $router.push(`/resource/${res.id}`)" />
        </div>
        <div v-else class="resource-row-list">
          <div v-for="r in resourceResults.slice(0, 3)" :key="r.id" class="row-item" @click="$router.push(`/resource/${r.id}`)">
            <div class="ri-badge" :style="{ background: resourceTypeColor(r.resourceType) + '14', color: resourceTypeColor(r.resourceType) }">
              <el-icon :size="16"><component :is="resourceTypeIcon(r.resourceType)" /></el-icon>
            </div>
            <div class="ri-info">
              <span class="ri-title">{{ r.title }}</span>
              <span class="ri-meta">{{ r.course?.title }}<template v-if="r.chapter?.title"> · {{ r.chapter.title }}</template></span>
            </div>
            <span class="ri-type">{{ resourceTypeText[r.resourceType] }}</span>
          </div>
        </div>
      </section>

      <!-- 标签结果 -->
      <section v-if="tagResults?.length" class="result-section">
        <div class="result-head">
          <h3 class="result-title">标签（{{ tagTotal }}）</h3>
        </div>
        <div class="tag-cloud">
          <el-tag v-for="t in tagResults" :key="t.id" size="large" effect="plain" round class="tag-item" @click="keyword = t.name; doSearch(1)">
            {{ t.name }}
          </el-tag>
        </div>
      </section>

      <EmptyState v-if="!courseResults?.length && !resourceResults?.length && !tagResults?.length" icon="Search" text="没有找到相关内容" sub-text="换个关键词试试" padding="60" />
    </template>

    <EmptyState v-else icon="Search" text="输入关键词开始搜索" sub-text="可搜索课程、学习资料与标签" padding="80" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import CourseCard from '@/components/CourseCard.vue'
import ResourceCard from '@/components/ResourceCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import { unifiedSearch } from '@/api/search'
import type { Course, Resource, Tag } from '@/types'
import { courseCover, resourceTypeColor, resourceTypeIcon, resourceTypeText } from '@/utils/format'

const keyword = ref('')
const loading = ref(false)
const searched = ref(false)
const expandType = ref('')

const courseResults = ref<Course[]>([])
const resourceResults = ref<Resource[]>([])
const tagResults = ref<Tag[]>([])
const courseTotal = ref(0)
const resourceTotal = ref(0)
const tagTotal = ref(0)

const hotWords = ['机器学习', '线性回归', '深度学习', '数据结构', 'Python', '算法']

async function doSearch(p: number) {
  const kw = keyword.value.trim()
  if (!kw) return
  loading.value = true
  try {
    const res = await unifiedSearch({ keyword: kw, page: p, size: 10 })
    courseResults.value = res.courses?.items || []
    resourceResults.value = res.resources?.items || []
    tagResults.value = res.tags?.items || []
    courseTotal.value = res.courses?.total || 0
    resourceTotal.value = res.resources?.total || 0
    tagTotal.value = res.tags?.total || 0
    searched.value = true
  } catch {
    ElMessage.warning('搜索失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  // 支持从 URL query 带入关键词
  const q = new URLSearchParams(location.search).get('q')
  if (q) {
    keyword.value = q
    doSearch(1)
  }
})
</script>

<style scoped lang="scss">
.page-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

// ---------- 搜索区 ----------
.search-hero {
  padding: 40px;
  text-align: center;
  background:
    radial-gradient(500px 200px at 50% 0%, rgba(79, 107, 255, 0.08), transparent 70%),
    var(--app-card-bg, #fff);
}

.search-title {
  font-size: 26px;
  font-weight: 800;
  margin-bottom: 22px;
}

.search-bar {
  display: flex;
  gap: 12px;
  max-width: 640px;
  margin: 0 auto;
}

.hot-words {
  margin-top: 16px;
  font-size: 13px;
  color: $color-text-3;

  .hot-label {
    color: $color-text-3;
  }

  .hot-word {
    color: $color-primary;
    cursor: pointer;
    margin: 0 6px;

    &:hover {
      text-decoration: underline;
    }
  }
}

// ---------- 结果 ----------
.result-section {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.result-head {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .result-title {
    font-size: 17px;
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

.compact-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.compact-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 16px;
  border-radius: 14px;
  background: var(--app-card-bg, #fff);
  border: 1px solid $color-border;
  cursor: pointer;
  transition: $transition-base;

  &:hover {
    border-color: rgba(79, 107, 255, 0.4);
    transform: translateX(4px);
  }

  .ci-cover {
    width: 80px;
    height: 52px;
    border-radius: 8px;
    object-fit: cover;
    flex-shrink: 0;
  }

  .ci-info {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 4px;

    .ci-title {
      font-size: 14px;
      font-weight: 700;
    }

    .ci-desc {
      font-size: 12px;
      color: $color-text-3;
      max-width: 500px;
    }
  }
}

.resource-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;

  @media (max-width: 900px) {
    grid-template-columns: 1fr;
  }
}

.resource-row-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.row-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 16px;
  border-radius: 14px;
  background: var(--app-card-bg, #fff);
  border: 1px solid $color-border;
  cursor: pointer;
  transition: $transition-base;

  &:hover {
    border-color: rgba(79, 107, 255, 0.4);
    transform: translateX(4px);
  }

  .ri-badge {
    width: 40px;
    height: 40px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .ri-info {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 3px;

    .ri-title {
      font-size: 14px;
      font-weight: 600;
    }

    .ri-meta {
      font-size: 12px;
      color: $color-text-3;
    }
  }

  .ri-type {
    font-size: 12px;
    color: $color-text-3;
    flex-shrink: 0;
  }
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 10px 0;

  .tag-item {
    cursor: pointer;
    font-size: 14px;
    padding: 8px 16px;
    transition: $transition-base;

    &:hover {
      transform: scale(1.05);
      color: $color-primary;
      border-color: $color-primary;
    }
  }
}
</style>
