# CodeSentinel

AI code review for Java teams. Give it a GitHub pull request URL and it posts a review with inline comments and
one-click **Apply suggestion** fixes. It also reviews and refactors pasted code. The language model is pluggable:
a local Ollama model or a cloud provider (Gemini, OpenAI), selected by a Spring profile.

- Java 25, Spring Boot 4.1, Spring AI 2.0, virtual threads
- Hexagonal architecture, with the dependency rules enforced by ArchUnit
- PostgreSQL + pgvector for optional project context (RAG)

> **Build status.** The domain and application layers were compiled and exercised with a scenario harness
> (36 checks) in an environment without Maven. The Spring-dependent adapters and the JUnit/ArchUnit tests have
> **not** been built or run by the author. Run `./mvnw verify` first and treat compile errors against Spring Boot 4 /
> Spring AI 2.0 APIs as the likeliest friction (see [Known limitations](#known-limitations)).

---

## Contents

1. [How it works](#how-it-works)
2. [Architecture](#architecture)
3. [Quick start](#quick-start)
4. [Profiles and providers](#profiles-and-providers)
5. [GitHub setup](#github-setup)
6. [API](#api)
7. [Indexing](#indexing)
8. [Configuration reference](#configuration-reference)
9. [Security](#security)
10. [Testing](#testing)
11. [Extending](#extending)
12. [Design decisions](#design-decisions)
13. [Troubleshooting](#troubleshooting)
14. [Known limitations](#known-limitations)

---

## How it works

### Pull request review

```
POST /api/v1/pull-requests/reviews { "url": ".../pull/42" }
   │
   ├─ PullRequestHost.fetch        title, description, head SHA, per-file patches (paged)
   ├─ ReviewableFiles              drop deleted, binary, huge, ignored files; cap at max-files
   ├─ per file, in parallel (one virtual thread each)
   │    ├─ DiffParser              prefix each line with its real new-file line number
   │    ├─ CodeIndex.findRelated   similar code from the indexed project (empty if not indexed)
   │    ├─ DiffReviewer            model returns structured findings
   │    └─ validate                keep findings on lines GitHub accepts, above min severity
   ├─ rank                         most severe first, capped at max-comments
   └─ PullRequestHost.submit       ONE review: summary + inline comments + suggestion blocks
```

Why the details matter:

- **Line accuracy.** Models miscount lines in raw diffs. The diff is rewritten as `[42] + added`, `[43]   context`,
  `[--] - removed`, so the model cites numbers instead of computing them. Every finding is then checked against the
  set of lines the diff actually exposes; anything else is discarded rather than risking a rejected review.
- **Apply suggestion.** A finding with a replacement becomes a ```` ```suggestion ```` block only when the commented
  range is exactly the range the replacement covers. If a range had to be collapsed to a single line, the code is
  shown as plain "Suggested change" instead, so the button never applies a wrong fix. Fences grow when the code
  contains backticks.
- **Failure isolation.** A file whose review fails is reported as skipped; the rest of the pull request still posts.
- **GitHub 422.** GitHub rejects an entire review if one comment anchors outside the diff. On a 422 the comments are
  folded into the review body and posted again, so the review is never lost.
- **Dry run.** `"dryRun": true` computes everything and returns it without posting.

### Snippet review and refactoring

`POST /api/v1/reviews` and `/api/v1/refactorings` take code, retrieve related code from the index, and ask the model
for structured output. No GitHub involved.

---

## Architecture

Ports and adapters. The domain knows nothing about frameworks; the application layer orchestrates use cases through
interfaces (ports); adapters implement those interfaces for a specific technology.

```
                 ┌────────────────────────────────────────────────┐
  HTTP  ───────▶ │ adapter.inbound.web                            │
                 └──────────────────────┬─────────────────────────┘
                                        ▼
                 ┌────────────────────────────────────────────────┐
                 │ application   use cases + ports (interfaces)   │
                 └───────────┬──────────────────────▲─────────────┘
                             ▼                      │ implements
                 ┌───────────────────┐   ┌──────────┴─────────────┐
                 │ domain            │   │ adapter.outbound       │
                 │ pure Java records │   │ llm vector parser      │
                 └───────────────────┘   │ github filesystem      │
                                         └────────────────────────┘
```

### Package layout

```
com.sentinel
├─ domain/                    no framework imports
│  ├─ code/                   CodeChunk, SourceFile, ChunkKind
│  ├─ review/                 Finding, Review, Refactoring, Severity, DiffReviewRequest
│  ├─ diff/                   DiffParser, AnnotatedPatch
│  └─ scm/                    PullRequest, ChangedFile, ReviewComment, ReviewSubmission, ...
├─ application/
│  ├─ port/                   SnippetReviewer, CodeRefactorer, DiffReviewer, CodeIndex, CodeChunker,
│  │                          SourceFileLocator, PullRequestHost, RepositoryFetcher, CheckedOutRepository
│  ├─ indexing/               ProjectIndexer, LocalProjectIndexingService, RepositoryIndexingService
│  ├─ review/                 SnippetReviewService
│  ├─ refactor/               RefactoringService
│  └─ pullrequest/            PullRequestReviewService (+ package-private FileReviewer, CommentFormatter, ...)
├─ adapter/
│  ├─ inbound/web/            controllers, ApiExceptionHandler (RFC 9457 problem details)
│  └─ outbound/
│     ├─ llm/                 StructuredChatClient, LlmSnippetReviewer, LlmCodeRefactorer, LlmDiffReviewer
│     ├─ vector/              PgVectorCodeIndex
│     ├─ parser/              JavaParserChunker
│     ├─ filesystem/          FileSystemSourceLocator
│     └─ github/              GitHubClient, GitHubPullRequestHost, GitHubRepositoryFetcher
├─ common/                    Texts, error/ (InvalidRequestException → 400, ExternalServiceException → 502)
└─ config/                    ResilienceConfig
```

### Dependency rules (enforced in `ArchitectureTest`)

| Rule | Why |
|---|---|
| `domain` depends on nothing in Spring, JavaParser, jakarta, application or adapters | Business types stay portable and trivially testable |
| `application` may not import adapters, Spring AI, Spring Web, JDBC or JavaParser | Use cases cannot know how the model, database or parser work |
| `application.port` contains only interfaces | A port is a contract, never an implementation |
| Outbound adapter packages do not depend on each other | Swapping GitHub for GitLab must not touch the LLM code |
| Inbound adapters do not use outbound adapters | Controllers go through use cases |
| No cycles between layers or between use-case packages | Keeps the structure a tree |

### Principles applied

- **Dependency inversion**: services depend on `CodeIndex`, `DiffReviewer`, `PullRequestHost`, not on pgvector, Spring AI or GitHub.
- **Interface segregation**: three small model ports (`SnippetReviewer`, `CodeRefactorer`, `DiffReviewer`) instead of one "LLM" interface.
- **Open/closed**: `PullRequestReviewService` receives `List<PullRequestHost>`; adding GitLab is a new bean, no edits.
- **Single responsibility**: `FileReviewer` reviews one file, `ReviewableFiles` filters, `CommentFormatter` renders, the service orchestrates.
- **Encapsulation**: collaborators inside a use case are package-private; only the service and its report/properties types are public.
- **Immutability**: domain types are records with defensive copies; configuration is bound to records.

---

## Quick start

### Prerequisites

- JDK 25, Docker, `git` on the PATH (used to clone repositories for indexing)
- For the local profile: [Ollama](https://ollama.com)

### Run locally with Ollama

```bash
ollama pull qwen2.5-coder:7b
ollama pull nomic-embed-text

./mvnw spring-boot:run
```

Spring Boot starts PostgreSQL + pgvector from `compose.yaml` automatically (`spring-boot-docker-compose`), and the
`local` profile is the default. Running from IntelliJ needs no arguments.

### Run with a cloud model

```bash
export GEMINI_API_KEY=...
SPRING_PROFILES_ACTIVE=gemini ./mvnw spring-boot:run
```

In IntelliJ set `SPRING_PROFILES_ACTIVE=gemini` and `GEMINI_API_KEY` in the run configuration environment.

### Try it

```bash
export GITHUB_TOKEN=github_pat_...

# Dry run: see what it would say, post nothing
curl -s localhost:8080/api/v1/pull-requests/reviews \
  -H 'Content-Type: application/json' \
  -d '{"url":"https://github.com/acme/shop/pull/42","dryRun":true}' | jq

# For real
curl -s localhost:8080/api/v1/pull-requests/reviews \
  -H 'Content-Type: application/json' \
  -d '{"url":"https://github.com/acme/shop/pull/42"}' | jq
```

---

## Profiles and providers

One OpenAI-compatible client (`spring-ai-starter-model-openai`) serves every provider; the profile only changes the
base URL, key and model names.

| Profile | Chat model | Embedding model | Vector table |
|---|---|---|---|
| `local` (default) | `qwen2.5-coder:7b` via Ollama | `nomic-embed-text` | `code_chunks_local` |
| `gemini` | `gemini-2.5-flash` | `gemini-embedding-001` (768 dims) | `code_chunks_gemini` |
| `openai` | `gpt-4.1-mini` | `text-embedding-3-small` (768 dims) | `code_chunks_openai` |

- Override models with `LLM_MODEL` and `EMBEDDING_MODEL`.
- **Each profile has its own vector table.** Embeddings from different models are not comparable, so switching
  providers means re-indexing (the other profile's table is left untouched).
- The embedding dimension is fixed at 768 everywhere. Changing it requires a new table name.
- Reviews work without an index; it only adds context.

### Base URL and `/v1`

The Spring AI 2.0 documentation puts `/v1` in the base URL for Ollama and OpenAI, and the profiles follow that.
Gemini uses `https://generativelanguage.googleapis.com/v1beta/openai`. If you get 404s from Ollama, remove or add
the `/v1` suffix in `OLLAMA_BASE_URL`; behaviour here has differed between reports, so treat it as the first thing
to toggle.

### Alternative: native Gemini

If the OpenAI-compatible Gemini endpoint misbehaves, use Spring AI's native starters instead
(`spring-ai-starter-model-google-genai`, `spring-ai-starter-model-google-genai-embedding`) and configure
`spring.ai.google.genai.api-key`. Only `StructuredChatClient`'s dependency (`ChatClient.Builder`) is involved, so no
application code changes.

---

## GitHub setup

Create a **fine-grained personal access token** limited to the repositories you want reviewed:

| Permission | Level | Needed for |
|---|---|---|
| Pull requests | Read and write | Reading diffs, posting reviews |
| Contents | Read | Cloning private repositories for indexing |
| Metadata | Read | Mandatory |

```bash
export GITHUB_TOKEN=github_pat_...
```

Reviews are posted as the token's owner. For a distinct identity, use a bot account (or a GitHub App later; see the
roadmap). For GitHub Enterprise set `sentinel.github.api-url` and `sentinel.github.clone-url`.

Public repositories can be read without a token, but posting always needs one.

---

## API

All errors are RFC 9457 problem details. Invalid input is **400**, a failing dependency (GitHub, model, git) is **502**.

### `POST /api/v1/pull-requests/reviews`

```json
{ "url": "https://github.com/acme/shop/pull/42", "project": "shop", "dryRun": false }
```

`project` (optional) names the index to take context from and defaults to the repository name.

```json
{
  "pullRequest": "https://github.com/acme/shop/pull/42",
  "posted": true,
  "reviewUrl": "https://github.com/acme/shop/pull/42#pullrequestreview-123",
  "filesReviewed": 6,
  "filesSkipped": ["package-lock.json: ignored by configuration"],
  "comments": [
    { "severity": "MAJOR", "path": "src/Orders.java", "startLine": 40, "line": 42, "body": "..." }
  ]
}
```

### `POST /api/v1/reviews`

```json
{ "code": "public int div(int a, int b) { return a / b; }", "project": "shop" }
```

Returns `{ "summary": "...", "findings": [{ "severity", "line", "startLine", "title", "explanation", "suggestion" }] }`.
`project` is optional; omitted, all indexed projects are searched.

### `POST /api/v1/refactorings`

```json
{ "code": "...", "goal": "extract the validation into its own method", "project": "shop" }
```

Returns `{ "refactoredCode": "...", "changes": ["..."] }`.

### `POST /api/v1/index`

Indexes a directory on the server. Must be inside `sentinel.workspace.root` (default: the user's home).

```json
{ "path": "/home/me/projects/shop", "project": "shop" }
```

### `POST /api/v1/repositories/index`

Shallow-clones a GitHub repository into a temporary directory, indexes it, deletes the clone.

```json
{ "repository": "acme/shop", "branch": "main", "project": "shop" }
```

Both index endpoints return `{ "project": "shop", "files": 120, "chunks": 845 }`.

---

## Indexing

Indexing is **separate and optional**. It is not triggered by reviews and not per pull request.

- Index a project **once**, then re-index occasionally (for example after significant merges).
- Re-indexing a project **replaces** its chunks; it never duplicates them.
- A review looks up code similar to the changed lines and shows it to the model as read-only context, which
  improves findings that depend on how the rest of the codebase works.
- Not indexed? The review still works; there is simply no extra context.

How it works: JavaParser splits each `.java` file into one chunk per method and constructor, each embedded and
stored in pgvector (HNSW index, cosine distance) with its file, owner, signature and line range. Chunk ids are
deterministic, so indexing is idempotent. Unparsable files yield no chunks instead of failing the run.

---

## Configuration reference

| Property | Default | Meaning |
|---|---|---|
| `sentinel.workspace.root` | user home | Local directories to index must be inside it |
| `sentinel.llm.max-concurrency` | `4` | Simultaneous model calls. Use 1 or 2 for a local model |
| `sentinel.index.top-k` | `5` | Related chunks per query |
| `sentinel.index.similarity-threshold` | `0.3` | Minimum similarity for a related chunk |
| `sentinel.index.batch-size` | `32` | Chunks written per batch |
| `sentinel.index.max-query-chars` | `4000` | Query text sent to the embedder |
| `sentinel.parser.max-chunk-chars` | `6000` | Chunk size cap |
| `sentinel.source.excluded-dirs` | build/VCS dirs | Skipped while indexing |
| `sentinel.github.token` | `${GITHUB_TOKEN}` | PAT; never logged |
| `sentinel.github.api-url` / `clone-url` | github.com | Override for Enterprise |
| `sentinel.pull-request.max-files` | `40` | Files reviewed per PR; the rest are reported skipped |
| `sentinel.pull-request.max-patch-chars` | `20000` | Larger diffs are skipped |
| `sentinel.pull-request.max-comments-per-file` | `5` | Findings kept per file |
| `sentinel.pull-request.max-comments` | `25` | Comments in one review, most severe first |
| `sentinel.pull-request.min-severity` | `MINOR` | `INFO`, `MINOR`, `MAJOR`, `CRITICAL` |
| `sentinel.pull-request.post-clean-reviews` | `true` | Post a summary even with no findings |
| `sentinel.pull-request.ignored-dirs` / `ignored-file-suffixes` | see class | Files not worth reviewing |

Model calls are retried twice with exponential backoff (`@Retryable`) and bounded by `@ConcurrencyLimit`
(Spring Framework 7 resilience annotations, enabled in `ResilienceConfig`).

Prompts live in `src/main/resources/prompts/`. System prompts must not contain braces (they are used as plain text);
user prompts use `{placeholders}`, bound as template parameters so braces in code are safe.

---

## Security

- **Token handling**: held in a record whose `toString` redacts it; sent as a bearer header to the GitHub API;
  given to `git` through `GIT_CONFIG_*` environment variables (`http.extraheader`), never on the command line, in the
  URL or in `.git/config`.
- **Command injection**: repository references are matched against a strict pattern, branches against
  `\w[\w./-]*`, and `--` separates options from arguments.
- **Path traversal**: local indexing is confined to `sentinel.workspace.root` after normalisation.
- **Prompt injection**: pull request content is untrusted and can try to instruct the model. The model has no tools
  and cannot act; its output is validated (line numbers must be in the diff, severity filtered, counts capped), so
  the worst outcome is a misleading comment from the token's identity. Review the bot's output like any other.
- **Data egress**: with `gemini` or `openai`, diffs and related code are sent to that provider. Use `local` for code
  that must not leave your network.
- **No authentication on this service**: the API is unauthenticated. Do not expose it publicly; put it behind your
  gateway or add Spring Security before deploying.

---

## Testing

```bash
./mvnw verify
```

| Test | Covers |
|---|---|
| `DiffParserTest` | line numbering, hunks, blank context lines, commentable ranges |
| `JavaParserChunkerTest` | method and constructor chunks, owners, lines, truncation, broken source |
| `GitHubUrlsTest` | URL parsing and rejection of unsafe references |
| `CommentFormatterTest` | suggestion blocks, fence lengthening, summaries |
| `PullRequestReviewServiceTest` | validation, severity filter, range collapse, dry run, skip reasons, failure isolation, ranking |
| `ProjectIndexerTest`, `SnippetReviewServiceTest` | orchestration through ports |
| `ArchitectureTest` | the dependency rules above |

Use cases are tested with hand-written fakes of the ports, so no mocking framework or Spring context is needed.
Adapters (`GitHubClient`, `PgVectorCodeIndex`, the LLM adapters) have no tests yet; see the roadmap.

---

## Extending

**Another model provider.** If it speaks the OpenAI protocol, add `application-<name>.yml` with a base URL, key,
models and its own `table-name`. Otherwise swap the Spring AI starter. No Java changes.

**GitLab / Bitbucket.** Add a package under `adapter.outbound`, implement `PullRequestHost` (and
`RepositoryFetcher` for indexing) as beans. `PullRequestReviewService` picks the host by `supports(url)`. Note
`RepositoryFetcher` is currently single-bean; with several hosts, introduce selection by reference as `PullRequestHost`
does.

**Another language.** Implement `CodeChunker` and `SourceFileLocator` for it. Diff review already works on any language
because it reads patches, not syntax trees.

**A different vector store or embedder.** Implement `CodeIndex`.

---

## Design decisions

- **Hexagonal over layered.** The same use cases are reachable from HTTP today and could be reached from a webhook or
  CLI tomorrow, and swapping GitHub or the model touches one package.
- **One OpenAI-compatible client.** Fewer dependencies and a profile switch, at the cost of relying on each provider's
  compatibility layer. The native-starter fallback is documented above.
- **Validate model output instead of trusting it.** The model proposes, the code disposes.
- **One review per PR, not one comment per finding.** Notifies once, and a single failure cannot leave half a review.
- **Virtual threads, not reactive.** Blocking code stays readable; the concurrency limit protects the provider.
- **Synchronous API.** A large PR takes as long as its slowest files. Fine for now; see the roadmap.
- **Method-level chunks.** Small, self-describing units embed well. Types, fields and non-Java files are not indexed.

---

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| `EmbeddingModel` bean not found at startup | `spring.ai.model.chat` / `embedding` resolved to something other than `openai`. `application.yml` sets both; find what overrides them (environment variables, IDE run configuration, `spring.config.import`). Passing `--spring.ai.model.chat=openai --spring.ai.model.embedding=openai` works around it |
| 404 from Ollama | Toggle `/v1` on `OLLAMA_BASE_URL` |
| `Posting a review requires sentinel.github.token` | Set `GITHUB_TOKEN` |
| GitHub 403 or 404 on a private repo | Token lacks access to that repository or the Pull requests permission |
| Review posted without inline comments | GitHub returned 422 and comments were folded into the body (the log says so) |
| Most files "skipped" | Check `filesSkipped`: GitHub omits patches for very large or binary files |
| Slow or timing-out local reviews | Lower `sentinel.llm.max-concurrency`, use a smaller PR or a faster model |
| `git clone failed` | `git` missing, repository name wrong, or no access |
| Dimension mismatch from pgvector | Table was created with another dimension; use a new `table-name` |

---

## Known limitations

- **Unbuilt adapters.** See the note at the top. `@ConcurrencyLimit(limitString = ...)` and Spring Boot 4 / Spring AI
  2.0 package names are the likeliest places to need adjustment.
- The Gemini path through the OpenAI-compatible endpoint has not been exercised.
- Only GitHub, only Java indexing, only whole-PR synchronous reviews.
- Findings are only as good as the model; local 7B models produce noticeably weaker reviews than cloud models.

## Roadmap

- Webhook entry point with signature verification, asynchronous processing and a job status endpoint
- GitHub App authentication instead of a personal token
- Adapter tests (WireMock for GitHub, Testcontainers for pgvector)
- GitLab host, incremental indexing on push
- Resolve or update earlier CodeSentinel comments on new pushes
