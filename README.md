# Portal Backend

团队门户后端服务，提供项目导航、知识文章、AI 资产管理、生活空间和成长落地舱等功能的 RESTful API。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17 | 运行环境 |
| Spring Boot | 3.5.x | Web 框架 |
| MyBatis | 3.0.5 | ORM / 数据访问 |
| MySQL | 8.x+ | 关系型数据库 |
| Lombok | - | 简化 POJO 样板代码 |
| Maven | 3.x | 构建工具 |

## 项目结构

```
src/main/java/kd/address/view/
├── PortalApplication.java          # Spring Boot 启动类
├── common/                         # 通用响应封装
│   ├── ApiResponse.java            # 统一 API 响应体
│   └── PageResponse.java           # 分页响应体
├── config/                         # 配置类
│   └── WebConfig.java              # CORS 跨域配置
├── controller/                     # REST 控制器
│   ├── ProjectController.java      # 项目导航 API
│   ├── PortalController.java       # 门户概览 / 运维工作台 / 最近浏览
│   ├── ContentController.java      # 内容资源管理 (文章/生活)
│   ├── AiAssetController.java      # AI 资产管理
│   └── GrowthCapsuleController.java# 成长落地舱
├── dto/                            # 数据传输对象
├── entity/                         # 数据库实体
├── mapper/                         # MyBatis Mapper 接口
└── service/                        # 业务逻辑层
    ├── ProjectService.java
    ├── PortalCatalogService.java
    └── GrowthCapsuleService.java
```

## 数据库表

| 表名 | 说明 |
|------|------|
| `project` | 项目地址导航 |
| `portal_resource` | 门户统一资源（文章 / AI 资产 / 生活内容） |
| `operation_log` | 后台操作日志 |
| `recent_view_history` | 最近浏览历史 |
| `user_growth_capsule` | 用户灵感落地舱 |
| `user_growth_item` | 落地舱行动卡 |
| `user_growth_checkin` | 落地舱打卡记录 |

应用启动时自动执行 `schema.sql`（建表）和 `data.sql`（种子数据）。

## API 接口

### 项目导航 `/api/projects`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/projects/categories` | 获取所有分类 |
| GET | `/api/projects?category={category}&keyword={keyword}` | 按分类和关键词查询项目（参数均可选） |
| POST | `/api/projects` | 新增/更新项目 |
| DELETE | `/api/projects/{id}` | 删除项目 |

### 门户概览 `/api/portal`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/portal/overview` | 获取门户概览数据 |
| GET | `/api/portal/ops-workbench` | 获取运维工作台数据 |
| GET | `/api/portal/recent-views?clientId={clientId}` | 获取最近浏览记录 |
| POST | `/api/portal/recent-views` | 保存浏览记录 |

### 内容管理 `/api/content`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/content/resources?keyword=&type=&tag=&page=1&size=10` | 搜索内容资源（支持分页） |
| GET | `/api/content/tags` | 获取标签及计数 |
| POST | `/api/content/resources` | 新增/更新内容资源 |
| DELETE | `/api/content/resources/{id}` | 删除内容资源 |

### AI 资产 `/api/ai-assets`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ai-assets?type=&owner=&status=&page=1&size=10` | 查询 AI 资产（支持分页） |
| POST | `/api/ai-assets` | 新增/更新 AI 资产 |
| DELETE | `/api/ai-assets/{id}` | 删除 AI 资产 |

### 成长落地舱 `/api/growth-capsule`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/growth-capsule?userId={userId}` | 获取落地舱概览 |
| POST | `/api/growth-capsule/items` | 添加行动卡 |
| POST | `/api/growth-capsule/items/{itemId}/status?status=&note=` | 更新行动卡状态 |
| POST | `/api/growth-capsule/items/{itemId}/checkins` | 添加打卡记录 |

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+

### 数据库准备

```sql
CREATE DATABASE IF NOT EXISTS protal DEFAULT CHARACTER SET utf8mb4;
```

### 配置

编辑 `src/main/resources/application.yml` 或通过环境变量覆盖：

```bash
export DB_PASSWORD=your_password
```

### 构建与运行

```bash
# 编译打包
mvn clean package -DskipTests

# 运行
java -jar target/portal-backend-1.0.0.jar

# 或使用 Maven 插件运行
mvn spring-boot:run
```

服务默认启动在 `http://localhost:8089`。

## 开发规范

- **分层架构**：Controller -> Service -> Mapper，禁止跨层调用
- **统一响应**：所有接口使用 `ApiResponse<T>` 封装返回值
- **分页**：使用 `PageResponse<T>` 封装分页结果
- **软删除**：`portal_resource` 表使用 `deleted` 字段标记删除
- **操作日志**：增删改操作记录到 `operation_log` 表
- **命名约定**：数据库字段使用 snake_case，Java 属性使用 camelCase，MyBatis 自动映射
