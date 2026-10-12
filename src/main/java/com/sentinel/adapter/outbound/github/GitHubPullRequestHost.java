package com.sentinel.adapter.outbound.github;

import java.util.List;

import com.sentinel.adapter.outbound.github.GitHubClient.CreateReview;
import com.sentinel.adapter.outbound.github.GitHubClient.FilePayload;
import com.sentinel.adapter.outbound.github.GitHubClient.InlineComment;
import com.sentinel.adapter.outbound.github.GitHubClient.PullRequestPayload;
import com.sentinel.application.port.PullRequestHost;
import com.sentinel.common.error.InvalidRequestException;
import com.sentinel.domain.scm.ChangeType;
import com.sentinel.domain.scm.ChangedFile;
import com.sentinel.domain.scm.PullRequest;
import com.sentinel.domain.scm.PullRequestRef;
import com.sentinel.domain.scm.ReviewComment;
import com.sentinel.domain.scm.ReviewSubmission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class GitHubPullRequestHost implements PullRequestHost {

    private static final Logger log = LoggerFactory.getLogger(GitHubPullRequestHost.class);

    private final GitHubClient client;
    private final GitHubProperties properties;

    GitHubPullRequestHost(GitHubClient client, GitHubProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public boolean supports(String url) {
        return GitHubUrls.pullRequest(url).isPresent();
    }

    @Override
    public PullRequest fetch(String url) {
        PullRequestRef ref = GitHubUrls.pullRequest(url)
                .orElseThrow(() -> new InvalidRequestException("Not a GitHub pull request URL: " + url));
        PullRequestPayload payload = client.pullRequest(ref);
        List<ChangedFile> files = client.files(ref).stream().map(GitHubPullRequestHost::toChangedFile).toList();
        return new PullRequest(ref, payload.title(), payload.body(), payload.head().sha(), files);
    }

    @Override
    public String submit(PullRequest pullRequest, ReviewSubmission review) {
        if (!properties.hasToken()) {
            throw new InvalidRequestException("Posting a review requires sentinel.github.token (GITHUB_TOKEN)");
        }
        PullRequestRef ref = pullRequest.ref();
        var inline = review.comments().stream().map(GitHubPullRequestHost::toInline).toList();
        try {
            return client.createReview(ref, newReview(pullRequest, review.body(), inline)).htmlUrl();
        } catch (GitHubApiException e) {
            if (e.status() != 422 || inline.isEmpty()) {
                throw e;
            }
            // GitHub rejects the whole review if a single comment anchors outside the diff.
            // Losing the inline placement is better than losing the review.
            log.warn("GitHub rejected inline comments ({}); posting them in the review body instead", e.getMessage());
            String body = review.body() + foldedComments(review.comments());
            return client.createReview(ref, newReview(pullRequest, body, List.of())).htmlUrl();
        }
    }

    private static CreateReview newReview(PullRequest pullRequest, String body, List<InlineComment> comments) {
        return new CreateReview(pullRequest.headSha(), body, "COMMENT", comments);
    }

    private static InlineComment toInline(ReviewComment comment) {
        boolean range = comment.startLine() < comment.line();
        return new InlineComment(comment.path(), comment.body(), comment.line(), "RIGHT",
                range ? comment.startLine() : null,
                range ? "RIGHT" : null);
    }

    private static String foldedComments(List<ReviewComment> comments) {
        var text = new StringBuilder("\n\n---\n\n#### Comments that could not be placed inline\n");
        for (ReviewComment comment : comments) {
            text.append("\n**`").append(comment.path()).append(':').append(comment.line()).append("`**\n\n")
                    .append(comment.body()).append('\n');
        }
        return text.toString();
    }

    private static ChangedFile toChangedFile(FilePayload file) {
        return new ChangedFile(file.filename(), changeType(file.status()), file.patch());
    }

    private static ChangeType changeType(String status) {
        return switch (status == null ? "" : status) {
            case "added" -> ChangeType.ADDED;
            case "modified", "changed" -> ChangeType.MODIFIED;
            case "removed" -> ChangeType.REMOVED;
            case "renamed" -> ChangeType.RENAMED;
            default -> ChangeType.OTHER;
        };
    }
}
