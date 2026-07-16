# 论文生成系统（Thesis Generator）

在线论文文档自动生成工具——Web 端论文在线编辑与标准化文档生成系统。

## 技术栈

| 层 | 技术 |
|------|------|
| **后端** | Spring Boot 4.1.0 + Java 21 + PostgreSQL + Redis + JPA |
| **前端** | Vue 3 + Vite + Element Plus |
| **文档** | LaTeX（基于 `dlc/softeng.cls` 模板） |

## 环境要求

- **JDK 21**：https://adoptium.net/download/
- **PostgreSQL 16+**
- **Redis 7+**
- **Node.js 18+**：https://nodejs.org/
- **Maven**（已内置 mvnw，无需额外安装）

## 快速启动

```bash
# 1. 克隆仓库
git clone https://github.com/zzy154204-gif/thesis-generator.git
cd thesis-generator

# 2. 创建数据库
psql -U postgres -c "CREATE DATABASE thesis_generator;"

# 3. 配置数据库密码
cp src/main/resources/application.properties src/main/resources/application-local.properties
# 编辑 application-local.properties，填入你的数据库密码和 Redis 密码

# 4. 启动后端
./mvnw spring-boot-spring-boot:run    # Linux/Mac
mvnw.cmd spring-boot:run              # Windows

# 5. 启动前端
cd thesis-generator-web
npm install
npm run dev
```

## 目录结构

```
thesis-generator/
├── dlc/                        # 软件工程文档（LaTeX）
│   ├── softeng.cls              # 文档模板类文件
│   ├── 01_需求分析/main.tex
│   ├── 02_概要设计/main.tex
│   ├── 03_详细设计/main.tex
│   └── 04_测试报告/main.tex
├── src/                         # 后端 Java 源码
│   └── main/java/com/example/thesisgenerator/
│       ├── config/              # 配置类（CORS/Redis/Security）
│       ├── common/              # 统一返回体/异常处理
│       ├── entity/              # JPA 实体类
│       ├── repository/          # 数据访问层
│       ├── dto/                 # 数据传输对象
│       ├── service/             # 业务逻辑层
│       └── controller/          # RESTful 控制器
├── thesis-generator-web/          # 前端 Vue 3 源码
│   ├── src/
│   │   ├── api/                    # API 接口层（auth/paper/section/...）
│   │   ├── components/editor/      # 编辑器组件（Toolbar/SectionTree/ReferencePanel）
│   │   ├── views/                  # 页面（Login/Register/PaperList/PaperEditor/...）
│   │   ├── stores/                 # Pinia 状态管理
│   │   ├── router/                 # 路由配置 + 鉴权守卫
│   │   └── types/                  # TypeScript 类型定义
└── pom.xml                      # Maven 配置
```

## API 文档

后端启动后访问：http://localhost:8080/doc.html

## 团队

- 6 人实训小组
- 2026 年软件工程实训项目
