package com.sentinel.codesentinel.vectorstore;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RuleIngestionService {

    private final VectorStore vectorStore;
    private final TokenTextSplitter splitter;

    public RuleIngestionService(VectorStore vectorStore, TokenTextSplitter splitter) {
        this.vectorStore = vectorStore;
        this.splitter = splitter;
    }

    /** Splits a rules document into chunks, embeds them and stores them in pgvector. */
    public int ingest(String fileName, String content) {
        Document document = new Document(content, Map.<String, Object>of("source", fileName));
        List<Document> chunks = splitter.apply(List.of(document));
        vectorStore.add(chunks);
        return chunks.size();
    }
}