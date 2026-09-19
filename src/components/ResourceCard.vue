<template>
  <div class="resource-card app-card" @click="$emit('open', resource)">
    <div class="type-badge" :style="{ background: typeColor + '14', color: typeColor }">
      <el-icon :size="18"><component :is="iconName" /></el-icon>
    </div>

    <div class="info">
      <div class="title-row">
        <h4 class="title text-ellipsis" :title="resource.title">{{ resource.title }}</h4>
        <span class="favorite" v-if="resource.favorite"><el-icon color="#f59e0b"><StarFilled /></el-icon></span>
      </div>
      <p class="desc text-ellipsis-2">{{ resource.description || '暂无简介' }}</p>
      <div class="meta">
        <span class="meta-item">
          <el-icon><View /></el-icon>{{ resource.viewCount ?? 0 }}
        </span>
        <span class="meta-item">
          <el-icon><Download /></el-icon>{{ resource.downloadCount ?? 0 }}
        </span>
        <span v-if="resource.course?.title" class="meta-item course-name text-ellipsis">{{ resource.course.title }}</span>
        <span v-if="resource.aiIndexStatus" class="meta-item">
          <ai-index-badge :status="resource.aiIndexStatus" />
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Resource } from '@/types'
import { resourceTypeColor, resourceTypeIcon } from '@/utils/format'
import AiIndexBadge from './AiIndexBadge.vue'

const props = defineProps<{ resource: Resource }>()
defineEmits<{ open: [resource: Resource] }>()

const typeColor = computed(() => resourceTypeColor(props.resource.resourceType))
const iconName = computed(() => resourceTypeIcon(props.resource.resourceType))
</script>

<style scoped lang="scss">
.resource-card {
  display: flex;
  gap: 12px;
  padding: 14px;
  cursor: pointer;
  transition: $transition-base;

  &:hover {
    transform: translateY(-3px);
    box-shadow: $shadow-hover;
  }
}

.type-badge {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.info {
  flex: 1;
  min-width: 0;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 6px;

  .title {
    font-size: 14px;
    font-weight: 600;
    flex: 1;
    min-width: 0;
  }
}

.desc {
  font-size: 12px;
  color: $color-text-3;
  margin: 4px 0 8px;
  min-height: 34px;
}

.meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: $color-text-3;

  .meta-item {
    display: inline-flex;
    align-items: center;
    gap: 3px;
  }

  .course-name {
    max-width: 120px;
  }
}
</style>
