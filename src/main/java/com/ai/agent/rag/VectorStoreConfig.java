package com.ai.agent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * 向量数据库配置（支持 SimpleVectorStore 和 PgVectorStore）
 */
@Configuration
public class VectorStoreConfig {

    @Resource
    private DocumentLoader documentLoader;

    @Resource
    private TokenTextSplitter tokenTextSplitter;

    @Resource
    private KeywordEnricher keywordEnricher;

    @Bean
    VectorStore appVectorStore(
            EmbeddingModel dashscopeEmbeddingModel,
            @org.springframework.beans.factory.annotation.Autowired(required = false) JdbcTemplate jdbcTemplate) {
        VectorStore vectorStore;

        // 如果注入了 JdbcTemplate，使用 PgVectorStore；否则使用 SimpleVectorStore
        if (jdbcTemplate != null) {
            try {
                // 测试数据库连接
                String dbUrl = jdbcTemplate.execute((java.sql.Connection conn) -> conn.getMetaData().getURL());
                System.out.println("========================================");
                System.out.println("✓ 数据库连接成功");
                System.out.println("  数据库地址: " + dbUrl);

                // 使用 PgVectorStore（PostgreSQL + pgvector）
                vectorStore = PgVectorStore.builder(jdbcTemplate, dashscopeEmbeddingModel)
                        .initializeSchema(true)  // 自动创建表结构
                        .build();
                System.out.println("✓ 使用 PgVectorStore（远程持久化存储）");
                System.out.println("========================================");
            } catch (Exception e) {
                System.err.println("========================================");
                System.err.println("❌ 数据库连接失败，回退到 SimpleVectorStore");
                System.err.println("  错误: " + e.getMessage());
                System.err.println("========================================");
                vectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel).build();
            }
        } else {
            System.out.println("⚠ 未检测到数据源配置，使用 SimpleVectorStore（内存存储）");
            vectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel).build();
        }

        // 加载文档
        List<Document> documentList = documentLoader.loadMarkdowns();

        if (documentList != null && !documentList.isEmpty()) {
            // 过滤出需要加载的新文档
            List<Document> documentsToLoad = documentList;

            if (jdbcTemplate != null) {
                // 使用 PgVectorStore，检查哪些文档已存在（根据 filename 元数据）
                try {
                    // 获取数据库中已存在的文件名列表
                    List<String> existingFilenames = jdbcTemplate.queryForList(
                        "SELECT DISTINCT metadata->>'filename' FROM vector_store WHERE metadata->>'filename' IS NOT NULL",
                        String.class
                    );

                    if (!existingFilenames.isEmpty()) {
                        // 过滤出未在数据库中的文档
                        documentsToLoad = documentList.stream()
                            .filter(doc -> {
                                Object filenameObj = doc.getMetadata().get("filename");
                                if (filenameObj == null) {
                                    return true; // 没有 filename 元数据，视为新文档
                                }
                                String filename = filenameObj.toString();
                                return !existingFilenames.contains(filename);
                            })
                            .toList();

                        int skippedCount = documentList.size() - documentsToLoad.size();
                        System.out.println("✓ 数据库中已有 " + skippedCount + " 个文档，跳过加载");
                        System.out.println("✓ 发现 " + documentsToLoad.size() + " 个新文档需要加载");
                    }
                } catch (Exception e) {
                    // 查询失败（表可能不存在），加载所有文档
                    System.out.println("⚠ 无法检查已有文档，将加载所有文档");
                }
            }

            // 只有当有新文档时才添加到向量存储
            if (!documentsToLoad.isEmpty()) {
                // DashScope Embedding API 限制每次最多 25 个文本，需要分批处理
                int batchSize = 20; // 设置为 20，留点余量
                int totalDocs = documentsToLoad.size();
                System.out.println("准备加载 " + totalDocs + " 个文档（分批处理，每批 " + batchSize + " 个）");

                for (int i = 0; i < totalDocs; i += batchSize) {
                    int end = Math.min(i + batchSize, totalDocs);
                    List<Document> batch = documentsToLoad.subList(i, end);
                    try {
                        vectorStore.add(batch);
                        System.out.println("✓ 已加载第 " + (i / batchSize + 1) + " 批文档 (" + batch.size() + " 个)");
                    } catch (Exception e) {
                        System.err.println("✗ 第 " + (i / batchSize + 1) + " 批文档加载失败: " + e.getMessage());
                    }
                }
                System.out.println("✓ 文档加载完成，共 " + totalDocs + " 个");
            } else {
                System.out.println("✓ 所有文档均已存在，无需加载");
            }
        } else {
            System.out.println("⚠ 未找到文档，向量数据库为空");
        }

        return vectorStore;
    }
}
