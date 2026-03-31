# 权限控制功能部署说明

## 概述
本次更新为项目导航模块添加了基于所有者的权限控制功能，确保用户只能修改和删除自己创建的项目。

## 功能说明

### 前端功能
1. **用户身份识别**
   - 基于浏览器指纹 + 时间戳生成唯一用户ID
   - 用户信息存储在 localStorage 中
   - 导航栏显示当前用户标识，hover 可查看完整信息

2. **权限控制**
   - 项目卡片显示上传者信息
   - 只有创建者可以看到编辑/删除按钮
   - 新增项目时显示权限提示

3. **用户体验**
   - 紧急推送功能已隐藏（代码保留）
   - 资源广场统计显示优化

### 后端功能
1. **数据模型扩展**
   - Project 表新增 `owner_id` 和 `owner_name` 字段
   - ProjectDTO、Project 实体同步更新

2. **权限校验**
   - Service 层添加权限校验逻辑
   - Controller 层通过请求头传递用户ID
   - 更新和删除操作前校验所有权

3. **API 变更**
   - DELETE `/api/projects/{id}` 新增必需请求头 `X-User-Id`

## 部署步骤

### 1. 数据库迁移

```bash
# 连接到 MySQL 数据库
mysql -u your_username -p your_database

# 执行表结构初始化（如果表不存在）
source /path/to/address-view/src/main/resources/init_project_table.sql

# 如果表已存在，执行字段迁移
source /path/to/address-view/src/main/resources/migration_add_owner_fields.sql
```

### 2. 后端部署

```bash
cd /Users/gaozhiwei/address-view

# 重新编译
mvn clean package -DskipTests

# 启动服务
java -jar target/portal-backend-1.0.0.jar
```

### 3. 前端部署

```bash
cd /Users/gaozhiwei/address-plat

# 安装依赖（如果需要）
npm install

# 构建生产版本
npm run build

# 部署到 Web 服务器（如 Nginx）
# 或使用 npm run preview 预览
```

## 文件变更清单

### 后端文件
- `src/main/java/kd/address/view/entity/Project.java` - 添加所有者字段
- `src/main/java/kd/address/view/dto/ProjectDTO.java` - 添加所有者字段
- `src/main/java/kd/address/view/mapper/ProjectMapper.java` - 更新 SQL 语句
- `src/main/java/kd/address/view/service/ProjectService.java` - 添加权限校验
- `src/main/java/kd/address/view/controller/ProjectController.java` - 添加用户ID请求头
- `src/main/resources/init_project_table.sql` - 表结构初始化脚本（新增）
- `src/main/resources/migration_add_owner_fields.sql` - 字段迁移脚本（新增）

### 前端文件
- `src/api/project.js` - 添加请求拦截器，自动传递用户ID
- `src/utils/userIdentity.js` - 优化用户ID生成逻辑
- `src/components/ProjectCard.vue` - 显示上传者，添加删除按钮
- `src/components/ProjectForm.vue` - 添加权限提示
- `src/components/ProjectList.vue` - 传递删除事件
- `src/components/LayoutHeader.vue` - 显示用户信息
- `src/views/HomeView.vue` - 传递删除事件
- `src/views/LayoutView.vue` - 隐藏紧急推送
- `src/views/ProjectsView.vue` - 优化统计显示

## 注意事项

1. **数据兼容性**
   - 如果数据库中已有项目数据，新增字段会为 NULL
   - 建议：可以为现有项目分配一个默认所有者，或者允许用户认领

2. **安全性说明**
   - 当前权限控制基于前端传递的用户ID
   - 适合内部平台使用，不适合高安全场景
   - 如需更高安全性，建议接入完整的认证系统（如腾讯云 CloudBase）

3. **浏览器存储**
   - 用户ID存储在 localStorage 中
   - 清除浏览器缓存会导致用户身份变更
   - 同一浏览器多个标签页会共享同一用户身份

4. **紧急推送**
   - 功能已在前端隐藏，但代码保留
   - 如需恢复，取消 `LayoutView.vue` 中的注释即可

## 测试建议

1. **权限测试**
   - 使用两个不同浏览器/无痕模式创建项目
   - 验证只能看到自己创建项目的编辑/删除按钮
   - 尝试修改/删除他人项目，应收到权限错误

2. **用户体验测试**
   - 验证上传者信息正确显示
   - 检查权限提示文案是否清晰
   - 确认导航栏用户信息正确显示

3. **边界情况**
   - 清除 localStorage 后重新创建项目
   - 尝试在请求头不包含用户ID的情况下删除项目
   - 验证空用户ID和空项目ID的处理

## 回滚方案

如果需要回滚到之前的版本：

### 后端回滚
```bash
# 移除数据库字段
ALTER TABLE project DROP COLUMN owner_id;
ALTER TABLE project DROP COLUMN owner_name;

# 恢复旧代码
git checkout <commit-hash>
```

### 前端回滚
```bash
# 恢复旧代码
git checkout <commit-hash>
```

## 技术支持

如有问题，请联系技术支持团队。
