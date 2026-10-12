package com.sentinel.application.pullrequest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.sentinel.domain.review.Finding;
import com.sentinel.domain.review.Severity;
import com.sentinel.domain.scm.ReviewComment;
import org.junit.jupiter.api.Test;

class CommentFormatterTest {

    private final CommentFormatter formatter = new CommentFormatter();

    @Test
    void rendersAnApplicableSuggestionBlock() {
        Finding finding = new Finding(Severity.MAJOR, 5, null, "Null deref", "May be null.", "return Optional.empty();");

        String body = formatter.commentBody(finding, true);

        assertThat(body).startsWith("**MAJOR: Null deref**")
                .contains("```suggestion\nreturn Optional.empty();\n```");
    }

    @Test
    void rendersANonApplicableSuggestionAsPlainCode() {
        Finding finding = new Finding(Severity.MINOR, 5, 3, "Long", "Too long.", "int x = 1;");

        String body = formatter.commentBody(finding, false);

        assertThat(body).doesNotContain("```suggestion").contains("Suggested change:").contains("int x = 1;");
    }

    @Test
    void omitsTheCodeBlockWithoutASuggestion() {
        Finding finding = new Finding(Severity.INFO, 1, null, "Note", "Just a note.", null);

        assertThat(formatter.commentBody(finding, true)).doesNotContain("```");
    }

    @Test
    void lengthensTheFenceWhenTheCodeContainsBackticks() {
        Finding finding = new Finding(Severity.MINOR, 1, null, "Doc", "x", "String s = \"\"\"```\"\"\";");

        String body = formatter.commentBody(finding, true);

        assertThat(body).contains("````suggestion").endsWith("````");
    }

    @Test
    void summarisesCountsFromMostToLeastSevere() {
        var comments = List.of(
                new ReviewComment(Severity.MINOR, "A.java", 1, 1, "a"),
                new ReviewComment(Severity.CRITICAL, "B.java", 2, 2, "b"),
                new ReviewComment(Severity.MINOR, "C.java", 3, 3, "c"));

        String summary = formatter.summary(comments, 3, List.of());

        assertThat(summary).contains("left 3 comment(s): 1 CRITICAL, 2 MINOR");
    }

    @Test
    void reportsACleanReviewAndSkippedFiles() {
        String summary = formatter.summary(List.of(), 2, List.of("a.png: ignored by configuration"));

        assertThat(summary).contains("No issues found in 2 reviewed file(s)")
                .contains("Skipped files (1)")
                .contains("- a.png: ignored by configuration");
    }
}
