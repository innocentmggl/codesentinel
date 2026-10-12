package com.sentinel.application.pullrequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.sentinel.application.port.CodeIndex;
import com.sentinel.application.port.DiffReviewer;
import com.sentinel.application.port.PullRequestHost;
import com.sentinel.common.error.ExternalServiceException;
import com.sentinel.common.error.InvalidRequestException;
import com.sentinel.domain.code.CodeChunk;
import com.sentinel.domain.review.DiffReviewRequest;
import com.sentinel.domain.review.Finding;
import com.sentinel.domain.review.Review;
import com.sentinel.domain.review.Severity;
import com.sentinel.domain.scm.*;
import org.junit.jupiter.api.Test;

class PullRequestReviewServiceTest {

    private static final String URL = "https://example.test/acme/shop/pull/7";
    private static final String PATCH = "@@ -1,2 +1,3 @@\n context\n+added\n+another";

    private final FakeHost host = new FakeHost();
    private final FakeReviewer reviewer = new FakeReviewer();

    @Test
    void postsOnlyFindingsThatPointAtCommentableLines() {
        host.files = List.of(file("src/A.java", PATCH));
        reviewer.review = new Review("ok", List.of(
                new Finding(Severity.MAJOR, 2, null, "Valid", "x", null),
                new Finding(Severity.MAJOR, 99, null, "Outside the diff", "x", null)));

        var report = service(properties(true)).review(URL, null, false);

        assertThat(report.posted()).isTrue();
        assertThat(report.comments()).hasSize(1);
        assertThat(report.comments().getFirst().line()).isEqualTo(2);
        assertThat(host.submitted).hasSize(1);
    }

    @Test
    void dropsFindingsBelowTheMinimumSeverity() {
        host.files = List.of(file("src/A.java", PATCH));
        reviewer.review = new Review("ok", List.of(new Finding(Severity.INFO, 2, null, "Nit", "x", null)));

        var report = service(properties(true)).review(URL, null, false);

        assertThat(report.comments()).isEmpty();
    }

    @Test
    void collapsesARangeThatLeavesTheDiffAndDropsItsSuggestionBlock() {
        host.files = List.of(file("src/A.java", PATCH));
        reviewer.review = new Review("ok", List.of(
                new Finding(Severity.MAJOR, 2, 0, "Range", "x", "fix();")));

        var comment = service(properties(true)).review(URL, null, true).comments().getFirst();

        assertThat(comment.startLine()).isEqualTo(2);
        assertThat(comment.body()).doesNotContain("```suggestion");
    }

    @Test
    void dryRunNeverPosts() {
        host.files = List.of(file("src/A.java", PATCH));
        reviewer.review = new Review("ok", List.of(new Finding(Severity.MAJOR, 2, null, "t", "x", null)));

        var report = service(properties(true)).review(URL, null, true);

        assertThat(report.posted()).isFalse();
        assertThat(report.comments()).hasSize(1);
        assertThat(host.submitted).isEmpty();
    }

    @Test
    void skipsCleanReviewsWhenConfigured() {
        host.files = List.of(file("src/A.java", PATCH));
        reviewer.review = new Review("ok", List.of());

        var report = service(properties(false)).review(URL, null, false);

        assertThat(report.posted()).isFalse();
        assertThat(host.submitted).isEmpty();
    }

    @Test
    void reportsSkippedFilesWithReasons() {
        host.files = List.of(
                file("src/A.java", PATCH),
                file("web/logo.png", PATCH),
                new ChangedFile("src/Old.java", ChangeType.REMOVED, PATCH),
                new ChangedFile("src/Binary.bin", ChangeType.MODIFIED, null));
        reviewer.review = new Review("ok", List.of());

        var report = service(properties(true)).review(URL, null, true);

        assertThat(report.filesReviewed()).isEqualTo(1);
        assertThat(report.filesSkipped()).hasSize(3)
                .anyMatch(entry -> entry.startsWith("web/logo.png"))
                .anyMatch(entry -> entry.startsWith("src/Old.java"))
                .anyMatch(entry -> entry.startsWith("src/Binary.bin"));
    }

    @Test
    void aFailingFileDoesNotFailTheReview() {
        host.files = List.of(file("src/A.java", PATCH), file("src/B.java", PATCH));
        reviewer.failFor = "src/A.java";
        reviewer.review = new Review("ok", List.of(new Finding(Severity.MAJOR, 2, null, "t", "x", null)));

        var report = service(properties(true)).review(URL, null, true);

        assertThat(report.filesReviewed()).isEqualTo(1);
        assertThat(report.comments()).hasSize(1);
        assertThat(report.filesSkipped()).singleElement().asString().startsWith("src/A.java: review failed");
    }

    @Test
    void usesTheRepositoryNameAsTheDefaultProject() {
        host.files = List.of(file("src/A.java", PATCH));
        reviewer.review = new Review("ok", List.of());

        service(properties(true)).review(URL, "  ", true);

        assertThat(reviewer.index.projects).containsOnly("shop");
    }

    @Test
    void rejectsUnsupportedUrls() {
        assertThatThrownBy(() -> service(properties(true)).review("https://other.test/x", null, false))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void keepsTheMostSevereCommentsWhenOverTheLimit() {
        host.files = List.of(file("src/A.java", PATCH));
        reviewer.review = new Review("ok", List.of(
                new Finding(Severity.MINOR, 1, null, "minor", "x", null),
                new Finding(Severity.CRITICAL, 2, null, "critical", "x", null)));
        var limited = new PullRequestReviewProperties(40, 20000, 5, 1, Severity.MINOR, true, Set.of(), Set.of(".png"));

        var report = service(limited).review(URL, null, true);

        assertThat(report.comments()).singleElement().extracting(ReviewComment::severity).isEqualTo(Severity.CRITICAL);
    }

    // ---- fixtures ----------------------------------------------------------------------------

    private PullRequestReviewService service(PullRequestReviewProperties properties) {
        var formatter = new CommentFormatter();
        var fileReviewer = new FileReviewer(reviewer.index, reviewer, formatter, properties);
        return new PullRequestReviewService(
                List.of(host), new ReviewableFiles(properties), fileReviewer, formatter, properties);
    }

    private static PullRequestReviewProperties properties(boolean postClean) {
        return new PullRequestReviewProperties(
                40, 20000, 5, 25, Severity.MINOR, postClean, Set.of(), Set.of(".png"));
    }

    private static ChangedFile file(String path, String patch) {
        return new ChangedFile(path, ChangeType.MODIFIED, patch);
    }

    private static final class FakeHost implements PullRequestHost {
        List<ChangedFile> files = List.of();
        final List<ReviewSubmission> submitted = new ArrayList<>();

        @Override
        public boolean supports(String url) {
            return url.startsWith("https://example.test/");
        }

        @Override
        public PullRequest fetch(String url) {
            return new PullRequest(new PullRequestRef("acme", "shop", 7), "Title", "Body", "sha", files);
        }

        @Override
        public String submit(PullRequest pullRequest, ReviewSubmission review) {
            submitted.add(review);
            return "https://example.test/review/1";
        }
    }

    private static final class FakeReviewer implements DiffReviewer {
        final RecordingIndex index = new RecordingIndex();
        Review review = new Review("", List.of());
        String failFor;

        @Override
        public Review review(DiffReviewRequest request) {
            if (request.filePath().equals(failFor)) {
                throw new ExternalServiceException("model unavailable");
            }
            return review;
        }
    }

    private static final class RecordingIndex implements CodeIndex {
        final List<String> projects = new ArrayList<>();

        @Override
        public void replaceProject(String project, List<CodeChunk> chunks) {
        }

        @Override
        public synchronized List<CodeChunk> findRelated(String query, String project) {
            projects.add(project);
            return List.of();
        }
    }
}
