package com.sentinel.adapter.outbound.github;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.sentinel.application.port.CheckedOutRepository;
import com.sentinel.application.port.RepositoryFetcher;
import com.sentinel.common.Texts;
import com.sentinel.common.error.ExternalServiceException;
import com.sentinel.common.error.InvalidRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class GitHubRepositoryFetcher implements RepositoryFetcher {

    private static final Logger log = LoggerFactory.getLogger(GitHubRepositoryFetcher.class);
    private static final Pattern SAFE_REF = Pattern.compile("\\w[\\w./-]*");
    private static final long TIMEOUT_MINUTES = 5;

    private final GitHubProperties properties;

    GitHubRepositoryFetcher(GitHubProperties properties) {
        this.properties = properties;
    }

    @Override
    public CheckedOutRepository checkout(String repository, String branch) {
        GitHubUrls.Repository target = GitHubUrls.repository(repository);
        if (branch != null && !branch.isBlank() && !SAFE_REF.matcher(branch).matches()) {
            throw new InvalidRequestException("Invalid branch name: " + branch);
        }

        Path workDir = createWorkDir();
        Path repoDir = workDir.resolve("repo");
        try {
            runGit(workDir, cloneCommand(target, branch, repoDir));
            return new TemporaryCheckout(workDir, repoDir, target.name());
        } catch (RuntimeException e) {
            deleteRecursively(workDir);
            throw e;
        }
    }

    private List<String> cloneCommand(GitHubUrls.Repository target, String branch, Path repoDir) {
        var command = new ArrayList<>(List.of("git", "clone", "--depth", "1", "--single-branch"));
        if (branch != null && !branch.isBlank()) {
            command.addAll(List.of("--branch", branch));
        }
        command.add("--");
        command.add(properties.cloneUrl() + "/" + target.path() + ".git");
        command.add(repoDir.toString());
        return command;
    }

    private void runGit(Path workDir, List<String> command) {
        Path logFile = workDir.resolve("git.log");
        ProcessBuilder builder = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(logFile.toFile());
        builder.environment().put("GIT_TERMINAL_PROMPT", "0");
        if (properties.hasToken()) {
            String credentials = Base64.getEncoder()
                    .encodeToString(("x-access-token:" + properties.token()).getBytes(StandardCharsets.UTF_8));
            builder.environment().put("GIT_CONFIG_COUNT", "1");
            builder.environment().put("GIT_CONFIG_KEY_0", "http.extraheader");
            builder.environment().put("GIT_CONFIG_VALUE_0", "Authorization: Basic " + credentials);
        }

        try {
            Process process = builder.start();
            if (!process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                process.destroyForcibly();
                throw new ExternalServiceException("git clone timed out after " + TIMEOUT_MINUTES + " minutes");
            }
            if (process.exitValue() != 0) {
                throw new ExternalServiceException("git clone failed: " + Texts.truncate(readLog(logFile), 500));
            }
        } catch (IOException e) {
            throw new ExternalServiceException("Could not run git (is it installed?): " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalServiceException("git clone was interrupted", e);
        }
    }

    private static String readLog(Path logFile) {
        try {
            return Files.readString(logFile).strip();
        } catch (IOException e) {
            return "(no output)";
        }
    }

    private static Path createWorkDir() {
        try {
            return Files.createTempDirectory("codesentinel-");
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create a temporary directory", e);
        }
    }

    private static void deleteRecursively(Path directory) {
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        } catch (IOException e) {
            log.warn("Could not fully delete {}: {}", directory, e.getMessage());
        }
    }

    private record TemporaryCheckout(Path workDir, Path root, String name) implements CheckedOutRepository {
        @Override
        public void close() {
            deleteRecursively(workDir);
        }
    }
}
