# 知学 · 课程资料与智能学习平台（前端）

基于 **Vue 3 + TypeScript + Element Plus + Pinia + Vue Router + Axios** 的课程资料与智能学习平台前端工程，完整对接后端 `/api/v1` 接口规范。

## 功能总览

| 模块 | 页面 | 说明 |
|---|---|---|
| 认证 | `/login` | 登录 / 注册（学生、教师身份） |
| 首页 | `/home` | Hero 欢迎区、快捷入口、推荐课程、继续学习 |
| 我的学习 | `/my-learning` | 学习仪表盘（统计卡片、课程进度、最近学习、整体完成率） |
| 课程 | `/courses`、`/course/:id` | 课程广场（搜索/筛选）、课程详情（章节树 + 资料列表 + 进度） |
| 资料 | `/resource/:id` | 文件预览（PDF/视频/链接）、收藏、学习进度、笔记、AI 索引管理 |
| AI 问答 | `/ai/:courseId?` | 课程级 AI 问答，支持章节/资料检索范围、对话历史、回答来源引用 |
| 搜索 | `/search` | 课程 / 资料 / 标签统一搜索 |
| 收藏 | `/favorites` | 我的收藏 |
| 笔记 | `/notes` | 我的全部笔记 |
| 个人中心 | `/profile` | 修改昵称 / 邮箱 / 简介 / 头像 |
| 教师端 | `/teacher/*` | 课程管理、创建/编辑课程（章节 + 资料 + 上传）、成员管理、RAG 索引重建 |
| 管理端 | `/admin` | 平台统计、用户 / 课程 / 资料管理 |

## 快速开始

### 🚀 最省事的方式（推荐）：双击启动器

| 系统 | 操作 |
|---|---|
| **Windows** | 解压后双击 `启动.bat` |
| **macOS** | 解压后双击 `启动.command`（首次需右键→打开 允许运行） |

启动器会自动完成：检查 Node.js → 安装依赖（仅首次）→ 启动服务 → 自动打开浏览器 http://localhost:5173。
以后每次想打开平台，只需双击启动器即可，**不需要手动敲任何命令**。

> 提示：启动后那个黑色窗口/终端请保持开启，关闭它服务就会停止。

### 手动方式（进阶）

```bash
# 1. 安装依赖
npm install

# 2. 启动开发服务器（默认 http://localhost:5173）
npm run dev

# 3. 生产构建
npm run build

# 4. 预览构建产物
npm run preview
```

## 对接后端

1. **接口前缀**：默认 `/api/v1`，由 `src/api/request.ts` 中的 `BASE_URL` 读取 `VITE_API_BASE` 环境变量。
2. **开发代理**：`vite.config.ts` 已配置 `/api/v1` 与 `/files` 代理到 `http://127.0.0.1:8080`，按实际后端地址修改 `target`。
3. **生产环境**：修改 `.env.production` 中的 `VITE_API_BASE` 为实际网关地址（如 `https://api.example.com/api/v1`）。
4. **Token 机制**：登录后自动携带 `Authorization: Bearer <accessToken>`；401 时自动用 refreshToken 刷新并重放请求。

### 接口模块映射

所有接口封装在 `src/api/` 下，与后端文档一一对应：

| 文件 | 覆盖模块 |
|---|---|
| `auth.ts` | 注册、登录、刷新、当前用户、修改资料/头像 |
| `user.ts` | 我的课程、创建课程、收藏、笔记、最近学习、学习仪表盘 |
| `course.ts` | 课程 CRUD、成员、章节 CRUD/排序、课程进度、课程聚合、课程资料 |
| `resource.ts` | 资料 CRUD、标签、收藏、进度、笔记、AI 索引状态/重建/删除 |
| `file.ts` | 上传、信息、预览地址、下载地址 |
| `search.ts` | 统一搜索、课程搜索、资料搜索 |
| `ai.ts` | AI 对话创建/列表/详情/删除、AI 提问 |
| `admin.ts` | 管理端：用户 / 课程 / 资料 / 统计 |

## 目录结构

```
src/
├── api/          # Axios 封装 + 全量接口（对齐后端文档）
├── components/   # 通用组件（布局、课程卡、资源卡、进度环、AI 气泡、章节树等）
├── router/       # 路由 + 登录守卫 + 角色守卫
├── stores/       # Pinia（用户态、主题态）
├── styles/       # 设计变量（品牌色/圆角/阴影）与全局样式（含暗色模式）
├── types/        # 与后端响应对齐的 TS 类型
├── utils/        # 格式化、封面/头像生成（无图自动生成渐变 SVG）
└── views/        # 学生端 / 教师端 / 管理端全部页面
```

## 设计说明

- **品牌视觉**：主色 `#4F6BFF → #8B5CF6` 蓝紫渐变，大圆角卡片 + 柔和阴影 + 悬浮微动效，风格精致统一。
- **暗色模式**：右上角主题开关，基于 CSS 变量 + Element Plus dark 主题。
- **无图依赖**：课程封面、用户头像在无图片时自动生成确定性渐变 SVG，开箱即用。
- **AI 问答**：消息气泡 + 打字动画 + 回答来源（资源标题 / 页码 / 片段）引用卡片，点击可跳转对应资料。
- **响应式**：适配 PC / 平板 / 手机宽度。

## 注意事项

1. 文件预览地址（`/files/{id}/preview`）在 `<iframe>` 中无法携带 `Authorization` Header，当前实现以 `?token=` 参数形式附加；若后端不支持 query token，请改为后端放行 cookie 认证，或接入后端提供的临时预览签名地址（`src/views/resource/ResourceDetail.vue` 中 `previewUrl`）。
2. 成员移除（`MemberManage.vue`）当前复用「退出课程」接口；后端若提供 `DELETE /courses/{courseId}/members/{userId}`，替换为对应调用即可。
3. 所有请求均通过拦截器统一处理错误提示与 401 刷新。
