# 知学：课程资料与智能学习平台

知学是一个面向学生、教师和管理员的课程学习平台。当前版本已经完成基础业务平台，以及由独立 Python Worker 执行的 RAG 资料解析、向量建库和课程智能问答。

## 已实现功能

- 学生注册、JWT 登录、Refresh Token 轮换和退出登录
- 个人资料编辑和图片头像上传
- 教师资格申请与管理员审核
- 课程、成员和章节管理
- 学生自由加入和退出课程
- 文件上传、预览、下载和 HTTP Range 视频播放
- 学习资料、标签和课程内容筛选
- 收藏、学习进度、文档页码与视频时间点
- 课程主页聚合加载、资料浏览量和下载量统计
- 个人笔记、最近学习和学习仪表盘
- 课程、资料、标签统一搜索与多条件资料筛选
- 管理员用户、角色、状态、课程、资料和平台统计管理
- RAG 索引任务、PDF/DOCX/PPTX/Markdown/TXT 文本解析与 LangChain 切块
- 资料索引状态查询、异步重建和立即删除
- 课程、章节、单份资料三级范围的 RAG 问答
- AI 对话创建、历史消息、来源引用和立即删除
- 无密钥本地测试模式，以及可配置的 DeepSeek 云端回答模式
- PostgreSQL 数据迁移、Redis、健康检查和 Swagger UI

## 技术栈

| 部分 | 技术 |
| --- | --- |
| 前端 | Vue 3、TypeScript、Vite、Element Plus、Pinia、Vue Router、Axios |
| 后端 | Java 21、Spring Boot 3.5、Spring Security、Spring JDBC、JWT、Flyway |
| AI Worker | Python 3.12、FastAPI、LangChain、LangChain OpenAI、Psycopg 3、DeepSeek API |
| 数据 | PostgreSQL 17、Redis 7.4 |
| 文件存储 | 本地 Docker Volume（预留阿里云 OSS 适配） |
| 测试与部署 | JUnit 5、Mockito、Docker Compose、Actuator、OpenAPI |

## 项目目录

应用文件直接位于仓库根目录：

```text
course-study-platform/
├── backend/             # Spring Boot 后端
├── ai-worker/           # Python LangChain RAG 索引 Worker
├── src/                 # Vue 前端
├── docker-compose.yml   # PostgreSQL、Redis、后端、AI Worker
├── .env.example         # 本地环境变量示例
└── package.json
```

## Docker 启动

### 环境要求

- Docker Desktop 或 Docker Engine + Compose
- JDK 21
- Maven 3.9+

由于 Compose 默认使用离线友好的运行时镜像，需要先在本机生成后端 JAR，再交给 Docker 封装和运行。

### Windows PowerShell

```powershell
git clone https://github.com/axv-axv/course-study-platform.git
cd course-study-platform

Copy-Item .env.example .env
mvn -f backend/pom.xml -B clean package -DskipTests
docker compose up -d --build --wait
```

### macOS / Linux

```bash
git clone https://github.com/axv-axv/course-study-platform.git
cd course-study-platform

cp .env.example .env
mvn -f backend/pom.xml -B clean package -DskipTests
docker compose up -d --build --wait
```

启动完成后：

- 后端 API：<http://localhost:8080/api/v1>
- 健康检查：<http://localhost:8080/actuator/health>
- Swagger UI：<http://localhost:8080/swagger-ui.html>
- PostgreSQL：`localhost:5432`
- Redis：`localhost:6379`

本地开发管理员默认账号为 `admin`，默认密码为 `ChangeMe123!`。这些值只用于本地开发，部署前必须修改 `.env` 中的管理员密码、数据库密码和 `JWT_SECRET`。

如果端口被占用，可以在 `.env` 中修改：

```dotenv
BACKEND_PORT=18080
POSTGRES_PORT=15432
REDIS_PORT=16379
```

查看状态与日志：

```bash
docker compose ps
docker compose logs -f backend
```

停止服务但保留数据库和文件：

```bash
docker compose down
```

同时删除本地数据库、Redis 和文件卷：

```bash
docker compose down -v
```

## 启动前端开发服务器

Docker Compose 当前负责后端、AI Worker、PostgreSQL 和 Redis。前端开发服务器单独启动：

```bash
npm install
npm run dev
```

浏览器访问 <http://localhost:5173>。前端生产构建命令：

```bash
npm run build
```

## AI 问答配置

默认的 `AI_PROVIDER=local` 不需要 API Key，会返回检索到的课程原文和来源，适合本地开发与 Docker 验收。生产环境切换 DeepSeek 时，在 `.env` 中配置：

```dotenv
AI_PROVIDER=deepseek
DEEPSEEK_API_KEY=你的密钥
DEEPSEEK_BASE_URL=https://api.deepseek.com
DEEPSEEK_MODEL=deepseek-flash
AI_WORKER_INTERNAL_TOKEN=请替换为随机内部密钥
```

修改环境变量后重新创建后端和 Worker：

```bash
docker compose up -d --build --force-recreate backend ai-worker
```

AI 对外接口由 Java 后端统一提供，登录后可以在 Swagger UI 调试：

- `POST /api/v1/ai/conversations`：创建课程对话
- `GET /api/v1/ai/conversations`：分页查询自己的对话
- `GET /api/v1/ai/conversations/{id}`：读取消息和引用来源
- `DELETE /api/v1/ai/conversations/{id}`：立即删除对话及消息
- `POST /api/v1/ai/chat`：按课程、章节或资料范围提问

课程详情页使用 `GET /api/v1/courses/{id}/overview` 一次返回课程、章节、个人进度、最近资料和 AI 可用状态。个人头像通过 `POST /api/v1/users/me/avatar` 上传；只有已经绑定为用户头像的图片可以通过公开头像地址读取，普通课程文件仍需登录并经过课程权限校验。

## 测试

```bash
mvn -f backend/pom.xml -B test
npm run build
cd ai-worker && python -m unittest discover -s tests -v
```

当前后端包含认证、课程、章节、文件、资料、收藏、进度、笔记、搜索、管理端和 AI 对话等模块的自动化测试。

AI Worker 使用不依赖云端密钥的确定性开发向量，可完整验证解析、切块、任务队列、索引生命周期、范围检索和来源引用。配置 DeepSeek 后由 LangChain OpenAI 兼容客户端生成最终回答，资料索引 API 无需改变。

## 当前开发阶段

- M0：工程底座与 Docker 环境
- M1：认证与教师审核
- M2：课程、成员与章节
- M3：文件、资料与标签
- M4：收藏、进度、笔记与学习仪表盘
- M5：普通搜索与管理员后台
- M6：Python LangChain RAG Worker、资料解析与向量建库
- M7：DeepSeek 课程问答、三级范围检索、来源引用与对话历史
- M8：课程主页聚合、头像上传、浏览与下载统计闭环
