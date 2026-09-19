<template>
  <div class="login-page">
    <!-- 左侧品牌展示 -->
    <div class="brand-panel">
      <div class="brand-inner">
        <div class="brand-logo">
          <el-icon :size="34"><Reading /></el-icon>
        </div>
        <h1 class="brand-title">知学<span class="dot">·</span>ZhiXue</h1>
        <p class="brand-slogan">课程资料与智能学习平台</p>

        <div class="feature-list">
          <div v-for="f in features" :key="f.title" class="feature-item">
            <div class="feature-icon">
              <el-icon :size="18"><component :is="f.icon" /></el-icon>
            </div>
            <div>
              <div class="feature-title">{{ f.title }}</div>
              <div class="feature-desc">{{ f.desc }}</div>
            </div>
          </div>
        </div>
      </div>
      <div class="brand-decoration d1"></div>
      <div class="brand-decoration d2"></div>
    </div>

    <!-- 右侧表单 -->
    <div class="form-panel">
      <div class="form-card app-card">
        <div class="form-tabs">
          <button class="tab" :class="{ active: mode === 'login' }" @click="mode = 'login'">登录</button>
          <button class="tab" :class="{ active: mode === 'register' }" @click="mode = 'register'">注册</button>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large" @submit.prevent>
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="User" clearable />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" :prefix-icon="Lock" show-password @keyup.enter="submit" />
          </el-form-item>

          <el-form-item v-if="mode === 'register'" label="邮箱（选填）" prop="email">
            <el-input v-model="form.email" placeholder="请输入邮箱" :prefix-icon="Message" clearable />
          </el-form-item>

          <el-form-item v-if="mode === 'register'" label="注册身份" prop="role">
            <el-radio-group v-model="form.role">
              <el-radio value="STUDENT">学生</el-radio>
              <el-radio value="TEACHER">教师</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-button class="submit-btn btn-gradient" size="large" :loading="loading" @click="submit">
            {{ mode === 'login' ? '登 录' : '注 册' }}
          </el-button>
        </el-form>

        <p class="switch-tip">
          {{ mode === 'login' ? '还没有账号？' : '已有账号？' }}
          <a @click="mode = mode === 'login' ? 'register' : 'login'">
            {{ mode === 'login' ? '立即注册' : '去登录' }}
          </a>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock, Message } from '@element-plus/icons-vue'
import { login, register } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const features = [
  { icon: 'Collection', title: '课程资料管理', desc: '课程 · 章节 · 资料一站式管理' },
  { icon: 'MagicStick', title: 'AI 课程问答', desc: '基于课程知识库的智能问答与来源引用' },
  { icon: 'DataBoard', title: '学习进度追踪', desc: '进度、笔记、收藏，学习状态一目了然' }
]

const mode = ref<'login' | 'register'>('login')
const loading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  username: '',
  password: '',
  email: '',
  role: 'STUDENT'
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ]
}

async function submit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    if (mode.value === 'register') {
      await register({ username: form.username, password: form.password, email: form.email, role: form.role })
      ElMessage.success('注册成功，请登录')
      mode.value = 'login'
      form.password = ''
    } else {
      // ========== 前端硬编码账号，不走api请求 ==========
      const initUser = {
      username: "lizixuan",
      password: "5120251815"
      }
      if(form.username === initUser.username && form.password === initUser.password){
        // 手动模拟token，适配你的userStore
        userStore.setToken("demo-access-001", "demo-refresh-001")
        // 模拟用户信息
        userStore.userInfo = {
          username: "lizixuan",
          role: "STUDENT"
        }
        ElMessage.success('登录成功，欢迎回来')
        const redirect = (route.query.redirect as string) || '/home'
        router.push(redirect)
      }else{
        ElMessage.error("账号或密码错误")
      }
    }

  } catch {
    // 错误已由拦截器统一提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  display: flex;
  background: var(--app-bg, $color-bg-page);
}

// ---------- 左品牌面板 ----------
.brand-panel {
  flex: 1.15;
  position: relative;
  background: linear-gradient(145deg, #3f5ef0 0%, #6a4df0 55%, #9333ea 100%);
  color: #fff;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
}

.brand-decoration {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.07);

  &.d1 {
    width: 420px;
    height: 420px;
    top: -120px;
    right: -100px;
  }

  &.d2 {
    width: 320px;
    height: 320px;
    bottom: -110px;
    left: -80px;
  }
}

.brand-inner {
  position: relative;
  z-index: 1;
  max-width: 460px;
}

.brand-logo {
  width: 60px;
  height: 60px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.18);
}

.brand-title {
  font-size: 40px;
  font-weight: 800;
  letter-spacing: 2px;

  .dot {
    color: #fbbf24;
    margin: 0 4px;
  }
}

.brand-slogan {
  font-size: 16px;
  opacity: 0.85;
  margin-top: 8px;
  margin-bottom: 44px;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.feature-item {
  display: flex;
  gap: 14px;
  align-items: flex-start;

  .feature-icon {
    width: 42px;
    height: 42px;
    border-radius: 12px;
    background: rgba(255, 255, 255, 0.14);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .feature-title {
    font-size: 15px;
    font-weight: 700;
  }

  .feature-desc {
    font-size: 13px;
    opacity: 0.75;
    margin-top: 3px;
  }
}

// ---------- 右表单面板 ----------
.form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
}

.form-card {
  width: 400px;
  max-width: 100%;
  padding: 36px 36px 28px;
}

.form-tabs {
  display: flex;
  gap: 26px;
  margin-bottom: 28px;
  border-bottom: 2px solid $color-border;
  padding-bottom: 14px;

  .tab {
    border: none;
    background: none;
    font-size: 18px;
    font-weight: 700;
    color: $color-text-3;
    cursor: pointer;
    padding: 0 2px 12px;
    position: relative;
    transition: $transition-base;

    &.active {
      color: $color-primary;

      &::after {
        content: '';
        position: absolute;
        left: 0;
        right: 0;
        bottom: -16px;
        height: 3px;
        border-radius: 3px;
        background: $gradient-primary;
      }
    }
  }
}

.submit-btn {
  width: 100%;
  margin-top: 6px;
  font-size: 16px;
  letter-spacing: 6px;
}

.switch-tip {
  text-align: center;
  margin-top: 20px;
  font-size: 13px;
  color: $color-text-3;

  a {
    color: $color-primary;
    cursor: pointer;
    font-weight: 600;

    &:hover {
      text-decoration: underline;
    }
  }
}

// 响应式：窄屏隐藏品牌面板
@media (max-width: 900px) {
  .brand-panel {
    display: none;
  }
}
</style>
