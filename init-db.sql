-- 初始化 PostgreSQL 数据库并启用 pgvector 扩展

-- 启用 pgvector 扩展（用于向量存储）
CREATE EXTENSION IF NOT EXISTS vector;

-- 创建向量存储表（Spring AI PgVectorStore 会自动创建，这里仅作备份参考）
-- CREATE TABLE IF NOT EXISTS vector_store (
--     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
--     content TEXT,
--     metadata JSONB,
--     embedding vector(1536)
-- );

-- 创建索引以提高查询性能
-- CREATE INDEX IF NOT EXISTS vector_store_embedding_idx ON vector_store
-- USING hnsw (embedding vector_cosine_ops);

-- 验证扩展是否安装成功
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector') THEN
        RAISE NOTICE '✓ pgvector 扩展已成功安装';
    ELSE
        RAISE EXCEPTION '✗ pgvector 扩展安装失败';
    END IF;
END $$;
