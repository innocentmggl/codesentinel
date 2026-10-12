package com.sentinel.application.pullrequest;

import java.util.Comparator;
import java.util.List;

import com.sentinel.application.port.CodeIndex;
import com.sentinel.application.port.DiffReviewer;
import com.sentinel.domain.code.CodeChunk;
import com.sentinel.domain.diff.AnnotatedPatch;
import com.sentinel.domain.diff.DiffParser;
import com.sentinel.domain.review.DiffReviewRequest;
import com.sentinel.domain.review.Finding;
import com.sentinel.domain.review.Review;
import com.sentinel.domain.scm.ChangedFile;
import com.sentinel.domain.scm.PullRequest;
import com.sentinel.domain.scm.ReviewComment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class FileReviewer {

    private static final Logger log = LoggerFactory.getLogger(FileReviewer.class);
    private static final Comparator<Finding> MOST_SEVERE_FIRST = Comparator.comparing(Finding::severity).reversed();

    private final CodeIndex index;
    private final DiffReviewer reviewer;
    private final CommentFormatter formatter;
    private final PullRequestReviewProperties properties;

    FileReviewer(CodeIndex index,
                 DiffReviewer reviewer,
                 CommentFormatter formatter,
                 PullRequestReviewProperties properties) {
        this.index = index;
        this.reviewer = reviewer;
        this.formatter = formatter;
        this.properties = properties;
    }

    FileOutcome review(PullRequest pullRequest, ChangedFile file, String project) {
        try {
            AnnotatedPatch patch = DiffParser.parse(file.patch());
            if (patch.commentableLines().isEmpty()) {
                return new FileOutcome.Skipped(file.path(), "no reviewable lines in the diff");
            }

            String query = patch.addedCode().isBlank() ? file.patch() : patch.addedCode();
            List<CodeChunk> related = index.findRelated(query, project);

            Review review = reviewer.review(new DiffReviewRequest(
                    pullRequest.title(),
                    pullRequest.description(),
                    file.path(),
                    patch.numberedText(),
                    related,
                    properties.maxCommentsPerFile()));
            return new FileOutcome.Reviewed(commentsFrom(file.path(), review, patch));
        } catch (RuntimeException e) {
            log.warn("Review of {} failed: {}", file.path(), e.toString());
            return new FileOutcome.Skipped(file.path(), "review failed (" + e.getClass().getSimpleName() + ")");
        }
    }

    /** Keeps findings that are severe enough and point at a line the platform will accept a comment on. */
    private List<ReviewComment> commentsFrom(String path, Review review, AnnotatedPatch patch) {
        return review.findings().stream()
                .filter(this::isSevereEnough)
                .filter(finding -> finding.line() != null && patch.accepts(finding.line()))
                .sorted(MOST_SEVERE_FIRST)
                .limit(properties.maxCommentsPerFile())
                .map(finding -> toComment(path, finding, patch))
                .toList();
    }

    private boolean isSevereEnough(Finding finding) {
        return finding.severity() != null && finding.severity().compareTo(properties.minSeverity()) >= 0;
    }

    private ReviewComment toComment(String path, Finding finding, AnnotatedPatch patch) {
        int line = finding.line();
        boolean rangeFits = finding.spansLines() && patch.accepts(finding.startLine(), line);
        int startLine = rangeFits ? finding.startLine() : line;
        // A suggestion replaces exactly the commented lines; if the range had to be collapsed it no longer fits.
        boolean suggestionFits = !finding.spansLines() || rangeFits;
        return new ReviewComment(
                finding.severity(), path, startLine, line, formatter.commentBody(finding, suggestionFits));
    }
}
