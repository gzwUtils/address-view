# 门户与社区部署

## 数据库升级

先备份 MySQL 数据库。已有库依次执行以下脚本，每个迁移脚本只执行一次：

```bash
mysql -u "$DB_USERNAME" -p protal < src/main/resources/migration_guest_identity.sql
mysql -u "$DB_USERNAME" -p protal < src/main/resources/migration_community_forum.sql
```

新库须先使用现有项目初始化脚本创建 `project` 等基础表，再执行上述迁移。`migration_guest_identity.sql` 添加 `owner_account_id`，不能重复执行。社区脚本预置“项目分享”“技术交流”“闲聊”三个板块。部署过程不会自动迁移既有项目归属。

## 服务配置

服务端通过环境变量读取连接和安全配置：

```bash
export DB_URL='jdbc:mysql://localhost:13306/protal?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true'
export DB_USERNAME='root'
export DB_PASSWORD='数据库密码'
export PORTAL_ALLOWED_ORIGINS='https://portal.example.com'
export PORTAL_COOKIE_SECURE=true
export PORTAL_ADMIN_PASSWORD_HASH='BCrypt 哈希'
```

生成 BCrypt 哈希后，将结果放入部署环境的 `PORTAL_ADMIN_PASSWORD_HASH`，不要提交密码或哈希。未配置时管理登录会拒绝访问。生产环境使用 HTTPS，在同一域名反向代理 `/api` 到后端 8089；前端所有请求使用相对路径 `/api`。`PORTAL_ALLOWED_ORIGINS` 必须列出实际门户源站，多个值用逗号分隔，不要使用 `*`。本地 `http://localhost:3000` 联调时设置 `PORTAL_COOKIE_SECURE=false`。

```bash
mvn test
mvn -DskipTests package
java -jar target/portal-backend-1.0.0.jar
```

在 `/Users/gaozhiwei/address-plat` 执行 `npm run build`，部署 `dist`。Web 服务器需把前端路由回退到 `index.html`。

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
