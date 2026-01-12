package com.ai.agent.rag;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 自定义基于 Token 的切词器
 */
@Component
public class TokenTextSplitter {
    public List<Document> splitDocuments(List<Document> documents) {
        org.springframework.ai.transformer.splitter.TokenTextSplitter splitter =
            new org.springframework.ai.transformer.splitter.TokenTextSplitter();
        return splitter.apply(documents);
    }

    public List<Document> splitCustomized(List<Document> documents) {
        org.springframework.ai.transformer.splitter.TokenTextSplitter splitter =
            new org.springframework.ai.transformer.splitter.TokenTextSplitter(200, 100, 10, 5000, true);
        return splitter.apply(documents);
    }
}
