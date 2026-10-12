package com.sentinel.adapter.outbound.github;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sentinel.common.error.ExternalServiceException;
import com.sentinel.domain.scm.PullRequestRef;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
class GitHubClient {

    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGES = 30;

    private final RestClient http;

    GitHubClient(GitHubProperties properties) {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(properties.apiUrl())
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28");
        if (properties.hasToken()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.token());
        }
        this.http = builder.build();
    }

    PullRequestPayload pullRequest(PullRequestRef ref) {
        return call(() -> http.get()
                .uri("/repos/{owner}/{repo}/pulls/{number}", ref.owner(), ref.repo(), ref.number())
                .retrieve()
                .onStatus(HttpStatusCode::isError, GitHubClient::fail)
                .body(PullRequestPayload.class));
    }

    List<FilePayload> files(PullRequestRef ref) {
        var all = new ArrayList<FilePayload>();
        for (int page = 1; page <= MAX_PAGES; page++) {
            int current = page;
            List<FilePayload> batch = call(() -> http.get()
                    .uri("/repos/{owner}/{repo}/pulls/{number}/files?per_page={size}&page={page}",
                            ref.owner(), ref.repo(), ref.number(), PAGE_SIZE, current)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, GitHubClient::fail)
                    .body(new ParameterizedTypeReference<>() {
                    }));
            if (batch == null || batch.isEmpty()) {
                break;
            }
            all.addAll(batch);
            if (batch.size() < PAGE_SIZE) {
                break;
            }
        }
        return all;
    }

    ReviewPayload createReview(PullRequestRef ref, CreateReview review) {
        return call(() -> http.post()
                .uri("/repos/{owner}/{repo}/pulls/{number}/reviews", ref.owner(), ref.repo(), ref.number())
                .contentType(MediaType.APPLICATION_JSON)
                .body(review)
                .retrieve()
                .onStatus(HttpStatusCode::isError, GitHubClient::fail)
                .body(ReviewPayload.class));
    }

    private static <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientException e) {
            throw new ExternalServiceException("Could not reach GitHub: " + e.getMessage(), e);
        }
    }

    private static void fail(HttpRequest request,
                             ClientHttpResponse response) throws IOException {
        String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        throw new GitHubApiException(response.getStatusCode().value(), body);
    }

    // ---- wire format ------------------------------------------------------------------------

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PullRequestPayload(String title, String body, Head head) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        record Head(String sha) {
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FilePayload(String filename, String status, String patch) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ReviewPayload(@JsonProperty("html_url") String htmlUrl) {
    }

    record CreateReview(
            @JsonProperty("commit_id") String commitId,
            String body,
            String event,
            List<InlineComment> comments) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record InlineComment(
            String path,
            String body,
            int line,
            String side,
            @JsonProperty("start_line") Integer startLine,
            @JsonProperty("start_side") String startSide) {
    }
}
