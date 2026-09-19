<template>
  <div class="page-container resource-detail" v-loading="loading">
    <template v-if="resource">
      <!-- 资料头部 -->
      <section class="rd-header app-card">
        <div class="type-badge" :style="{ background: typeColor + '14', color: typeColor }">
          <el-icon :size="26"><component :is="typeIcon" /></el-icon>
        </div>
        <div class="rd-info">
          <div class="rd-title-row">
            <h1 class="rd-title">{{ resource.title }}</h1>
            <ai-index-badge v-if="resource.aiIndexStatus" :status="resource.aiIndexStatus" :error="resource.aiIndexError" />
          </div>
          <p v-if="resource.description" class="rd-desc">{{ resource.description }}</p>
          <div class="rd-meta">
            <span v-if="resource.course" class="meta-link" @click="$router.push(`/course/${resource.course.id}`)">
              <el-icon><Collection /></el-icon>{{ resource.course.title }}
            </span>
            <span v-if="resource.chapter" class="meta-item">
              <el-icon><FolderOpened /></el-icon>{{ resource.chapter.title }}
            </span>
            <span class="meta-item">
              <el-icon><View /></el-icon>{{ resource.viewCount ?? 0 }} 次浏览
            </span>
            <span class="meta-item">
              <el-icon><Download /></el-icon>{{ resource.downloadCount ?? 0 }} 次下载
            </span>
            <span v-if="resource.file" class="meta-item">
              <el-icon><Files /></el-icon>{{ resource.file.name || resource.file.fileName }} · {{ formatSize(resource.file.size) }}
            </span>
          </div>
          <div v-if="resource.tags?.length" class="rd-tags">
            <el-tag v-for="t in resource.tags" :key="t.id" size="small" effect="plain" round>{{ t.name }}</el-tag>
          </div>
        </div>
        <div class="rd-actions">
          <el-button :type="resource.favorite ? 'warning' : 'primary'" :plain="!resource.favorite" size="large" @click="toggleFavorite">
            <el-icon class="mr6"><StarFilled v-if="resource.favorite" /><Star v-else /></el-icon>
            {{ resource.favorite ? '已收藏' : '收藏' }}
          </el-button>
          <el-button size="large" plain type="primary" :loading="downloading" @click="handleDownload">
            <el-icon class="mr6"><Download /></el-icon>下载
          </el-button>
          <el-button v-if="canManage" size="large" plain type="warning" @click="$router.push(`/teacher/course/${resource.courseId}/edit`)">
            <el-icon class="mr6"><Edit /></el-icon>管理资料
          </el-button>
        </div>
      </section>

      <el-row :gutter="20">
        <!-- 左：预览区 -->
        <el-col :xs="24" :lg="16">
          <div class="app-card preview-panel">
            <div class="panel-head">
              <h3 class="panel-title">内容预览</h3>
              <el-tag size="small" effect="light" round>{{ resourceTypeText[resource.resourceType] }}</el-tag>
            </div>

            <!-- PDF / 文档类 iframe 预览 -->
            <div v-if="isPreviewable && previewUrl" class="preview-iframe-wrap">
              <iframe :src="previewUrl" class="preview-iframe" frameborder="0" />
            </div>

            <!-- 视频 -->
            <div v-else-if="resource.resourceType === 'VIDEO' && videoUrl" class="video-wrap">
              <video :src="videoUrl" controls class="video-player" />
            </div>

            <!-- 外部链接 -->
            <div v-else-if="resource.resourceType === 'LINK' && resource.externalUrl" class="link-wrap">
              <el-empty description="该资料为外部链接">
                <el-button type="primary" @click="openExternal">
                  <el-icon class="mr6"><Link /></el-icon>打开外部链接
                </el-button>
              </el-empty>
              <p class="external-url">{{ resource.externalUrl }}</p>
            </div>

            <!-- 不可预览：提示下载 -->
            <div v-else class="no-preview">
              <el-empty :description="`该类型资料暂不支持在线预览`">
                <el-button type="primary" @click="handleDownload">
                  <el-icon class="mr6"><Download /></el-icon>下载文件
                </el-button>
              </el-empty>
            </div>
          </div>
        </el-col>

        <!-- 右：学习侧栏 -->
        <el-col :xs="24" :lg="8">
          <!-- 学习进度 -->
          <div class="app-card side-panel">
            <div class="panel-head">
              <h3 class="panel-title">学习进度</h3>
              <el-tag :type="statusTagType" size="small" effect="light" round>{{ studyStatusText[progress.status] }}</el-tag>
            </div>

            <div class="progress-ring-wrap">
              <ProgressRing :value="progress.progress" :size="110" :stroke-width="10" :font-size="20" :display-text="`${progress.progress}%`" />
              <div class="progress-tip">
                <el-button v-if="progress.status !== 'COMPLETED'" class="btn-gradient" size="small" @click="markCompleted">标记已完成</el-button>
                <el-button v-else size="small" plain @click="markInProgress">重新学习</el-button>
              </div>
            </div>
            <el-slider v-model="progress.progress" :step="5" :show-tooltip="true" @change="saveProgress" />
          </div>

          <!-- AI 索引（教师） -->
          <div v-if="canManage" class="app-card side-panel">
            <div class="panel-head">
              <h3 class="panel-title">AI 知识库索引</h3>
              <ai-index-badge :status="resource.aiIndexStatus || 'NOT_INDEXED'" :error="resource.aiIndexError" />
            </div>
            <p class="ai-tip">将本资料转换为可被 AI 检索的知识库片段（PDF / Word / PPT / Markdown / TXT）</p>
            <el-button
              v-if="resource.aiIndexStatus !== 'PROCESSING'"
              class="w-full"
              type="primary"
              plain
              :loading="reindexing"
              @click="handleReindex"
            >
              <el-icon class="mr6"><Refresh /></el-icon>{{ resource.aiIndexStatus === 'INDEXED' ? '重新建立索引' : '建立索引' }}
            </el-button>
            <el-button v-else class="w-full" disabled>
              <el-icon class="mr6 spin"><Loading /></el-icon>正在建立索引…
            </el-button>
          </div>

          <!-- 学习笔记 -->
          <div class="app-card side-panel notes-panel">
            <div class="panel-head">
              <h3 class="panel-title">学习笔记</h3>
              <span class="notes-count">{{ notes.length }} 条</span>
            </div>

            <div class="note-editor">
              <el-input
                v-model="noteDraft"
                type="textarea"
                :rows="3"
                placeholder="记录你的想法、重点与疑问…"
                maxlength="2000"
                show-word-limit
              />
              <el-button class="btn-gradient note-submit" size="small" :loading="savingNote" @click="saveNote">保存笔记</el-button>
            </div>

            <div class="note-list">
              <div v-for="n in notes" :key="n.id" class="note-item">
                <div class="note-content">{{ n.content }}</div>
                <div class="note-footer">
                  <span class="note-time">{{ formatRelativeTime(n.updatedAt || n.createdAt) }}</span>
                  <span v-if="n.position?.page" class="note-pos">第 {{ n.position.page }} 页</span>
                  <div class="note-ops">
                    <el-button text size="small" @click="startEditNote(n)">编辑</el-button>
                    <el-button text size="small" type="danger" @click="removeNote(n.id)">删除</el-button>
                  </div>
                </div>
              </div>
              <EmptyState v-if="!notes.length" icon="Notebook" text="还没有笔记" sub-text="记录第一条学习笔记吧" padding="28" />
            </div>
          </div>
        </el-col>
      </el-row>
    </template>

    <EmptyState v-else-if="!loading" icon="Warning" text="资料不存在或已被删除" padding="80" />

    <!-- 编辑笔记弹窗 -->
    <el-dialog v-model="editDialog" title="编辑笔记" width="520px">
      <el-input v-model="editContent" type="textarea" :rows="5" maxlength="2000" show-word-limit />
      <template #footer>
        <el-button @click="editDialog = false">取消</el-button>
        <el-button type="primary" :loading="savingNote" @click="confirmEditNote">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import ProgressRing from '@/components/ProgressRing.vue'
import AiIndexBadge from '@/components/AiIndexBadge.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  createNote,
  deleteNote,
  favoriteResource,
  getFavoriteStatus,
  getResourceDetail,
  getResourceNotes,
  getResourceProgress,
  reindexResource,
  unfavoriteResource,
  updateNote,
  updateProgress
} from '@/api/resource'
import { fileDownloadUrl, filePreviewUrl } from '@/api/file'
import { useUserStore } from '@/stores/user'
import type { Note, Resource, StudyStatus } from '@/types'
import { formatRelativeTime, formatSize, resourceTypeColor, resourceTypeIcon, resourceTypeText, studyStatusText } from '@/utils/format'
import { tokenStore } from '@/api/request'
import service from '@/api/request'

const route = useRoute()
const userStore = useUserStore()

const resourceId = computed(() => Number(route.params.resourceId))

const loading = ref(false)
const resource = ref<Resource | null>(null)
const downloading = ref(false)

// 收藏
const favoriteLoading = ref(false)

// 进度
const progress = ref<{ status: StudyStatus; progress: number }>({ status: 'NOT_STARTED', progress: 0 })

// 笔记
const notes = ref<Note[]>([])
const noteDraft = ref('')
const savingNote = ref(false)
const editDialog = ref(false)
const editContent = ref('')
const editingNoteId = ref<number | null>(null)

// 索引
const reindexing = ref(false)

const typeColor = computed(() => resourceTypeColor(resource.value?.resourceType || 'OTHER'))
const typeIcon = computed(() => resourceTypeIcon(resource.value?.resourceType || 'OTHER'))

const canManage = computed(() => {
  const role = userStore.userInfo?.role
  if (role === 'ADMIN') return true
  if (role === 'TEACHER' && resource.value?.creatorId === userStore.userInfo?.id) return true
  return false
})

const isPreviewable = computed(() => ['PDF', 'DOC', 'PPT', 'TXT', 'MARKDOWN'].includes(resource.value?.resourceType || ''))

/** 预览地址：iframe 无法携带 header，若后端支持 cookie 认证可直接访问；否则可传 token query 参数 */
const previewUrl = computed(() => {
  if (!resource.value?.file?.fileId && !resource.value?.fileId) return ''
  const id = resource.value.file?.fileId ?? resource.value.fileId
  const base = filePreviewUrl(id!)
  return `${base}${base.includes('?') ? '&' : '?'}token=${encodeURIComponent(tokenStore.accessToken)}`
})

const videoUrl = computed(() => {
  if (resource.value?.resourceType !== 'VIDEO') return ''
  if (resource.value.externalUrl) return resource.value.externalUrl
  if (resource.value.file?.fileId) {
    const base = filePreviewUrl(resource.value.file.fileId)
    return `${base}${base.includes('?') ? '&' : '?'}token=${encodeURIComponent(tokenStore.accessToken)}`
  }
  return ''
})

const statusTagType = computed(() => {
  const map: Record<StudyStatus, 'info' | 'primary' | 'success'> = {
    NOT_STARTED: 'info',
    IN_PROGRESS: 'primary',
    COMPLETED: 'success'
  }
  return map[progress.value.status]
})

async function loadDetail() {
  loading.value = true
  try {
    const res = await getResourceDetail(resourceId.value)
    resource.value = res
    await Promise.all([loadProgress(), loadNotes(), loadFavorite()])
  } catch {
    resource.value = null
  } finally {
    loading.value = false
  }
}

async function loadProgress() {
  try {
    const p = await getResourceProgress(resourceId.value)
    progress.value = { status: p.status, progress: p.progress || 0 }
  } catch {
    // 默认值
  }
}

async function loadNotes() {
  try {
    notes.value = (await getResourceNotes(resourceId.value)) || []
  } catch {
    notes.value = []
  }
}

async function loadFavorite() {
  try {
    const f = await getFavoriteStatus(resourceId.value)
    if (resource.value) resource.value.favorite = f.favorite
  } catch {
    // 忽略
  }
}

async function saveProgress() {
  try {
    const status: StudyStatus = progress.value.progress >= 100 ? 'COMPLETED' : progress.value.progress > 0 ? 'IN_PROGRESS' : 'NOT_STARTED'
    await updateProgress(resourceId.value, { status, progress: progress.value.progress })
    progress.value.status = status
    if (status === 'COMPLETED') ElMessage.success('恭喜完成本资料的学习！')
  } catch {
    // 拦截器已提示
  }
}

async function markCompleted() {
  progress.value.progress = 100
  progress.value.status = 'COMPLETED'
  await saveProgress()
}

async function markInProgress() {
  progress.value.progress = 0
  progress.value.status = 'IN_PROGRESS'
  await saveProgress()
}

async function toggleFavorite() {
  if (!resource.value) return
  favoriteLoading.value = true
  try {
    if (resource.value.favorite) {
      await unfavoriteResource(resourceId.value)
      resource.value.favorite = false
      ElMessage.success('已取消收藏')
    } else {
      await favoriteResource(resourceId.value)
      resource.value.favorite = true
      ElMessage.success('收藏成功')
    }
  } catch {
    // 已提示
  } finally {
    favoriteLoading.value = false
  }
}

async function handleDownload() {
  if (!resource.value?.file?.fileId && !resource.value?.fileId) {
    ElMessage.warning('该资料没有关联文件')
    return
  }
  downloading.value = true
  try {
    const id = resource.value.file?.fileId ?? resource.value.fileId!
    // 用 axios 以 Bearer token 拉取二进制流下载
    const resp = await service.get<Blob>(`/files/${id}/download`, { responseType: 'blob' })
    const blob = resp as unknown as Blob
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = resource.value.file?.name || resource.value.file?.fileName || `resource-${id}`
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
    ElMessage.success('开始下载')
  } catch {
    // 后端可能直接返回文件流或需要浏览器直接访问，给兜底
    const id = resource.value.file?.fileId ?? resource.value.fileId!
    window.open(fileDownloadUrl(id), '_blank')
  } finally {
    downloading.value = false
  }
}

function openExternal() {
  if (resource.value?.externalUrl) window.open(resource.value.externalUrl, '_blank', 'noopener')
}

// ---------- 笔记操作 ----------
async function saveNote() {
  if (!noteDraft.value.trim()) {
    ElMessage.warning('笔记内容不能为空')
    return
  }
  savingNote.value = true
  try {
    await createNote(resourceId.value, { content: noteDraft.value.trim() })
    ElMessage.success('笔记已保存')
    noteDraft.value = ''
    await loadNotes()
  } catch {
    // 已提示
  } finally {
    savingNote.value = false
  }
}

function startEditNote(n: Note) {
  editingNoteId.value = n.id
  editContent.value = n.content
  editDialog.value = true
}

async function confirmEditNote() {
  if (!editContent.value.trim() || editingNoteId.value === null) return
  savingNote.value = true
  try {
    await updateNote(editingNoteId.value, { content: editContent.value.trim() })
    ElMessage.success('笔记已更新')
    editDialog.value = false
    await loadNotes()
  } catch {
    // 已提示
  } finally {
    savingNote.value = false
  }
}

function removeNote(noteId: number) {
  ElMessageBox.confirm('确定删除这条笔记吗？', '删除笔记', { type: 'warning' })
    .then(async () => {
      await deleteNote(noteId)
      ElMessage.success('已删除')
      await loadNotes()
    })
    .catch(() => {})
}

// ---------- 索引操作 ----------
async function handleReindex() {
  reindexing.value = true
  try {
    const res = await reindexResource(resourceId.value)
    ElMessage.success('已提交索引任务，正在处理中…')
    if (resource.value) resource.value.aiIndexStatus = res.status
  } catch {
    // 已提示
  } finally {
    reindexing.value = false
  }
}

onMounted(loadDetail)
</script>

<style scoped lang="scss">
.resource-detail {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

// ---------- 头部 ----------
.rd-header {
  display: flex;
  gap: 20px;
  padding: 24px;
  align-items: flex-start;

  @media (max-width: 800px) {
    flex-direction: column;
  }
}

.type-badge {
  width: 60px;
  height: 60px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.rd-info {
  flex: 1;
  min-width: 0;
}

.rd-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.rd-title {
  font-size: 22px;
  font-weight: 800;
}

.rd-desc {
  font-size: 13px;
  color: $color-text-2;
  margin-top: 8px;
  line-height: 1.6;
}

.rd-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 12px;
  font-size: 13px;
  color: $color-text-3;

  .meta-item,
  .meta-link {
    display: inline-flex;
    align-items: center;
    gap: 5px;
  }

  .meta-link {
    color: $color-primary;
    cursor: pointer;
    font-weight: 600;

    &:hover {
      text-decoration: underline;
    }
  }
}

.rd-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 12px;
}

.rd-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex-shrink: 0;

  @media (max-width: 800px) {
    flex-direction: row;
    flex-wrap: wrap;
  }
}

// ---------- 预览 ----------
.preview-panel {
  padding: 20px;
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
}

.preview-iframe-wrap {
  height: 640px;
  border: 1px solid $color-border;
  border-radius: 12px;
  overflow: hidden;
  background: #f8fafc;

  .preview-iframe {
    width: 100%;
    height: 100%;
  }
}

.video-wrap {
  border-radius: 12px;
  overflow: hidden;
  background: #000;

  .video-player {
    width: 100%;
    max-height: 480px;
    display: block;
  }
}

.link-wrap {
  padding: 30px 0;

  .external-url {
    text-align: center;
    font-size: 12px;
    color: $color-text-3;
    margin-top: 10px;
    word-break: break-all;
    padding: 0 30px;
  }
}

.no-preview {
  padding: 20px 0;
}

// ---------- 侧栏 ----------
.side-panel {
  padding: 20px;
  margin-bottom: 20px;

  &:last-child {
    margin-bottom: 0;
  }
}

.progress-ring-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  padding: 8px 0 16px;

  .progress-tip {
    display: flex;
    gap: 8px;
  }
}

.ai-tip {
  font-size: 12px;
  color: $color-text-3;
  line-height: 1.6;
  margin-bottom: 12px;
}

.w-full {
  width: 100%;
}

// ---------- 笔记 ----------
.notes-count {
  font-size: 13px;
  color: $color-text-3;
  font-weight: 600;
}

.note-editor {
  display: flex;
  flex-direction: column;
  gap: 8px;

  .note-submit {
    align-self: flex-end;
  }
}

.note-list {
  margin-top: 18px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.note-item {
  border: 1px solid $color-border;
  border-radius: 12px;
  padding: 12px 14px;
  transition: $transition-base;

  &:hover {
    border-color: rgba(79, 107, 255, 0.35);
  }
}

.note-content {
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  color: $color-text-1;
}

.note-footer {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
  font-size: 12px;
  color: $color-text-3;

  .note-pos {
    color: $color-primary;
    font-weight: 600;
  }

  .note-ops {
    margin-left: auto;
    display: flex;
    gap: 2px;
  }
}

.spin {
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  to {
    transform: rotate(360deg);
  }
}

.mr6 {
  margin-right: 6px;
}
</style>
