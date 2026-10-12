package com.sentinel.adapter.outbound.vector;

import java.util.ArrayList;
import java.util.List;

import com.sentinel.application.port.CodeIndex;
import com.sentinel.common.Texts;
import com.sentinel.domain.code.CodeChunk;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class PgVectorCodeIndex implements CodeIndex {

    private final VectorStore vectorStore;
    private final IndexProperties properties;

    PgVectorCodeIndex(VectorStore vectorStore, IndexProperties properties) {
        this.vectorStore = vectorStore;
        this.properties = properties;
    }

    @Override
    public void replaceProject(String project, List<CodeChunk> chunks) {
        vectorStore.delete(projectFilter(project));
        List<Document> documents = chunks.stream()
                .map(chunk -> ChunkDocuments.toDocument(project, chunk))
                .toList();
        for (List<Document> batch : inBatches(documents, properties.batchSize())) {
            vectorStore.add(batch);
        }
    }

    @Override
    public List<CodeChunk> findRelated(String query, String project) {
        SearchRequest.Builder request = SearchRequest.builder()
                .query(Texts.truncate(query, properties.maxQueryChars()))
                .topK(properties.topK())
                .similarityThreshold(properties.similarityThreshold());
        if (StringUtils.hasText(project)) {
            request.filterExpression(projectFilter(project));
        }

        List<Document> matches = vectorStore.similaritySearch(request.build());
        return matches.stream().map(ChunkDocuments::toChunk).toList();
    }

    private static Filter.Expression projectFilter(String project) {
        return new FilterExpressionBuilder().eq(ChunkDocuments.PROJECT, project).build();
    }

    private static <T> List<List<T>> inBatches(List<T> items, int size) {
        var batches = new ArrayList<List<T>>();
        for (int from = 0; from < items.size(); from += size) {
            batches.add(items.subList(from, Math.min(from + size, items.size())));
        }
        return batches;
    }
}
