<template>
  <div class="page-container">
    <div class="page-head">
      <div>
        <h1 class="page-title">我的收藏</h1>
        <p class="page-subtitle">收藏的学习资料都在这里，方便随时复习</p>
      </div>
      <el-tag size="large" effect="light" type="warning" round>
        <el-icon class="mr6"><Star /></el-icon>{{ total }} 条收藏
      </el-tag>
    </div>

    <!-- 筛选 -->
    <div class="filter-bar app-card">
      <el-input v-model="keyword" placeholder="搜索收藏的资料" :prefix-icon="Search" clearable size="large" class="filter-keyword" @keyup.enter="load(1)" @clear="load(1)" />
      <el-select v-model="typeFilter" placeholder="资料类型" clearable size="large" class="filter-type" @change="load(1)">
        <el-option v-for="(text, key) in resourceTypeText" :key="key" :label="text" :value="key" />
      </el-select>
      <el-button class="btn-gradient" size="large" @click="load(1)">筛选</el-button>
    </div>

    <div v-loading="loading" class="favorite-grid">
      <div v-for="f in favorites" :key="f.id" class="favorite-card app-card" @click="$router.push(`/resource/${f.id}`)">
        <div class="fav-head">
          <div class="fav-type" :style="{ background: resourceTypeColor(f.resourceType || 'OTHER') + '14', color: resourceTypeColor(f.resourceType || 'OTHER') }">
            <el-icon :size="18"><component :is="resourceTypeIcon(f.resourceType || 'OTHER')" /></el-icon>
          </div>
          <el-icon class="fav-star" color="#f59e0b"><StarFilled /></el-icon>
        </div>
        <h3 class="fav-title text-ellipsis-2">{{ f.title }}</h3>
        <p class="fav-course text-ellipsis">{{ f.course?.title || f.courseTitle || '' }}</p>
        <div class="fav-footer">
          <span class="fav-type-text">{{ resourceTypeText[f.resourceType || 'OTHER'] }}</span>
          <el-button text size="small" type="danger" @click.stop="removeFavorite(f.id)">取消收藏</el-button>
        </div>
      </div>
    </div>

    <EmptyState v-if="!loading && !favorites.length" icon="Star" text="还没有收藏任何资料" sub-text="遇到好的学习资料，点击收藏按钮即可保存到这里" padding="60">
      <el-button class="btn-gradient" @click="$router.push('/courses')">去发现课程</el-button>
    </EmptyState>

    <div v-if="total > size" class="pagination-wrap">
      <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="prev, pager, next" background @current-change="load()" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import EmptyState from '@/components/EmptyState.vue'
import { getMyFavorites } from '@/api/user'
import { unfavoriteResource } from '@/api/resource'
import type { ResourceType } from '@/types'
import { resourceTypeColor, resourceTypeIcon, resourceTypeText } from '@/utils/format'

type FavoriteItem = {
  id: number
  title: string
  description?: string
  resourceType?: ResourceType
  course?: { id: number; title: string }
  courseTitle?: string
  progress?: number
}

const favorites = ref<FavoriteItem[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(12)
const total = ref(0)
const keyword = ref('')
const typeFilter = ref('')

async function load(p?: number) {
  if (p) page.value = p
  loading.value = true
  try {
    const res = await getMyFavorites({
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined,
      type: typeFilter.value || undefined
    })
    favorites.value = (res.items || []) as FavoriteItem[]
    total.value = res.total || 0
  } catch {
    favorites.value = []
  } finally {
    loading.value = false
  }
}

function removeFavorite(id: number) {
  ElMessageBox.confirm('取消收藏该资料？', '取消收藏', { type: 'warning' })
    .then(async () => {
      await unfavoriteResource(id)
      ElMessage.success('已取消收藏')
      await load()
    })
    .catch(() => {})
}

onMounted(() => load())
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

.filter-bar {
  display: flex;
  gap: 12px;
  padding: 16px;

  .filter-keyword {
    flex: 1;
    max-width: 420px;
  }

  .filter-type {
    width: 150px;
  }
}

.favorite-grid {
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

.favorite-card {
  padding: 16px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 10px;

  &:hover {
    transform: translateY(-4px);
    box-shadow: $shadow-hover;
  }
}

.fav-head {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .fav-type {
    width: 42px;
    height: 42px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

.fav-title {
  font-size: 15px;
  font-weight: 700;
  min-height: 44px;
}

.fav-course {
  font-size: 12px;
  color: $color-text-3;
}

.fav-footer {
  margin-top: auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid $color-border;
  padding-top: 10px;

  .fav-type-text {
    font-size: 12px;
    color: $color-text-3;
  }
}

.pagination-wrap {
  display: flex;
  justify-content: center;
}

.mr6 {
  margin-right: 6px;
}
</style>
