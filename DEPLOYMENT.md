# AI Financial Agent - Docker 部署指南

## 目录结构

```
ai-agent/
├── docker-compose.yml          # Docker Compose 编排文件
├── Dockerfile.backend          # 后端 Dockerfile
├── Dockerfile.frontend         # 前端 Dockerfile
├── nginx.conf                  # Nginx 配置
├── deploy.sh                   # 一键部署脚本
├── .dockerignore              # Docker 忽略文件
└── src/main/resources/
    ├── application.yml         # 基础配置
    ├── application-prod.yml    # 生产环境配置（已配置 pgvector）
    └── document/               # 知识库文档目录
```

## 架构说明

```
┌─────────────┐      ┌─────────────┐      ┌──────────────────┐
│   Nginx     │─────▶│ Spring Boot │─────▶│ PostgreSQL       │
│  (前端托管)  │      │  (后端服务)  │      │ + pgvector扩展   │
│   Port 80   │      │  Port 8123  │      │ localhost:15432  │
└─────────────┘      └─────────────┘      └──────────────────┘
```

**说明**：项目已配置连接到你现有的 pgvector 数据库（localhost:15432）

## 快速部署（推荐）

### 方法一：一键部署脚本

```bash
# 1. 上传项目到服务器（建议位置：/opt/ai-agent）
cd /opt
git clone <你的仓库地址> ai-agent
cd ai-agent

# 2. 给脚本执行权限
chmod +x deploy.sh

# 3. 运行一键部署
bash deploy.sh
```

脚本会自动完成：
- ✓ 检查 Docker 环境
- ✓ 初始化 pgvector 扩展
- ✓ 构建并启动服务
- ✓ 验证部署状态

---

## 手动部署步骤

### 1. 前置要求

- ✅ Docker 20.10+
- ✅ Docker Compose 2.0+
- ✅ 服务器至少 4GB 内存
- ✅ PostgreSQL + pgvector 已运行（localhost:15432）

### 2. 初始化 pgvector 扩展

```bash
# 启用 vector 扩展
docker exec -it pgvector psql -U postgres -d postgres -c "CREATE EXTENSION IF NOT EXISTS vector;"

# 验证安装
docker exec -it pgvector psql -U postgres -d postgres -c "SELECT * FROM pg_extension WHERE extname = 'vector';"
```

### 3. 准备知识库文档（可选）

```bash
# 将 Markdown 文档放到 document 目录
mkdir -p src/main/resources/document
# 复制你的 .md 文件到该目录
```

### 4. 启动服务

```bash
# 构建并启动所有服务
docker-compose up -d --build

# 查看构建日志（首次构建需要 5-10 分钟）
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f backend
docker-compose logs -f frontend
```

### 5. 验证部署

**访问地址**：
- 前端页面：http://117.72.103.15
- 后端 API：http://117.72.103.15:8123/api
- API 文档：http://117.72.103.15:8123/api/doc.html

**检查日志**：
```bash
# 查看后端是否成功连接 PgVector
docker logs ai-agent-backend | grep "使用 PgVectorStore"

# 应该看到：✓ 使用 PgVectorStore（持久化存储）
```

### 6. 验证向量数据库

```bash
# 进入 PostgreSQL 容器
docker exec -it pgvector psql -U postgres -d postgres

# 查看向量存储表（自动创建）
\dt vector_store

# 查看表结构
\d vector_store

# 查看已存储的文档数量
SELECT COUNT(*) FROM vector_store;
```

## 常用命令

```bash
# 停止服务
docker-compose down

# 停止并删除数据
docker-compose down -v

# 重启特定服务
docker-compose restart backend

# 查看运行状态
docker-compose ps

# 进入容器调试
docker exec -it ai-agent-backend /bin/sh
```

## 数据持久化

### 持久化目录

- `postgres_data/` - PostgreSQL 数据（包括向量数据）
- `agent-memory/` - 对话记忆文件
- `downloads/` - 下载的资源文件

### 备份数据库

```bash
# 备份
docker exec ai-agent-postgres pg_dump -U ai_agent_user ai_agent_db > backup.sql

# 恢复
cat backup.sql | docker exec -i ai-agent-postgres psql -U ai_agent_user -d ai_agent_db
```

## 性能优化

### 1. PostgreSQL 调优

编辑 `docker-compose.yml`，在 `postgres` 服务下添加：

```yaml
command:
  - postgres
  - -c
  - shared_buffers=256MB
  - -c
  - max_connections=100
  - -c
  - work_mem=16MB
```

### 2. JVM 内存调优

编辑 `docker-compose.yml`，在 `backend` 服务下修改：

```yaml
environment:
  JAVA_OPTS: "-Xms1g -Xmx2g -XX:+UseG1GC"
```

### 3. Nginx 缓存优化

已在 `nginx.conf` 中配置 Gzip 压缩和静态资源缓存。

## 安全建议

1. **修改默认密码**：更改 `docker-compose.yml` 中的数据库密码
2. **使用环境变量**：通过 `.env` 文件管理敏感信息（不要提交到 Git）
3. **启用 HTTPS**：生产环境建议使用 Nginx + Let's Encrypt
4. **防火墙配置**：仅开放 80、443 端口，数据库端口不对外暴露

## 常见问题排查

### 1. 后端无法连接数据库

**症状**：后端日志报错 `Connection refused` 或 `FATAL: password authentication failed`

**解决方法**：
```bash
# 1. 检查 pgvector 容器是否运行
docker ps | grep pgvector

# 2. 测试数据库连接
docker exec -it pgvector psql -U postgres -d postgres -c "SELECT 1;"

# 3. 检查端口映射
docker port pgvector

# 4. 查看后端容器网络（应使用 host 模式）
docker inspect ai-agent-backend | grep NetworkMode
```

### 2. 前端无法访问后端 API

**症状**：浏览器控制台报错 502 Bad Gateway

**解决方法**：
```bash
# 1. 检查后端是否监听在 8123 端口
netstat -tlnp | grep 8123

# 2. 测试后端 API（在服务器上）
curl http://localhost:8123/api/doc.html

# 3. 检查 nginx 配置中的代理地址
docker exec ai-agent-frontend cat /etc/nginx/conf.d/default.conf | grep proxy_pass
```

### 3. pgvector 扩展未启用

**症状**：后端日志报错 `type "vector" does not exist`

**解决方法**：
```bash
# 手动启用扩展
docker exec -it pgvector psql -U postgres -d postgres -c "CREATE EXTENSION IF NOT EXISTS vector;"

# 重启后端服务
docker-compose restart backend
```

### 4. 文档未加载到向量数据库

**症状**：日志显示 `⚠ 未找到文档，向量数据库为空`

**解决方法**：
```bash
# 1. 检查文档目录
ls -la src/main/resources/document/

# 2. 添加测试文档
echo "# 测试文档\n这是一个测试文档。" > src/main/resources/document/test.md

# 3. 重新构建并启动
docker-compose up -d --build

# 4. 查看加载日志
docker logs ai-agent-backend | grep "已加载"
```

### 5. 80 端口被占用

**症状**：前端容器启动失败，报错 `port is already allocated`

**解决方法**：
```bash
# 查看占用进程
netstat -tlnp | grep :80

# 方案 A：停止占用 80 端口的服务
systemctl stop nginx  # 或其他服务

# 方案 B：修改前端端口
# 编辑 docker-compose.yml，将 "80:80" 改为 "8080:80"
```

## 升级部署

```bash
# 拉取最新代码
git pull

# 重新构建并启动
docker-compose up -d --build

# 查看更新状态
docker-compose logs -f backend
```

## 监控和日志

```bash
# 实时日志
docker-compose logs -f --tail=100

# 查看容器资源使用
docker stats

# 导出日志到文件
docker-compose logs > logs.txt
```

## 生产环境注意事项

1. 使用反向代理（Nginx/Caddy）处理 SSL/TLS
2. 配置日志轮转避免磁盘占满
3. 设置自动重启策略：`restart: unless-stopped`
4. 定期备份 PostgreSQL 数据
5. 监控容器健康状态和资源使用

## 技术栈

- **后端**：Spring Boot 3.4 + Java 21 + Spring AI
- **前端**：Vue.js 3 + Vite
- **数据库**：PostgreSQL 16 + pgvector
- **Web 服务器**：Nginx
- **容器化**：Docker + Docker Compose

## 联系支持

如有问题，请查看项目文档或提交 Issue。
