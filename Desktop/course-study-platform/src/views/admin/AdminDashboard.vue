<template>
  <div class="page-container admin-page">
    <!-- 统计 -->
    <div class="stat-grid">
      <div v-for="s in statCards" :key="s.label" class="stat-card app-card">
        <div class="stat-icon" :style="{ background: s.bg, color: s.color }">
          <el-icon :size="22"><component :is="s.icon" /></el-icon>
        </div>
        <div>
          <div class="stat-value">{{ s.value }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </div>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="admin-tabs">
      <!-- 用户管理 -->
      <el-tab-pane label="用户管理" name="users">
        <div class="app-card admin-panel">
          <div class="toolbar">
            <el-input v-model="userKeyword" placeholder="搜索用户名 / 昵称 / 邮箱" :prefix-icon="Search" clearable size="large" class="toolbar-input" @keyup.enter="loadUsers(1)" @clear="loadUsers(1)" />
            <el-select v-model="userRoleFilter" placeholder="角色" clearable size="large" class="role-select" @change="loadUsers(1)">
              <el-option label="学生" value="STUDENT" />
              <el-option label="教师" value="TEACHER" />
              <el-option label="管理员" value="ADMIN" />
            </el-select>
            <el-button class="btn-gradient" size="large" @click="loadUsers(1)">搜索</el-button>
          </div>

          <el-table :data="users" v-loading="usersLoading" style="width: 100%">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column label="用户" min-width="200">
              <template #default="{ row }">
                <div class="user-cell">
                  <el-avatar :size="34" :src="userAvatar(row.nickname || row.username, row.avatarUrl)">{{ (row.nickname || row.username || 'U').charAt(0) }}</el-avatar>
                  <div>
                    <div class="user-name">{{ row.nickname || row.username }}</div>
                    <div class="user-sub">@{{ row.username }}<template v-if="row.email"> · {{ row.email }}</template></div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="角色" width="140">
              <template #default="{ row }">
                <el-select :model-value="row.role" size="small" class="role-edit" @change="(v: string) => changeRole(row, v)">
                  <el-option label="学生" value="STUDENT" />
                  <el-option label="教师" value="TEACHER" />
                  <el-option label="管理员" value="ADMIN" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="注册时间" width="160">
              <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="100" align="right">
              <template #default="{ row }">
                <el-button text size="small" type="danger" :disabled="row.role === 'ADMIN'" @click="disableUser(row)">停用</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div v-if="userTotal > userSize" class="pagination-wrap">
            <el-pagination v-model:current-page="userPage" :page-size="userSize" :total="userTotal" layout="prev, pager, next" background @current-change="loadUsers()" />
          </div>
        </div>
      </el-tab-pane>

      <!-- 课程管理 -->
      <el-tab-pane label="课程管理" name="courses">
        <div class="app-card admin-panel">
          <div class="toolbar">
            <el-input v-model="courseKeyword" placeholder="搜索课程标题" :prefix-icon="Search" clearable size="large" class="toolbar-input" @keyup.enter="loadCourses(1)" @clear="loadCourses(1)" />
            <el-button class="btn-gradient" size="large" @click="loadCourses(1)">搜索</el-button>
          </div>

          <el-table :data="adminCourses" v-loading="coursesLoading" style="width: 100%">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column label="课程" min-width="240">
              <template #default="{ row }">
                <div class="course-cell">
                  <img :src="courseCover(row.title, row.coverUrl)" class="cell-cover" alt="" />
                  <span class="cell-title">{{ row.title }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="memberCount" label="成员" width="80" />
            <el-table-column prop="resourceCount" label="资料" width="80" />
            <el-table-column label="可见性" width="90">
              <template #default="{ row }">
                <el-tag :type="row.visibility === 'PUBLIC' ? 'success' : 'warning'" size="small" effect="light" round>
                  {{ row.visibility === 'PUBLIC' ? '公开' : '私密' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="130" align="right">
              <template #default="{ row }">
                <el-button text size="small" @click="$router.push(`/course/${row.id}`)">查看</el-button>
                <el-button text size="small" type="danger" @click="removeCourse(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div v-if="courseTotal > courseSize" class="pagination-wrap">
            <el-pagination v-model:current-page="coursePage" :page-size="courseSize" :total="courseTotal" layout="prev, pager, next" background @current-change="loadCourses()" />
          </div>
        </div>
      </el-tab-pane>

      <!-- 资源管理 -->
      <el-tab-pane label="资源管理" name="resources">
        <div class="app-card admin-panel">
          <div class="toolbar">
            <el-input v-model="resourceKeyword" placeholder="搜索资料标题" :prefix-icon="Search" clearable size="large" class="toolbar-input" @keyup.enter="loadResources(1)" @clear="loadResources(1)" />
            <el-button class="btn-gradient" size="large" @click="loadResources(1)">搜索</el-button>
          </div>

          <el-table :data="adminResources" v-loading="resourcesLoading" style="width: 100%">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column label="资料" min-width="240">
              <template #default="{ row }">
                <div class="course-cell">
                  <div class="cell-type" :style="{ background: resourceTypeColor(row.resourceType) + '14', color: resourceTypeColor(row.resourceType) }">
                    <el-icon :size="15"><component :is="resourceTypeIcon(row.resourceType)" /></el-icon>
                  </div>
                  <div>
                    <div class="cell-title">{{ row.title }}</div>
                    <div class="cell-sub">{{ row.course?.title || '' }}</div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="类型" width="110">
              <template #default="{ row }">{{ typeText(row.resourceType) }}</template>
            </el-table-column>
            <el-table-column label="AI 索引" width="110">
              <template #default="{ row }">
                <ai-index-badge v-if="row.aiIndexStatus" :status="row.aiIndexStatus" :error="row.aiIndexError" />
                <span v-else class="muted">—</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" align="right">
              <template #default="{ row }">
                <el-button text size="small" type="danger" @click="removeResource(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div v-if="resourceTotal > resourceSize" class="pagination-wrap">
            <el-pagination v-model:current-page="resourcePage" :page-size="resourceSize" :total="resourceTotal" layout="prev, pager, next" background @current-change="loadResources()" />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import AiIndexBadge from '@/components/AiIndexBadge.vue'
import { adminDeleteCourse, adminDeleteResource, adminGetCourses, adminGetResources, adminGetUsers, adminStatistics, adminUpdateUserRole, adminUpdateUserStatus } from '@/api/admin'
import type { AdminStatistics, Course, Resource, ResourceType, UserInfo } from '@/types'
import { courseCover, formatDateTime, resourceTypeColor, resourceTypeIcon, resourceTypeText, userAvatar } from '@/utils/format'

function typeText(t: string): string {
  return resourceTypeText[t as ResourceType] || '其他'
}

const activeTab = ref('users')
const stats = ref<AdminStatistics | null>(null)

const statCards = computed(() => [
  { label: '平台用户', value: stats.value?.userCount ?? '-', icon: 'User', bg: 'rgba(79,107,255,0.1)', color: '#4f6bff' },
  { label: '课程总数', value: stats.value?.courseCount ?? '-', icon: 'Collection', bg: 'rgba(16,185,129,0.12)', color: '#10b981' },
  { label: '学习资料', value: stats.value?.resourceCount ?? '-', icon: 'Document', bg: 'rgba(245,158,11,0.12)', color: '#f59e0b' },
  { label: '文件总数', value: stats.value?.fileCount ?? '-', icon: 'Files', bg: 'rgba(139,92,246,0.12)', color: '#8b5cf6' },
  { label: '已索引资料', value: stats.value?.indexedResourceCount ?? '-', icon: 'MagicStick', bg: 'rgba(6,182,212,0.12)', color: '#06b6d4' },
  { label: '存储占用', value: formatSize(stats.value?.storageUsage), icon: 'Coin', bg: 'rgba(239,68,68,0.1)', color: '#ef4444' }
])

// ---------- 用户管理 ----------
const users = ref<UserInfo[]>([])
const usersLoading = ref(false)
const userPage = ref(1)
const userSize = ref(20)
const userTotal = ref(0)
const userKeyword = ref('')
const userRoleFilter = ref('')

async function loadUsers(p?: number) {
  if (p) userPage.value = p
  usersLoading.value = true
  try {
    const res = await adminGetUsers({
      page: userPage.value,
      size: userSize.value,
      keyword: userKeyword.value || undefined,
      role: userRoleFilter.value || undefined
    })
    users.value = res.items || []
    userTotal.value = res.total || 0
  } catch {
    users.value = []
  } finally {
    usersLoading.value = false
  }
}

function changeRole(row: UserInfo, role: string) {
  ElMessageBox.confirm(`将 ${row.nickname || row.username} 的角色修改为「${role}」？`, '修改角色', { type: 'warning' })
    .then(async () => {
      await adminUpdateUserRole(row.id, role)
      ElMessage.success('角色已更新')
      await loadUsers()
    })
    .catch(() => {})
}

function disableUser(row: UserInfo) {
  ElMessageBox.confirm(`确定停用用户 ${row.nickname || row.username} 吗？`, '停用用户', { type: 'warning' })
    .then(async () => {
      await adminUpdateUserStatus(row.id, 'DISABLED')
      ElMessage.success('已停用')
      await loadUsers()
    })
    .catch(() => {})
}

// ---------- 课程管理 ----------
const adminCourses = ref<Course[]>([])
const coursesLoading = ref(false)
const coursePage = ref(1)
const courseSize = ref(20)
const courseTotal = ref(0)
const courseKeyword = ref('')

async function loadCourses(p?: number) {
  if (p) coursePage.value = p
  coursesLoading.value = true
  try {
    const res = await adminGetCourses({ page: coursePage.value, size: courseSize.value, keyword: courseKeyword.value || undefined })
    adminCourses.value = res.items || []
    courseTotal.value = res.total || 0
  } catch {
    adminCourses.value = []
  } finally {
    coursesLoading.value = false
  }
}

function removeCourse(id: number) {
  ElMessageBox.confirm('删除课程将同时删除章节与资料，且不可恢复。确定删除吗？', '删除课程', { type: 'error', confirmButtonText: '确认删除' })
    .then(async () => {
      await adminDeleteCourse(id)
      ElMessage.success('课程已删除')
      await loadCourses()
    })
    .catch(() => {})
}

// ---------- 资源管理 ----------
const adminResources = ref<Resource[]>([])
const resourcesLoading = ref(false)
const resourcePage = ref(1)
const resourceSize = ref(20)
const resourceTotal = ref(0)
const resourceKeyword = ref('')

async function loadResources(p?: number) {
  if (p) resourcePage.value = p
  resourcesLoading.value = true
  try {
    const res = await adminGetResources({ page: resourcePage.value, size: resourceSize.value, keyword: resourceKeyword.value || undefined })
    adminResources.value = res.items || []
    resourceTotal.value = res.total || 0
  } catch {
    adminResources.value = []
  } finally {
    resourcesLoading.value = false
  }
}

function removeResource(id: number) {
  ElMessageBox.confirm('确定删除该资料吗？', '删除资料', { type: 'warning' })
    .then(async () => {
      await adminDeleteResource(id)
      ElMessage.success('资料已删除')
      await loadResources()
    })
    .catch(() => {})
}

function formatSize(bytes?: number) {
  if (!bytes && bytes !== 0) return '-'
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)} MB`
  return `${(bytes / 1024 / 1024 / 1024).toFixed(2)} GB`
}

onMounted(async () => {
  try {
    stats.value = await adminStatistics()
  } catch {
    stats.value = null
  }
  await Promise.all([loadUsers(), loadCourses(), loadResources()])
})
</script>

<style scoped lang="scss">
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 14px;

  @media (max-width: 1300px) {
    grid-template-columns: repeat(3, 1fr);
  }

  @media (max-width: 700px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 18px;

  .stat-icon {
    width: 44px;
    height: 44px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .stat-value {
    font-size: 22px;
    font-weight: 800;
  }

  .stat-label {
    font-size: 12px;
    color: $color-text-3;
  }
}

.admin-panel {
  padding: 20px;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 18px;

  .toolbar-input {
    flex: 1;
    max-width: 380px;
  }

  .role-select {
    width: 130px;
  }
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;

  .user-name {
    font-size: 13px;
    font-weight: 600;
  }

  .user-sub {
    font-size: 11px;
    color: $color-text-3;
  }
}

.course-cell {
  display: flex;
  align-items: center;
  gap: 10px;

  .cell-cover {
    width: 56px;
    height: 38px;
    border-radius: 8px;
    object-fit: cover;
    flex-shrink: 0;
  }

  .cell-type {
    width: 32px;
    height: 32px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .cell-title {
    font-size: 13px;
    font-weight: 600;
    display: block;
    max-width: 260px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .cell-sub {
    font-size: 11px;
    color: $color-text-3;
  }
}

.muted {
  color: $color-text-3;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
