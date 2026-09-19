<template>
  <div class="page-container course-edit" v-loading="loading">
    <div class="page-head">
      <div>
        <h1 class="page-title">{{ isEdit ? '编辑课程' : '创建课程' }}</h1>
        <p class="page-subtitle">{{ isEdit ? '管理课程信息、章节与学习资料' : '填写课程信息，然后添加章节与资料' }}</p>
      </div>
      <el-button @click="$router.push('/teacher/courses')">
        <el-icon class="mr6"><Back /></el-icon>返回我的课程
      </el-button>
    </div>

    <el-tabs v-model="activeTab" class="edit-tabs">
      <!-- ============ Tab1 基本信息 ============ -->
      <el-tab-pane label="基本信息" name="basic">
        <div class="app-card form-card">
          <el-form ref="basicFormRef" :model="basicForm" :rules="basicRules" label-position="top" size="large">
            <el-row :gutter="24">
              <el-col :xs="24" :md="16">
                <el-form-item label="课程标题" prop="title">
                  <el-input v-model="basicForm.title" placeholder="例如：机器学习基础" maxlength="100" show-word-limit />
                </el-form-item>
                <el-form-item label="课程简介">
                  <el-input v-model="basicForm.description" type="textarea" :rows="5" maxlength="500" show-word-limit placeholder="介绍一下这门课程的内容与适合人群" />
                </el-form-item>
                <el-form-item label="可见性">
                  <el-radio-group v-model="basicForm.visibility">
                    <el-radio value="PUBLIC">
                      <div class="radio-block">
                        <span class="radio-title">公开课程</span>
                        <span class="radio-desc">所有用户可浏览并加入</span>
                      </div>
                    </el-radio>
                    <el-radio value="PRIVATE">
                      <div class="radio-block">
                        <span class="radio-title">私密课程</span>
                        <span class="radio-desc">仅成员可见</span>
                      </div>
                    </el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>

              <el-col :xs="24" :md="8">
                <el-form-item label="课程封面">
                  <div class="cover-uploader" @click="coverInput?.click()">
                    <img v-if="coverPreview" :src="coverPreview" class="cover-preview" alt="封面预览" />
                    <div v-else class="cover-placeholder">
                      <el-icon :size="30"><Picture /></el-icon>
                      <span>点击上传封面</span>
                    </div>
                    <input ref="coverInput" type="file" accept="image/*" class="hidden-input" @change="handleCoverChange" />
                  </div>
                  <div class="form-tip">建议尺寸 480 × 300，支持 JPG / PNG，不超过 5MB</div>
                </el-form-item>
              </el-col>
            </el-row>

            <div class="form-actions">
              <el-button class="btn-gradient" size="large" :loading="savingBasic" @click="saveBasic">
                {{ isEdit ? '保存修改' : '创建课程' }}
              </el-button>
              <el-button v-if="isEdit" size="large" type="danger" plain @click="removeCourse">删除课程</el-button>
            </div>
          </el-form>
        </div>
      </el-tab-pane>

      <!-- ============ Tab2 章节与资料 ============ -->
      <el-tab-pane v-if="isEdit" label="章节与资料" name="content">
        <el-row :gutter="20">
          <!-- 左：章节管理 -->
          <el-col :xs="24" :md="8">
            <div class="app-card manage-panel">
              <div class="panel-head">
                <h3 class="panel-title">章节管理</h3>
                <el-button size="small" type="primary" plain @click="openChapterDialog()">
                  <el-icon class="mr6"><Plus /></el-icon>新增章节
                </el-button>
              </div>

              <div class="chapter-list">
                <div v-for="(ch, idx) in chapters" :key="ch.id" class="chapter-item" :class="{ active: activeChapterId === ch.id }" @click="selectChapter(ch)">
                  <div class="chapter-order">{{ idx + 1 }}</div>
                  <div class="chapter-info">
                    <span class="chapter-title text-ellipsis">{{ ch.title }}</span>
                    <span class="chapter-count">{{ ch.resourceCount ?? 0 }} 份资料</span>
                  </div>
                  <div class="chapter-ops" @click.stop>
                    <el-button text size="small" :disabled="idx === 0" @click="moveChapter(idx, -1)"><el-icon><Top /></el-icon></el-button>
                    <el-button text size="small" :disabled="idx === chapters.length - 1" @click="moveChapter(idx, 1)"><el-icon><Bottom /></el-icon></el-button>
                    <el-button text size="small" @click="openChapterDialog(ch)"><el-icon><Edit /></el-icon></el-button>
                    <el-button text size="small" type="danger" @click="removeChapter(ch.id)"><el-icon><Delete /></el-icon></el-button>
                  </div>
                </div>
                <EmptyState v-if="!chapters.length" icon="FolderOpened" text="暂无章节" sub-text="点击右上角新增章节" padding="32" />
              </div>
            </div>
          </el-col>

          <!-- 右：资料管理 -->
          <el-col :xs="24" :md="16">
            <div class="app-card manage-panel">
              <div class="panel-head">
                <div>
                  <h3 class="panel-title">{{ activeChapter ? activeChapter.title : '全部资料' }}</h3>
                  <span class="panel-sub">{{ resourceTotal }} 份资料</span>
                </div>
                <el-button size="small" type="primary" @click="openResourceDialog()">
                  <el-icon class="mr6"><Plus /></el-icon>添加资料
                </el-button>
              </div>

              <div v-loading="resourcesLoading" class="resource-table">
                <div v-for="r in resources" :key="r.id" class="resource-row">
                  <div class="row-type" :style="{ background: typeColor(r.resourceType) + '14', color: typeColor(r.resourceType) }">
                    <el-icon :size="16"><component :is="typeIcon(r.resourceType)" /></el-icon>
                  </div>
                  <div class="row-info">
                    <span class="row-title text-ellipsis">{{ r.title }}</span>
                    <span class="row-meta">{{ resourceTypeText[r.resourceType] }}<template v-if="r.file?.name"> · {{ r.file.name }}</template></span>
                  </div>
                  <ai-index-badge v-if="r.aiIndexStatus" :status="r.aiIndexStatus" :error="r.aiIndexError" />
                  <div class="row-ops">
                    <el-button text size="small" :loading="reindexingId === r.id" @click="reindex(r.id)">
                      <el-icon><Refresh /></el-icon>
                    </el-button>
                    <el-button text size="small" @click="openResourceDialog(r)"><el-icon><Edit /></el-icon></el-button>
                    <el-button text size="small" type="danger" @click="removeResource(r.id)"><el-icon><Delete /></el-icon></el-button>
                  </div>
                </div>
                <EmptyState v-if="!resourcesLoading && !resources.length" icon="Document" text="暂无资料" sub-text="点击右上角添加学习资料" padding="40" />
              </div>

              <div v-if="resourceTotal > 20" class="pagination-wrap">
                <el-pagination v-model:current-page="resourcePage" :page-size="20" :total="resourceTotal" layout="prev, pager, next" small background @current-change="loadResources()" />
              </div>
            </div>
          </el-col>
        </el-row>
      </el-tab-pane>
    </el-tabs>

    <!-- 章节对话框 -->
    <el-dialog v-model="chapterDialog" :title="chapterForm.id ? '编辑章节' : '新增章节'" width="480px">
      <el-form :model="chapterForm" label-position="top">
        <el-form-item label="章节标题" required>
          <el-input v-model="chapterForm.title" placeholder="例如：第一章 机器学习简介" maxlength="100" />
        </el-form-item>
        <el-form-item label="章节描述">
          <el-input v-model="chapterForm.description" type="textarea" :rows="3" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="chapterDialog = false">取消</el-button>
        <el-button type="primary" :loading="savingChapter" @click="saveChapter">保存</el-button>
      </template>
    </el-dialog>

    <!-- 资料对话框 -->
    <el-dialog v-model="resourceDialog" :title="resourceForm.id ? '编辑资料' : '添加资料'" width="560px">
      <el-form :model="resourceForm" label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="资料标题" required>
              <el-input v-model="resourceForm.title" placeholder="例如：线性回归课件" maxlength="120" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="资料类型" required>
              <el-select v-model="resourceForm.resourceType" class="w-full" @change="resourceForm.fileId = undefined; resourceForm.externalUrl = ''">
                <el-option v-for="(text, key) in resourceTypeText" :key="key" :label="text" :value="key" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="所属章节">
          <el-select v-model="resourceForm.chapterId" class="w-full" placeholder="不选则属于课程级资料">
            <el-option v-for="ch in chapters" :key="ch.id" :label="ch.title" :value="ch.id" />
          </el-select>
        </el-form-item>

        <!-- 文件类型：上传 -->
        <el-form-item v-if="isFileType(resourceForm.resourceType) || resourceForm.resourceType === 'VIDEO'" :label="resourceForm.resourceType === 'VIDEO' ? '上传视频（与外部链接二选一）' : '上传文件'" :required="resourceForm.resourceType !== 'VIDEO'">
          <div class="file-uploader">
            <input ref="fileInput" type="file" class="hidden-input" @change="handleFileChange" />
            <div v-if="resourceForm.fileId" class="file-picked">
              <el-icon color="#22c55e" :size="20"><CircleCheck /></el-icon>
              <span class="file-name text-ellipsis">{{ pickedFileName || '文件已就绪' }}</span>
              <el-button text size="small" type="danger" @click="resourceForm.fileId = undefined; pickedFileName = ''">移除</el-button>
            </div>
            <el-button v-else type="primary" plain @click="fileInput?.click()" :loading="uploading">
              <el-icon class="mr6"><Upload /></el-icon>{{ uploading ? '上传中…' : '选择文件' }}
            </el-button>
          </div>
        </el-form-item>

        <!-- 链接类型：外部链接 -->
        <el-form-item v-if="resourceForm.resourceType === 'LINK' || resourceForm.resourceType === 'VIDEO'" label="外部链接">
          <el-input v-model="resourceForm.externalUrl" placeholder="https://example.com/..." clearable />
        </el-form-item>

        <el-form-item label="资料描述">
          <el-input v-model="resourceForm.description" type="textarea" :rows="2" maxlength="300" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resourceDialog = false">取消</el-button>
        <el-button type="primary" :loading="savingResource" @click="saveResource">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import EmptyState from '@/components/EmptyState.vue'
import AiIndexBadge from '@/components/AiIndexBadge.vue'
import { createChapter, createCourse, deleteChapter, deleteCourse, getChapters, getCourseDetail, getCourseResources, reorderChapters, updateChapter, updateCourse } from '@/api/course'
import { createResource, deleteResource, reindexResource, updateResource } from '@/api/resource'
import { uploadFile } from '@/api/file'
import type { Chapter, Course, Resource, ResourceType } from '@/types'
import { resourceTypeColor, resourceTypeIcon, resourceTypeText } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const courseId = computed(() => (route.params.courseId ? Number(route.params.courseId) : null))
const isEdit = computed(() => !!courseId.value)

const loading = ref(false)
const activeTab = ref('basic')

// ---------- 基本信息 ----------
const basicFormRef = ref<FormInstance>()
const basicForm = reactive({
  title: '',
  description: '',
  visibility: 'PUBLIC' as 'PUBLIC' | 'PRIVATE',
  coverUrl: ''
})
const basicRules: FormRules = {
  title: [
    { required: true, message: '请输入课程标题', trigger: 'blur' },
    { max: 100, message: '标题最长 100 字', trigger: 'blur' }
  ]
}
const savingBasic = ref(false)
const coverPreview = ref('')
const coverInput = ref<HTMLInputElement>()

// ---------- 章节 ----------
const chapters = ref<Chapter[]>([])
const activeChapterId = ref<number | undefined>(undefined)
const activeChapter = computed(() => chapters.value.find((c) => c.id === activeChapterId.value))
const chapterDialog = ref(false)
const savingChapter = ref(false)
const chapterForm = reactive<{ id: number | null; title: string; description: string }>({ id: null, title: '', description: '' })

// ---------- 资料 ----------
const resources = ref<Resource[]>([])
const resourcesLoading = ref(false)
const resourceTotal = ref(0)
const resourcePage = ref(1)
const resourceDialog = ref(false)
const savingResource = ref(false)
const uploading = ref(false)
const pickedFileName = ref('')
const fileInput = ref<HTMLInputElement>()
const reindexingId = ref<number | null>(null)
const resourceForm = reactive<{
  id: number | null
  title: string
  description: string
  resourceType: ResourceType
  chapterId?: number
  fileId?: number
  externalUrl: string
}>({ id: null, title: '', description: '', resourceType: 'PDF', chapterId: undefined, fileId: undefined, externalUrl: '' })

const typeColor = (t: ResourceType) => resourceTypeColor(t)
const typeIcon = (t: ResourceType) => resourceTypeIcon(t)

function isFileType(t: ResourceType) {
  return ['PDF', 'DOC', 'PPT', 'MARKDOWN', 'TXT', 'CODE', 'OTHER'].includes(t)
}

// ---------- 初始化 ----------
async function init() {
  if (!isEdit.value) return
  loading.value = true
  try {
    const course = await getCourseDetail(courseId.value!)
    basicForm.title = course.title || ''
    basicForm.description = course.description || ''
    basicForm.visibility = course.visibility || 'PUBLIC'
    basicForm.coverUrl = course.coverUrl || ''
    coverPreview.value = course.coverUrl || ''
    await loadChapters()
    await loadResources(1)
  } catch {
    ElMessage.error('课程加载失败')
  } finally {
    loading.value = false
  }
}

// ---------- 基本信息保存 ----------
async function saveBasic() {
  if (!basicFormRef.value) return
  const valid = await basicFormRef.value.validate().catch(() => false)
  if (!valid) return
  savingBasic.value = true
  try {
    if (isEdit.value) {
      await updateCourse(courseId.value!, { title: basicForm.title, description: basicForm.description, visibility: basicForm.visibility, coverUrl: basicForm.coverUrl || undefined })
      ElMessage.success('课程信息已更新')
    } else {
      const res = await createCourse({ title: basicForm.title, description: basicForm.description, visibility: basicForm.visibility })
      ElMessage.success('课程创建成功，开始添加章节与资料吧！')
      router.replace(`/teacher/course/${res.id}/edit`)
    }
  } catch {
    // 已提示
  } finally {
    savingBasic.value = false
  }
}

async function handleCoverChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    ElMessage.warning('仅支持 JPG / PNG / WEBP 图片')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('封面不能超过 5MB')
    return
  }
  try {
    const res = await uploadFile(file)
    basicForm.coverUrl = res.url || `/files/${res.fileId}`
    coverPreview.value = URL.createObjectURL(file)
    ElMessage.success('封面上传成功，保存课程信息后生效')
  } catch {
    ElMessage.error('封面上传失败')
  } finally {
    input.value = ''
  }
}

function removeCourse() {
  ElMessageBox.confirm('删除课程将同时删除其章节与资料，且不可恢复。确定删除吗？', '删除课程', { type: 'error', confirmButtonText: '确认删除' })
    .then(async () => {
      await deleteCourse(courseId.value!)
      ElMessage.success('课程已删除')
      router.push('/teacher/courses')
    })
    .catch(() => {})
}

// ---------- 章节操作 ----------
async function loadChapters() {
  if (!courseId.value) return
  try {
    chapters.value = (await getChapters(courseId.value)) || []
  } catch {
    chapters.value = []
  }
}

function selectChapter(ch: Chapter) {
  activeChapterId.value = ch.id
  loadResources(1)
}

function openChapterDialog(ch?: Chapter) {
  chapterForm.id = ch?.id || null
  chapterForm.title = ch?.title || ''
  chapterForm.description = ch?.description || ''
  chapterDialog.value = true
}

async function saveChapter() {
  if (!chapterForm.title.trim()) {
    ElMessage.warning('请输入章节标题')
    return
  }
  savingChapter.value = true
  try {
    if (chapterForm.id) {
      await updateChapter(chapterForm.id, { title: chapterForm.title.trim(), description: chapterForm.description.trim() || undefined })
      ElMessage.success('章节已更新')
    } else {
      await createChapter(courseId.value!, { title: chapterForm.title.trim(), description: chapterForm.description.trim() || undefined })
      ElMessage.success('章节已创建')
    }
    chapterDialog.value = false
    await loadChapters()
  } catch {
    // 已提示
  } finally {
    savingChapter.value = false
  }
}

async function moveChapter(idx: number, dir: -1 | 1) {
  const target = idx + dir
  if (target < 0 || target >= chapters.value.length) return
  const next = [...chapters.value]
  ;[next[idx], next[target]] = [next[target], next[idx]]
  chapters.value = next
  try {
    await reorderChapters(courseId.value!, chapters.value.map((c) => c.id))
    ElMessage.success('顺序已调整')
  } catch {
    await loadChapters()
  }
}

function removeChapter(chapterId: number) {
  ElMessageBox.confirm('删除章节将同时删除该章节下的资料，确定删除吗？', '删除章节', { type: 'warning' })
    .then(async () => {
      await deleteChapter(chapterId)
      ElMessage.success('章节已删除')
      if (activeChapterId.value === chapterId) activeChapterId.value = undefined
      await loadChapters()
    })
    .catch(() => {})
}

// ---------- 资料操作 ----------
async function loadResources(p?: number) {
  if (p) resourcePage.value = p
  if (!courseId.value) return
  resourcesLoading.value = true
  try {
    const params: Record<string, unknown> = { page: resourcePage.value, size: 20 }
    if (activeChapterId.value) params.chapterId = activeChapterId.value
    const res = await getCourseResources(courseId.value, params)
    resources.value = res.items || []
    resourceTotal.value = res.total || 0
  } catch {
    resources.value = []
  } finally {
    resourcesLoading.value = false
  }
}

function openResourceDialog(r?: Resource) {
  resourceForm.id = r?.id || null
  resourceForm.title = r?.title || ''
  resourceForm.description = r?.description || ''
  resourceForm.resourceType = r?.resourceType || 'PDF'
  resourceForm.chapterId = r?.chapterId || activeChapterId.value
  resourceForm.fileId = r?.file?.fileId || r?.fileId
  resourceForm.externalUrl = r?.externalUrl || ''
  pickedFileName.value = r?.file?.name || r?.file?.fileName || ''
  resourceDialog.value = true
}

async function handleFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  uploading.value = true
  try {
    const res = await uploadFile(file)
    resourceForm.fileId = res.fileId
    pickedFileName.value = res.fileName || file.name
    ElMessage.success('文件上传成功')
  } catch {
    ElMessage.error('文件上传失败')
  } finally {
    uploading.value = false
    input.value = ''
  }
}

async function saveResource() {
  if (!resourceForm.title.trim()) {
    ElMessage.warning('请输入资料标题')
    return
  }
  const needFile = isFileType(resourceForm.resourceType) && !resourceForm.fileId
  const needLink = resourceForm.resourceType === 'LINK' && !resourceForm.externalUrl
    || resourceForm.resourceType === 'VIDEO' && !resourceForm.externalUrl && !resourceForm.fileId
  if (needFile) {
    ElMessage.warning('请先上传文件')
    return
  }
  if (needLink) {
    ElMessage.warning('请填写外部链接')
    return
  }

  savingResource.value = true
  const payload = {
    title: resourceForm.title.trim(),
    description: resourceForm.description.trim() || undefined,
    resourceType: resourceForm.resourceType,
    chapterId: resourceForm.chapterId,
    fileId: resourceForm.fileId,
    externalUrl: resourceForm.externalUrl || undefined
  }
  try {
    if (resourceForm.id) {
      await updateResource(resourceForm.id, payload)
      ElMessage.success('资料已更新')
    } else {
      await createResource({ ...payload, courseId: courseId.value! })
      ElMessage.success('资料已添加')
    }
    resourceDialog.value = false
    await loadResources()
  } catch {
    // 已提示
  } finally {
    savingResource.value = false
  }
}

function removeResource(resourceId: number) {
  ElMessageBox.confirm('删除该资料？其 AI 索引也会被移除。', '删除资料', { type: 'warning' })
    .then(async () => {
      await deleteResource(resourceId)
      ElMessage.success('资料已删除')
      await loadResources()
    })
    .catch(() => {})
}

async function reindex(resourceId: number) {
  reindexingId.value = resourceId
  try {
    const res = await reindexResource(resourceId)
    ElMessage.success('已提交索引任务')
    const target = resources.value.find((r) => r.id === resourceId)
    if (target) target.aiIndexStatus = res.status
  } catch {
    // 已提示
  } finally {
    reindexingId.value = null
  }
}

onMounted(init)
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

.edit-tabs {
  :deep(.el-tabs__item) {
    font-size: 15px;
    font-weight: 600;
  }
}

// ---------- 基本信息 ----------
.form-card {
  padding: 28px;
  max-width: 980px;
}

.radio-block {
  display: flex;
  flex-direction: column;

  .radio-title {
    font-size: 14px;
    font-weight: 600;
  }

  .radio-desc {
    font-size: 12px;
    color: $color-text-3;
  }
}

.cover-uploader {
  width: 100%;
  height: 160px;
  border: 2px dashed $color-border;
  border-radius: 14px;
  cursor: pointer;
  overflow: hidden;
  transition: $transition-base;
  display: flex;
  align-items: center;
  justify-content: center;

  &:hover {
    border-color: $color-primary;
    background: $gradient-soft;
  }

  .cover-preview {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .cover-placeholder {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    color: $color-text-3;
    font-size: 13px;
  }
}

.hidden-input {
  display: none;
}

.form-tip {
  font-size: 12px;
  color: $color-text-3;
  margin-top: 6px;
}

.form-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 12px;
}

// ---------- 管理面板 ----------
.manage-panel {
  padding: 20px;
  min-height: 420px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;

  .panel-title {
    font-size: 17px;
    font-weight: 800;
  }

  .panel-sub {
    font-size: 12px;
    color: $color-text-3;
    margin-top: 3px;
    display: block;
  }
}

// ---------- 章节列表 ----------
.chapter-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.chapter-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid $color-border;
  border-radius: 12px;
  cursor: pointer;
  transition: $transition-base;

  &:hover {
    border-color: rgba(79, 107, 255, 0.4);
  }

  &.active {
    border-color: $color-primary;
    background: $gradient-soft;
  }
}

.chapter-order {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  background: var(--app-hover, rgba(79, 107, 255, 0.08));
  color: $color-primary;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.chapter-info {
  flex: 1;
  min-width: 0;

  .chapter-title {
    display: block;
    font-size: 13px;
    font-weight: 600;
  }

  .chapter-count {
    font-size: 11px;
    color: $color-text-3;
  }
}

.chapter-ops {
  display: flex;
  opacity: 0.5;
  transition: $transition-base;

  &:hover {
    opacity: 1;
  }
}

// ---------- 资料列表 ----------
.resource-table {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 200px;
}

.resource-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid $color-border;
  border-radius: 12px;
  transition: $transition-base;

  &:hover {
    border-color: rgba(79, 107, 255, 0.4);
  }
}

.row-type {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.row-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;

  .row-title {
    font-size: 13px;
    font-weight: 600;
  }

  .row-meta {
    font-size: 11px;
    color: $color-text-3;
  }
}

.row-ops {
  display: flex;
  flex-shrink: 0;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 14px;
}

.file-uploader {
  width: 100%;

  .file-picked {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 14px;
    border-radius: 10px;
    background: rgba(34, 197, 94, 0.08);
    border: 1px solid rgba(34, 197, 94, 0.3);

    .file-name {
      flex: 1;
      font-size: 13px;
      color: $color-text-2;
    }
  }
}

.w-full {
  width: 100%;
}

.mr6 {
  margin-right: 6px;
}
</style>
