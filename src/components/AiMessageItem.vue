<template>
  <div class="msg-row" :class="isUser ? 'user' : 'ai'">
    <template v-if="!isUser">
      <div class="avatar ai-avatar">
        <el-icon :size="17"><MagicStick /></el-icon>
      </div>
    </template>

    <div class="msg-body">
      <div class="bubble" :class="isUser ? 'bubble-user' : 'bubble-ai'">
        <!-- AI 回答支持简单 markdown 渲染 -->
        <div v-if="!isUser" class="md-content" v-html="renderedContent"></div>
        <div v-else class="plain-content">{{ content }}</div>

        <!-- AI 回答来源引用 -->
        <div v-if="!isUser && sources && sources.length" class="sources">
          <div class="sources-title">
            <el-icon :size="13"><Collection /></el-icon>
            参考资料（{{ sources.length }}）
          </div>
          <div v-for="(s, i) in sources" :key="i" class="source-item" @click="$router.push(`/resource/${s.resourceId}`)">
            <div class="source-head">
              <el-tag size="small" type="warning" effect="light" round>来源 {{ i + 1 }}</el-tag>
              <span class="source-name text-ellipsis">{{ s.resourceTitle }}</span>
              <span v-if="s.page" class="source-page">第 {{ s.page }} 页</span>
              <el-icon class="source-go"><Right /></el-icon>
            </div>
            <p v-if="s.snippet" class="source-snippet">“{{ s.snippet }}”</p>
            <p v-if="s.chapterTitle" class="source-chapter">{{ s.chapterTitle }}</p>
          </div>
        </div>
      </div>
      <span v-if="time" class="msg-time">{{ time }}</span>
    </div>

    <template v-if="isUser">
      <div class="avatar user-avatar">{{ initial }}</div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import type { AiSource } from '@/types'
import { formatRelativeTime } from '@/utils/format'

const props = defineProps<{
  isUser: boolean
  content: string
  sources?: AiSource[]
  time?: string
}>()

const userStore = useUserStore()
const initial = computed(() => (userStore.userInfo?.nickname || userStore.userInfo?.username || '我').charAt(0).toUpperCase())

/** 极简 markdown 渲染：加粗 / 斜体 / 行内代码 / 换行 / 链接 / 标题 */
const renderedContent = computed(() => {
  let html = escapeHtml(props.content || '')
  html = html.replace(/```([\s\S]*?)```/g, (_m, code: string) => `<pre class="code-block">${code.trim()}</pre>`)
  html = html.replace(/`([^`\n]+)`/g, '<code class="inline-code">$1</code>')
  html = html.replace(/\*\*([^*\n]+)\*\*/g, '<strong>$1</strong>')
  html = html.replace(/\*([^*\n]+)\*/g, '<em>$1</em>')
  html = html.replace(/^###\s+(.+)$/gm, '<h4 class="md-h">$1</h4>')
  html = html.replace(/^##\s+(.+)$/gm, '<h4 class="md-h">$1</h4>')
  html = html.replace(/^#\s+(.+)$/gm, '<h4 class="md-h">$1</h4>')
  html = html.replace(/^\s*[-*]\s+(.+)$/gm, '<li class="md-li">$1</li>')
  html = html.replace(/(https?:\/\/[^\s<]+)/g, '<a href="$1" target="_blank" rel="noopener">$1</a>')
  return html.replace(/\n/g, '<br/>')
})

function escapeHtml(s: string): string {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

const time = computed(() => (props.time ? formatRelativeTime(props.time) : ''))
</script>

<style scoped lang="scss">
.msg-row {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;

  &.user {
    justify-content: flex-end;
  }

  &.ai {
    justify-content: flex-start;
  }
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-weight: 700;
  font-size: 14px;
}

.ai-avatar {
  background: $gradient-primary;
  color: #fff;
  box-shadow: 0 3px 10px rgba(79, 107, 255, 0.35);
}

.user-avatar {
  background: linear-gradient(135deg, #0ea5e9, #6366f1);
  color: #fff;
}

.msg-body {
  max-width: 76%;
  min-width: 0;
}

.bubble {
  padding: 13px 16px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.75;
  word-break: break-word;
}

.bubble-user {
  background: $gradient-primary;
  color: #fff;
  border-top-right-radius: 4px;
  box-shadow: 0 4px 14px rgba(79, 107, 255, 0.28);
}

.bubble-ai {
  background: var(--app-card-bg, #fff);
  border: 1px solid $color-border;
  border-top-left-radius: 4px;
  box-shadow: $shadow-card;
}

.plain-content {
  white-space: pre-wrap;
}

.msg-time {
  display: block;
  font-size: 11px;
  color: $color-text-3;
  margin-top: 6px;
  padding-left: 4px;
}

// ---------- 来源引用 ----------
.sources {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed $color-border;

  .sources-title {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    font-weight: 700;
    color: $color-text-2;
    margin-bottom: 8px;
  }
}

.source-item {
  background: rgba(245, 158, 11, 0.06);
  border: 1px solid rgba(245, 158, 11, 0.18);
  border-radius: 10px;
  padding: 8px 12px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: $transition-base;

  &:hover {
    border-color: $color-primary;
    background: rgba(79, 107, 255, 0.05);
  }
}

.source-head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;

  .source-name {
    font-weight: 600;
    color: $color-text-1;
    flex: 1;
    min-width: 0;
  }

  .source-page {
    font-size: 12px;
    color: #d97706;
    font-weight: 600;
    flex-shrink: 0;
  }

  .source-go {
    color: $color-text-3;
    font-size: 12px;
    flex-shrink: 0;
  }
}

.source-snippet {
  font-size: 12px;
  color: $color-text-2;
  margin-top: 5px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.source-chapter {
  font-size: 11px;
  color: $color-text-3;
  margin-top: 3px;
}

// ---------- markdown 渲染样式 ----------
.md-content {
  :deep(.code-block) {
    background: #0f172a;
    color: #e2e8f0;
    padding: 10px 12px;
    border-radius: 8px;
    font-size: 12px;
    line-height: 1.6;
    overflow-x: auto;
    margin: 6px 0;
    font-family: 'JetBrains Mono', Consolas, monospace;
  }

  :deep(.inline-code) {
    background: rgba(79, 107, 255, 0.1);
    color: $color-primary;
    padding: 1px 6px;
    border-radius: 4px;
    font-size: 12px;
    font-family: Consolas, monospace;
  }

  :deep(.md-h) {
    font-size: 15px;
    margin: 8px 0 4px;
  }

  :deep(.md-li) {
    margin-left: 18px;
    list-style: disc;
  }

  :deep(a) {
    color: $color-primary;
  }
}
</style>
