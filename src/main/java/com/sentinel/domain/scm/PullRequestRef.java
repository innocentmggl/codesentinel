package com.sentinel.domain.scm;

public record PullRequestRef(String owner, String repo, int number) {
}
