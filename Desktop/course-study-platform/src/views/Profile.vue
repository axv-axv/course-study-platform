<template>
  <div class="page-container profile-page">
    <el-row :gutter="20">
      <!-- 左：个人信息卡 -->
      <el-col :xs="24" :md="8">
        <div class="app-card profile-card">
          <div class="profile-cover">
            <div class="avatar-wrap">
              <el-avatar :size="88" :src="avatar" class="profile-avatar">
                {{ (userStore.userInfo?.nickname || userStore.userInfo?.username || 'U').charAt(0) }}
              </el-avatar>
              <el-icon class="avatar-edit" @click="avatarInput?.click()"><CameraFilled /></el-icon>
              <input ref="avatarInput" type="file" accept="image/*" class="hidden-input" @change="handleAvatarChange" />
            </div>
          </div>

          <div class="profile-body">
            <h2 class="profile-name">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</h2>
            <el-tag :type="roleTagType" effect="light" round class="profile-role">{{ roleText }}</el-tag>
            <p class="profile-bio">{{ userStore.userInfo?.bio || '这个人很懒，还没有填写简介' }}</p>

            <div class="profile-stats">
              <div class="stat">
                <span class="stat-value">{{ userStore.userInfo?.id || '-' }}</span>
                <span class="stat-label">用户 ID</span>
              </div>
              <div class="stat">
                <span class="stat-value">{{ userStore.userInfo?.email || '-' }}</span>
                <span class="stat-label">邮箱</span>
              </div>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 右：编辑表单 -->
      <el-col :xs="24" :md="16">
        <div class="app-card edit-card">
          <h3 class="edit-title">编辑个人资料</h3>

          <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large">
            <el-row :gutter="20">
              <el-col :xs="24" :md="12">
                <el-form-item label="用户名">
                  <el-input v-model="form.username" disabled />
                  <div class="form-tip">用户名不可修改</div>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="昵称" prop="nickname">
                  <el-input v-model="form.nickname" placeholder="请输入昵称" clearable />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="邮箱" prop="email">
                  <el-input v-model="form.email" placeholder="请输入邮箱" clearable />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="角色">
                  <el-input :model-value="roleText" disabled />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="个人简介">
                  <el-input v-model="form.bio" type="textarea" :rows="4" maxlength="200" show-word-limit placeholder="介绍一下自己吧" />
                </el-form-item>
              </el-col>
            </el-row>

            <div class="form-actions">
              <el-button class="btn-gradient" size="large" :loading="saving" @click="save">保存修改</el-button>
            </div>
          </el-form>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { updateAvatar, updateMe } from '@/api/auth'
import { userAvatar } from '@/utils/format'

const userStore = useUserStore()
const formRef = ref<FormInstance>()
const avatarInput = ref<HTMLInputElement>()
const saving = ref(false)

const form = reactive({
  username: '',
  nickname: '',
  email: '',
  bio: ''
})

const rules: FormRules = {
  nickname: [{ max: 20, message: '昵称最长 20 个字符', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

const avatar = computed(() => userAvatar(userStore.userInfo?.nickname || userStore.userInfo?.username, userStore.userInfo?.avatarUrl))
const roleText = computed(() => {
  const map: Record<string, string> = { STUDENT: '学生', TEACHER: '教师', ADMIN: '管理员' }
  return map[userStore.userInfo?.role || ''] || ''
})
const roleTagType = computed(() => {
  const map: Record<string, 'info' | 'warning' | 'danger'> = { STUDENT: 'info', TEACHER: 'warning', ADMIN: 'danger' }
  return map[userStore.userInfo?.role || ''] || 'info'
})

function fillForm() {
  const u = userStore.userInfo
  if (!u) return
  form.username = u.username || ''
  form.nickname = u.nickname || ''
  form.email = u.email || ''
  form.bio = u.bio || ''
}

async function save() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await updateMe({ nickname: form.nickname, bio: form.bio, email: form.email })
    ElMessage.success('资料已更新')
    await userStore.fetchUserInfo()
  } catch {
    // 已提示
  } finally {
    saving.value = false
  }
}

async function handleAvatarChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('头像图片不能超过 5MB')
    return
  }
  try {
    // 直接以 multipart/form-data 上传头像
    const formData = new FormData()
    formData.append('file', file)
    const resp = await updateAvatar(formData)
    if (resp.avatarUrl) {
      await userStore.fetchUserInfo()
      ElMessage.success('头像已更新')
    }
  } catch {
    ElMessage.error('头像上传失败')
  } finally {
    input.value = ''
  }
}

onMounted(() => {
  userStore.fetchUserInfo().then(() => fillForm())
})
</script>

<style scoped lang="scss">
.profile-page {
  max-width: 980px;
}

// ---------- 个人卡片 ----------
.profile-card {
  overflow: hidden;
}

.profile-cover {
  height: 140px;
  background:
    radial-gradient(300px 140px at 30% 20%, rgba(255, 255, 255, 0.25), transparent 60%),
    $gradient-primary;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.avatar-wrap {
  position: relative;
  transform: translateY(44px);

  .profile-avatar {
    border: 4px solid #fff;
    box-shadow: $shadow-card;
    background: var(--app-card-bg, #fff);
  }

  .avatar-edit {
    position: absolute;
    right: -4px;
    bottom: 6px;
    width: 28px;
    height: 28px;
    border-radius: 50%;
    background: $color-primary;
    color: #fff;
    padding: 6px;
    cursor: pointer;
    box-shadow: 0 2px 8px rgba(79, 107, 255, 0.4);
    transition: $transition-base;

    &:hover {
      transform: scale(1.1);
    }
  }
}

.hidden-input {
  display: none;
}

.profile-body {
  padding: 60px 24px 24px;
  text-align: center;
}

.profile-name {
  font-size: 20px;
  font-weight: 800;
}

.profile-role {
  margin-top: 8px;
}

.profile-bio {
  font-size: 13px;
  color: $color-text-3;
  margin-top: 12px;
  min-height: 20px;
}

.profile-stats {
  display: flex;
  justify-content: center;
  gap: 36px;
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px solid $color-border;

  .stat {
    display: flex;
    flex-direction: column;

    .stat-value {
      font-size: 15px;
      font-weight: 800;
      color: $color-primary;
      max-width: 160px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .stat-label {
      font-size: 12px;
      color: $color-text-3;
      margin-top: 4px;
    }
  }
}

// ---------- 编辑 ----------
.edit-card {
  padding: 28px;
}

.edit-title {
  font-size: 18px;
  font-weight: 800;
  margin-bottom: 24px;
  padding-bottom: 14px;
  border-bottom: 1px solid $color-border;
}

.form-tip {
  font-size: 12px;
  color: $color-text-3;
  margin-top: 4px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
}
</style>
