# 问题排查指南

## 当前问题
访问 `http://101.42.236.45:8089/api/projects/categories` 和 `http://101.42.236.45:8089/api/projects` 时报错：
- 跨域错误 (CORS)
- 403 错误

## 已修复的问题

### 1. CORS 配置
- ✅ 已添加 `X-User-Id` 到允许的请求头列表
- 文件: `src/main/java/kd/address/view/config/WebConfig.java`

### 2. 权限验证优化
- ✅ DELETE 接口的 `X-User-Id` 请求头改为可选
- ✅ 服务端允许删除没有所有者的旧数据
- 文件: `src/main/java/kd/address/view/controller/ProjectController.java`
- 文件: `src/main/java/kd/address/view/service/ProjectService.java`

### 3. 异常处理
- ✅ 添加 `SecurityException` 的全局异常处理器
- 文件: `src/main/java/kd/address/view/common/GlobalExceptionHandler.java`

## 可能的剩余问题

### 数据库连接问题
当前配置连接到 `localhost:13306`，但服务部署在远程服务器上。

**检查步骤：**

1. 登录到服务器 `101.42.236.45`
```bash
ssh user@101.42.236.45
```

2. 检查 MySQL 是否运行在 13306 端口
```bash
netstat -tlnp | grep 13306
# 或
ss -tlnp | grep 13306
```

3. 检查 MySQL 服务状态
```bash
systemctl status mysql
# 或
docker ps | grep mysql
```

4. 测试数据库连接
```bash
mysql -h 127.0.0.1 -P 13306 -u root -p
# 密码: gzw941120
```

### 解决方案

**方案 A：数据库运行在本地**
如果 MySQL 就在服务器本地的 13306 端口，配置正确，无需修改。

**方案 B：数据库在其他服务器**
如果 MySQL 在其他地方，需要修改配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://数据库IP:端口/protal?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: gzw941120
```

**方案 C：使用 Docker**
如果 MySQL 在 Docker 容器中运行：

```yaml
spring:
  datasource:
    url: jdbc:mysql://mysql:3306/protal?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
```

并在 docker-compose 中配置网络。

## 数据库初始化

如果是全新数据库，使用统一脚本创建全部表：

```bash
mysql -h 127.0.0.1 -P 13306 -u "$DB_USERNAME" -p protal < src/main/resources/schema.sql
```

## 部署步骤

### 1. 重新编译
```bash
cd /Users/gaozhiwei/address-view
mvn clean package -DskipTests
```

### 2. 上传到服务器
```bash
scp target/portal-backend-1.0.0.jar user@101.42.236.45:/path/to/deploy/
```

### 3. 在服务器上启动
```bash
# 停止旧服务
pkill -f portal-backend

# 启动新服务
java -jar /path/to/portal-backend-1.0.0.jar

# 或使用 nohup 后台运行
nohup java -jar /path/to/portal-backend-1.0.0.jar > app.log 2>&1 &
```

### 4. 验证服务
```bash
# 检查端口监听
netstat -tlnp | grep 8089

# 测试 API
curl http://127.0.0.1:8089/api/projects/categories
```

## 日志查看

如果启动失败，查看日志：

```bash
# 如果使用 nohup
tail -f app.log

# 或查看 Spring Boot 默认日志
tail -f logs/spring.log
```

## 防火墙检查

确保 8089 端口开放：

```bash
# Ubuntu/Debian
sudo ufw allow 8089/tcp

# CentOS/RHEL
sudo firewall-cmd --permanent --add-port=8089/tcp
sudo firewall-cmd --reload

# 检查防火墙状态
sudo firewall-cmd --list-all
```

## 测试 API

```bash
# 测试分类接口
curl http://101.42.236.45:8089/api/projects/categories

# 测试项目列表
curl http://101.42.236.45:8089/api/projects

# 测试删除（带用户ID）
curl -X DELETE http://101.42.236.45:8089/api/projects/1 \
  -H "X-User-Id: user-test123"
```

## 前端配置

确保前端 API 地址正确：

```javascript
// src/api/project.js
baseURL: 'http://101.42.236.45:8089/api'
```

## 联系支持

如果问题仍未解决，请提供：
1. 服务器日志输出
2. 数据库连接状态
3. 防火墙规则
4. curl 测试结果
