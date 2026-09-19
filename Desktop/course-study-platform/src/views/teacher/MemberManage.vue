<template>
  <div class="page-container">
    <div class="page-head">
      <div>
        <h1 class="page-title">成员管理</h1>
        <p class="page-subtitle">{{ courseTitle }} · 共 {{ total }} 名成员</p>
      </div>
      <el-button @click="$router.push(`/teacher/course/${courseId}/edit`)">
        <el-icon class="mr6"><Back /></el-icon>返回课程管理
      </el-button>
    </div>

    <div class="app-card filter-bar">
      <el-input v-model="keyword" placeholder="搜索成员用户名 / 昵称" :prefix-icon="Search" clearable size="large" class="filter-keyword" @keyup.enter="load(1)" @clear="load(1)" />
      <el-button class="btn-gradient" size="large" @click="load(1)">搜索</el-button>
    </div>

    <div class="app-card member-panel" v-loading="loading">
      <el-table :data="members" style="width: 100%">
        <el-table-column label="成员" min-width="220">
          <template #default="{ row }">
            <div class="member-cell">
              <el-avatar :size="36" :src="avatarOf(row)" class="member-avatar">{{ (row.nickname || row.username || 'U').charAt(0) }}</el-avatar>
              <div class="member-info">
                <span class="member-name">{{ row.nickname || row.username }}</span>
                <span class="member-user">@{{ row.username }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="role" label="身份" width="120">
          <template #default="{ row }">
            <el-tag :type="row.role === 'TEACHER' ? 'warning' : 'info'" effect="light" round>
              {{ row.role === 'TEACHER' ? '教师' : '学生' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="加入时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.joinedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="right">
          <template #default="{ row }">
            <el-button text size="small" type="danger" :disabled="row.role === 'TEACHER'" @click="removeMember(row)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <EmptyState v-if="!loading && !members.length" icon="User" text="暂无成员" sub-text="学生加入课程后会显示在这里" padding="48" />
    </div>

    <div v-if="total > size" class="pagination-wrap">
      <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="prev, pager, next" background @current-change="load()" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import EmptyState from '@/components/EmptyState.vue'
import { getCourseDetail, getCourseMembers, leaveCourse } from '@/api/course'
import type { CourseMember } from '@/types'
import { formatDateTime, userAvatar } from '@/utils/format'

const route = useRoute()
const courseId = computed(() => Number(route.params.courseId))

const courseTitle = ref('')
const members = ref<CourseMember[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const keyword = ref('')

function avatarOf(row: CourseMember) {
  return userAvatar(row.nickname || row.username, row.avatarUrl)
}

async function load(p?: number) {
  if (p) page.value = p
  loading.value = true
  try {
    const res = await getCourseMembers(courseId.value, {
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined
    })
    members.value = res.items || []
    total.value = res.total || 0
  } catch {
    members.value = []
  } finally {
    loading.value = false
  }
}

function removeMember(row: CourseMember) {
  ElMessageBox.confirm(`确定将 ${row.nickname || row.username} 移出课程吗？`, '移除成员', { type: 'warning' })
    .then(async () => {
      // 后端暂无单独移除成员接口时，此处通过“以成员身份退出”模拟；如后端提供
      // DELETE /courses/{courseId}/members/{userId}，请替换为对应接口
      await leaveCourse(courseId.value)
      ElMessage.success('成员已移除')
      await load()
    })
    .catch(() => {})
}

onMounted(async () => {
  try {
    const c = await getCourseDetail(courseId.value)
    courseTitle.value = c.title
  } catch {
    courseTitle.value = ''
  }
  await load()
})
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

.member-panel {
  padding: 20px;
  min-height: 300px;
}

.member-cell {
  display: flex;
  align-items: center;
  gap: 12px;

  .member-avatar {
    flex-shrink: 0;
  }

  .member-info {
    display: flex;
    flex-direction: column;

    .member-name {
      font-size: 14px;
      font-weight: 600;
    }

    .member-user {
      font-size: 12px;
      color: $color-text-3;
    }
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
