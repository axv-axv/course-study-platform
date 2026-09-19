<template>
  <div class="chapter-tree">
    <div
      v-for="chapter in chapters"
      :key="chapter.id"
      class="chapter-node"
      :class="{ active: chapter.id === activeId }"
      @click="$emit('select', chapter)"
    >
      <div class="chapter-head">
        <div class="chapter-icon">
          <el-icon :size="15"><FolderOpened /></el-icon>
        </div>
        <div class="chapter-info">
          <span class="chapter-title text-ellipsis">{{ chapter.title }}</span>
          <span class="chapter-count">{{ chapter.resourceCount ?? 0 }} 份资料</span>
        </div>
        <el-icon class="chapter-arrow" :size="13"><ArrowRight /></el-icon>
      </div>
    </div>

    <div v-if="!chapters.length" class="no-chapter">
      <el-empty description="暂无章节" :image-size="60" />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Chapter } from '@/types'

defineProps<{
  chapters: Chapter[]
  activeId?: number
}>()

defineEmits<{ select: [chapter: Chapter] }>()
</script>

<style scoped lang="scss">
.chapter-tree {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.chapter-node {
  border: 1px solid $color-border;
  border-radius: 12px;
  padding: 12px 14px;
  cursor: pointer;
  transition: $transition-base;
  background: var(--app-card-bg, #fff);

  &:hover {
    border-color: rgba(79, 107, 255, 0.4);
    transform: translateX(3px);
  }

  &.active {
    border-color: $color-primary;
    background: $gradient-soft;

    .chapter-title {
      color: $color-primary;
      font-weight: 700;
    }

    .chapter-arrow {
      color: $color-primary;
      transform: rotate(90deg);
    }
  }
}

.chapter-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.chapter-icon {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: rgba(79, 107, 255, 0.1);
  color: $color-primary;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.chapter-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.chapter-title {
  font-size: 14px;
  font-weight: 600;
}

.chapter-count {
  font-size: 12px;
  color: $color-text-3;
}

.chapter-arrow {
  color: $color-text-3;
  transition: transform 0.2s ease;
  flex-shrink: 0;
}

.no-chapter {
  padding: 12px 0;
}
</style>
