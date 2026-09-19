<template>
  <div class="page-container ai-chat-page">
    <el-row :gutter="20" class="chat-layout">
      <!-- 左侧：课程 + 历史对话 -->
      <el-col :xs="24" :md="7" :lg="6">
        <div class="app-card side-panel">
          <div class="panel-head">
            <h3 class="panel-title">AI 课程问答</h3>
          </div>

          <!-- 课程选择 -->
          <el-select v-model="courseId" placeholder="选择课程开始提问" size="large" class="w-full course-select" @change="handleCourseChange" filterable>
            <el-option v-for="c in myCourses" :key="c.id" :label="c.title" :value="c.id">
              <div class="course-option">
                <span class="text-ellipsis">{{ c.title }}</span>
                <span class="option-joined" v-if="c.joined">已加入</span>
              </div>
            </el-option>
          </el-select>

          <!-- 检索范围 -->
          <div v-if="courseId" class="scope-box">
            <div class="scope-title">检索范围</div>
            <el-radio-group v-model="scope" class="scope-group">
              <el-radio value="course">整门课程</el-radio>
              <el-radio v-if="chapters.length" value="chapter">指定章节</el-radio>
              <el-radio v-if="resources.length" value="resource">指定资料</el-radio>
            </el-radio-group>
            <el-select v-if="scope === 'chapter'" v-model="chapterId" placeholder="选择章节" size="small" class="w-full scope-select">
              <el-option v-for="ch in chapters" :key="ch.id" :label="ch.title" :value="ch.id" />
            </el-select>
            <el-select v-if="scope === 'resource'" v-model="resourceId" placeholder="选择资料" size="small" class="w-full scope-select" filterable>
              <el-option v-for="r in resources" :key="r.id" :label="r.title" :value="r.id" />
            </el-select>
          </div>

          <el-divider />

          <!-- 历史对话 -->
          <div class="history-head">
            <span class="history-title">历史对话</span>
            <el-button text size="small" type="primary" @click="newConversation">新建</el-button>
          </div>
          <div class="history-list">
            <div
              v-for="conv in conversations"
              :key="conv.id"
              class="history-item"
              :class="{ active: conv.id === activeConversationId }"
              @click="openConversation(conv.id)"
            >
              <el-icon :size="14"><ChatDotRound /></el-icon>
              <span class="text-ellipsis history-name">{{ conv.title || '未命名对话' }}</span>
              <el-icon class="history-del" :size="13" @click.stop="removeConversation(conv.id)"><Close /></el-icon>
            </div>
            <EmptyState v-if="!conversations.length" icon="ChatDotRound" text="暂无对话" padding="24" />
          </div>
        </div>
      </el-col>

      <!-- 右侧：聊天窗口 -->
      <el-col :xs="24" :md="17" :lg="18">
        <div class="app-card chat-panel">
          <!-- 聊天头部 -->
          <div class="chat-head">
            <div class="chat-head-left">
              <div class="ai-logo">
                <el-icon :size="18"><MagicStick /></el-icon>
              </div>
              <div>
                <div class="chat-title">{{ currentCourseTitle || 'AI 课程助手' }}</div>
                <div class="chat-sub">基于课程知识库回答 · 自动附上参考来源</div>
              </div>
            </div>
            <el-tag v-if="!courseId" type="info" effect="plain" size="small">请先选择课程</el-tag>
          </div>

          <!-- 消息列表 -->
          <div ref="messageBoxRef" class="message-box">
            <div v-if="!messages.length" class="welcome">
              <div class="welcome-icon">
                <el-icon :size="40"><MagicStick /></el-icon>
              </div>
              <h3 class="welcome-title">向 AI 提问课程内容</h3>
              <p class="welcome-sub">选择一门课程后，AI 将只基于该课程的资料回答你的问题</p>
              <div class="suggestions">
                <button v-for="s in suggestions" :key="s" class="suggestion-chip" @click="quickAsk(s)">{{ s }}</button>
              </div>
            </div>

            <template v-else>
              <AiMessageItem
                v-for="(m, i) in messages"
                :key="i"
                :is-user="m.role === 'USER'"
                :content="m.content"
                :sources="m.sources"
                :time="m.createdAt"
              />
              <!-- 输入中动画 -->
              <div v-if="sending" class="typing-row">
                <div class="avatar ai-avatar"><el-icon :size="15"><MagicStick /></el-icon></div>
                <div class="typing-bubble">
                  <span class="dot"></span><span class="dot"></span><span class="dot"></span>
                </div>
              </div>
            </template>
          </div>

          <!-- 输入区 -->
          <div class="input-area">
            <el-input
              v-model="inputMsg"
              type="textarea"
              :rows="3"
              :disabled="!courseId"
              :placeholder="courseId ? '输入你的问题，例如：什么是梯度下降？' : '请先在左侧选择课程'"
              maxlength="1000"
              resize="none"
              @keydown.enter.exact.prevent="send"
            />
            <div class="input-actions">
              <span class="input-tip">Enter 发送 · Shift+Enter 换行</span>
              <el-button class="btn-gradient" :loading="sending" :disabled="!courseId" @click="send">
                <el-icon class="mr6"><Promotion /></el-icon>发送
              </el-button>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AiMessageItem from '@/components/AiMessageItem.vue'
import EmptyState from '@/components/EmptyState.vue'
import { chat, createConversation, deleteConversation, getConversationDetail, getConversations } from '@/api/ai'
import { getMyCourses } from '@/api/user'
import { getChapters, getCourseResources } from '@/api/course'
import type { AiConversation, AiMessage, Chapter, Course, Resource } from '@/types'

const route = useRoute()

const myCourses = ref<Course[]>([])
const courseId = ref<number | null>(null)
const currentCourseTitle = computed(() => myCourses.value.find((c) => c.id === courseId.value)?.title || '')

const scope = ref<'course' | 'chapter' | 'resource'>('course')
const chapters = ref<Chapter[]>([])
const resources = ref<Resource[]>([])
const chapterId = ref<number | undefined>(undefined)
const resourceId = ref<number | undefined>(undefined)

const messages = ref<AiMessage[]>([])
const inputMsg = ref('')
const sending = ref(false)
const messageBoxRef = ref<HTMLElement>()

const conversations = ref<AiConversation[]>([])
const activeConversationId = ref<number | null>(null)

const suggestions = ['总结一下这门课的重点', '解释一下最核心的概念', '帮我做一份复习提纲', '这门课有哪些易错点？']

function scrollToBottom() {
  nextTick(() => {
    if (messageBoxRef.value) messageBoxRef.value.scrollTop = messageBoxRef.value.scrollHeight
  })
}

async function loadMyCourses() {
  try {
    const res = await getMyCourses()
    myCourses.value = res.items || []
    // 路由带课程 ID 时自动选中
    const routeCourseId = Number(route.params.courseId)
    if (routeCourseId) {
      courseId.value = routeCourseId
      await handleCourseChange()
    }
  } catch {
    myCourses.value = []
  }
}

async function handleCourseChange() {
  messages.value = []
  activeConversationId.value = null
  scope.value = 'course'
  chapterId.value = undefined
  resourceId.value = undefined
  if (!courseId.value) return
  try {
    const [chaptersRes, resourcesRes] = await Promise.all([
      getChapters(courseId.value),
      getCourseResources(courseId.value, { page: 1, size: 50 })
    ])
    chapters.value = chaptersRes || []
    resources.value = resourcesRes.items || []
  } catch {
    chapters.value = []
    resources.value = []
  }
  await loadConversations()
}

async function loadConversations() {
  try {
    const res = await getConversations({ courseId: courseId.value || undefined, page: 1, size: 20 })
    conversations.value = res.items || []
  } catch {
    conversations.value = []
  }
}

async function newConversation() {
  if (!courseId.value) return
  try {
    const res = await createConversation({ courseId: courseId.value })
    activeConversationId.value = res.conversationId
    messages.value = []
    await loadConversations()
  } catch {
    // 已提示
  }
}

async function openConversation(id: number) {
  try {
    const conv = await getConversationDetail(id)
    activeConversationId.value = id
    messages.value = conv.messages || []
    scrollToBottom()
  } catch {
    // 已提示
  }
}

function removeConversation(id: number) {
  ElMessageBox.confirm('删除该对话及历史消息？', '删除对话', { type: 'warning' })
    .then(async () => {
      await deleteConversation(id)
      if (activeConversationId.value === id) {
        activeConversationId.value = null
        messages.value = []
      }
      await loadConversations()
      ElMessage.success('已删除')
    })
    .catch(() => {})
}

async function send() {
  const msg = inputMsg.value.trim()
  if (!msg || !courseId.value || sending.value) return
  inputMsg.value = ''

  messages.value.push({ role: 'USER', content: msg, createdAt: new Date().toISOString() })
  sending.value = true
  scrollToBottom()

  try {
    const payload: {
      conversationId?: number
      courseId: number
      chapterId?: number
      resourceId?: number
      message: string
    } = {
      courseId: courseId.value!,
      message: msg
    }
    if (activeConversationId.value) payload.conversationId = activeConversationId.value
    if (scope.value === 'chapter' && chapterId.value) payload.chapterId = chapterId.value
    if (scope.value === 'resource' && resourceId.value) payload.resourceId = resourceId.value

    const res = await chat(payload)
    messages.value.push({
      role: 'ASSISTANT',
      content: res.answer,
      sources: res.sources || [],
      createdAt: new Date().toISOString()
    })
    // 返回了 conversationId 则关联历史
    if (res.conversationId && activeConversationId.value !== res.conversationId) {
      activeConversationId.value = res.conversationId
      await loadConversations()
    }
  } catch {
    messages.value.push({
      role: 'ASSISTANT',
      content: '抱歉，AI 服务暂时不可用，请稍后重试。',
      sources: []
    })
  } finally {
    sending.value = false
    scrollToBottom()
  }
}

function quickAsk(s: string) {
  inputMsg.value = s
  send()
}

onMounted(loadMyCourses)
</script>

<style scoped lang="scss">
.ai-chat-page {
  height: calc(100vh - 120px);
  min-height: 560px;
}

.chat-layout {
  height: 100%;
}

// ---------- 左侧 ----------
.side-panel {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.panel-head {
  margin-bottom: 16px;

  .panel-title {
    font-size: 17px;
    font-weight: 800;
  }
}

.w-full {
  width: 100%;
}

.course-select {
  margin-bottom: 14px;
}

.course-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;

  .option-joined {
    font-size: 11px;
    color: #22c55e;
    flex-shrink: 0;
  }
}

.scope-box {
  background: var(--app-hover, rgba(79, 107, 255, 0.04));
  border-radius: 12px;
  padding: 12px;
  margin-bottom: 6px;

  .scope-title {
    font-size: 12px;
    font-weight: 700;
    color: $color-text-3;
    margin-bottom: 8px;
  }

  .scope-group {
    display: flex;
    flex-direction: column;
    gap: 4px;
    align-items: flex-start;
  }

  .scope-select {
    margin-top: 10px;
  }
}

.history-head {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .history-title {
    font-size: 13px;
    font-weight: 700;
    color: $color-text-3;
  }
}

.history-list {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 12px;
  border-radius: 10px;
  cursor: pointer;
  font-size: 13px;
  color: $color-text-2;
  transition: $transition-base;

  &:hover {
    background: var(--app-hover, rgba(79, 107, 255, 0.06));
  }

  &.active {
    background: $gradient-soft;
    color: $color-primary;
    font-weight: 600;
  }

  .history-name {
    flex: 1;
    min-width: 0;
  }

  .history-del {
    opacity: 0;
    transition: $transition-base;

    &:hover {
      color: $color-danger;
    }
  }

  &:hover .history-del {
    opacity: 1;
  }
}

// ---------- 右侧聊天 ----------
.chat-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid $color-border;

  .chat-head-left {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .ai-logo {
    width: 40px;
    height: 40px;
    border-radius: 12px;
    background: $gradient-primary;
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 4px 14px rgba(139, 92, 246, 0.35);
  }

  .chat-title {
    font-size: 15px;
    font-weight: 700;
  }

  .chat-sub {
    font-size: 12px;
    color: $color-text-3;
    margin-top: 2px;
  }
}

.message-box {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
}

// ---------- 欢迎区 ----------
.welcome {
  margin: auto;
  text-align: center;
  max-width: 460px;

  .welcome-icon {
    width: 84px;
    height: 84px;
    border-radius: 50%;
    background: $gradient-soft;
    color: $color-primary;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 18px;
  }

  .welcome-title {
    font-size: 20px;
    font-weight: 800;
    margin-bottom: 8px;
  }

  .welcome-sub {
    font-size: 13px;
    color: $color-text-3;
    margin-bottom: 22px;
  }
}

.suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;

  .suggestion-chip {
    border: 1px solid $color-border;
    background: var(--app-card-bg, #fff);
    color: $color-text-2;
    padding: 8px 16px;
    border-radius: 999px;
    font-size: 13px;
    cursor: pointer;
    transition: $transition-base;

    &:hover {
      border-color: $color-primary;
      color: $color-primary;
      background: $gradient-soft;
    }
  }
}

// ---------- 输入中 ----------
.typing-row {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.ai-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: $gradient-primary;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.typing-bubble {
  background: var(--app-card-bg, #fff);
  border: 1px solid $color-border;
  border-radius: 16px;
  border-top-left-radius: 4px;
  padding: 14px 18px;
  display: flex;
  gap: 5px;
  align-items: center;

  .dot {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: $color-primary;
    opacity: 0.4;
    animation: blink 1.2s infinite;

    &:nth-child(2) {
      animation-delay: 0.2s;
    }

    &:nth-child(3) {
      animation-delay: 0.4s;
    }
  }
}

@keyframes blink {
  0%,
  100% {
    opacity: 0.25;
    transform: translateY(0);
  }

  50% {
    opacity: 1;
    transform: translateY(-3px);
  }
}

// ---------- 输入区 ----------
.input-area {
  border-top: 1px solid $color-border;
  padding: 16px 24px;
  background: var(--app-card-bg, #fff);
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;

  .input-tip {
    font-size: 12px;
    color: $color-text-3;
  }
}

.mr6 {
  margin-right: 6px;
}
</style>
