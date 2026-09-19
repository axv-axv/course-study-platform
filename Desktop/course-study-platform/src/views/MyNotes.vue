<template>
  <div class="page-container">
    <div class="page-head">
      <div>
        <h1 class="page-title">我的笔记</h1>
        <p class="page-subtitle">记录学习过程中的思考、重点与疑问</p>
      </div>
      <el-tag size="large" effect="light" type="primary" round>
        <el-icon class="mr6"><Notebook /></el-icon>{{ total }} 条笔记
      </el-tag>
    </div>

    <!-- 筛选 -->
    <div class="filter-bar app-card">
      <el-input v-model="keyword" placeholder="搜索笔记内容" :prefix-icon="Search" clearable size="large" class="filter-keyword" @keyup.enter="load(1)" @clear="load(1)" />
      <el-button class="btn-gradient" size="large" @click="load(1)">搜索</el-button>
    </div>

    <!-- 笔记列表 -->
    <div v-loading="loading" class="note-grid">
      <div v-for="n in notes" :key="n.id" class="note-card app-card">
        <div class="note-content" @click="$router.push(`/resource/${n.resourceId}`)">{{ n.content }}</div>
        <div class="note-meta" @click="$router.push(`/resource/${n.resourceId}`)">
          <span v-if="n.resource" class="note-resource text-ellipsis">
            <el-icon :size="13"><Document /></el-icon>{{ n.resource.title }}
          </span>
          <span v-if="n.resource?.courseTitle" class="note-course text-ellipsis">{{ n.resource.courseTitle }}</span>
          <span class="note-time">{{ formatRelativeTime(n.updatedAt || n.createdAt) }}</span>
          <span v-if="n.position?.page" class="note-page">第 {{ n.position.page }} 页</span>
        </div>
        <div class="note-actions">
          <el-button text size="small" @click="openEdit(n)">编辑</el-button>
          <el-button text size="small" type="danger" @click="removeNote(n.id)">删除</el-button>
        </div>
      </div>
    </div>

    <EmptyState v-if="!loading && !notes.length" icon="Notebook" text="还没有学习笔记" sub-text="在资料详情页可以快速记录笔记" padding="60">
      <el-button class="btn-gradient" @click="$router.push('/courses')">去学习</el-button>
    </EmptyState>

    <div v-if="total > size" class="pagination-wrap">
      <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="prev, pager, next" background @current-change="load()" />
    </div>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editDialog" title="编辑笔记" width="560px">
      <el-input v-model="editContent" type="textarea" :rows="6" maxlength="2000" show-word-limit />
      <template #footer>
        <el-button @click="editDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="confirmEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import EmptyState from '@/components/EmptyState.vue'
import { getMyNotes } from '@/api/user'
import { deleteNote, updateNote } from '@/api/resource'
import type { Note } from '@/types'
import { formatRelativeTime } from '@/utils/format'

const notes = ref<Note[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(12)
const total = ref(0)
const keyword = ref('')

const editDialog = ref(false)
const editContent = ref('')
const editingId = ref<number | null>(null)
const saving = ref(false)

async function load(p?: number) {
  if (p) page.value = p
  loading.value = true
  try {
    const res = await getMyNotes({ page: page.value, size: size.value, keyword: keyword.value || undefined })
    notes.value = res.items || []
    total.value = res.total || 0
  } catch {
    notes.value = []
  } finally {
    loading.value = false
  }
}

function openEdit(n: Note) {
  editingId.value = n.id
  editContent.value = n.content
  editDialog.value = true
}

async function confirmEdit() {
  if (!editContent.value.trim() || editingId.value === null) return
  saving.value = true
  try {
    await updateNote(editingId.value, { content: editContent.value.trim() })
    ElMessage.success('笔记已更新')
    editDialog.value = false
    await load()
  } catch {
    // 已提示
  } finally {
    saving.value = false
  }
}

function removeNote(id: number) {
  ElMessageBox.confirm('确定删除这条笔记吗？', '删除笔记', { type: 'warning' })
    .then(async () => {
      await deleteNote(id)
      ElMessage.success('已删除')
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
}

.note-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  min-height: 200px;

  @media (max-width: 1100px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 700px) {
    grid-template-columns: 1fr;
  }
}

.note-card {
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  transition: $transition-base;

  &:hover {
    transform: translateY(-3px);
    box-shadow: $shadow-hover;
  }
}

.note-content {
  font-size: 14px;
  line-height: 1.75;
  color: $color-text-1;
  cursor: pointer;
  white-space: pre-wrap;
  word-break: break-word;
  display: -webkit-box;
  -webkit-line-clamp: 6;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 120px;
}

.note-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-top: 12px;
  border-top: 1px solid $color-border;
  cursor: pointer;
  font-size: 12px;
  color: $color-text-3;

  .note-resource {
    display: flex;
    align-items: center;
    gap: 5px;
    font-weight: 600;
    color: $color-primary;
  }

  .note-course {
    color: $color-text-3;
  }

  .note-page {
    color: #d97706;
    font-weight: 600;
  }
}

.note-actions {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
}

.mr6 {
  margin-right: 6px;
}
</style>
