package com.sentinel.adapter.outbound.github;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sentinel.common.error.InvalidRequestException;
import com.sentinel.domain.scm.PullRequestRef;

final class GitHubUrls {

    private static final String NAME = "[A-Za-z0-9_.-]+";
    private static final String LAZY_NAME = "[A-Za-z0-9_.-]+?";
    private static final Pattern PULL_REQUEST = Pattern.compile(
            "^https://github\\.com/(" + NAME + ")/(" + NAME + ")/pull/(\\d+)(?:[/?#].*)?$");
    private static final Pattern REPOSITORY = Pattern.compile(
            "^(?:https://github\\.com/)?(" + NAME + ")/(" + LAZY_NAME + ")(?:\\.git)?/?$");

    private GitHubUrls() {
    }

    record Repository(String owner, String name) {
        String path() {
            return owner + "/" + name;
        }
    }

    static Optional<PullRequestRef> pullRequest(String url) {
        if (url == null) {
            return Optional.empty();
        }
        Matcher matcher = PULL_REQUEST.matcher(url.strip());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(new PullRequestRef(matcher.group(1), matcher.group(2), Integer.parseInt(matcher.group(3))));
    }

    static Repository repository(String reference) {
        Matcher matcher = reference == null ? null : REPOSITORY.matcher(reference.strip());
        if (matcher == null || !matcher.matches()) {
            throw new InvalidRequestException("Expected 'owner/repo' or a github.com repository URL: " + reference);
        }
        return new Repository(matcher.group(1), matcher.group(2));
    }
}
