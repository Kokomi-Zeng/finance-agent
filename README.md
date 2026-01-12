<div align="center">

# FinAgent4J - 基于 Java Spring AI的金融智能 Agent 系统

一个基于 Spring AI 和 Vue 3 的智能理财管理应用，提供专业的投资建议、市场分析和理财咨询服务。

RAG / ReAct Agent / SSE 流式对话 / Tool Calling / PgVector / 多轮记忆 / Spring AI

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.2.47-green.svg)](https://vuejs.org/)

![example3](./example3.svg)

</div>

---

## 目录

- [功能特性](#功能特性)
- [技术栈](#技术栈)
- [项目架构](#项目架构)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [Docker 部署](#docker-部署)
- [API 文档](#api-文档)
- [项目结构](#项目结构)
- [常见问题](#常见问题)

---

## 在线体验

想直接体验？Please contact the author: 2845728164@qq.com to get the demo website link.

---

## 功能特性

### 智能对话系统

- **多轮对话记忆**：基于文件的持久化存储（`FileBasedChatMemory`），重启后不丢失历史对话
- **会话管理**：支持创建新对话、查看历史对话、切换会话
- **流式响应（SSE）**：实时显示 AI 思考过程和回复内容
- **对话记忆压缩**：智能过滤重复消息、截断长文本，减少 70-80% 的上下文长度

### RAG 知识库

- **向量数据库**：支持 PgVector（PostgreSQL + pgvector 扩展）
- **智能检索**：基于语义相似度的文档检索
- **文档管理**：支持 Markdown 格式的知识库文档
- **关键词增强**：使用 `KeywordEnricher` 为文档添加关键词元数据，提高检索准确度

### 智能体（Agent）

- **ReAct 模式**：自主规划、推理、行动循环
- **工具调用**：集成多种工具（网络搜索、文件下载、网页抓取等）
- **步骤可视化**：前端实时展示智能体的思考和行动过程
- **智能规划**：最多 6 步完成任务（1-2 收集信息，3-4 处理分析，5 整理，6 返回结果）

### 工具集成

| 工具名称 | 功能描述 |
|---------|---------|
| `searchWebTool` | 搜索引擎（Brave Search API） |
| `downloadResource` | 下载文件 |
| `scrapeWebPage` | 网页内容抓取 |
| `saveMarkdownTool` | 保存 Markdown 文件 |
| `doTerminate` | 终止智能体（返回最终结果） |

### 前端界面

- **金融风格配色**：深蓝 + 金色主题
- **响应式设计**：支持桌面和移动端
- **用户协议弹窗**：完整的用户协议和隐私政策
- **会话持久化**：基于 `localStorage`，刷新页面不丢失对话

---

## 技术栈

### 后端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 21 | LTS 版本 |
| Spring Boot | 3.4.4 | 主框架 |
| Spring AI | 1.0.0 | AI 集成框架 |
| PostgreSQL | 14+ | 关系型数据库 |
| PgVector | 0.5.0+ | PostgreSQL 向量扩展 |

### 前端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.2.47 | 渐进式框架 |
| Vue Router | 4.1.6 | 路由管理 |
| Vite | 4.3.9 | 构建工具 |
| Axios | 1.3.6 | HTTP 客户端 |
| Marked | 9.1.6 | Markdown 渲染 |

### 部署技术

- **Docker** - 容器化部署
- **Nginx** - Web 服务器 + 反向代理

---

## 项目架构

```
┌─────────────┐         ┌─────────────┐         ┌─────────────┐
│   浏览器     │ ──────> │    Nginx    │ ──────> │  Spring Boot │
│  (Vue.js)   │         │  (前端容器)  │         │   (后端容器)  │
└─────────────┘         └─────────────┘         └─────────────┘
                               │                        │
                               │                        │
                               v                        v
                        静态文件服务              ┌─────────────┐
                         + API 反向代理           │ PostgreSQL  │
                                                  │ + PgVector  │
                                                  └─────────────┘
```

### 核心组件

1. **FinancialManagementApp** - 理财应用主类，管理智能体生命周期
2. **ToolCallAgent** - 工具调用智能体，实现 ReAct 循环
3. **FileBasedChatMemory** - 基于文件的对话记忆存储
4. **VectorStoreConfig** - 向量数据库配置
5. **KeywordEnricher** - 关键词增强器，为文档添加元数据，提高检索准确度

---

## 快速开始

### 前置要求

- **Java 21+** - [下载地址](https://www.oracle.com/tw/java/technologies/downloads/#jdk21-windows)
- **Maven 3.9+** - [下载地址](https://maven.apache.org/download.cgi)
- **Node.js 18+** - [下载地址](https://nodejs.org/)
- **PostgreSQL 14+** （可选）- 使用 PgVector 时需要

### 本地开发

#### 1. 克隆项目

```bash
git clone https://github.com/Kokomi-Zeng/finance-agent.git
```

#### 2. 配置后端

复制配置文件模板：

```bash
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
```

编辑 `application-local.yml`，填入真实密钥：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://your-host:15432/postgres?currentSchema=public
    username: postgres
    password: your-password

  ai:
    dashscope:
      api-key: sk-your-dashscope-api-key

search-api:
  api-key: your-search-api-key
```

#### 3. 启动后端

```bash
mvn spring-boot:run
```

后端将在 `http://localhost:8123` 启动。

#### 4. 启动前端

```bash
cd ai-agent-frontend
npm install
npm run dev
```

前端将在 `http://localhost:5173` 启动。

#### 5. 访问应用

打开浏览器访问 `http://localhost:5173`

---

## 配置说明

### 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `DATABASE_URL` | 数据库连接地址 | `jdbc:postgresql://localhost:15432/postgres` |
| `DATABASE_USERNAME` | 数据库用户名 | `postgres` |
| `DATABASE_PASSWORD` | 数据库密码 | `changeme` |
| `DASHSCOPE_API_KEY` | 阿里云 DashScope API Key | - |
| `SEARCH_API_KEY` | 搜索 API Key | - |
| `SPRING_PROFILES_ACTIVE` | 激活的配置文件 | `local` |

### 配置文件说明

- **`application.yml`** - 基础配置（已隐藏敏感信息）
- **`application-prod.yml`** - 生产环境配置
- **`application-local.yml`** - 本地开发配置（包含真实密钥，不会提交到 GitHub）
- **`application-local.yml.example`** - 配置文件模板

### 向量数据库配置

#### 使用 PgVector（推荐）

1. 安装 PostgreSQL 和 pgvector 扩展
2. 创建数据库并启用扩展：

```sql
CREATE DATABASE postgres;
\c postgres
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
```

3. 配置数据库连接（见上方环境变量）

#### 使用 SimpleVectorStore（内存存储）

如果不配置数据源，系统会自动使用内存存储（重启后数据丢失）。

---

## Docker 部署

### 一键部署 

```bash
# 1. 克隆项目
git clone https://github.com/Kokomi-Zeng/finance-agent
cd finance-agent

# 2. 修正脚本行尾符（Linux/Mac）
sed -i 's/\r$//' deploy.sh
chmod +x deploy.sh

# 3. 执行部署
bash deploy.sh
```

### 手动部署

#### 1. 拉取 Docker 镜像

```bash
# 后端 JRE 镜像
docker pull eclipse-temurin:21-jre-jammy

# 前端构建镜像
docker pull node:18-alpine

# 前端运行镜像
docker pull nginx:alpine
```

#### 2. 配置环境变量

编辑 `docker-compose.yml`，添加环境变量：

```yaml
services:
  backend:
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DATABASE_URL: jdbc:postgresql://host.docker.internal:15432/postgres
      DATABASE_PASSWORD: your-password
      DASHSCOPE_API_KEY: sk-your-api-key
      SEARCH_API_KEY: your-search-key
```

#### 3. 构建并启动

```bash
# 构建镜像
docker-compose build

# 启动服务
docker-compose up -d

# 查看日志
docker-compose logs -f
```

#### 4. 访问应用

- **前端页面**：http://your-server-ip
- **API 文档**：http://your-server-ip:8123/api/doc.html

### Docker Compose 配置说明

```yaml
services:
  backend:
    network_mode: "host"  # 使用宿主机网络，访问本地 PgVector
    volumes:
      - ./agent-memory:/app/agent-memory     # 对话记忆持久化
      - ./downloads:/app/downloads           # 文件下载目录

  frontend:
    ports:
      - "80:80"           # 前端服务端口
```

---

## API 文档

### RESTful API 端点

| 端点 | 方法 | 说明 | 参数 |
|------|------|------|------|
| `/ai/financial/chat/sync` | GET | 同步对话 | `message`, `chatId` |
| `/ai/financial/chat/sse` | GET | SSE 流式对话 | `message`, `chatId` |
| `/ai/financial/agent` | GET | 智能体模式（SSE） | `message`, `chatId` |
| `/ai/financial/report` | GET | 生成理财报告 | `message`, `chatId` |

### SSE 响应格式

智能体模式返回的 SSE 消息格式：

```json
// 思考过程
data:{"type":"thinking","step":1,"content":"正在搜索相关信息..."}

// 最终结果开始
data:{"type":"result_start"}

// 结果内容（流式）
data:{"type":"result_chunk","content":"你好"}

// 结果结束
data:{"type":"result_end"}
```

---

## 项目结构

```
ai-agent/
├── src/main/java/com/ai/agent/
│   ├── app/                          # 应用主类
│   │   └── FinancialManagementApp.java  # 理财应用主类
│   ├── agent/                        # 智能体相关
│   │   ├── BaseAgent.java            # 智能体基类
│   │   ├── ReActAgent.java           # ReAct 智能体
│   │   └── ToolCallAgent.java        # 工具调用智能体
│   ├── advisor/                      # Advisor（暂未使用）
│   ├── rag/                          # RAG 相关
│   │   ├── DocumentLoader.java       # 文档加载器
│   │   ├── TokenTextSplitter.java    # 文本分割器
│   │   ├── KeywordEnricher.java      # 关键词增强器，加载文档时添加关键词元数据
│   │   └── VectorStoreConfig.java    # 向量数据库配置
│   ├── tools/                        # 工具类
│   │   ├── SearchWebTool.java        # 网络搜索工具
│   │   ├── ResourceDownloadTool.java # 文件下载工具
│   │   └── ScrapeWebPageTool.java    # 网页抓取工具
│   ├── controller/                   # 控制器
│   │   └── AiController.java         # AI 接口控制器
│   ├── chatmemory/                  # 对话记忆
│   │   └── FileBasedChatMemory.java  # 基于文件的对话记忆
│   └── config/                       # 配置类
├── src/main/resources/
│   ├── application.yml               # 主配置文件
│   ├── application-prod.yml          # 生产环境配置
│   ├── application-local.yml.example # 本地配置模板
│   └── document/                     # 知识库文档目录
├── ai-agent-frontend/               # 前端项目
│   ├── src/
│   │   ├── views/
│   │   │   ├── Home.vue             # 首页
│   │   │   └── FinancialAgent.vue   # 智能体页面
│   │   ├── components/
│   │   │   ├── ChatRoom.vue         # 聊天组件
│   │   │   └── AppFooter.vue        # 页脚组件
│   │   ├── api/
│   │   │   └── index.js             # API 封装
│   │   └── router/
│   │       └── index.js             # 路由配置
│   └── index.html                   # HTML 入口
├── docker-compose.yml               # Docker Compose 配置
├── Dockerfile.backend               # 后端 Dockerfile
├── Dockerfile.frontend              # 前端 Dockerfile
├── nginx.conf                       # Nginx 配置
├── deploy.sh                        # 一键部署脚本
└── README.md                        # 项目文档
```

---

## 常见问题

### 1. 如何获取 DashScope API Key？

访问 [阿里云 DashScope 控制台](https://dashscope.console.aliyun.com/)，创建 API Key。

### 2. 如何获取 Search API Key？

访问 [Search API](https://www.searchapi.io/)，创建 API Key。

### 3. 为什么向量数据库连接失败？

检查：
- PostgreSQL 是否启动
- pgvector 扩展是否安装（`CREATE EXTENSION IF NOT EXISTS vector;`）
- 数据库连接配置是否正确

### 4. Docker 部署时端口冲突怎么办？

编辑 `docker-compose.yml`，修改端口映射：

```yaml
services:
  backend:
    ports:
      - "8124:8123"  # 修改为其他端口
```

### 5. 前端请求后端 301 重定向？

检查 `nginx.conf` 是否包含以下配置：

```nginx
location /api/ {
    proxy_pass http://172.17.0.1:8123/api/;
    proxy_redirect off;           # 禁止重定向
    proxy_http_version 1.1;       # SSE 必需
    proxy_set_header Connection "";
}
```

### 6. 如何清除对话记忆？

删除 `agent-memory/` 目录下的对应文件：

```bash
rm -rf agent-memory/<chatId>.json
```
