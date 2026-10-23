# 门户与社区部署

## 全新数据库初始化

先创建空的 `protal` 数据库，再执行唯一的数据库脚本：

```bash
mysql -u "$DB_USERNAME" -p protal < src/main/resources/schema.sql
```

`schema.sql` 包含项目、站外来源、项目解读、门户、访客账户和社区的全部 20 张表，以及初始门户内容和“项目分享”“技术交流”“闲聊”三个板块。应用启动时也会执行这个脚本；建表与初始数据写入可重复执行。脚本不会删除旧表或迁移旧数据。若要重建已有库，须先备份并在应用停止后清空该库中的旧表，再执行脚本。

## 服务配置

服务端通过环境变量读取连接和安全配置：

```bash
export DB_URL='jdbc:mysql://localhost:13306/protal?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true'
export DB_USERNAME='root'
export DB_PASSWORD='数据库密码'
export PORTAL_ALLOWED_ORIGINS='https://portal.example.com'
export PORTAL_COOKIE_SECURE=true
export PORTAL_ADMIN_PASSWORD_HASH='BCrypt 哈希'
# 使用带鉴权的解读模型时设置；所有后续部署必须保持同一个值
export PORTAL_AI_ENCRYPTION_KEY='至少32字符的长期随机密钥'
# 可选：GitHub API Token，仅用于提高请求额度，不写进前端或数据库
export GITHUB_API_TOKEN='...'
```

生成 BCrypt 哈希后，将结果放入部署环境的 `PORTAL_ADMIN_PASSWORD_HASH`，不要提交密码或哈希。未配置时管理登录会拒绝访问。生产环境使用 HTTPS，在同一域名反向代理 `/api` 到后端 8089；前端所有请求使用相对路径 `/api`。`PORTAL_ALLOWED_ORIGINS` 必须列出实际门户源站，多个值用逗号分隔，不要使用 `*`。本地 `http://localhost:3000` 联调时设置 `PORTAL_COOKIE_SECURE=false`。

```bash
mvn test
mvn -DskipTests package
java -jar target/portal-backend-1.0.0.jar
```

在 `/Users/gaozhiwei/address-plat` 执行 `npm run build`，部署 `dist`。Web 服务器需把前端路由回退到 `index.html`。

## 站外项目自动收录

管理员在 `/admin/sources` 配置来源。支持 GitHub 仓库搜索与公开 HTTPS RSS/Atom 订阅；每个来源可独立设置启停、最多收录条数和执行间隔。GitHub 来源还可设置搜索条件、创建时间窗口和最低星标数。`schema.sql` 初始化一个可编辑、可停用的 GitHub 示例来源；管理员也可以添加任意公开订阅源。

调度器启动 10 秒后检查到期来源，之后每分钟检查一次。每次网络请求最多等待 8 秒，超时记为 `FAILED`，本次直接结束；无立即重试，下次执行时间为本次开始时间加配置间隔。管理员可在页面手动执行。失败或空结果会保留上一期记录。来源配置、上次状态和下次执行时间存于 `external_source`；项目摘要存于独立的 `external_project`，通过 `GET /api/open-source/featured` 返回。后台来源列表、保存、手动执行分别使用 `GET /api/admin/external-sources`、`POST /api/admin/external-sources`、`POST /api/admin/external-sources/{id}/run`，要求管理员会话。

GitHub 搜索选公开非 fork 仓库，并过滤为常见开源许可证；按**收录时总星标数**排序，不是 GitHub Trending 或最近新增星标榜。RSS/Atom 条目展示来源标识及原站链接，许可证信息以原站为准。系统只保存公开元数据和摘要，不复制 README 或代码。

## 项目解读

管理员在 `/admin/project-reading` 配置 OpenAI 兼容的 Chat Completions 接口，填写接口地址、模型名和 API Key 后可先测试连接，再设为公开项目的默认引擎。API Key 使用 AES-GCM 加密后写入 `project_reading_engine_profile`；解密依赖至少 32 个字符的 `PORTAL_AI_ENCRYPTION_KEY`，后续部署必须保持同一密钥。无鉴权的兼容接口可不填 API Key。模型服务地址建议使用 HTTPS；HTTP 会明文传输 API Key。

项目同步后，精选 GitHub 项目会进入解读队列。资料读取器只读取公开仓库中的 README、少量文档、项目清单和一个入口文件，不执行仓库代码；超时或模型返回格式错误时记为失败，不自动重试。管理员可以在工作台查看失败原因并手动重试。生成结果包含阅读路径、带来源链接的设计观察、实践建议和读者反思问题；模型结论仍应由读者通过原文核验。

当前仅支持 GitHub 公开仓库。企业私有仓库的身份验证、团队访问控制、资料保留策略和私有模型路由尚未实现，不得把内部代码加入此公开流程。自有智能体需提供 OpenAI 兼容的 `/v1/chat/completions` 接口，或后续为其增加独立适配器。

## 账户与管理

访客首次打开门户自动获得随机账户 ID、昵称和一次性恢复码。用户应保存账户 ID 与恢复码；换浏览器时用两者恢复同一账户。恢复码只在首次创建或主动重置时展示，服务端只保存哈希。丢失恢复码且原浏览器也不可用时，无法证明账户归属。

项目新建时由服务端记录账户归属。旧项目因原有浏览器 ID 可以伪造，升级后保持只读。管理员核实项目归属后，可调用 `PATCH /api/admin/projects/{id}/owner`，请求体为 `{"publicId":"P-1234567890"}`，每个旧项目只能分配一次。

管理员登录后可在 `/admin/community/reports` 审核举报。内容与 AI 资产的后台写入接口使用同一管理员会话；项目分享仍使用游客账户。会话 Cookie 为 HttpOnly、SameSite=Lax，生产环境需 Secure；写入请求还须通过同源来源校验和 CSRF 校验。

## 上线检查

1. 用浏览器 A 领取账户、保存恢复码、发布项目主题并回复。
2. 用浏览器 B 输入相同账户 ID 和恢复码，确认昵称、项目编辑权限与“我的讨论”一致。
3. 用浏览器 C 验证无法修改 A 的项目、主题和回复。
4. 验证举报、管理员隐藏内容、分页和手机端阅读。
5. 确认 HTTPS、同域 `/api`、`PORTAL_ALLOWED_ORIGINS`、管理员密码哈希和数据库备份均已配置。
