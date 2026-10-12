package com.sentinel.adapter.outbound.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sentinel.common.error.InvalidRequestException;
import com.sentinel.domain.scm.PullRequestRef;
import org.junit.jupiter.api.Test;

class GitHubUrlsTest {

    @Test
    void parsesAPullRequestUrl() {
        assertThat(GitHubUrls.pullRequest("https://github.com/acme/shop/pull/42"))
                .contains(new PullRequestRef("acme", "shop", 42));
    }

    @Test
    void ignoresTrailingPathQueryAndFragment() {
        assertThat(GitHubUrls.pullRequest("https://github.com/acme/shop/pull/42/files?w=1#diff"))
                .contains(new PullRequestRef("acme", "shop", 42));
    }

    @Test
    void rejectsOtherHostsAndShapes() {
        assertThat(GitHubUrls.pullRequest("https://evil.example/acme/shop/pull/1")).isEmpty();
        assertThat(GitHubUrls.pullRequest("https://github.com/acme/shop/issues/1")).isEmpty();
        assertThat(GitHubUrls.pullRequest(null)).isEmpty();
    }

    @Test
    void parsesRepositoryReferences() {
        var expected = new GitHubUrls.Repository("acme", "shop");

        assertThat(GitHubUrls.repository("acme/shop")).isEqualTo(expected);
        assertThat(GitHubUrls.repository("acme/shop.git")).isEqualTo(expected);
        assertThat(GitHubUrls.repository("https://github.com/acme/shop")).isEqualTo(expected);
        assertThat(GitHubUrls.repository("https://github.com/acme/shop.git")).isEqualTo(expected);
    }

    @Test
    void keepsDotsInsideRepositoryNames() {
        assertThat(GitHubUrls.repository("acme/my.repo").name()).isEqualTo("my.repo");
    }

    @Test
    void rejectsUnsafeRepositoryReferences() {
        assertThatThrownBy(() -> GitHubUrls.repository("acme/shop;rm -rf"))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> GitHubUrls.repository("--upload-pack=x/y"))
                .isInstanceOf(InvalidRequestException.class);
    }
}
