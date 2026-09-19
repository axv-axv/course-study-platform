<template>
  <div class="course-card app-card app-card-hover" @click="$router.push(`/course/${course.id}`)">
    <div class="cover">
      <img :src="cover" :alt="course.title" loading="lazy" />
      <div class="cover-mask">
        <el-tag v-if="course.visibility === 'PRIVATE'" size="small" effect="dark" round class="vis-tag">
          <el-icon><Lock /></el-icon> 私密
        </el-tag>
        <span v-else class="vis-tag public">
          <el-icon><Unlock /></el-icon> 公开
        </span>
        <div class="go-detail">
          <el-icon><Right /></el-icon>
        </div>
      </div>
    </div>

    <div class="body">
      <h3 class="title text-ellipsis" :title="course.title">{{ course.title }}</h3>
      <p class="desc text-ellipsis-2">{{ course.description || '暂无课程简介' }}</p>

      <div class="meta">
        <span class="meta-item">
          <el-icon><User /></el-icon>
          {{ course.memberCount ?? 0 }} 人在学
        </span>
        <span class="meta-item">
          <el-icon><Document /></el-icon>
          {{ course.resourceCount ?? 0 }} 份资料
        </span>
      </div>

      <div class="footer">
        <span class="creator" v-if="course.creator?.nickname">
          <el-avatar :size="20" :src="creatorAvatar" class="creator-avatar">
            {{ (course.creator.nickname || '师').charAt(0) }}
          </el-avatar>
          {{ course.creator.nickname }}
        </span>
        <el-tag v-if="course.joined" size="small" type="success" effect="light" round>已加入</el-tag>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Course } from '@/types'
import { courseCover, userAvatar } from '@/utils/format'

const props = defineProps<{ course: Course }>()

const cover = computed(() => courseCover(props.course.title, props.course.coverUrl))
const creatorAvatar = computed(() => userAvatar(props.course.creator?.nickname))
</script>

<style scoped lang="scss">
.course-card {
  overflow: hidden;
  cursor: pointer;
  display: flex;
  flex-direction: column;
}

.cover {
  position: relative;
  height: 150px;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.4s ease;
  }

  .cover-mask {
    position: absolute;
    inset: 0;
    background: linear-gradient(180deg, rgba(0, 0, 0, 0.02) 40%, rgba(0, 0, 0, 0.35) 100%);
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    padding: 10px;
  }

  .vis-tag {
    border: none;

    &.public {
      background: rgba(255, 255, 255, 0.25);
      color: #fff;
      backdrop-filter: blur(4px);
      border-radius: 999px;
      padding: 2px 10px;
      font-size: 12px;
      display: inline-flex;
      align-items: center;
      gap: 4px;
    }
  }

  .go-detail {
    width: 30px;
    height: 30px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.9);
    color: $color-primary;
    display: flex;
    align-items: center;
    justify-content: center;
    opacity: 0;
    transform: translateX(-6px);
    transition: $transition-base;
  }
}

.course-card:hover .cover img {
  transform: scale(1.06);
}

.course-card:hover .go-detail {
  opacity: 1;
  transform: translateX(0);
}

.body {
  padding: 14px 16px 12px;
  display: flex;
  flex-direction: column;
  flex: 1;
}

.title {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 6px;
}

.desc {
  font-size: 13px;
  color: $color-text-3;
  min-height: 38px;
  margin-bottom: 10px;
}

.meta {
  display: flex;
  gap: 14px;
  font-size: 12px;
  color: $color-text-3;
  margin-bottom: 10px;

  .meta-item {
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }
}

.footer {
  margin-top: auto;
  display: flex;
  justify-content: space-between;
  align-items: center;

  .creator {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: $color-text-2;

    .creator-avatar {
      flex-shrink: 0;
    }
  }
}
</style>
