#!/bin/bash

# AI Financial Agent - 一键部署脚本
# 使用方法：bash deploy.sh

set -e  # 遇到错误立即退出

echo "=========================================="
echo "  AI Financial Agent - 部署脚本"
echo "=========================================="
echo ""

# 检查 Docker 是否安装
if ! command -v docker &> /dev/null; then
    echo "❌ Docker 未安装，请先安装 Docker"
    exit 1
fi

# 检查 Docker Compose 是否安装
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose 未安装，请先安装 Docker Compose"
    exit 1
fi

echo "✓ Docker 环境检查通过"
echo ""

# 第一步：初始化 pgvector 扩展
echo "=========================================="
echo "第 1 步：初始化 pgvector 扩展"
echo "=========================================="
echo "正在检查 pgvector 容器..."

if docker ps | grep -q "pgvector"; then
    echo "✓ 找到 pgvector 容器"

    # 创建 vector 扩展
    echo "正在启用 vector 扩展..."
    docker exec -it pgvector psql -U postgres -d postgres -c "CREATE EXTENSION IF NOT EXISTS vector;" || true

    # 验证安装
    echo "验证 vector 扩展..."
    if docker exec -it pgvector psql -U postgres -d postgres -c "SELECT * FROM pg_extension WHERE extname = 'vector';" | grep -q "vector"; then
        echo "✓ pgvector 扩展已成功启用"
    else
        echo "❌ pgvector 扩展启用失败"
        exit 1
    fi
else
    echo "⚠️  未找到名为 'pgvector' 的容器"
    echo "请确认你的 PostgreSQL 容器名称是否为 'pgvector'"
    read -p "是否继续部署？(y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

echo ""

# 第二步：检查文档目录
echo "=========================================="
echo "第 2 步：检查知识库文档"
echo "=========================================="

if [ -d "src/main/resources/document" ] && [ "$(ls -A src/main/resources/document)" ]; then
    DOC_COUNT=$(find src/main/resources/document -type f -name "*.md" | wc -l)
    echo "✓ 找到 $DOC_COUNT 个 Markdown 文档"
else
    echo "⚠️  未找到知识库文档"
    echo "你可以稍后将 Markdown 文档放到 src/main/resources/document/ 目录"
fi

echo ""

# 第三步：使用服务器 Maven 构建后端 JAR
echo "=========================================="
echo "第 3 步：使用 Maven 构建后端 JAR"
echo "=========================================="

# 检查 Maven 是否安装
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven 未安装或未配置环境变量"
    exit 1
fi

# 检查 Java 版本
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "⚠️  警告：需要 Java 21+，当前版本：$JAVA_VERSION"
    read -p "是否继续？(y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

echo "✓ Maven 版本：$(mvn -version | head -n 1)"
echo "✓ Java 版本：$(java -version 2>&1 | head -n 1)"
echo ""

# 清理并构建
echo "正在清理旧构建..."
mvn clean

echo ""
echo "正在构建 JAR 包（完全跳过测试编译和执行）..."
mvn package -Dmaven.test.skip=true -B

if [ ! -f target/*.jar ]; then
    echo "❌ JAR 构建失败"
    exit 1
fi

echo "✓ JAR 构建成功：$(ls -lh target/*.jar | awk '{print $9, $5}')"
echo ""

# 第四步：构建并启动服务
echo "=========================================="
echo "第 4 步：构建并启动 Docker 服务"
echo "=========================================="
echo "这可能需要 2-3 分钟..."
echo ""

# 停止旧服务
if docker ps -a | grep -q "ai-agent-backend\|ai-agent-frontend"; then
    echo "停止旧服务..."
    docker-compose down
fi

# 构建并启动
echo "开始构建镜像..."
docker-compose build

echo ""
echo "启动服务..."
docker-compose up -d

echo ""
echo "等待服务启动（30秒）..."
sleep 30

echo ""

# 第五步：验证部署
echo "=========================================="
echo "第 5 步：验证部署状态"
echo "=========================================="

# 检查容器状态
if docker ps | grep -q "ai-agent-backend"; then
    echo "✓ 后端服务已启动"
else
    echo "❌ 后端服务启动失败"
    docker-compose logs backend
    exit 1
fi

if docker ps | grep -q "ai-agent-frontend"; then
    echo "✓ 前端服务已启动"
else
    echo "❌ 前端服务启动失败"
    docker-compose logs frontend
    exit 1
fi

# 检查后端日志
echo ""
echo "检查后端日志..."
if docker logs ai-agent-backend 2>&1 | grep -q "使用 PgVectorStore"; then
    echo "✓ 后端已成功连接到 PgVector 数据库"
else
    echo "⚠️  未检测到 PgVector 连接信息，查看详细日志："
    docker logs ai-agent-backend --tail 50
fi

echo ""

# 第六步：显示访问信息
echo "=========================================="
echo "  部署完成！"
echo "=========================================="
echo ""
echo "访问地址："
echo "  前端页面：http://117.72.103.15"
echo "  API 文档：http://117.72.103.15:8123/api/doc.html"
echo ""
echo "常用命令："
echo "  查看日志：docker-compose logs -f"
echo "  重启服务：docker-compose restart"
echo "  停止服务：docker-compose down"
echo ""
echo "如有问题，请查看完整日志："
echo "  docker-compose logs backend"
echo ""
