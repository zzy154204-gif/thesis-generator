# 论文生成系统（Thesis Generator）

在线论文文档自动生成工具——Web 端论文在线编辑与标准化文档（DOCX/PDF）导出系统。

## 功能概览

| 角色 | 功能 |
|------|------|
| **学生** | 论文创建与编辑（富文本/TipTap）、模板选择、参考文献管理、DOCX/PDF 导出、提交审阅 |
| **教师** | 论文审阅与批注（批注系统）、提交管理、评分 |
| **管理员** | 模板管理（版本控制）、学院管理、用户管理 |

核心 API 模块：

- 用户认证（JWT 登录/注册）
- 论文 CRUD 与章节管理
- 富文本编辑器（基于 TipTap，支持图片、表格、公式）
- 参考文献管理
- 模板系统（含版本管理）
- 导出系统（DOCX/PDF，含图片嵌入）
- 图片上传与访问
- 提交与审阅工作流
- 批注系统
- 草稿自动保存

## 技术栈

| 层 | 技术 |
|------|------|
| **后端** | Spring Boot 4.1.0 + Java 21 + PostgreSQL + JPA |
| **前端** | Vue 3 + Vite + TypeScript + Element Plus + TipTap |
| **文档生成** | Apache POI（DOCX）+ LibreOffice（DOCX → PDF） |
| **API 文档** | Springdoc OpenAPI 3.x（Swagger UI） |
| **认证** | JWT（jjwt） |

## 快速启动（Docker 方式 —— 推荐）

### 前置条件

- [Docker Desktop](https://www.docker.com/products/docker-desktop/)（Windows/Mac/Linux 均可）

### 启动步骤

```bash
# 1. 克隆仓库
git clone https://github.com/zzy154204-gif/thesis-generator.git
cd thesis-generator

# 2. （可选）自定义环境变量
cp .env.example .env
# 编辑 .env，修改 DB_PASSWORD 和 JWT_SECRET

# 3. 构建并启动所有服务
docker compose up -d --build

# 4. 访问系统
#     前端页面:  http://localhost:8081
#     API 文档:  http://localhost:8081/swagger-ui/index.html
#     后端 API:  http://localhost:8080
```

### 常用 Docker 命令

```bash
docker compose ps              # 查看服务状态
docker compose logs -f         # 查看所有日志
docker compose logs -f backend # 只看后端日志
docker compose down            # 停止服务
docker compose down -v         # 停止服务并删除数据卷（⚠️ 清空数据库和上传文件）
docker compose up -d --build   # 重新构建并启动（改代码后用）
```

## 本地开发（不依赖 Docker）

### 环境要求

- **JDK 21**：https://adoptium.net/download/
- **PostgreSQL 16+**
- **Node.js 18+**：https://nodejs.org/
- **Maven**（项目已内置 `mvnw`，无需额外安装）
- **LibreOffice**（可选，用于 DOCX → PDF 导出）

### 启动步骤

```bash
# 1. 创建数据库
psql -U postgres -c "CREATE DATABASE thesis_generator;"

# 2. 配置数据库密码（可选，默认密码 123456）
#    或直接修改 application.properties

# 3. 启动后端
./mvnw spring-boot:run

# 4. 启动前端（新开终端）
cd thesis-generator-web
npm install
npm run dev
# 前端运行在 http://localhost:5173，自动代理 /api 到后端 8080
```

## 目录结构

```
thesis-generator/
├── docker-compose.yml            # Docker 编排（PostgreSQL + 后端 + 前端）
├── Dockerfile                    # 后端 Docker 构建（Maven + JRE + LibreOffice）
├── nginx.conf                    # Nginx 配置（前端路由 + API 反向代理）
├── .env.example                  # 环境变量模板
├── .dockerignore
│
├── thesis-generator-web/         # 前端 Vue 3 + TypeScript
│   ├── Dockerfile                # 前端 Docker 构建（Node + Nginx）
│   ├── src/
│   │   ├── api/                  # API 调用封装（axios）
│   │   ├── views/                # 页面组件
│   │   │   ├── auth/             # 登录/注册
│   │   │   ├── paper/            # 论文管理、编辑器
│   │   │   ├── student/          # 学生端（模板浏览、参考文献）
│   │   │   ├── teacher/          # 教师端（审阅）
│   │   │   ├── admin/            # 管理端（模板、学院）
│   │   │   ├── preview/          # 预览
│   │   │   └── user/             # 个人信息、导出历史
│   │   ├── stores/               # Pinia 状态管理
│   │   ├── router/               # Vue Router 路由
│   │   ├── components/           # 通用组件
│   │   └── extensions/           # TipTap 编辑器扩展
│   └── package.json
│
├── src/                          # 后端 Java 源码
│   └── main/java/com/example/thesisgenerator/
│       ├── config/               # 配置类（CORS、JWT、Swagger）
│       ├── common/               # 统一返回体、异常处理
│       ├── entity/               # JPA 实体
│       ├── repository/           # 数据访问层
│       ├── dto/                  # 数据传输对象
│       ├── service/              # 业务逻辑
│       │   ├── DocxExportService # DOCX 导出（含图片嵌入）
│       │   ├── PdfExportService  # PDF 导出（DOCX → LibreOffice → PDF）
│       │   ├── ImageService      # 图片上传与存储
│       │   └── ...
│       └── controller/           # RESTful 控制器
│           ├── AuthController        # /api/v1/auth
│           ├── PaperController       # /api/v1/papers
│           ├── SectionController     # /api/v1/papers/{id}/sections
│           ├── ImageController       # /api/v1/images
│           ├── TemplateController    # /api/v1/templates
│           ├── ReferenceController   # /api/v1/references
│           ├── SubmissionController  # /api/v1/submissions
│           ├── ReviewRecordController# /api/v1/reviews
│           ├── AnnotationController  # /api/v1/annotations
│           ├── DraftController       # /api/v1/drafts
│           ├── ExportRecordController# /api/v1/exports
│           └── CollegeController     # /api/v1/colleges
│
├── uploads/                      # 用户上传的图片（本地开发时）
├── dlc/                          # 软件工程文档（LaTeX）
└── pom.xml                       # Maven 配置
```

## Docker 数据卷（Volumes）

系统使用了两个命名数据卷来持久化数据，确保容器删除后数据不丢失：

| 卷名 | 挂载路径 | 存储内容 | 说明 |
|------|---------|---------|------|
| `pgdata` | `/var/lib/postgresql/data` | PostgreSQL 数据库文件 | 论文数据、用户数据、配置数据 |
| `uploads` | `/app/uploads` | 用户上传的图片文件 | 富文本编辑器中插入的图片 |

> **什么是 Volume？**
> Docker 容器默认写磁盘的文件在容器删除后就没了。Volume（数据卷）是 Docker 管理的持久化存储，独立于容器生命周期。即使 `docker compose down` 停止服务，数据仍在；只有 `docker compose down -v` 才会删除卷数据。

查看 volume 的位置：

```bash
docker volume ls                           # 列出所有卷
docker volume inspect thesis-generator_pgdata  # 查看数据库卷详情
```

## API 文档

启动服务后访问：http://localhost:8081/swagger-ui/index.html

Swagger UI 会列出所有 REST 接口，支持在线 "Try it out" 测试。

## Docker 端口映射

| 宿主机端口 | 容器端口 | 服务 | 备注 |
|-----------|---------|------|------|
| 8081 | 80 | 前端（Nginx） | 避免与本机 IIS/80 冲突 |
| 8082 | 8080 | 后端 API | 避免与服务器考勤系统 8080 冲突 |
| 5433 | 5432 | PostgreSQL | 避免与本机 PG/5432 冲突 |

## 团队

- 6 人实训小组
- 2026 年软件工程实训项目
