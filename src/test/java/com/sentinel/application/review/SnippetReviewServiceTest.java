package com.sentinel.application.review;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.sentinel.application.port.CodeIndex;
import com.sentinel.domain.code.ChunkKind;
import com.sentinel.domain.code.CodeChunk;
import com.sentinel.domain.review.Review;
import org.junit.jupiter.api.Test;

class SnippetReviewServiceTest {

    @Test
    void givesTheReviewerTheRelatedCodeFoundForTheSnippet() {
        var related = new CodeChunk("Util.java", "Util", "void help()", ChunkKind.METHOD, 1, 3, "code");
        CodeIndex index = new CodeIndex() {
            @Override
            public void replaceProject(String project, List<CodeChunk> chunks) {
            }

            @Override
            public List<CodeChunk> findRelated(String query, String project) {
                return "snippet".equals(query) && "shop".equals(project) ? List.of(related) : List.of();
            }
        };
        var seen = new java.util.concurrent.atomic.AtomicReference<List<CodeChunk>>();
        var service = new SnippetReviewService(index, (_, chunks) -> {
            seen.set(chunks);
            return new Review("done", List.of());
        });

        Review review = service.review("snippet", "shop");

        assertThat(review.summary()).isEqualTo("done");
        assertThat(seen.get()).containsExactly(related);
    }
}
