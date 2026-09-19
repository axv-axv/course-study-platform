# 知学：课程资料与智能学习平台

知学是一个面向学生、教师和管理员的课程学习平台。当前版本已经完成基础工程、身份认证、教师审核、课程与章节、文件与学习资料、收藏、学习进度、笔记、普通搜索、管理后台和个人学习仪表盘。RAG 与 AI Worker 将在基础业务稳定后独立接入。

## 已实现功能

- 学生注册、JWT 登录、Refresh Token 轮换和退出登录
- 教师资格申请与管理员审核
- 课程、成员和章节管理
- 学生自由加入和退出课程
- 文件上传、预览、下载和 HTTP Range 视频播放
- 学习资料、标签和课程内容筛选
- 收藏、学习进度、文档页码与视频时间点
- 个人笔记、最近学习和学习仪表盘
- 课程、资料、标签统一搜索与多条件资料筛选
- 管理员用户、角色、状态、课程、资料和平台统计管理
- PostgreSQL 数据迁移、Redis、健康检查和 Swagger UI

## 技术栈

| 部分 | 技术 |
| --- | --- |
| 前端 | Vue 3、TypeScript、Vite、Element Plus、Pinia、Vue Router、Axios |
| 后端 | Java 21、Spring Boot 3.5、Spring Security、Spring JDBC、JWT、Flyway |
| 数据 | PostgreSQL 17、Redis 7.4 |
| 文件存储 | 本地 Docker Volume（预留阿里云 OSS 适配） |
| 测试与部署 | JUnit 5、Mockito、Docker Compose、Actuator、OpenAPI |

## 项目目录

应用文件直接位于仓库根目录：

```text
course-study-platform/
├── backend/             # Spring Boot 后端
├── src/                 # Vue 前端
├── docker-compose.yml   # PostgreSQL、Redis、后端
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

Docker Compose 当前负责后端、PostgreSQL 和 Redis。前端开发服务器单独启动：

```bash
npm install
npm run dev
```

浏览器访问 <http://localhost:5173>。前端生产构建命令：

```bash
npm run build
```

## 测试

```bash
mvn -f backend/pom.xml -B test
npm run build
```

当前后端包含认证、课程、章节、文件、资料、收藏、进度、笔记、搜索和管理端等模块的自动化测试。

## 当前开发阶段

- M0：工程底座与 Docker 环境
- M1：认证与教师审核
- M2：课程、成员与章节
- M3：文件、资料与标签
- M4：收藏、进度、笔记与学习仪表盘
- M5：普通搜索与管理员后台
- 后续：Python LangChain RAG Worker、DeepSeek 云端模型接入
